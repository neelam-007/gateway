package com.l7tech.common.xml;

import com.l7tech.common.util.SoapUtil;
import com.l7tech.common.util.XmlUtil;
import com.l7tech.console.util.Registry;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.wsdl.*;
import javax.wsdl.extensions.ExtensibilityElement;
import javax.wsdl.extensions.http.HTTPBinding;
import javax.wsdl.extensions.mime.MIMEMultipartRelated;
import javax.wsdl.extensions.mime.MIMEPart;
import javax.wsdl.extensions.soap.SOAPAddress;
import javax.wsdl.extensions.soap.SOAPBinding;
import javax.wsdl.extensions.soap.SOAPBody;
import javax.wsdl.extensions.soap.SOAPOperation;
import javax.wsdl.factory.WSDLFactory;
import javax.wsdl.xml.WSDLReader;
import javax.wsdl.xml.WSDLWriter;
import javax.xml.soap.SOAPConstants;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.RemoteException;
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
     * bitmask for soap bindings filtering
     */
    public static final int SOAP_BINDINGS = 0x1;
    /**
     * bitmask for http bindings filtering
     */
    public static final int HTTP_BINDINGS = 0x2;
    /**
     * bitmask that accepts all bindings
     */
    public static final int ALL_BINDINGS = SOAP_BINDINGS | HTTP_BINDINGS;

    private int showBindings = ALL_BINDINGS;

    /**
     * The protected constructor accepting the <code>Definition</code>
     * instance. Used by instance factory methods.
     *
     * @param d the wsdl4j definition instance
     */
    public Wsdl(Definition d) {
        if (d == null) {
            throw new IllegalArgumentException();
        }
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
     *                                                                                                                                                                                         // Retrieve the WSDL definition from the string representing
     *                                                                                                                                                                                         // the wsdl and iterate over the services.
     *                                                                                                                                                                                         <p/>
     *                                                                                                                                                                                         Wsdl wsdl = Wsdl.newInstance(null, new StringReader(wsdlString));
     *                                                                                                                                                                                         Iterator services = wsdl.getServices().iterator();
     *                                                                                                                                                                                         ...
     *                                                                                                                                                                                         </pre>
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
        final Collection allBindings = new ArrayList();
        collectElements(new ElementCollector() {
            public void collect(Definition def) {
                if(def == null) return;

                Map bindings = def.getBindings();
                if(bindings != null && bindings.values() != null) {
                    allBindings.addAll(bindings.values());
                }
            }
        }, definition);

        if (showBindings == ALL_BINDINGS) {
            return allBindings;
        }
        Collection filtered = new ArrayList();
        for (Iterator iterator = allBindings.iterator(); iterator.hasNext();) {
            Binding binding = (Binding)iterator.next();
            ExtensibilityElement bp = getBindingProtocol(binding);
            if (bp instanceof HTTPBinding &&
              (showBindings & HTTP_BINDINGS) == HTTP_BINDINGS) {
                filtered.add(binding);
            } else if (bp instanceof SOAPBinding &&
              (showBindings & SOAP_BINDINGS) == SOAP_BINDINGS) {
                filtered.add(binding);
            }
        }
        return filtered;
    }

    /**
     * @return the binding filtering bitmask
     */
    public int getShowBindings() {
        return showBindings;
    }

    /**
     * Set the new value of what bindings are shown.
     * <p/>
     * Note that this does not affect the direct access using the
     * definition. Only usages of {@link Wsdl#getBindings()} are
     * affected.
     *
     * @param showBindings the new bindings bitmask filtering value
     */
    public void setShowBindings(int showBindings) {
        this.showBindings = showBindings;
    }

    /**
     * @param bo the binding operation
     * @return the binding where the binding operation is defined or <b>null</b>
     */
    public Binding getBinding(BindingOperation bo) {
        Iterator bindings = getBindings().iterator();
        while (bindings.hasNext()) {
            Binding binding = (Binding)bindings.next();
            if (binding.getBindingOperations().contains(bo)) {
                return binding;
            }
        }
        return null;
    }

    /**
     * @param localName the binding local name
     * @return the binding with the given local name or <b>null</b>
     */
    public Binding getBinding(String localName) {
        if (localName == null) {
            throw new IllegalArgumentException();
        }
        Iterator bindings = getBindings().iterator();
        while (bindings.hasNext()) {
            Binding binding = (Binding)bindings.next();
            if (localName.equals(binding.getQName().getLocalPart())) {
                return binding;
            }
        }
        return null;
    }


    /**
     * @return the collection of WSDL <code>Message</code>
     *         instances described in this definition and
     *         imported definitions.
     */
    public Collection getMessages() {
        final Collection allMessages = new ArrayList();

        collectElements(new ElementCollector() {
            public void collect(Definition def) {
                if(def == null) return;

                Map messages = def.getMessages();
                if(messages != null && messages.values() != null) {
                    allMessages.addAll(def.getMessages().values());
                }
            }
        }, definition);
        return allMessages;
    }

    /**
     * @return the collection of WSDL <code>PortType</code>
     *         instances described in this definition and imported
     *         definitions.
     */
    public Collection getPortTypes() {
        final Collection allPortTypes = new ArrayList();

        collectElements(new ElementCollector() {
            public void collect(Definition def) {
                if(def == null) return;

                Map portTypes = def.getPortTypes();
                if( portTypes != null && portTypes.values() != null) {
                    allPortTypes.addAll(def.getPortTypes().values());
                }
            }
        }, definition);

        return allPortTypes;
    }

    /**
     * @return the collection of WSDL <code>Service</code>
     *         instances described in this definition and
     *         imported definitions.
     */
    public Collection getServices() {
        final Collection allServices = new ArrayList();

        collectElements(new ElementCollector() {
            public void collect(Definition def) {
                if(def == null) return;

                Map services = def.getServices();
                if(services != null && services.values() != null) {
                    allServices.addAll(def.getServices().values());
                }
            }
        }, definition);

        return allServices;
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
     *               <code>
     *               // Retrieve the WSDL definition from the string representing
     *               // the wsdl and iterate over the services.
     *               Wsdl wsdl = Wsdl.newInstance(null, new StringReader(wsdlString));
     *               Iterator services = wsdl.getServices().iterator();
     *               ...
     *               StringWriter sw = new StringWriter();
     *               wsdl.toWriter(sw);
     *               System.out.println(sw.toString()):
     *               ...
     *               </code>
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
     *           <code>
     *           // Retrieve the WSDL definition from the string representing
     *           // the wsdl and iterate over the services.
     *           Wsdl wsdl = Wsdl.newInstance(null, new StringReader(wsdlString));
     *           Iterator services = wsdl.getServices().iterator();
     *           ...
     *           FileOutputStream fos = new FileOutputStream("./service.wsdl");
     *           wsdl.toOutputStream(fos);
     *           ...
     *           </code>
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
     *
     * @param bo the binding operation
     * @return the operation style "rpc" | "document", if undefined "rpc" is returned
     */
    public String getBindingStyle(BindingOperation bo) {
        List elements = bo.getExtensibilityElements();
        for (Iterator iterator = elements.iterator(); iterator.hasNext();) {
            Object o = (Object)iterator.next();
            if (o instanceof SOAPOperation) {
                SOAPOperation so = (SOAPOperation)o;
                String style = so.getStyle();
                if (style == null || "".equals(style)) {
                    break;
                }
                return style;
            }
        }
        Iterator bindings = getBindings().iterator();
        while (bindings.hasNext()) {
            Binding binding = (Binding)bindings.next();
            if (binding.getBindingOperations().contains(bo)) {
                ExtensibilityElement bindingProtocol = getBindingProtocol(binding);
                if (bindingProtocol == null ||
                  bindingProtocol instanceof HTTPBinding) {
                    return "document"; // GET/POST uses document?
                } else if (bindingProtocol instanceof SOAPBinding) {
                    SOAPBinding sb = (SOAPBinding)bindingProtocol;
                    return sb.getStyle();
                }
            }
        }
        return "document"; //default
    }

    /**
     * Return the protocol binding (soap, http etc) extensibility
     * element for a given binding.
     * <p/>
     * The <code>ExtensibilityElement</code> subclass such as
     * <code>SOAPBinding</code> or <code>HTTPBinding</code>
     * is returned, or <b>null</b> if it cannot be determined.
     *
     * @param b the binding to
     * @return the proticol binding or null if unspecified
     */
    public ExtensibilityElement getBindingProtocol(Binding b) {
        List elements = b.getExtensibilityElements();
        for (Iterator iterator = elements.iterator(); iterator.hasNext();) {
            Object o = (Object)iterator.next();
            if (o instanceof SOAPBinding) {
                return (SOAPBinding)o;
            } else if (o instanceof HTTPBinding) {
                return (HTTPBinding)o;
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
            String prefix = definition.getPrefix(uri);
            if (prefix == null) prefix = (String)namespaceMap.get(uri);

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

    public String getBindingOutputNS(BindingOperation operation) {
        BindingOutput output = operation.getBindingOutput();
        if (output != null) {
            Iterator eels = output.getExtensibilityElements().iterator();
            ExtensibilityElement ee;
            while (eels.hasNext()) {
                ee = (ExtensibilityElement)eels.next();
                if (ee instanceof SOAPBody) {
                    SOAPBody body = (SOAPBody)ee;
                    if (body.getNamespaceURI() != null) {
                        return body.getNamespaceURI();
                    }
                }
            }
        }
        return definition.getTargetNamespace();
    }

    public String getBindingInputNS(BindingOperation operation) {
        BindingInput input = operation.getBindingInput();
        if (input != null) {
            Iterator eels = input.getExtensibilityElements().iterator();
            ExtensibilityElement ee;
            while (eels.hasNext()) {
                ee = (ExtensibilityElement)eels.next();
                if (ee instanceof SOAPBody) {
                    SOAPBody body = (SOAPBody)ee;
                    if (body.getNamespaceURI() != null) {
                        return body.getNamespaceURI();
                    }
                }
            }
        }
        return definition.getTargetNamespace();
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

    public Collection getInputParameters(BindingOperation bindingOperation) {
        if (bindingOperation != null) {
            return bindingOperation.getBindingInput().getExtensibilityElements();
        } else {
            throw new IllegalArgumentException("The argument bindingOperation is NULL");
        }
    }

    public Collection getOutputParameters(BindingOperation bindingOperation) {
        if (bindingOperation == null) {
            throw new IllegalArgumentException("The argument bindingOperation is NULL");
        }
        return bindingOperation.getBindingOutput().getExtensibilityElements();
    }


    /**
     * Get a <i>MIME Multipart Related</i> container for the input parameters
     * declared for the given binding operation.
     *
     * @param bo the binding operation
     */
    public MIMEMultipartRelated getMimeMultipartRelatedInput(BindingOperation bo) {
        Collection elements = getInputParameters(bo);

        // for each input parameter of the binding operation
        for (Iterator itr = elements.iterator(); itr.hasNext();) {
            Object o = (Object)itr.next();
            if (o instanceof MIMEMultipartRelated) {
                return (MIMEMultipartRelated)o;
            }
        }
        return null;
    }

    /**
     * Get a <i>MIME Multipart Related</i> container for the input parameters
     * declared for the given binding operation.
     *
     * @param bo the binding operation
     */
    public MIMEMultipartRelated getMimeMultipartRelateOutput(BindingOperation bo) {
        Collection elements = getOutputParameters(bo);

        // for each input parameter of the binding operation
        for (Iterator itr = elements.iterator(); itr.hasNext();) {

            Object o = (Object)itr.next();
            if (o instanceof MIMEMultipartRelated) {
                return (MIMEMultipartRelated)o;
            }
        }
        return null;
    }


    public Collection getMimePartSubElements(MIMEPart mimePart) {
        if (mimePart != null) {
            return mimePart.getExtensibilityElements();
        } else {
            throw new IllegalArgumentException("The argument mimePart is NULL");
        }
    }

    /**
     * extract base URI from the URL specified.
     * @param url
     * @return
     */
    public static String extractBaseURI(String url) {

        int lastIndexOfSlash = url.lastIndexOf('/');
        String baseURL;
        if(lastIndexOfSlash == -1) {
            baseURL = "./";
        } else {
            baseURL = url.substring(0, lastIndexOfSlash + 1);
        }

        return baseURL;
    }


    public String getUriFromPort(Port wsdlPort) throws MalformedURLException {
        if (wsdlPort == null)
            throw new IllegalArgumentException("No WSDL port was provided");
        List elements = wsdlPort.getExtensibilityElements();
        String uri = null;
        ExtensibilityElement eel;
        int num = 0;
        for (int i = 0; i < elements.size(); i++) {
            eel = (ExtensibilityElement)elements.get(i);
            if (eel instanceof SOAPAddress) {
                SOAPAddress sadd = (SOAPAddress)eel;
                num++;
                uri = sadd.getLocationURI();
            }
        }

        if (num > 1) logger.warning("WSDL " + getDefinition().getTargetNamespace() + " contained multiple <soap:address> elements");

        return uri;
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

    public String getPortUrl(Port wsdlPort) {
        if (wsdlPort == null)
            throw new IllegalArgumentException("No WSDL port was provided");
        List elements = wsdlPort.getExtensibilityElements();
        ExtensibilityElement eel;
        int num = 0;
        for (int i = 0; i < elements.size(); i++) {
            eel = (ExtensibilityElement)elements.get(i);
            if (eel instanceof SOAPAddress) {
                SOAPAddress sadd = (SOAPAddress)eel;
                num++;
                if (sadd.getLocationURI() != null) {
                    return sadd.getLocationURI();
                }
            }
        }
        return null;
    }

    /**
     * Traverses all the imported definitions and invokes collect on the collector
     * for reach definition
     * @param collector
     */
    private void collectElements(ElementCollector collector, Definition def) {
        collector.collect(def);
        final Map imports = def.getImports();
        for (Iterator iterator = imports.values().iterator(); iterator.hasNext();) {
            List importList = (List) iterator.next();
            for (int i = 0; i < importList.size(); i++) {
                Import importDef = (Import) importList.get(i);
                if(importDef.getDefinition() != null) {
                    collectElements(collector, importDef.getDefinition());
                }
            }
        }
    }

    /**
     * Get the schema element from the wsdl definiton.
     *
     * @param def  the wsdl definition
     * @return Element the "schema" element in the wsdl
     * @throws RemoteException  if failed to call the remote object
     * @throws MalformedURLException  if URL format is invalide
     * @throws IOException   when error occured in reading the wsdl document
     * @throws SAXException  when error occured in parsing XML
     */
    public static Element getSchemaElement(Definition def)
            throws RemoteException, MalformedURLException, IOException, SAXException {
        Element schemaElement = null;
        Import imp = null;
        if (def.getImports().size() > 0) {
            Iterator itr = def.getImports().keySet().iterator();
            while (itr.hasNext()) {
                Object importDef = itr.next();
                List importList = (List) def.getImports().get(importDef);
                for (int k = 0; k < importList.size(); k++) {
                    imp = (Import) importList.get(k);
                    // check if the schema is inside the file
                    String url = imp.getLocationURI();
                    String resolvedXml = null;
                    resolvedXml =  Registry.getDefault().getServiceManager().resolveWsdlTarget(url);

                    if(resolvedXml != null) {
                        Document resultDoc = XmlUtil.stringToDocument(resolvedXml);
                        if(resultDoc != null) {
                            NodeList nodeList = resultDoc.getElementsByTagName("schema");

                            if(nodeList != null && nodeList.item(0) != null) {
                                // should only have one
                                return (Element) nodeList.item(0);
                            }
                        }
                    }
                }
            }
        }

        // if not found
        if(schemaElement == null) {
            if (imp != null && imp.getDefinition() != null) {
                schemaElement = getSchemaElement(imp.getDefinition());
            }
        }

        return schemaElement;
    }

    private Definition definition;

    private transient Logger logger = Logger.getLogger(getClass().getName());

    private interface ElementCollector  {
        /**
         * The implementation collects the elements of the interest fro mthe definition
         * @param def the wsdl DEfinition
         */
        void collect(Definition def);
    }
}
