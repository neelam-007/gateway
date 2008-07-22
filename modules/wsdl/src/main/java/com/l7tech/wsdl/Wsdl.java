package com.l7tech.wsdl;

import com.l7tech.common.io.IOExceptionThrowingInputStream;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.util.*;
import org.apache.ws.policy.Assertion;
import org.apache.ws.policy.Policy;
import org.apache.ws.policy.PolicyReference;
import org.apache.ws.policy.util.DOMPolicyReader;
import org.apache.ws.policy.util.PolicyFactory;
import org.apache.ws.policy.util.PolicyRegistry;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.wsdl.*;
import javax.wsdl.Service;
import javax.wsdl.extensions.*;
import javax.wsdl.extensions.http.HTTPBinding;
import javax.wsdl.extensions.mime.MIMEMultipartRelated;
import javax.wsdl.extensions.mime.MIMEPart;
import javax.wsdl.extensions.soap.SOAPAddress;
import javax.wsdl.extensions.soap.SOAPBinding;
import javax.wsdl.extensions.soap.SOAPBody;
import javax.wsdl.extensions.soap.SOAPOperation;
import javax.wsdl.extensions.soap12.SOAP12Address;
import javax.wsdl.factory.WSDLFactory;
import javax.wsdl.xml.WSDLLocator;
import javax.wsdl.xml.WSDLReader;
import javax.wsdl.xml.WSDLWriter;
import javax.xml.namespace.QName;
import javax.xml.soap.SOAPConstants;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;
import java.util.logging.Level;
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
public class Wsdl implements Serializable {

    //- PUBLIC

    public static final String WSDL_SOAP_NAMESPACE = "http://schemas.xmlsoap.org/wsdl/soap/";

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

    /**
     * rpc style constaint
     */
    public static final String STYLE_RPC = "rpc";

    /**
     * document style constaint
     */
    public static final String STYLE_DOCUMENT = "document";


    /**
     * SOAP binding use literal - default
     */
    public static final String USE_LITERAL = "literal";

    /**
     * SOAP binding use encoded (deprecated but WS-I)
     */
    public static final String USE_ENCODED = "encoded";

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
     * @throws javax.wsdl.WSDLException throw on error parsing the WSDL definition
     */
    public static Wsdl newInstance(String documentBaseURI, Reader reader)
      throws WSDLException {
        InputSource source = new InputSource();
        source.setSystemId(documentBaseURI);
        source.setCharacterStream(reader);
        WSDLFactory fac = getWSDLFactory(false);
        WSDLReader wsdlReader = fac.newWSDLReader();
        wsdlReader.setFeature("javax.wsdl.verbose", false);
        disableSchemaExtensions(fac, wsdlReader);
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
        WSDLFactory fac = getWSDLFactory(false);
        WSDLReader reader = fac.newWSDLReader();
        reader.setFeature("javax.wsdl.verbose", false);
        disableSchemaExtensions(fac, reader);
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
     * @param reader          the character stream that contains the WSDL document,
     *                        an XML document obeying the WSDL schema.
     * @param allowLocalImports True to allow imports to be resolved to the
     *                        local file system.
     * @return the <code>Wsdl</code> instance
     * @throws javax.wsdl.WSDLException throw on error parsing the WSDL definition
     */
    public static Wsdl newInstance(final String documentBaseURI, final Reader reader, final boolean allowLocalImports)
            throws WSDLException {
        WSDLFactory fac = getWSDLFactory(false);
        return newInstance(fac, documentBaseURI, reader, allowLocalImports);
    }

    /**
     * Create the instance by reading a WSDL document into from
     * the <code>Reader</code> character stream.
     *
     * @param fac             the WSDLFactory to use
     * @param documentBaseURI the document base URI of the WSDL definition
     *                        described by the document.
     *                        Can be <b>null</b>, in which case it will be
     *                        ignored.
     * @param reader          the character stream that contains the WSDL document,
     *                        an XML document obeying the WSDL schema.
     * @param allowLocalImports True to allow imports to be resolved to the
     *                        local file system.
     * @return the <code>Wsdl</code> instance
     * @throws javax.wsdl.WSDLException throw on error parsing the WSDL definition
     */
    public static Wsdl newInstance(final WSDLFactory fac, final String documentBaseURI, final Reader reader, final boolean allowLocalImports)
            throws WSDLException {

        InputSource is = new InputSource();
        is.setCharacterStream(reader);
        is.setSystemId(documentBaseURI);

        return newInstance(fac, getWSDLLocator(is, allowLocalImports));
    }

    /**
     * Create the instance by reading a WSDL document into from
     * the given <code>WSDLLocator</code>.
     *
     * @param locator         the WSDLLocator to use
     * @return the <code>Wsdl</code> instance
     * @throws javax.wsdl.WSDLException throw on error parsing the WSDL definition
     */
    public static Wsdl newInstance(final WSDLLocator locator)
            throws WSDLException {
        return newInstance(getWSDLFactory(false), locator);
    }

    /**
     * Create the instance by reading a WSDL document into from
     * the given <code>WSDLLocator</code>.
     *
     * @param fac             the WSDLFactory to use
     * @param locator         the WSDLLocator to use
     * @return the <code>Wsdl</code> instance
     * @throws javax.wsdl.WSDLException throw on error parsing the WSDL definition
     */
    public static Wsdl newInstance(final WSDLFactory fac, final WSDLLocator locator)
            throws WSDLException {
        WSDLReader wsdlReader = fac.newWSDLReader();
        wsdlReader.setFeature("javax.wsdl.verbose", false);
        disableSchemaExtensions(fac, wsdlReader);

        return new Wsdl(wsdlReader.readWSDL(locator));
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
        WSDLFactory fac = getWSDLFactory(false);
        WSDLReader reader = fac.newWSDLReader();
        reader.setFeature("javax.wsdl.verbose", false);
        disableSchemaExtensions(fac, reader);
        return new Wsdl(reader.readWSDL(documentBaseURI, inputSource));
    }

    /**
     * Get a WSDLLocator that resolves URIs to the given Map.
     *
     * <p>The documentBaseURI must be one of the resources in the Map.</p>
     *
     * @param documentBaseURI The document base URI of the WSDL definition
     * @param resourceMap The Map of URIs to content for the WSDL and all dependencies
     * @param logger A logger to write debug information
     * @return a WSDLLocator instance
     */
    public static WSDLLocator getWSDLLocator(final String documentBaseURI,
                                             final Map<String,String> resourceMap,
                                             final Logger logger ) {
        return new CachedDocumentResolver( documentBaseURI, resourceMap, logger );
    }

    /**
     * Get a WSDLLocator that resolves http and file documents.
     *
     * <p>The system identifier in the baseInputSource is returned from getBaseURI.</p>
     *
     * @param baseInputSource the base input source
     * @param allowLocalImports True to allow local (file) imports
     * @return a WSDLLocator instance
     */
    public static WSDLLocator getWSDLLocator(final InputSource baseInputSource, final boolean allowLocalImports) {
        return new WSDLLocator() {
            private String lastResolvedUri = null;

            public InputSource getBaseInputSource() {
                lastResolvedUri = getBaseURI();
                return baseInputSource;
            }

            public String getBaseURI() {
                return baseInputSource.getSystemId();
            }

            /**
             * Resolve a (possibly relative) import.
             *
             * @param parentLocation A URI specifying the location of the document doing the importing.
             *                       This can be null if the import location is not relative to the
             *                       parent location.
             * @param importLocation A URI specifying the location of the document to import. This might
             *                       be relative to the parent document's location.
             * @return the InputSource object or null if the import cannot be found.
             */
            public InputSource getImportInputSource(String parentLocation, String importLocation) {
                InputSource is = null;
                URI resolvedUri;
                try {
                    lastResolvedUri = importLocation; // ensure set even if not valid

                    if (parentLocation != null) {
                        URI base = new URI(parentLocation);
                        URI relative = new URI(importLocation);
                        resolvedUri = base.resolve(relative);
                    }
                    else {
                        resolvedUri = new URI(importLocation);
                    }

                    lastResolvedUri = resolvedUri.toString();

                    if (resolvedUri.isAbsolute()) {
                        if (allowLocalImports || !"file".equals(resolvedUri.getScheme())) {
                            is = new InputSource();
                            is.setSystemId(resolvedUri.toString());
                        } else {
                            is = new InputSource();
                            is.setSystemId(resolvedUri.toString());
                            //noinspection ThrowableInstanceNeverThrown
                            is.setByteStream( new IOExceptionThrowingInputStream(new IOException("Local import not permitted '"+resolvedUri.toString()+"'.")) );
                        }
                    }
                }
                catch (URISyntaxException use) {
                    // of interest?
                }

                return is;
            }

            public String getLatestImportURI() {
                return lastResolvedUri;
            }

            public void close() {
                //?
            }
        };
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
        for (Service svc : getServices()) {
            //noinspection unchecked
            Map<QName, Port> ports = svc.getPorts();
            for (Port p : ports.values()) {
                //noinspection unchecked
                List<ExtensibilityElement> elements = p.getExtensibilityElements();
                for (ExtensibilityElement element : elements) {
                    if (element instanceof SOAPAddress) {
                        SOAPAddress sa = (SOAPAddress) element;
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
        Collection<Service> services = getServices();
        if (services.isEmpty()) {
            return null;
        }
        Service svc = services.iterator().next();
        return getLocalName(svc);
    }

    /**
     * Get the Bindings for the WSDL (which may be from an import)
     *
     * @return the collection of WSDL <code>Binding</code>
     *         instances described in this definition (may be empty but not null).
     */
    public Collection<Binding> getBindings() {
        final Collection<Binding> allBindings = new ArrayList<Binding>();
        collectElements(new ElementCollector() {
            public void collect(Definition def) {
                if (def == null) return;

                //noinspection unchecked
                Map<QName, Binding> bindings = def.getBindings();
                if (bindings != null && bindings.values() != null) {
                    allBindings.addAll(bindings.values());
                }
            }
        }, definition);

        if (showBindings == ALL_BINDINGS) {
            return allBindings;
        }
        Collection<Binding> filtered = new ArrayList<Binding>();
        for (Binding binding : allBindings) {
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
     * Check if any operations for any bindings of interest use MIME multipart.
     *
     * @return true if multipart
     */
    public boolean hasMultipartOperations() {
        boolean multipart = false;

        Collection<Binding> bindings = getBindings();

        // for each binding in WSDL
        for (Binding binding : bindings) {
            //noinspection unchecked
            Collection<BindingOperation> operations = binding.getBindingOperations();

            // for each operation in WSDL
            for (BindingOperation bo : operations) {
                MIMEMultipartRelated mimeMultipart = getMimeMultipartRelatedInput(bo);
                if (mimeMultipart != null) {
                    multipart = true;
                    break;
                }
            }

            if (multipart) break;
        }

        return multipart;
    }

    /** @return true if the specified ExtensibilityElement is a wsp:Policy element. */
    public boolean isPolicy(ExtensibilityElement ee) {
        return getPolicyUue(ee) != null;
    }

    public static class BadPolicyReferenceException extends Exception {
        public BadPolicyReferenceException() {
            super();
        }

        public BadPolicyReferenceException(String message) {
            super(message);
        }

        public BadPolicyReferenceException(String message, Throwable cause) {
            super(message, cause);
        }

        public BadPolicyReferenceException(Throwable cause) {
            super(cause);
        }
    }

    /**
     * @return a Policy if the specified ExtensibilityElement is a wsp:Policy or resolvable wsp:PolicyReference,
     *         or null if it isn't.
     * @throws BadPolicyReferenceException if the element is a PolicyReference but it can't be resolved to a Policy
     */
    public Policy toPolicy(ExtensibilityElement ee) throws BadPolicyReferenceException {
        UnknownExtensibilityElement uee = getPolicyUue(ee);
        if (uee != null)
            return readPolicySafely(uee.getElement());

        uee = getPolicyReferenceUue(ee);
        if (uee == null) return null;
        PolicyReference ref = readPolicyReferenceSafely(uee.getElement());
        if (ref == null) throw new BadPolicyReferenceException("Unable to parse wsp:PolicyReference");
        Policy p = getPolicyRegistry().lookup(ref.getPolicyURIString());
        if (p == null) throw new BadPolicyReferenceException("Unable to resolve policy reference: URI=" + ref.getPolicyURIString());
        return p;
    }

    /** @return true if the specified ExtensibilityElkement is a wsp:PolicyReference element. */
    public boolean isPolicyReference(ExtensibilityElement ee) {
        return getPolicyReferenceUue(ee) != null;
    }

    /** @return a PolicyReference if the specified ExtensibilityElement is a wsp:PolicyReference, or null if it isn't. */
    public PolicyReference toPolicyReference(ExtensibilityElement ee) {
        UnknownExtensibilityElement uee = getPolicyReferenceUue(ee);
        if (uee == null) return null;
        return readPolicyReferenceSafely(uee.getElement());
    }


    /** @return a PolicyRegistry that will find top-level policies in this WSDL. */
    public PolicyRegistry getPolicyRegistry() {
        if (policyRegistry != null) return policyRegistry;
        policyRegistry = new PolicyRegistry();

        // Pre-register all top-level policies
        List<Policy> policies = getPolicies();
        for (Policy policy : policies) {
            policyRegistry.register(policy.getPolicyURI(), policy);
        }

        return policyRegistry;
    }

    /**
     * @return the top-level Policy elements of this WSDL, as {@link org.apache.ws.policy.Policy} instances.
     *         May be empty, but never null.
     *
     */
    public List<Policy> getPolicies() {
        if (topLevelPolicies != null) return topLevelPolicies;
        List<Policy> ret = new ArrayList<Policy>();
        //noinspection unchecked
        List<ExtensibilityElement> eels = getDefinition().getExtensibilityElements();
        for (ExtensibilityElement eel : eels) {
            UnknownExtensibilityElement uee = getPolicyUue(eel);
            if (uee != null) {
                Policy p = readPolicySafely(uee.getElement());
                if (p != null) ret.add(p);
            }
        }
        return topLevelPolicies = ret;
    }

    /**
     * @return the computed effective policy for this input message, merged with per-operation and per-binding policies (if any), or null if no policy is in effect.
     */
    @SuppressWarnings({"unchecked"})
    public Assertion getEffectiveInputPolicy(Binding binding, BindingOperation operation)
            throws BadPolicyReferenceException
    {
        getPolicyRegistry();
        // Accumulate the effective policy
        Assertion ep = null;
        ep = mergePolicies(ep, binding.getExtensibilityElements());
        ep = mergePolicies(ep, operation.getExtensibilityElements());
        ep = mergePolicies(ep, operation.getBindingInput().getExtensibilityElements());
        return ep;
    }

    @SuppressWarnings({"unchecked"})
    public Assertion getEffectiveOutputPolicy(Binding binding, BindingOperation operation)
            throws BadPolicyReferenceException
    {
        getPolicyRegistry();
        // Accumulate the effective policy
        Assertion ep = null;
        ep = mergePolicies(ep, binding.getExtensibilityElements());
        ep = mergePolicies(ep, operation.getExtensibilityElements());
        ep = mergePolicies(ep, operation.getBindingOutput().getExtensibilityElements());
        return ep;
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
        for (Binding binding : getBindings()) {
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
        for (Binding binding : getBindings()) {
            if (localName.equals(getLocalName(binding))) {
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
        final Collection<Message> allMessages = new ArrayList<Message>();

        collectElements(new ElementCollector() {
            public void collect(Definition def) {
                if (def == null) return;

                Map messages = def.getMessages();
                if (messages != null && messages.values() != null) {
                    //noinspection unchecked
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
        final Collection<PortType> allPortTypes = new ArrayList<PortType>();

        collectElements(new ElementCollector() {
            public void collect(Definition def) {
                if (def == null) return;

                Map portTypes = def.getPortTypes();
                if (portTypes != null && portTypes.values() != null) {
                    //noinspection unchecked
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
    public Collection<Service> getServices() {
        final Collection<Service> allServices = new ArrayList<Service>();

        collectElements(new ElementCollector() {
            public void collect(Definition def) {
                if (def == null) return;

                Map services = def.getServices();
                if (services != null && services.values() != null) {
                    //noinspection unchecked
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
    public Collection<Types> getTypes() {
        final Collection<Types> allTypes = new ArrayList<Types>();

        collectElements(new ElementCollector() {
            public void collect(final Definition def) {
                if (def == null) return;

                Types types = def.getTypes();
                if ( types != null ) {
                    allTypes.add(types);
                }
            }
        }, definition);

        return allTypes;
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
     * @param writer the target writer
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
        WSDLFactory fac = getWSDLFactory(true);
        WSDLWriter wsdlWriter = fac.newWSDLWriter();
        wsdlWriter.writeWSDL(definition, writer);
    }

    /**
     * Write the internal WSDL definition to the <code>OutputStream</code>
     *
     * @param os the target output stream
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
        WSDLFactory fac = getWSDLFactory(true);
        WSDLWriter wsdlWriter = fac.newWSDLWriter();
        wsdlWriter.writeWSDL(definition, os);
    }


    /**
     * Returens the instance string representatiton.
     * Delegated to the internal definition instance.
     *
     * @return the string representation of the object
     */
    @Override
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
        //noinspection unchecked
        List<ExtensibilityElement> elements = bo.getExtensibilityElements();
        for (ExtensibilityElement element : elements) {
            if (element instanceof SOAPOperation) {
                SOAPOperation so = (SOAPOperation) element;
                String style = so.getStyle();
                if (style == null || "".equals(style)) {
                    break;
                }
                return style;
            }
        }
        for (Binding binding : getBindings()) {
            if (binding.getBindingOperations().contains(bo)) {
                return getBindingStyle(binding);
            }
        }
        return Wsdl.STYLE_DOCUMENT; //default
    }

    /**
     * Determine the binding style ("document | "rpc") for an binding.
     *
     * @param binding the binding to determine the style for
     * @return the operation style "rpc" | "document", if undefined "rpc" is returned
     */
    public String getBindingStyle(Binding binding) {
        ExtensibilityElement bindingProtocol = getBindingProtocol(binding);
        if (bindingProtocol == null ||
          bindingProtocol instanceof HTTPBinding) {
            return Wsdl.STYLE_DOCUMENT; // GET/POST uses document?
        } else if (bindingProtocol instanceof SOAPBinding) {
            SOAPBinding sb = (SOAPBinding)bindingProtocol;
            return sb.getStyle();
        }
        return Wsdl.STYLE_DOCUMENT; //default
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
        //noinspection unchecked
        List<ExtensibilityElement> elements = b.getExtensibilityElements();
        for (ExtensibilityElement eel : elements) {
            if (eel instanceof SOAPBinding) {
                return eel;
            } else if (eel instanceof HTTPBinding) {
                return eel;
            }
        }
        return null;
    }

    /**
     * Returns SOAP use (literal, encoded) for the <code>Binding</code>.  Examines each binding operation input and
     * output message. Every input and output are expected to have the same use throughout the <code<Binding</code>,
     * and if mixed uses are detected the WSDLException is raised.
     * The default use if unspecified is 'literal'.
     *
     * @param binding the binding to examine
     * @throws IllegalArgumentException
     * @throws WSDLException            if the input and output for the bindfing operation messages specify different
     *                                  use (mixed encoded and literal)
     */
    public String getSoapUse(Binding binding) throws IllegalArgumentException, WSDLException {
        String soapUse;
        ExtensibilityElement ee = getBindingProtocol(binding);
        if (!(ee instanceof SOAPBinding)) {
            throw new IllegalArgumentException("Must be SOAP binding. +( " + getLocalName(binding) + " )");
        }
        Set<String> soapUseSet = new HashSet<String>();
        //noinspection unchecked
        List<BindingOperation> bindingOperations = binding.getBindingOperations();
        for (BindingOperation bindingOperation : bindingOperations) {
            soapUseSet.add(getSoapUse(bindingOperation));
        }
        if (soapUseSet.size() > 1) {
            throw new WSDLException(WSDLException.INVALID_WSDL, "Mixed/unsupported uses in '" + getLocalName(binding) + "' found in this WSDL.");
        } else if ( soapUseSet.isEmpty() ) {
            soapUse = USE_LITERAL;
        } else {
            soapUse = soapUseSet.iterator().next();            
        }

        return soapUse;
    }

    /**
     * Returns SOAP use (literal, encoded) for the <code>BindingOperation</code>.  Examines both input and
     * output messages. Both input and output are expected to have the same use, and if mixed the IllegalArgumentException
     * is raised. The default use if unspecified is 'literal'.
     *
     * @param bindingOperation the binding operation
     * @return the String indicating the
     * @throws IllegalArgumentException if the <code>Binding</code> for the presented <code>BidningOperation</code>
     *                                  could not be determined
     * @throws WSDLException            if the input and output message specify different use (mixed encoded and literal)
     */
    public String getSoapUse(BindingOperation bindingOperation) throws IllegalArgumentException, WSDLException {
        Binding binding = getBinding(bindingOperation);
        if (binding == null) {
            throw new IllegalArgumentException("The binding for binding operation '" + bindingOperation.getName() + "' is not found in this WSDL.");
        }
        BindingInput bindingInput = bindingOperation.getBindingInput();

        String use = USE_LITERAL;

        if (bindingInput == null) {
            return USE_LITERAL;
        }

        //noinspection unchecked
        List<ExtensibilityElement> extensibilityElements = bindingInput.getExtensibilityElements();
        for (ExtensibilityElement eel : extensibilityElements) {
            if (eel instanceof SOAPBody) {
                SOAPBody soapBody = (SOAPBody) eel;
                if (soapBody.getUse() != null) {
                    use = soapBody.getUse();
                    break;
                }
            }
        }
        Set<String> useSet = new HashSet<String>();
        useSet.add(use);
        // output
        use = USE_LITERAL;
        BindingOutput bindingOutput = bindingOperation.getBindingOutput();
        if (bindingOutput != null) { // with some wsdls, this could be null (see bugzilla #2309)
            //noinspection unchecked
            extensibilityElements = bindingOutput.getExtensibilityElements();
            for (ExtensibilityElement eel : extensibilityElements) {
                if (eel instanceof SOAPBody) {
                    SOAPBody soapBody = (SOAPBody) eel;
                    if (soapBody.getUse() != null) {
                        use = soapBody.getUse();
                        break;
                    }
                }
            }
            useSet.add(use);
        }
        if (useSet.size() > 1) {
            throw new WSDLException(WSDLException.INVALID_WSDL, "Mixed/unsupported uses for '" + bindingOperation.getName() + "' found in this WSDL.");
        }

        return useSet.iterator().next();
    }

    /**
     * Finds a single Port that contains a SOAPAddress from whatever Services are in the WSDL.
     *
     * @return a Port that contains a SOAPAddress
     */
    public Port getSoapPort() {
        Iterator services = getServices().iterator();
        Service wsdlService = null;
        Port pork;
        Port soapPort = null;
        Port soap12Port = null;

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

                    //noinspection unchecked
                    List<ExtensibilityElement> elements = pork.getExtensibilityElements();
                    // Find the first Port that contains a SOAPAddress eel
                    for (ExtensibilityElement eel : elements) {
                        if (eel instanceof SOAPAddress) {
                            soapPort = pork;
                            numPorts++;
                        } else if (eel instanceof SOAP12Address) {
                            soap12Port = pork;
                            numPorts++;
                        }
                    }
                }
            }
            if (numPorts > 1)
                logger.warning("WSDL " + getDefinition().getTargetNamespace() + " has more than one port, used the first.");
        }
        if (soapPort == null) {
            return soap12Port;
        }
        return soapPort;
    }

    public Map getNamespaces() {
        List<String> soapUris = SoapConstants.ENVELOPE_URIS;
        //noinspection unchecked
        Map<String, String> namespaceMap = definition.getNamespaces();
        String tnsUri = definition.getTargetNamespace();
        int ns = 1;
        final String TEMP = "l7tempprefix";

        // Get it into URL order
        SortedMap<String, String> uris = new TreeMap<String, String>();
        for (String prefix : namespaceMap.keySet()) {
            String uri = namespaceMap.get(prefix);
            if (prefix == null || prefix.length() == 0) prefix = TEMP + ns++;
            uris.put(uri, prefix);
        }

        // Now assign prefixes
        LinkedHashMap<String, String> result = new LinkedHashMap<String, String>();

        boolean soapenv = false;
        for (String uri : uris.keySet()) {
            String prefix = definition.getPrefix(uri);
            if (prefix == null) prefix = namespaceMap.get(uri);

            if (soapUris.contains(uri)) {
                result.put(prefix, uri);
                soapenv = true;
            } else if (uri.equals(tnsUri)) {
                result.put(prefix, uri);
            }
        }

        if (!soapenv) {
            if (result.get( SoapConstants.SOAP_ENV_PREFIX) == null)
                result.put( SoapConstants.SOAP_ENV_PREFIX, SOAPConstants.URI_NS_SOAP_ENVELOPE);
            else if (!result.containsValue(SOAPConstants.URI_NS_SOAP_ENVELOPE))
                result.put(TEMP + ns++, SOAPConstants.URI_NS_SOAP_ENVELOPE);
        }

        Collection<BindingOperation> operations = getBindingOperations();
        for (BindingOperation operation : operations) {
            BindingInput input = operation.getBindingInput();
            //noinspection unchecked
            Collection<ExtensibilityElement> eels = input.getExtensibilityElements();
            for (ExtensibilityElement eel : eels) {
                if (eel instanceof SOAPBody) {
                    SOAPBody body = (SOAPBody) eel;
                    String uri = body.getNamespaceURI();
                    if (uri != null && !result.containsValue(uri)) {
                        result.put(TEMP + ns++, uri);
                    }
                }
            }
        }

        ns = 1;
        LinkedHashMap<String, String> result2 = new LinkedHashMap<String, String>();
        for (String prefix : result.keySet()) {
            String uri = result.get(prefix);
            if (prefix == null || prefix.length() == 0 || prefix.startsWith(TEMP)) prefix = NS + ns++;
            result2.put(prefix, uri);
        }

        return result2;
    }


    public String getTargetNamespace() {

        return getDefinition().getTargetNamespace();

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

    public Collection<BindingOperation> getBindingOperations() {
        Iterator bindings = getBindings().iterator();
        Binding binding;
        List<BindingOperation> operations = new ArrayList<BindingOperation>();
        while (bindings.hasNext()) {
            binding = (Binding)bindings.next();
            //noinspection unchecked
            operations.addAll(binding.getBindingOperations());
        }
        return operations;
    }

    public Collection<ExtensibilityElement> getInputParameters(BindingOperation bindingOperation) {
        if (bindingOperation != null) {
            BindingInput bin = bindingOperation.getBindingInput();
            if ( bin != null ) {
                //noinspection unchecked
                return bin.getExtensibilityElements();
            } else {
                return Collections.emptySet();
            }
        } else {
            throw new IllegalArgumentException("The argument bindingOperation is NULL");
        }
    }

    public Collection<ExtensibilityElement> getOutputParameters(BindingOperation bindingOperation) {
        if (bindingOperation == null) {
            throw new IllegalArgumentException("The argument bindingOperation is NULL");
        }
        
        BindingOutput bout = bindingOperation.getBindingOutput();
        if ( bout != null ) {
            //noinspection unchecked
            return bout.getExtensibilityElements();
        } else {
            return Collections.emptySet();            
        }
    }

    /**
     * Get a <i>MIME Multipart Related</i> container for the input parameters
     * declared for the given binding operation.
     *
     * @param bo the binding operation
     */
    public MIMEMultipartRelated getMimeMultipartRelatedInput(BindingOperation bo) {
        Collection<ExtensibilityElement> elements = getInputParameters(bo);

        // for each input parameter of the binding operation
        for (ExtensibilityElement element : elements) {
            if (element instanceof MIMEMultipartRelated) {
                return (MIMEMultipartRelated) element;
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
        Collection<ExtensibilityElement> elements = getOutputParameters(bo);

        // for each input parameter of the binding operation
        for (ExtensibilityElement element : elements) {
            if (element instanceof MIMEMultipartRelated) {
                return (MIMEMultipartRelated) element;
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
     *
     * @param url The url to use
     */
    public static String extractBaseURI(String url) {

        int lastIndexOfSlash = url.lastIndexOf('/');
        String baseURL;
        if (lastIndexOfSlash == -1) {
            baseURL = "./";
        } else {
            baseURL = url.substring(0, lastIndexOfSlash + 1);
        }

        return baseURL;
    }

    public String getUriFromPort(Port wsdlPort) throws MalformedURLException {
        if (wsdlPort == null)
            throw new IllegalArgumentException("No WSDL port was provided");
        //noinspection unchecked
        List<ExtensibilityElement> elements = wsdlPort.getExtensibilityElements();
        String uri = null;
        String soap12URI = null;
        int num = 0;
        for (ExtensibilityElement eel : elements) {
            if (eel instanceof SOAPAddress) {
                SOAPAddress sadd = (SOAPAddress) eel;
                num++;
                uri = sadd.getLocationURI();
            } else if (eel instanceof SOAP12Address) {
                SOAP12Address sadd = (SOAP12Address) eel;
                num++;
                soap12URI = sadd.getLocationURI();
            }
        }

        if (num > 1)
            logger.warning("WSDL " + getDefinition().getTargetNamespace() + " contained multiple <soap:address> elements");

        if (uri == null) {
            return soap12URI;
        }
        return uri;
    }

    /**
     * In-place adjust the port URL of this WSDL.
     *
     * @param wsdlPort The port to the the URL for
     * @param url The URL to use
     */
    public void setPortUrl(Port wsdlPort, URL url) {
        if (wsdlPort == null)
            throw new IllegalArgumentException("No WSDL port was provided");
        if (url == null)
            throw new IllegalArgumentException("No new WSDL Port URL was provided");
        //noinspection unchecked
        List<ExtensibilityElement> elements = wsdlPort.getExtensibilityElements();
        int num = 0;
        for (ExtensibilityElement eel : elements) {
            if (eel instanceof SOAPAddress) {
                SOAPAddress sadd = (SOAPAddress) eel;
                num++;
                sadd.setLocationURI(url.toString());
            }
        }

        if (num > 1)
            logger.warning("WSDL " + getDefinition().getTargetNamespace() + " contained multiple <soap:address> elements");
    }

    public String getPortUrl(Port wsdlPort) {
        if (wsdlPort == null)
            throw new IllegalArgumentException("No WSDL port was provided");
        //noinspection unchecked
        List<ExtensibilityElement> elements = wsdlPort.getExtensibilityElements();
        for (ExtensibilityElement eel : elements) {
            if (eel instanceof SOAPAddress) {
                SOAPAddress sadd = (SOAPAddress) eel;
                if (sadd.getLocationURI() != null) {
                    return sadd.getLocationURI();
                }
            }
        }
        return null;
    }

    public static interface UrlGetter {
        String get(String url) throws IOException;
    }

    /**
     * Get the schema element from the wsdl definiton.
     *
     * @param def the wsdl definition
     * @param getter The url retriever
     * @return Element the "schema" element in the wsdl
     * @throws MalformedURLException if URL format is invalide
     * @throws IOException           when error occured in reading the wsdl document
     * @throws SAXException          when error occured in parsing XML
     */
    public static Element getSchemaElement(Definition def, UrlGetter getter)
      throws IOException, SAXException {
        String baseUrlStr = def.getDocumentBaseURI();
        URL baseUrl = baseUrlStr != null ? new URL(baseUrlStr) : null;
        Element schemaElement = null;
        Import imp = null;
        //noinspection unchecked
        Map<String, List<Import>> imports = def.getImports();
        if (imports.size() > 0) {
            for (String uri : imports.keySet()) {
                List<Import> importList = imports.get(uri);
                for (Import anImport : importList) {
                    imp = anImport;
                    // check if the schema is inside the file
                    String url = imp.getLocationURI();
                    String resolvedXml;
                    resolvedXml = getter.get(new URL(baseUrl, url).toString());
                    if (resolvedXml != null) {
                        Document resultDoc = XmlUtil.stringToDocument(resolvedXml);
                        if (resultDoc != null) {
                            NodeList nodeList = resultDoc.getElementsByTagName("schema");

                            if (nodeList != null && nodeList.getLength()>0 && nodeList.item(0) != null) {
                                // should only have one
                                return (Element) nodeList.item(0);
                            }
                        }
                    }
                }
            }
        }

        // if not found
        if (imp != null && imp.getDefinition() != null) {
            schemaElement = getSchemaElement(imp.getDefinition(), getter);
        }

        return schemaElement;
    }

    public static void disableSchemaExtensions(WSDLFactory factory, WSDLReader reader) {
        if (reader.getExtensionRegistry() != null) {
            reader.setExtensionRegistry(disableSchemaExtensions(reader.getExtensionRegistry()));
        }
        else {
            reader.setExtensionRegistry(disableSchemaExtensions(factory.newPopulatedExtensionRegistry()));            
        }
    }

    public static ExtensionRegistry disableSchemaExtensions(ExtensionRegistry extensionRegistry) {
        return extensionRegistry == null ? null : new NoSchemaExtensionRegistry(extensionRegistry);
    }

    /**
     * Set the builder for WSDLFactory instances.
     *
     * @param wsdlFactoryBuilder The builder to use.
     */
    public static void setWSDLFactoryBuilder(final WSDLFactoryBuilder wsdlFactoryBuilder) {
        Wsdl.wsdlFactoryBuilder = wsdlFactoryBuilder;
    }

    /**
     * Get a WSDLFactory instance using the configured builder.
     *
     * @param writeEnabled true if a factory that supports writing is required
     * @return a WSDLFactory
     * @throws WSDLException if a factory cannot be created
     * @see #setWSDLFactoryBuilder
     */
    public static WSDLFactory getWSDLFactory(final boolean writeEnabled) throws WSDLException {
        return wsdlFactoryBuilder.getWSDLFactory(writeEnabled);
    }

    public interface WSDLFactoryBuilder {
        WSDLFactory getWSDLFactory(final boolean writeEnabled) throws WSDLException;
    }

    //- PRIVATE

    private static final long serialVersionUID = 1L;

    private static final String NS = "ns";

    private static WSDLFactoryBuilder wsdlFactoryBuilder = new WSDLFactoryBuilder() {
        public WSDLFactory getWSDLFactory(final boolean writeEnabled) throws WSDLException {
            return WSDLFactory.newInstance();
        }
    };

    private transient Logger logger = Logger.getLogger(getClass().getName());

    /**
     * All bindings by default
     */
    private int showBindings = ALL_BINDINGS;

    /**
     * The wrapped WSDL definition
     */
    private transient Definition definition;

    /**
     * wsp:Policy and PolicyReference parser.
     */
    private transient DOMPolicyReader policyReader = (DOMPolicyReader)PolicyFactory.getPolicyReader(PolicyFactory.DOM_POLICY_READER);

    /**
     * Stores top-level policies found in this WSDL.
     */
    private transient List<Policy> topLevelPolicies = null;

    /**
     * Registry for top-level policies in this WSDL
     */
    private transient PolicyRegistry policyRegistry = null;

    /**
     * Traverses all the imported definitions and invokes collect on the collector
     * for reach definition
     */
    private void collectElements(ElementCollector collector, Definition def) {
        collector.collect(def);
        //noinspection unchecked
        final Map<String, List<Import>> imports = def.getImports();
        for (List<Import> importList : imports.values()) {
            for (Import importDef : importList) {
                if (importDef.getDefinition() != null) {
                    collectElements(collector, importDef.getDefinition());
                }
            }
        }
    }

    private String getLocalName( Service service ) {
        String name = null;

        if ( service != null && service.getQName() != null ) {
            name = service.getQName().getLocalPart();
        }

        return name;
    }

    private String getLocalName( Binding binding ) {
        String name = null;

        if ( binding != null && binding.getQName() != null ) {
            name = binding.getQName().getLocalPart();
        }

        return name;
    }

    private Assertion mergePolicies(Assertion currentPolicy, List<ExtensibilityElement> extensibilityElements) throws BadPolicyReferenceException {
        for (ExtensibilityElement eel : extensibilityElements) {
            Policy p = toPolicy(eel);
            if (p == null) continue;
            currentPolicy = currentPolicy == null ? p : currentPolicy.merge(p, getPolicyRegistry());
        }
        return currentPolicy;
    }

    /** Read a policy element, canonicalizing it first to bring down any needed namespace decls. */
    private Policy readPolicySafely(Element e) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            XmlUtil.canonicalize(e, baos);
            return policyReader.readPolicy(new ByteArrayInputStream(baos.toByteArray()));
        } catch (NullPointerException npe) {
            // TODO fix this bug in policyReader
            logger.log(Level.WARNING, "Unable to read policy element: " + ExceptionUtils.getMessage(npe), npe);
            return null;
        } catch (IOException e1) {
            throw new RuntimeException(e1); // can't happen
        }
    }

    /** Read a policy reference element, canonicalizing it first to bring down any needed namespace decls. */
    private PolicyReference readPolicyReferenceSafely(Element e) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            XmlUtil.canonicalize(e, baos);
            Document d = XmlUtil.parse(new ByteArrayInputStream(baos.toByteArray()));
            Element ele = d.getDocumentElement();
            String uri = ele.getAttribute("URI");
            if (uri == null || uri.length() < 1) {
                // Work-around NPE in policy reader when reference uses wsp:URI="..." instead of just URI="..."
                uri = ele.getAttributeNS(ele.getNamespaceURI(), "URI");
                if (uri == null || uri.length() < 1) {
                    logger.warning("Ignoring policy reference with no URI attribute");
                    return null;
                }
                // Hack the element so it contains the correct attribute, without a wsp: prefix
                ele.removeAttributeNS(ele.getNamespaceURI(), "URI");
                ele.setAttribute("URI", uri);
            }
            return policyReader.readPolicyReference(ele);
        } catch (NullPointerException npe) {
            // TODO fix this bug in policyReader
            logger.log(Level.WARNING, "Unable to read policy reference element: " + ExceptionUtils.getMessage(npe), npe);
            return null;
        } catch (IOException e1) {
            throw new RuntimeException(e1); // can't happen
        } catch (SAXException e1) {
            throw new RuntimeException(e1); // can't happen
        }
    }

    /** @return a UEE if this is a wsp:Policy element, or null if it isn't. */
    private UnknownExtensibilityElement getPolicyUue(ExtensibilityElement ee) {
        if (!(ee instanceof UnknownExtensibilityElement))
            return null;

        QName qname = ee.getElementType();
        if (!("Policy".equals(qname.getLocalPart())))
            return null;
        if (!("http://schemas.xmlsoap.org/ws/2004/09/policy".equals(qname.getNamespaceURI())))
            return null;

        return (UnknownExtensibilityElement)ee;
    }

    /** @return a UEE if this is a wsp:PolicyReference element, or null if it isn't. */
    private UnknownExtensibilityElement getPolicyReferenceUue(ExtensibilityElement ee) {
        if (!(ee instanceof UnknownExtensibilityElement))
            return null;

        QName qname = ee.getElementType();
        if (!("PolicyReference".equals(qname.getLocalPart())))
            return null;
        if (!("http://schemas.xmlsoap.org/ws/2004/09/policy".equals(qname.getNamespaceURI())))
            return null;

        return (UnknownExtensibilityElement)ee;
    }

    /**
     * Write definition into given map of uris to contents 
     */
    private void writeWsdl( final Definition definition, final Map<String,String> content ) throws WSDLException {
        String uri = definition.getDocumentBaseURI();

        if ( !content.keySet().contains( uri ) ) {
            // Add this definition
            StringWriter writer = new StringWriter();
            WSDLFactory fac = getWSDLFactory(true);
            WSDLWriter wsdlWriter = fac.newWSDLWriter();
            wsdlWriter.writeWSDL( definition, writer );

            // Add Wsdl imports
            content.put( uri, writer.toString() );

            //noinspection unchecked
            Map<String, List<Import>> imports = (Map<String, List<Import>>) definition.getImports();
            for ( List<Import> importList : imports.values() ) {
                for ( Import imp : importList ) {
                    Definition importedDef = imp.getDefinition();
                    if ( importedDef != null ) {
                        writeWsdl( importedDef, content );
                    }
                }
            }
        }
    }    

    /**
     * The wrapped WSDL may not be Serializable, so we'll convert to a String.
     */
    private void writeObject( final ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();

        Map<String,String> content = new HashMap<String,String>();
        try {
            writeWsdl( definition, content );            
        } catch (WSDLException we) {
            throw new CausedIOException("Error serializing WSDL.", we);
        }

        out.writeObject( definition.getDocumentBaseURI() );
        out.writeObject( content );
    }

    /**
     * Restore the wrapped WSDL from String representation.
     */
    private void readObject( final ObjectInputStream in ) throws IOException, ClassNotFoundException {
        logger = Logger.getLogger(getClass().getName());
        in.defaultReadObject();

        String documentBaseURI = (String) in.readObject();
        //noinspection unchecked
        Map<String,String> content = (Map<String,String>) in.readObject();

        try {
            Wsdl wsdl = Wsdl.newInstance( getWSDLLocator(documentBaseURI, content, logger) );
            definition = wsdl.getDefinition();
        } catch (WSDLException we) {
            throw new CausedIOException("Error deserializing WSDL.", we);
        }
    }

    private static final class CachedDocumentResolver implements WSDLLocator {
        private final Logger logger;
        private final String uri;
        private final Map<String,String> contentByUri;
        private String lastUri = null;

        CachedDocumentResolver( final String uri, final Map<String,String> contentByUri, final Logger logger ) {
            this.uri = uri;
            this.contentByUri = contentByUri;
            this.logger = logger;
        }

        /**
         * WSDLLocator
         */
        public InputSource getBaseInputSource() {
            InputSource inputSource = new InputSource();
            inputSource.setSystemId(uri);
            inputSource.setCharacterStream(new StringReader(getDocumentByUri(uri)));
            return inputSource;
        }

        /**
         * WSDLLocator
         */
        public String getBaseURI() {
            lastUri = uri;
            return uri;
        }

        /**
         * WSDLLocator
         */
        public InputSource getImportInputSource(final String parentLocation, final String importLocation) {
            InputSource is = null;
            try {
                URI resolvedUri;
                lastUri = importLocation; // ensure set even if not valid

                if (parentLocation != null) {
                    URI base = new URI(parentLocation);
                    URI relative = new URI(importLocation);
                    resolvedUri = base.resolve(relative);
                }
                else {
                    resolvedUri = new URI(importLocation);
                }

                lastUri = resolvedUri.toString();
                String content = getDocumentByUri(lastUri);

                logger.log(Level.FINE, "Resolving WSDL uri '"+resolvedUri.toString()+"', document found '"+(content != null)+"'.");

                if (content != null) {
                    is = new InputSource();
                    is.setSystemId(lastUri);
                    is.setCharacterStream(new StringReader(content));
                }
            }
            catch (URISyntaxException use) {
                // of interest?
            }
            return is;
        }

        /**
         * WSDLLocator
         */
        public String getLatestImportURI() {
            return lastUri;
        }

        /**
         * WSDLLocator
         */
        public void close() {
        }

        /**
         * Get the document for the given uri
         */
        private String getDocumentByUri(final String uri) {
            return contentByUri.get(uri);
        }
    }

    private interface ElementCollector {
        /**
         * The implementation collects the elements of the interest fro mthe definition
         *
         * @param def the wsdl DEfinition
         */
        void collect(Definition def);
    }

    private static final class NoSchemaExtensionRegistry extends ExtensionRegistry {
        private final ExtensionRegistry delegate;

        private NoSchemaExtensionRegistry(ExtensionRegistry extensionRegistry) {
            this.delegate = extensionRegistry;
        }

        @Override
        public ExtensibilityElement createExtension(Class parentType, QName elementType) throws WSDLException {
            return delegate.createExtension(parentType, elementType);
        }

        @Override
        public Set getAllowableExtensions(Class parentType) {
            return delegate.getAllowableExtensions(parentType);
        }

        @Override
        public ExtensionDeserializer getDefaultDeserializer() {
            return delegate.getDefaultDeserializer();
        }

        @Override
        public ExtensionSerializer getDefaultSerializer() {
            return delegate.getDefaultSerializer();
        }

        @Override
        public void mapExtensionTypes(Class parentType, QName elementType, Class extensionType) {
            delegate.mapExtensionTypes(parentType, elementType, extensionType);
        }

        @Override
        public ExtensionDeserializer queryDeserializer(Class parentType, QName elementType) throws WSDLException {
            if (SchemaUtil.isSchema(elementType)) return delegate.getDefaultDeserializer();
            return delegate.queryDeserializer(parentType, elementType);
        }

        @Override
        public ExtensionSerializer querySerializer(Class parentType, QName elementType) throws WSDLException {
            if (SchemaUtil.isSchema(elementType)) return delegate.getDefaultSerializer();
            return delegate.querySerializer(parentType, elementType);
        }

        @Override
        public void registerDeserializer(Class parentType, QName elementType, ExtensionDeserializer ed) {
            delegate.registerDeserializer(parentType, elementType, ed);
        }

        @Override
        public void registerSerializer(Class parentType, QName elementType, ExtensionSerializer es) {
            delegate.registerSerializer(parentType, elementType, es);
        }

        @Override
        public void setDefaultDeserializer(ExtensionDeserializer defaultDeser) {
            if (delegate != null)
                delegate.setDefaultDeserializer(defaultDeser);
        }

        @Override
        public void setDefaultSerializer(ExtensionSerializer defaultSer) {
           if (delegate != null)
               delegate.setDefaultSerializer(defaultSer);
        }
    }
}
