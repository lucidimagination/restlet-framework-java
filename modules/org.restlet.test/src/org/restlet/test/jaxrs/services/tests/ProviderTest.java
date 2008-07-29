/*
 * Copyright 2005-2008 Noelios Consulting.
 * 
 * The contents of this file are subject to the terms of the Common Development
 * and Distribution License (the "License"). You may not use this file except in
 * compliance with the License.
 * 
 * You can obtain a copy of the license at
 * http://www.opensource.org/licenses/cddl1.txt See the License for the specific
 * language governing permissions and limitations under the License.
 * 
 * When distributing Covered Code, include this CDDL HEADER in each file and
 * include the License file at http://www.opensource.org/licenses/cddl1.txt If
 * applicable, add the following below this CDDL HEADER, with the fields
 * enclosed by brackets "[]" replaced with your own identifying information:
 * Portions Copyright [yyyy] [name of copyright owner]
 */
package org.restlet.test.jaxrs.services.tests;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

import org.restlet.data.Form;
import org.restlet.data.MediaType;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.ext.jaxrs.internal.provider.JaxbElementProvider;
import org.restlet.resource.DomRepresentation;
import org.restlet.resource.Representation;
import org.restlet.resource.StringRepresentation;
import org.restlet.test.jaxrs.services.others.Person;
import org.restlet.test.jaxrs.services.resources.ProviderTestService;
import org.w3c.dom.DOMException;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * @author Stephan Koops
 * @see ProviderTestService
 * @see MessageBodyReader
 * @see MessageBodyWriter
 * @see JsonTest
 */
public class ProviderTest extends JaxRsTestCase {

    private static Form createForm() {
        final Form form = new Form();
        form.add("firstname", "Angela");
        form.add("lastname", "Merkel");
        return form;
    }

    /**
     * @param subPath
     * @throws IOException
     * @throws DOMException
     */
    private void getAndCheckJaxb(String subPath) throws Exception {
        final Response response = get(subPath);
        assertEquals(Status.SUCCESS_OK, response.getStatus());
        final DomRepresentation entity = response.getEntityAsDom();
        final Node xml = entity.getDocument().getFirstChild();
        System.out.println(subPath + ": " + entity.getText());
        assertEquals("person", xml.getNodeName());
        final NodeList nodeList = xml.getChildNodes();
        Node node = nodeList.item(0);
        assertEquals("firstname", node.getNodeName());
        assertEquals("Angela", node.getFirstChild().getNodeValue());
        node = nodeList.item(1);
        assertEquals("lastname", node.getNodeName());
        assertEquals("Merkel", node.getFirstChild().getNodeValue());
        assertEquals(2, nodeList.getLength());
    }

    /**
     * @param subPath
     * @throws IOException
     */
    private Response getAndExpectAlphabet(String subPath) throws IOException {
        final Response response = get(subPath);
        sysOutEntityIfError(response);
        assertEquals(Status.SUCCESS_OK, response.getStatus());
        final Representation entity = response.getEntity();
        assertEquals(ProviderTestService.ALPHABET, entity.getText());
        return response;
    }

    @Override
    protected Class<?> getRootResourceClass() {
        return ProviderTestService.class;
    }

    /**
     * @param subPath
     * @throws IOException
     */
    private void postAndCheckXml(String subPath) throws Exception {
        final Representation send = new DomRepresentation(
                new StringRepresentation(
                        "<person><firstname>Helmut</firstname><lastname>Kohl</lastname></person>",
                        MediaType.TEXT_XML));
        final Response response = post(subPath, send);
        assertEquals(Status.SUCCESS_OK, response.getStatus());
        final Representation respEntity = response.getEntity();
        assertEquals("Helmut Kohl", respEntity.getText());
    }

    /**
     * @param subPath
     * @param postEntity
     * @param postMediaType
     * @param responseMediaType
     *                if null, it will not be testet
     * @throws IOException
     */
    private void postAndExceptGiven(String subPath, String postEntity,
            MediaType postMediaType, MediaType responseMediaType)
            throws IOException {
        Representation entity = new StringRepresentation(postEntity,
                postMediaType);
        final Response response = post(subPath, entity);
        sysOutEntityIfError(response);
        assertEquals(Status.SUCCESS_OK, response.getStatus());
        entity = response.getEntity();
        assertEquals(postEntity, entity.getText());
        if (responseMediaType != null) {
            assertEqualMediaType(responseMediaType, entity);
        }
    }

    public void testBufferedReaderGet() throws Exception {
        getAndExpectAlphabet("BufferedReader");
    }

    public void testBufferedReaderPost() throws Exception {
        Representation entity = new StringRepresentation("big test",
                MediaType.APPLICATION_OCTET_STREAM);
        final Response response = post("BufferedReader", entity);
        sysOutEntityIfError(response);
        assertEquals(Status.SUCCESS_OK, response.getStatus());
        entity = response.getEntity();
        assertEquals("big test", entity.getText());
    }

    public void testByteArrayGet() throws Exception {
        getAndExpectAlphabet("byteArray");
    }

    public void testByteArrayPost() throws Exception {
        final Representation entity = new StringRepresentation("big test",
                MediaType.APPLICATION_OCTET_STREAM);
        final Response response = post("byteArray", entity);
        assertEquals(Status.SUCCESS_OK, response.getStatus());
        assertEquals("big test", response.getEntity().getText());
    }

    public void testCharSequenceGet() throws Exception {
        final Response response = get("CharSequence");
        sysOutEntityIfError(response);
        assertEquals(Status.SUCCESS_OK, response.getStatus());
        final Representation entity = response.getEntity();
        assertEquals(ProviderTestService.createCS(), entity.getText());
    }

    public void testCharSequencePost() throws Exception {
        postAndExceptGiven("CharSequence", "a character sequence",
                MediaType.TEXT_PLAIN, MediaType.TEXT_PLAIN);
    }

    public void testFileGet() throws Exception {
        getAndExpectAlphabet("file");
    }

    public void testFilePost() throws Exception {
        final Response response = post("file", new StringRepresentation(
                "big test", MediaType.APPLICATION_OCTET_STREAM));
        assertEquals(Status.SUCCESS_OK, response.getStatus());
        assertEquals("big test", response.getEntity().getText());
    }

    public void testFormGet() throws Exception {
        final Response response = get("form");
        assertEquals(Status.SUCCESS_OK, response.getStatus());
        final Representation entity = response.getEntity();
        assertEquals("firstname=Angela&lastname=Merkel", entity.getText());
    }

    public void testFormPost() throws Exception {
        final Response response = post("form", createForm()
                .getWebRepresentation());
        assertEquals(Status.SUCCESS_OK, response.getStatus());
        final String respEntity = response.getEntity().getText();
        assertEquals("[firstname: Angela, lastname: Merkel]", respEntity);
    }

    public void testInputStreamGet() throws Exception {
        getAndExpectAlphabet("InputStream");
    }

    public void testInputStreamPost() throws Exception {
        Representation entity = new StringRepresentation("big test",
                MediaType.APPLICATION_OCTET_STREAM);
        final Response response = post("InputStream", entity);
        assertEquals(Status.SUCCESS_OK, response.getStatus());
        entity = response.getEntity();
        assertEquals("big test", entity.getText());
    }

    public void testJaxbElementGet() throws Exception {
        getAndCheckJaxb("jaxbElement");
    }

    /** @see ProviderTestService#jaxbPost(javax.xml.bind.JAXBElement) */
    public void testJaxbElementPost() throws Exception {
        if(true) // TODO conversion to JAXBElement doesn't work
            return;
        postAndCheckXml("jaxbElement");
    }

    /**
     * @param subPath
     * @throws IOException
     * @see ProviderTestService#jaxbPostNamespace(javax.xml.bind.JAXBElement)
     */
    public void testJaxbElementPostRootElement() throws Exception {
        if(true) // TODO conversion to JAXBElement doesn't work
            return;
        final Representation send = new DomRepresentation(
                new StringRepresentation(
                        "<person><firstname>Helmut</firstname><lastname>Kohl</lastname></person>\n",
                        MediaType.TEXT_XML));
        final Response response = post("jaxbElement/rootElement", send);
        assertEquals(Status.SUCCESS_OK, response.getStatus());
        final Representation respEntity = response.getEntity();
        assertEquals("person", respEntity.getText());
    }

    public static void main(String[] args) throws Exception {
        Person person = new Person("vn", "nn");
        JaxbElementProvider jaxbElementProvider = new JaxbElementProvider();
        jaxbElementProvider.contextResolver = new ContextResolver<JAXBContext>() {
            public JAXBContext getContext(Class<?> type) {
                return null;
            }
        };
        JAXBElement<Person> jaxbElement = new JAXBElement<Person>(new QName(
                "xyz"), Person.class, person);
        jaxbElementProvider.writeTo(jaxbElement, Person.class, Person.class,
                null, null, null, System.out);

        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><person><firstname>vn</firstname><lastname>nn</lastname></person>";

        Type type = new ParameterizedType() {
            public Type[] getActualTypeArguments() {
                return new Type[] { Person.class };
            }
            public Type getOwnerType() {
                throw new UnsupportedOperationException("not implemented for this test");
            }
            public Type getRawType() {
                throw new UnsupportedOperationException("not implemented for this test");
            }
        };
        JAXBElement je = jaxbElementProvider.readFrom(
                (Class) JAXBElement.class, type, null, null, null,
                new ByteArrayInputStream(xml.getBytes()));
        System.out.println();
    }

    public void testJaxbGet() throws Exception {
        getAndCheckJaxb("jaxb");
    }

    public void testJaxbPost() throws Exception {
        if (usesTcp()) {
            return;
        }
        postAndCheckXml("jaxb");
    }

    public void testMultivaluedMapGet() throws Exception {
        final Response response = get("MultivaluedMap");
        assertEquals(Status.SUCCESS_OK, response.getStatus());
        final Representation entity = response.getEntity();
        assertEquals("lastname=Merkel&firstname=Angela", entity.getText());
    }

    public void testMultivaluedMapPost() throws Exception {
        final Response response = post("MultivaluedMap", createForm()
                .getWebRepresentation());
        assertEquals(Status.SUCCESS_OK, response.getStatus());
        final MediaType respMediaType = response.getEntity().getMediaType();
        assertEqualMediaType(MediaType.TEXT_PLAIN, respMediaType);
        final String respEntity = response.getEntity().getText();
        assertEquals("[lastname: Merkel, firstname: Angela]", respEntity);
    }

    public void testReaderGet() throws Exception {
        getAndExpectAlphabet("Reader");
    }

    public void testReaderPost() throws Exception {
        postAndExceptGiven("Reader", "big test",
                MediaType.APPLICATION_OCTET_STREAM, null);
    }

    public void testStringBuilderGet() throws Exception {
        getAndExpectAlphabet("StringBuilder");
    }

    public void testStringGet() throws Exception {
        getAndExpectAlphabet("String");
    }

    public void testStringPost() throws Exception {
        postAndExceptGiven("String", "another String", MediaType.TEXT_PLAIN,
                MediaType.TEXT_PLAIN);
    }

    public void testSubStringGet() throws Exception {
        final Response response = get("String/substring;start=5;end=9");
        sysOutEntityIfError(response);
        assertEquals(Status.SUCCESS_OK, response.getStatus());
        assertEquals("FGHI", response.getEntity().getText());
    }

    public void testXmlTransformGet() throws Exception {
        final Response response = get("source");
        sysOutEntityIfError(response);
        assertEquals(Status.SUCCESS_OK, response.getStatus());
        final String entity = response.getEntity().getText();
        assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?><abc/>", entity);
    }

    public void testXmlTransformPost() throws Exception {
        final Response response = post("source", new StringRepresentation(
                "abcdefg", MediaType.TEXT_XML));
        sysOutEntityIfError(response);
        assertEquals(Status.SUCCESS_OK, response.getStatus());
        assertEquals("abcdefg", response.getEntity().getText());
    }
}