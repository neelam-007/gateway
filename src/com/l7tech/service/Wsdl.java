package com.l7tech.service;

import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import javax.wsdl.Definition;
import javax.wsdl.Types;
import javax.wsdl.WSDLException;
import javax.wsdl.factory.WSDLFactory;
import javax.wsdl.xml.WSDLReader;
import javax.wsdl.xml.WSDLWriter;
import java.io.Reader;
import java.io.Writer;
import java.io.OutputStream;
import java.util.Collection;

/**
 * <code>Wsdl</code> is the internal object structure for
 * representing WSDL. The internal WSDL object structure is
 * generally created by parsing existing WSDL XML documents.
 * <p>
 * The class is a convenience wrapper arround the WSDL4J
 * (the reference implementation of
 * <a href="http://www.jcp.org/jsr/detail/110.jsp"> Java APIs
 * for WSDL - JWSDL</a>).
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 */
public class Wsdl {
    /**
     * The protected constructor accepting the <code>Definition</code>
     * instance. Used by instance factory methods.
     *
     * @param d      the wsdl4j definition instance
     */
    protected Wsdl(Definition d) {
        definition = d;
    }

    /**
     * Create the instance by reading a WSDL document from the
     * <code>Reader</code> character stream.
     *
     * @param documentBaseURI
     *               the document base URI of the WSDL definition
     *               described by the document.
     *               Can be <b>null</b>, in which case it will be
     *               ignored.
     * @param reader the character stream that contains the WSDL document,
     *               an XML document obeying the WSDL schema.
     *
     * <p>The example reads the WSDL definition from StringReader: <pre>
     * // Retrieve the WSDL definition from the string representing
     * // the wsdl and iterate over the services.
     *
     * Wsdl wsdl = Wsdl.newInstance(null, new StringReader(wsdlString));
     * Iterator services = wsdl.getServices().iterator();
     * ...
     * </pre>
     *
     * @exception WSDLException
     *                   throw on error parsing the WSDL definition
     */
    public static Wsdl newInstance(String documentBaseURI, Reader reader)
      throws WSDLException {
        InputSource source = new InputSource(reader);
        WSDLFactory fac = WSDLFactory.newInstance();
        WSDLReader wsdlReader = fac.newWSDLReader();
        return new Wsdl(wsdlReader.readWSDL(documentBaseURI, source));
    }

    /**
     * Create the instance by reading a WSDL document into from
     * the <code>Reader</code> character stream.
     *
     * @param documentBaseURI
     *               the document base URI of the WSDL definition
     *               described by the document.
     *               Can be <b>null</b>, in which case it will be
     *               ignored.
     * @param wsdlDocument
     *               the <i>dom</i> XML document obeying the WSDL
     *               schema.
     *
     * @return the <code>Wsdl</code> instance
     * @exception WSDLException
     *                   throw on error parsing the WSDL definition
     */
    public static Wsdl newInstance(String documentBaseURI, Document wsdlDocument)
      throws WSDLException {
        WSDLFactory fac = WSDLFactory.newInstance();
        WSDLReader reader = fac.newWSDLReader();
        return new Wsdl(reader.readWSDL(documentBaseURI, wsdlDocument));
    }

    /**
     * Create the instance by reading a WSDL document into from
     * the <code>Reader</code> character stream.
     *
     * @param documentBaseURI
     *               the document base URI of the WSDL definition
     *               described by the document.
     *               Can be <b>null</b>, in which case it will be
     *               ignored.
     * @param inputSource
     *               the <i>sax</i> XML document input source obeying
     *               the WSDL schema.
     *
     * @return the <code>Wsdl</code> instance
     * @exception WSDLException
     *                   throw on error parsing the WSDL definition
     */
    public static Wsdl newInstance(String documentBaseURI, InputSource inputSource)
      throws WSDLException {
        WSDLFactory fac = WSDLFactory.newInstance();
        WSDLReader reader = fac.newWSDLReader();
        return new Wsdl(reader.readWSDL(documentBaseURI, inputSource));
    }


    /**
     * @return the collection of WSDL <code>Binding</code>
     *         instances described in this definition.
     */
    public Collection getBindings() {
        return definition.getBindings().values();
    }

    /**
     * @return the collection of WSDL <code>Message</code>
     *         instances described in this definition.
     */
    public Collection getMessages() {
        return definition.getMessages().values();
    }

    /**
     * @return the collection of WSDL <code>PortType</code>
     *         instances described in this definition.
     */
    public Collection getPortTypes() {
        return definition.getPortTypes().values();
    }

    /**
     * @return the collection of WSDL <code>Service</code>
     *         instances described in this definition.
     */
    public Collection getServices() {
        return definition.getServices().values();
    }

    /**
     * @return the WSDL <code>Types</code> described in
     * this definition.
     */
    public Types getTypes() {
        return definition.getTypes();
    }

    /**
     * Access the internal <code>Definition</code> instance.
     *
     * @return the internal WSDL definition instance.
     */
    public Definition getDefinition() {
        return definition;
    }

    /**
     * Write the internal WSDL definition to the <code>Writer</code
     * passed
     * @param writer the writer where the
     *
     * <p>The example reads the WSDL definition from StringReader
     * and then writes it ti the StringWriter:
     * <p>
     * <pre>
     * // Retrieve the WSDL definition from the string representing
     * // the wsdl and iterate over the services.
     *
     * Wsdl wsdl = Wsdl.newInstance(null, new StringReader(wsdlString));
     * Iterator services = wsdl.getServices().iterator();
     * ...
     * StringWriter sw = new StringWriter();
     * wsdl.toWriter(sw);
     * System.out.println(sw.toString()):
     * ...
     * </pre>
     *
     * @exception WSDLException
     *                   throw on error parsing the WSDL definition
     */
    public void toWriter(Writer writer) throws WSDLException {
        WSDLFactory fac = WSDLFactory.newInstance();
        WSDLWriter wsdlWriter = fac.newWSDLWriter();
        wsdlWriter.writeWSDL(definition, writer);
    }

    /**
     * Write the internal WSDL definition to the <code>OutputStream</code>
     * @param os the output stream
     *
     * <p>The example reads the WSDL definition from StringReader
     * and then writes it ti the file:
     * <p>
     * <pre>
     * // Retrieve the WSDL definition from the string representing
     * // the wsdl and iterate over the services.
     *
     * Wsdl wsdl = Wsdl.newInstance(null, new StringReader(wsdlString));
     * Iterator services = wsdl.getServices().iterator();
     * ...
     * FileOutputStream fos = new FileOutputStream("./service.wsdl");
     * wsdl.toOutputStream(fos);
     * ...
     * </pre>
     *
     * @exception WSDLException
     *                   throw on error parsing the WSDL definition
     */
    public void toOutputStream(OutputStream os) throws WSDLException {
        WSDLFactory fac = WSDLFactory.newInstance();
        WSDLWriter wsdlWriter = fac.newWSDLWriter();
        wsdlWriter.writeWSDL(definition, os);
    }


    /**
     * Returens the instance string representatiton.
     * Delegated to the internal definition instance.
     *
     * @return the string representation of the object
     */
    public String toString() {
        return definition.toString();
    }


    private Definition definition;
}
