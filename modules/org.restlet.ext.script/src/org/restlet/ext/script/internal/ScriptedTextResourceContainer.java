/**
 * Copyright 2005-2009 Noelios Technologies.
 * 
 * The contents of this file are subject to the terms of one of the following
 * open source licenses: LGPL 3.0 or LGPL 2.1 or CDDL 1.0 or EPL 1.0 (the
 * "Licenses"). You can select the license that you prefer but you may not use
 * this file except in compliance with one of these Licenses.
 * 
 * You can obtain a copy of the LGPL 3.0 license at
 * http://www.opensource.org/licenses/lgpl-3.0.html
 * 
 * You can obtain a copy of the LGPL 2.1 license at
 * http://www.opensource.org/licenses/lgpl-2.1.php
 * 
 * You can obtain a copy of the CDDL 1.0 license at
 * http://www.opensource.org/licenses/cddl1.php
 * 
 * You can obtain a copy of the EPL 1.0 license at
 * http://www.opensource.org/licenses/eclipse-1.0.php
 * 
 * See the Licenses for the specific language governing permissions and
 * limitations under the Licenses.
 * 
 * Alternatively, you can obtain a royalty free commercial license with less
 * limitations, transferable or non-transferable, directly at
 * http://www.noelios.com/products/restlet-engine
 * 
 * Restlet is a registered trademark of Noelios Technologies.
 */

package org.restlet.ext.script.internal;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.restlet.data.CharacterSet;
import org.restlet.data.Language;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.ext.script.ScriptedTextResource;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.representation.Variant;

import com.threecrickets.scripturian.EmbeddedScript;
import com.threecrickets.scripturian.ScriptSource;

/**
 * This is the type of the "container" variable exposed to the script. The name
 * is set according to {@link ScriptedTextResource#containerVariableName}.
 * 
 * @author Tal Liron
 */
public class ScriptedTextResourceContainer {
    private boolean startStreaming;

    private final Variant variant;

    private final Request request;

    private final Response response;

    private final Map<String, RepresentableString> cache;

    private MediaType mediaType;

    private CharacterSet characterSet;

    private Language language;

    protected boolean isStreaming;

    private Writer writer;

    private final Writer errorWriter = new StringWriter();

    private StringBuffer buffer;

    protected final ScriptedTextResourceScriptContextController scriptContextController = new ScriptedTextResourceScriptContextController(
            this);

    protected final Map<String, ScriptEngine> scriptEngines = new HashMap<String, ScriptEngine>();

    /**
     * Constructs a container with media type and character set according to the
     * variant, or {@link ScriptedTextResource#defaultCharacterSet} if none is
     * provided.
     * 
     * @param variant
     *            The variant
     * @param request
     *            The request
     * @param response
     *            The response
     * @param cache
     *            The cache
     */
    public ScriptedTextResourceContainer(Variant variant, Request request,
            Response response, Map<String, RepresentableString> cache) {
        this.variant = variant;
        this.request = request;
        this.response = response;
        this.cache = cache;
        this.mediaType = variant.getMediaType();
        this.characterSet = variant.getCharacterSet();
        if (this.characterSet == null) {
            this.characterSet = ScriptedTextResource.defaultCharacterSet;
        }
    }

    /**
     * The {@link CharacterSet} that will be used for the generated string.
     * Defaults to what the client requested (in container.variant), or to the
     * value of {@link ScriptedTextResource#defaultCharacterSet} if the client
     * did not specify it. If not in streaming mode, your script can change this
     * to something else.
     * 
     * @return The character set
     * @see #setCharacterSet(CharacterSet)
     */
    public CharacterSet getCharacterSet() {
        return this.characterSet;
    }

    /**
     * Same as {@link #getWriter()}, for standard error. (Nothing is currently
     * done with the contents of this, but this may change in future
     * implementations.)
     * 
     * @return The error writer
     */
    public Writer getErrorWriter() {
        return this.errorWriter;
    }

    /**
     * This boolean is true when the writer is in streaming mode.
     * 
     * @return True if in streaming mode, false if in caching mode
     */
    public boolean getIsStreaming() {
        return this.isStreaming;
    }

    /**
     * The {@link Language} that will be used for the generated string. Defaults
     * to null. If not in streaming mode, your script can change this to
     * something else.
     * 
     * @return The language or null if set
     * @see #setLanguage(Language)
     */
    public Language getLanguage() {
        return this.language;
    }

    /**
     * The {@link MediaType} that will be used for the generated string.
     * Defaults to what the client requested (in container.variant). If not in
     * streaming mode, your script can change this to something else.
     * 
     * @return The media type
     * @see #setMediaType(MediaType)
     */
    public MediaType getMediaType() {
        return this.mediaType;
    }

    /**
     * The {@link Request}. Useful for accessing URL attributes, form
     * parameters, etc.
     * 
     * @return The request
     */
    public Request getRequest() {
        return this.request;
    }

    /**
     * The {@link Response}. Useful for explicitly setting response
     * characteristics.
     * 
     * @return The response
     */
    public Response getResponse() {
        return this.response;
    }

    /**
     * This is the {@link ScriptEngineManager} used to create the script engine.
     * Scripts may use it to get information about what other engines are
     * available.
     * 
     * @return The script engine manager
     */
    public ScriptEngineManager getScriptEngineManager() {
        return ScriptedTextResource.scriptEngineManager;
    }

    /**
     * The {@link Variant} of this request. Useful for interrogating the
     * client's preferences.
     * 
     * @return The variant
     */
    public Variant getVariant() {
        return this.variant;
    }

    /**
     * Allows the script direct access to the {@link Writer}. This should rarely
     * be necessary, because by default the standard output for your scripting
     * engine would be directed to it, and the scripting platform's native
     * method for printing should be preferred. However, some scripting
     * platforms may not provide adequate access or may otherwise be broken.
     * Additionally, it may be useful to access the writer during streaming
     * mode. For example, you can call {@link Writer#flush()} to make sure all
     * output is sent to the client.
     * 
     * @return The writer
     */
    public Writer getWriter() {
        return this.writer;
    }

    /**
     * This powerful method allows scripts to execute other scripts in place,
     * and is useful for creating large, maintainable applications based on
     * scripts. Included scripts can act as a library or toolkit and can even be
     * shared among many applications. The included script does not have to be
     * in the same language or use the same engine as the calling script.
     * However, if they do use the same engine, then methods, functions,
     * modules, etc., could be shared. It is important to note that how this
     * works varies a lot per scripting platform. For example, in JRuby, every
     * script is run in its own scope, so that sharing would have to be done
     * explicitly in the global scope. See the included embedded Ruby script
     * example for a discussion of various ways to do this.
     * 
     * @param name
     *            The script name
     * @return A representation of the script's output
     * @throws IOException
     * @throws ScriptException
     */
    public Representation include(String name) throws IOException,
            ScriptException {
        return include(name, null);
    }

    /**
     * As {@link #include(String)}, except that the script is not embedded. As
     * such, you must explicitly specify the name of the scripting engine that
     * should evaluate it.
     * 
     * @param name
     *            The script name
     * @param scriptEngineName
     *            The script engine name (if null, behaves identically to
     *            {@link #include(String)}
     * @return A representation of the script's output
     * @throws IOException
     * @throws ScriptException
     */
    public Representation include(String name, String scriptEngineName)
            throws IOException, ScriptException {

        // Get script descriptor
        ScriptSource.ScriptDescriptor<EmbeddedScript> scriptDescriptor = ScriptedTextResource.scriptSource
                .getScriptDescriptor(name);

        EmbeddedScript script = scriptDescriptor.getScript();
        if (script == null) {
            // Create script from descriptor
            String text = scriptDescriptor.getText();
            if (scriptEngineName != null) {
                text = EmbeddedScript.delimiter1Start + scriptEngineName + " "
                        + text + EmbeddedScript.delimiter1End;
            }
            script = new EmbeddedScript(text,
                    ScriptedTextResource.scriptEngineManager,
                    ScriptedTextResource.defaultEngineName,
                    ScriptedTextResource.allowCompilation);
            scriptDescriptor.setScript(script);
        }

        // Special handling for trivial scripts
        String trivial = script.getTrivial();
        if (trivial != null) {
            if (getWriter() != null) {
                getWriter().write(trivial);
            }
            return new StringRepresentation(trivial, this.mediaType,
                    this.language, this.characterSet);
        }

        int startPosition = 0;

        // Make sure we have a valid writer for caching mode
        if (!this.isStreaming) {
            if (getWriter() == null) {
                StringWriter stringWriter = new StringWriter();
                this.buffer = stringWriter.getBuffer();
                setWriter(new BufferedWriter(stringWriter));
            } else {
                getWriter().flush();
                startPosition = this.buffer.length();
            }
        }

        try {
            // Do not allow caching in streaming mode
            if (script.run(getWriter(), this.errorWriter, this.scriptEngines,
                    this.scriptContextController, !this.isStreaming)) {

                // Did the script ask us to start streaming?
                if (this.startStreaming) {
                    this.startStreaming = false;

                    // Note that this will cause the script to run again!
                    return new ScriptedTextStreamingRepresentation(this, script);
                }

                if (this.isStreaming) {
                    // Nothing to return in streaming mode
                    return null;
                } else {
                    getWriter().flush();

                    // Get the buffer from when we ran the script
                    RepresentableString string = new RepresentableString(
                            this.buffer.substring(startPosition),
                            this.mediaType, this.language, this.characterSet);

                    // Cache it
                    this.cache.put(name, string);

                    // Return a representation of the entire buffer
                    if (startPosition == 0) {
                        return string.represent();
                    } else {
                        return new StringRepresentation(this.buffer.toString(),
                                this.mediaType, this.language,
                                this.characterSet);
                    }
                }
            } else {
                // Attempt to use cache
                RepresentableString string = this.cache.get(name);
                if (string != null) {
                    if (getWriter() != null) {
                        getWriter().write(string.getString());
                    }
                    return string.represent();
                } else {
                    return null;
                }
            }
        } catch (ScriptException x) {
            // Did the script ask us to start streaming?
            if (this.startStreaming) {
                this.startStreaming = false;

                // Note that this will cause the script to run again!
                return new ScriptedTextStreamingRepresentation(this, script);

                // Note that we will allow exceptions in scripts that ask us
                // to start streaming! In fact, throwing an exception is a
                // good way for the script to signal that it's done and is
                // ready to start streaming.
            } else {
                throw x;
            }
        }
    }

    /**
     * Throws an {@link IllegalStateException} if in streaming mode.
     * 
     * @param characterSet
     *            The character set
     * @see #getCharacterSet()
     */
    public void setCharacterSet(CharacterSet characterSet) {
        if (this.isStreaming) {
            throw new IllegalStateException(
                    "Cannot change character set while streaming");
        }
        this.characterSet = characterSet;
    }

    /**
     * Throws an {@link IllegalStateException} if in streaming mode.
     * 
     * @param language
     *            The language or null
     * @see #getLanguage()
     */
    public void setLanguage(Language language) {
        if (this.isStreaming) {
            throw new IllegalStateException(
                    "Cannot change language while streaming");
        }
        this.language = language;
    }

    /**
     * Throws an {@link IllegalStateException} if in streaming mode.
     * 
     * @param mediaType
     *            The media type
     * @see #getMediaType()
     */
    public void setMediaType(MediaType mediaType) {
        if (this.isStreaming) {
            throw new IllegalStateException(
                    "Cannot change media type while streaming");
        }
        this.mediaType = mediaType;
    }

    public void setWriter(Writer writer) {
        this.writer = writer;
    }

    /**
     * If you are in caching mode, calling this method will return true and
     * cause the script to run again, where this next run will be in streaming
     * mode. Whatever output the script created in the current run is discarded,
     * and all further exceptions are ignored. For this reason, it's probably
     * best to call container.stream() as early as possible in the script, and
     * then to quit the script as soon as possible if it returns true. For
     * example, your script can start by testing whether it will have a lot of
     * output, and if so, set output characteristics, call container.stream(),
     * and quit. If you are already in streaming mode, calling this method has
     * no effect and returns false. Note that a good way to quit the script is
     * to throw an exception, because it will end the script and otherwise be
     * ignored.
     * 
     * @return True is started streaming mode, false if already in streaming
     *         mode
     */
    public boolean stream() {
        if (this.isStreaming) {
            return false;
        }
        this.startStreaming = true;
        return true;
    }
}