package com.l7tech.common.xml;

import com.l7tech.common.util.SoapUtil;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import javax.wsdl.*;
import javax.wsdl.extensions.ExtensibilityElement;
import javax.wsdl.extensions.soap.SOAPAddress;
import javax.wsdl.extensions.soap.SOAPBinding;
import javax.wsdl.extensions.soap.SOAPBody;
import javax.wsdl.extensions.soap.SOAPOperation;
import javax.wsdl.factory.WSDLFactory;
import javax.wsdl.xml.WSDLReader;
import javax.wsdl.xml.WSDLWriter;
import javax.xml.soap.SOAPConstants;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.logging.Logger;


/**
 * <code>Wsdl</code> is the internal object structure for
 * representing WSDL. The internal WSDL object structure is
 * generally created by parsing existing WSDL XML documents.
 * <p/>
 * The class is a convenience wrapper arround the WSDL4J
 * (the reference implementation of
 * <a href="http://www.jcp.org/jsr/detail/110.jsp"> Java APIs
 * for WSDL - JWSDL</a>).
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 */
public class Wsdl {
    public static final String NS = "ns";

    /**
     * The protected constructor accepting the <code>Definition</code>
     * instance. Used by instance factory methods.
     *
     * @param d the wsdl4j definition instance
     */
    protected Wsdl(Definition d) {
        definition = d;
    }

    /**
     * Create the instance by reading a WSDL document from the
     * <code>Reader</code> character stream.
     *
     * @param documentBaseURI the document base URI of the WSDL definition
     *                        described by the document.
     *                        Can be <b>null</b>, in which case it will be
     *                        ignored.
     * @param reader          the character stream that contains the WSDL document,
     *                        an XML document obeying the WSDL schema.
     *                        <p/>
     *                        <p>The example reads the WSDL definition from StringReader: <pre>
     *                        // Retrieve the WSDL definition from the string representing
     *                        // the wsdl and iterate over the services.
     *                        <p/>
     *                        Wsdl wsdl = Wsdl.newInstance(null, new StringReader(wsdlString));
     *                        Iterator services = wsdl.getServices().iterator();
     *                        ...
     *                        </pre>
     * @throws javax.wsdl.WSDLException throw on error parsing the WSDL definition
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
     * @param documentBaseURI the document base URI of the WSDL definition
     *                        described by the document.
     *                        Can be <b>null</b>, in which case it will be
     *                        ignored.
     * @param wsdlDocument    the <i>dom</i> XML document obeying the WSDL
     *                        schema.
     * @return the <code>Wsdl</code> instance
     * @throws javax.wsdl.WSDLException throw on error parsing the WSDL definition
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
     * @param documentBaseURI the document base URI of the WSDL definition
     *                        described by the document.
     *                        Can be <b>null</b>, in which case it will be
     *                        ignored.
     * @param inputSource     the <i>sax</i> XML document input source obeying
     *                        the WSDL schema.
     * @return the <code>Wsdl</code> instance
     * @throws javax.wsdl.WSDLException throw on error parsing the WSDL definition
     */
    public static Wsdl newInstance(String documentBaseURI, InputSource inputSource)
      throws WSDLException {
        WSDLFactory fac = WSDLFactory.newInstance();
        WSDLReader reader = fac.newWSDLReader();
        return new Wsdl(reader.readWSDL(documentBaseURI, inputSource));
    }

    /**
     * Returns the service URI (url) from first WSDL <code>Port</code>
     * instance described in this definition. If there
     * are multiple <code>Port</code> instances it is arbitrary
     * as to which port object is returned.
     * <p/>
     *
     * @return the service url or <b>null</b> if service url
     *         is not found
     */
    public String getServiceURI() {
        for (Iterator it = getServices().iterator(); it.hasNext();) {
            Service svc = (Service)it.next();
            for (Iterator ports = svc.getPorts().values().iterator(); ports.hasNext();) {
                Port p = (Port)ports.next();
                List elements = p.getExtensibilityElements();
                for (Iterator ite = elements.iterator(); ite.hasNext();) {
                    Object o = ite.next();
                    if (o instanceof SOAPAddress) {
                        SOAPAddress sa = (SOAPAddress)o;
                        return sa.getLocationURI();
                    }
                }
            }
        }
        return null;
    }

    /**
     * Returns the service name element from WSDL <code>Port</code>
     * instances described in this definition.
     * If there are multiple <code>Service</code> instances it is
     * arbitrary as to which service name is returned.
     * Only the local name part is returned.
     * <p/>
     *
     * @return the service name or <b>null</b> if service name
     *         is not found
     */
    public String getServiceName() {
        for (Iterator it = getServices().iterator(); it.hasNext();) {
            Service svc = (Service)it.next();
            return svc.getQName().getLocalPart();
        }
        return null;
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
     *         this definition.
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
     *
     * @param writer the writer where the
     *               <p/>
     *               <p>The example reads the WSDL definition from StringReader
     *               and then writes it ti the StringWriter:
     *               <p/>
     *               <pre>
     *               // Retrieve the WSDL definition from the string representing
     *               // the wsdl and iterate over the services.
     *               <p/>
     *               Wsdl wsdl = Wsdl.newInstance(null, new StringReader(wsdlString));
     *               Iterator services = wsdl.getServices().iterator();
     *               ...
     *               StringWriter sw = new StringWriter();
     *               wsdl.toWriter(sw);
     *               System.out.println(sw.toString()):
     *               ...
     *               </pre>
     * @throws javax.wsdl.WSDLException throw on error parsing the WSDL definition
     */
    public void toWriter(Writer writer) throws WSDLException {
        WSDLFactory fac = WSDLFactory.newInstance();
        WSDLWriter wsdlWriter = fac.newWSDLWriter();
        wsdlWriter.writeWSDL(definition, writer);
    }

    /**
     * Write the internal WSDL definition to the <code>OutputStream</code>
     *
     * @param os the output stream
     *           <p/>
     *           <p>The example reads the WSDL definition from StringReader
     *           and then writes it ti the file:
     *           <p/>
     *           <pre>
     *           // Retrieve the WSDL definition from the string representing
     *           // the wsdl and iterate over the services.
     *           <p/>
     *           Wsdl wsdl = Wsdl.newInstance(null, new StringReader(wsdlString));
     *           Iterator services = wsdl.getServices().iterator();
     *           ...
     *           FileOutputStream fos = new FileOutputStream("./service.wsdl");
     *           wsdl.toOutputStream(fos);
     *           ...
     *           </pre>
     * @throws javax.wsdl.WSDLException throw on error parsing the WSDL definition
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

    /**
     * Determine the operation style ("document | "rpc") for an binding operation.
     * First the binding operation style is checked and if noit found, then the
     * enclosing bindings style is searched for.
     * Note that the binding operation must be owned by this wsdl instance.
     * @param bo the binding operation
     * @return the operation style "rpc" | "document" or <b>null</b> if it cannot
     *         be determined
     */
    public String getBindingStyle(BindingOperation bo) {
        List elements = bo.getExtensibilityElements();
        for (Iterator iterator = elements.iterator(); iterator.hasNext();) {
            Object o = (Object)iterator.next();
            if (o instanceof SOAPOperation) {
                SOAPOperation so = (SOAPOperation)o;
                return so.getStyle();
            }
        }
        Iterator bindings = getBindings().iterator();
        while (bindings.hasNext()) {
            Binding binding = (Binding)bindings.next();
            if (binding.getBindingOperations().contains(bo)) {
                elements = bo.getExtensibilityElements();
                for (Iterator iterator = elements.iterator(); iterator.hasNext();) {
                    Object o = (Object)iterator.next();
                    if (o instanceof SOAPBinding) {
                        SOAPBinding sb = (SOAPBinding)o;
                        return sb.getStyle();
                    }
                }
                break;
            }
        }
        return null;
    }

    /**
     * Finds a single Port that contains a SOAPAddress from whatever Services are in the WSDL.
     *
     * @return a Port that contains a SOAPAddress
     */
    public Port getSoapPort() {
        Iterator services = getServices().iterator();
        Service wsdlService = null;
        Port pork = null;
        Port soapPort = null;

        while (services.hasNext()) {
            int numPorts = 0;
            if (wsdlService != null) {
                logger.warning("WSDL " + getDefinition().getTargetNamespace() + " has more than one service, we will use only the first.");
                break;
            }

            wsdlService = (Service)services.next();
            Map ports = wsdlService.getPorts();
            if (ports == null) continue;

            Iterator portKeys = ports.keySet().iterator();
            String portKey;
            while (portKeys.hasNext()) {
                portKey = (String)portKeys.next();
                if (soapPort == null) {
                    pork = (Port)ports.get(portKey);

                    List elements = pork.getExtensibilityElements();
                    ExtensibilityElement eel;
                    // Find the first Port that contains a SOAPAddress eel
                    for (int i = 0; i < elements.size(); i++) {
                        eel = (ExtensibilityElement)elements.get(i);
                        if (eel instanceof SOAPAddress) {
                            soapPort = pork;
                            numPorts++;
                        }
                    }
                }
            }
            if (numPorts > 1) logger.warning("WSDL " + getDefinition().getTargetNamespace() + " has more than one port, used the first.");
        }
        return soapPort;
    }

    public Map getNamespaces() {
        List soapUris = SoapUtil.ENVELOPE_URIS;
        Map namespaceMap = definition.getNamespaces();
        String tnsUri = definition.getTargetNamespace();
        int ns = 1;
        final String TEMP = "l7tempprefix";

        // Get it into URL order
        SortedMap uris = new TreeMap();
        for (Iterator i = namespaceMap.keySet().iterator(); i.hasNext();) {
            String prefix = (String)i.next();
            String uri = (String)namespaceMap.get(prefix);
            if (prefix == null || prefix.length() == 0) prefix = TEMP + ns++;
            uris.put(uri, prefix);
        }

        // Now assign prefixes
        LinkedHashMap result = new LinkedHashMap();

        boolean soapenv = false;
        for (Iterator i = uris.keySet().iterator(); i.hasNext();) {
            String uri = (String)i.next();
            String prefix = (String)namespaceMap.get(uri);

            if (soapUris.contains(uri)) {
                result.put(prefix, uri);
                soapenv = true;
            } else if (uri.equals(tnsUri)) {
                result.put(prefix, uri);
            }
        }

        if (!soapenv) {
            if (result.get(SoapUtil.SOAP_ENV_PREFIX) == null)
                result.put(SoapUtil.SOAP_ENV_PREFIX, SOAPConstants.URI_NS_SOAP_ENVELOPE);
            else if (!result.containsValue(SOAPConstants.URI_NS_SOAP_ENVELOPE))
                result.put(TEMP + ns++, SOAPConstants.URI_NS_SOAP_ENVELOPE);
        }

        Collection operations = getBindingOperations();
        for (Iterator i = operations.iterator(); i.hasNext();) {
            BindingOperation operation = (BindingOperation)i.next();
            BindingInput input = operation.getBindingInput();
            Iterator eels = input.getExtensibilityElements().iterator();
            while (eels.hasNext()) {
                ExtensibilityElement ee = (ExtensibilityElement)eels.next();
                if (ee instanceof SOAPBody) {
                    SOAPBody body = (SOAPBody)ee;
                    String uri = body.getNamespaceURI();
                    if (uri != null && !result.containsValue(uri)) {
                        result.put(TEMP + ns++, uri);
                    }
                }
            }
        }

        ns = 1;
        LinkedHashMap result2 = new LinkedHashMap();
        for (Iterator i = result.keySet().iterator(); i.hasNext();) {
            String prefix = (String)i.next();
            String uri = (String)result.get(prefix);
            if (prefix == null || prefix.length() == 0 || prefix.startsWith(TEMP)) prefix = NS + ns++;
            result2.put(prefix, uri);
        }

        return result2;
    }

    public Collection getBindingOperations() {
        Iterator bindings = getBindings().iterator();
        Binding binding;
        List operations = new ArrayList();
        while (bindings.hasNext()) {
            binding = (Binding)bindings.next();
            operations.addAll(binding.getBindingOperations());
        }
        return operations;
    }

    public URL getUrlFromPort(Port wsdlPort) throws MalformedURLException {
        if (wsdlPort == null)
            throw new IllegalArgumentException("No WSDL port was provided");
        List elements = wsdlPort.getExtensibilityElements();
        URL url = null;
        ExtensibilityElement eel;
        int num = 0;
        for (int i = 0; i < elements.size(); i++) {
            eel = (ExtensibilityElement)elements.get(i);
            if (eel instanceof SOAPAddress) {
                SOAPAddress sadd = (SOAPAddress)eel;
                num++;
                url = new URL(sadd.getLocationURI());
            }
        }

        if (num > 1) logger.warning("WSDL " + getDefinition().getTargetNamespace() + " contained multiple <soap:address> elements");

        return url;
    }

    /**
     * In-place adjust the port URL of this WSDL.
     *
     * @param wsdlPort
     * @param url
     */
    public void setPortUrl(Port wsdlPort, URL url) {
        if (wsdlPort == null)
            throw new IllegalArgumentException("No WSDL port was provided");
        if (url == null)
            throw new IllegalArgumentException("No new WSDL Port URL was provided");
        List elements = wsdlPort.getExtensibilityElements();
        ExtensibilityElement eel;
        int num = 0;
        for (int i = 0; i < elements.size(); i++) {
            eel = (ExtensibilityElement)elements.get(i);
            if (eel instanceof SOAPAddress) {
                SOAPAddress sadd = (SOAPAddress)eel;
                num++;
                sadd.setLocationURI(url.toString());
            }
        }

        if (num > 1) logger.warning("WSDL " + getDefinition().getTargetNamespace() + " contained multiple <soap:address> elements");
    }

    private Definition definition;

    private transient Logger logger = Logger.getLogger(getClass().getName());


}
