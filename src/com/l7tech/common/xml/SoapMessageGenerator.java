package com.l7tech.common.xml;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.traversal.DocumentTraversal;
import org.w3c.dom.traversal.NodeFilter;
import org.w3c.dom.traversal.NodeIterator;

import javax.wsdl.*;
import javax.wsdl.extensions.ExtensibilityElement;
import javax.wsdl.extensions.UnknownExtensibilityElement;
import javax.wsdl.extensions.soap.SOAPOperation;
import javax.xml.namespace.QName;
import javax.xml.soap.*;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.l7tech.common.util.SoapUtil;

/**
 * The class creates and array of <code>SoapRequest</code> instances
 * from the given WSDL.
 * The SOAP message generation may be customized by optionally specifying
 * the callback <code>MessageInputGenerator</code> for user supplied parameter
 * values.
 * <p/>
 * Currently does only rpc requests.
 * 
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 */
public class SoapMessageGenerator {
    private MessageInputGenerator messageInputGenerator;
    private Wsdl wsdl;

    /**
     * default constructor
     */
    public SoapMessageGenerator() {
        this(null);
    }

    /**
     * constructor specifying the message input generator The generator
     * is used ot obtain the message input values
     * 
     * @param messageInputGenerator 
     * @see MessageInputGenerator
     */
    public SoapMessageGenerator(MessageInputGenerator messageInputGenerator) {
        this.messageInputGenerator = messageInputGenerator;
    }

    /**
     * Generate the soap requests for the
     *
     * @param wsdlResource the wsdl resource name
     * @return the array of <code>SoapRequest</code> instances
     * @throws WSDLException         on error parsing the wsdl
     * @throws FileNotFoundException if wsdl cannot be found
     */
    public Message[] generateRequests(String wsdlResource)
      throws WSDLException, FileNotFoundException, SOAPException {
        if (wsdlResource == null) {
            throw new IllegalArgumentException();
        }
        ClassLoader cl = getClass().getClassLoader();
        InputStream in = cl.getResourceAsStream(wsdlResource);
        if (in == null) {
            throw new FileNotFoundException(wsdlResource);
        }
        InputStreamReader reader = new InputStreamReader(in);

        Wsdl newWsdl = Wsdl.newInstance(null, reader);
        return generateRequests(newWsdl);
    }

    /**
     * @param wsdlResource the wsdl resource name
     * @return the array of <code>SoapRequest</code> instances
     * @throws WSDLException         on error parsing the wsdl
     * @throws FileNotFoundException if wsdl cannot be found
     */
    public Message[] generateResponses(String wsdlResource)
      throws WSDLException, FileNotFoundException, SOAPException {
        if (wsdlResource == null) {
            throw new IllegalArgumentException();
        }
        ClassLoader cl = getClass().getClassLoader();
        InputStream in = cl.getResourceAsStream(wsdlResource);
        if (in == null) {
            throw new FileNotFoundException(wsdlResource);
        }
        InputStreamReader reader = new InputStreamReader(in);

        Wsdl newWsdl = Wsdl.newInstance(null, reader);
        return generateResponses(newWsdl);
    }


    /**
     * @param wsdl the parsed wsdl instance
     * @return the array of <code>SoapRequest</code> instances
     * @throws SOAPException on error generating SOAP messages
     */
    public Message[] generateRequests(Wsdl wsdl) throws SOAPException {
        this.wsdl = wsdl;
        List requests = new ArrayList();
        Iterator it = wsdl.getBindings().iterator();
        while (it.hasNext()) {
            Binding binding = (Binding)it.next();
            requests.addAll(generateMessages(binding, true));
        }
        return (Message[])requests.toArray(new Message[]{});
    }

    /**
     * @param wsdl the parsed wsdl instance
     * @return the array of <code>SoapRequest</code> instances
     * @throws SOAPException on error generating SOAP messages
     */
    public Message[] generateResponses(Wsdl wsdl) throws SOAPException {
        this.wsdl = wsdl;
        List requests = new ArrayList();
        Iterator it = wsdl.getBindings().iterator();
        while (it.hasNext()) {
            Binding binding = (Binding)it.next();
            requests.addAll(generateMessages(binding, false));
        }
        return (Message[])requests.toArray(new Message[]{});
    }


    private List generateMessages(Binding binding, boolean request)
      throws SOAPException {
        List bindingMessages = new ArrayList();
        List boperations = binding.getBindingOperations();
        for (Iterator iterator = boperations.iterator(); iterator.hasNext();) {
            BindingOperation bindingOperation = (BindingOperation)iterator.next();
            String soapAction = getSoapAction(bindingOperation);
            SOAPMessage soapMessage = null;
            if (request)
                soapMessage = generateRequestMessage(bindingOperation);
            else
                soapMessage = generateResponseMessage(bindingOperation);

            bindingMessages.add(new Message(soapMessage, soapAction, binding.getQName().getLocalPart(), bindingOperation.getName()));
        }
        return bindingMessages;
    }

    /**
     * @param operation to get the soap action for
     * @return the soap action or null if none fould
     */
    private String getSoapAction(BindingOperation operation) {
        Iterator eels = operation.getExtensibilityElements().iterator();
        ExtensibilityElement ee;
        while (eels.hasNext()) {
            ee = (ExtensibilityElement)eels.next();
            if (ee instanceof SOAPOperation) {
                SOAPOperation sop = (SOAPOperation)ee;
                return sop.getSoapActionURI();
            }
        }
        return null;
    }

    /**
     * Generate the soap request envelope and the target namespace
     *
     * @param bindingOperation
     * @return
     * @throws SOAPException
     */
    private Object[] generateEnvelope(BindingOperation bindingOperation)
      throws SOAPException {
        MessageFactory messageFactory = SoapUtil.getMessageFactory();
        SOAPMessage soapMessage = messageFactory.createMessage();
        SOAPPart soapPart = soapMessage.getSOAPPart();
        SOAPEnvelope envelope = soapPart.getEnvelope();
        String targetNameSpace = wsdl.getDefinition().getTargetNamespace();

        BindingInput bi = bindingOperation.getBindingInput();
        if (bi != null) {
            Iterator eels = bi.getExtensibilityElements().iterator();
            ExtensibilityElement ee;
            while (eels.hasNext()) {
                ee = (ExtensibilityElement)eels.next();
                if (ee instanceof javax.wsdl.extensions.soap.SOAPBody) {
                    javax.wsdl.extensions.soap.SOAPBody body = (javax.wsdl.extensions.soap.SOAPBody)ee;
                    String uri = body.getNamespaceURI();
                    if (uri != null) targetNameSpace = uri;
                    List encodingStyles = body.getEncodingStyles();
                    if (encodingStyles != null && !encodingStyles.isEmpty()) {
                        envelope.setEncodingStyle(encodingStyles.get(0).toString());
                    }
                }
            }
        }
        return new Object[]{soapMessage, targetNameSpace};
    }

    private SOAPMessage generateRequestMessage(BindingOperation bindingOperation)
      throws SOAPException {
        Object[] soapEnvelope = generateEnvelope(bindingOperation);

        SOAPMessage soapMessage = (SOAPMessage)soapEnvelope[0];
        String targetNameSpace = wsdl.getDefinition().getTargetNamespace();
        // was the different ns discovered?
        if (soapEnvelope[1] != null) {
            targetNameSpace = soapEnvelope[1].toString();
        }
        SOAPPart soapPart = soapMessage.getSOAPPart();
        SOAPEnvelope envelope = soapPart.getEnvelope();

        String bindingStyle = wsdl.getBindingStyle(bindingOperation);
        SOAPBody body = envelope.getBody();
        SOAPBodyElement bodyElement = null;

        if ("rpc".equals(bindingStyle)) {
            final Map namespaces = wsdl.getNamespaces();
            Iterator it = namespaces.keySet().iterator();
            String prefix = "ns1";
            while (it.hasNext()) {
                Object key = it.next();
                if (targetNameSpace.equals(namespaces.get(key))) {
                    prefix = key.toString();
                    break;
                }
            }
            Name operationName = envelope.createName(bindingOperation.getName(), prefix, targetNameSpace);
            bodyElement = body.addBodyElement(operationName);
        } else {
            Input input = bindingOperation.getOperation().getInput();
            if (input != null && input.getMessage() != null) { // nothing more to do here
                javax.wsdl.Message message = input.getMessage();
                processDocumentStyle(bindingOperation, envelope, message);
            }

            return soapMessage;
        }

        // default rpc processing
        Operation operation = bindingOperation.getOperation();
        Input input = operation.getInput();
        if (input == null) { // nothing more to do here
            return soapMessage;
        }
        javax.wsdl.Message message = input.getMessage();
        List parts = message.getOrderedParts(null);

        for (Iterator iterator = parts.iterator(); iterator.hasNext();) {
            Part part = (Part)iterator.next();
            String elementName = "";
            String value = "value";
            SOAPElement parameterElement = null;


            elementName = part.getName();
            Name partName = envelope.createName(elementName);
            parameterElement = bodyElement.addChildElement(partName);
            QName typeName = part.getTypeName();
            if (typeName != null) {
                String typeNameLocalPart = typeName.getLocalPart();
                String uri = typeName.getNamespaceURI();
                if (uri != null) {
                    Iterator prefixes = envelope.getNamespacePrefixes();
                    while (prefixes.hasNext()) {
                        String prefix = (String)prefixes.next();
                        String nsURI = envelope.getNamespaceURI(prefix);
                        if (nsURI.equals(typeName.getNamespaceURI())) {
                            typeNameLocalPart = prefix + ":" + typeNameLocalPart;
                        }
                    }
                }
                parameterElement.addAttribute(envelope.createName("xsi:type"), typeNameLocalPart);
                value = typeName.getLocalPart();
            }
            if (messageInputGenerator != null) {
                value = messageInputGenerator.generate(elementName,
                  bindingOperation.getName(),
                  wsdl.getDefinition());
            }
            if (value != null) {
                parameterElement.addTextNode(value);
            }
        }
        return soapMessage;
    }


    private SOAPMessage generateResponseMessage(BindingOperation bindingOperation)
      throws SOAPException {
        Object[] soapEnvelope = generateEnvelope(bindingOperation);

        SOAPMessage soapMessage = (SOAPMessage)soapEnvelope[0];
        String targetNameSpace = wsdl.getDefinition().getTargetNamespace();
        // was the different ns discovered?
        if (soapEnvelope[1] != null) {
            targetNameSpace = soapEnvelope[1].toString();
        }
        SOAPPart soapPart = soapMessage.getSOAPPart();
        SOAPEnvelope envelope = soapPart.getEnvelope();

        String bindingStyle = wsdl.getBindingStyle(bindingOperation);
        SOAPBody body = envelope.getBody();
        SOAPBodyElement bodyElement = null;


        // default rpc processing
        Operation operation = bindingOperation.getOperation();
        Output output = operation.getOutput();
        if (output == null) { // nothing more to do here
            return soapMessage;
        }
        javax.wsdl.Message message = output.getMessage();
        if (message == null) { // nothing more to do here
            return soapMessage;
        }

        if ("rpc".equals(bindingStyle)) {
            Name responseName = envelope.createName(message.getQName().getLocalPart(), "ns1", targetNameSpace);
            bodyElement = body.addBodyElement(responseName);
        } else {
            processDocumentStyle(bindingOperation, envelope, message);
            return soapMessage;
        }

        List parts = message.getOrderedParts(null);

        for (Iterator iterator = parts.iterator(); iterator.hasNext();) {
            Part part = (Part)iterator.next();
            String elementName = "";
            String value = "value";
            SOAPElement parameterElement = null;


            elementName = part.getName();
            Name partName = envelope.createName(elementName);
            parameterElement = bodyElement.addChildElement(partName);
            QName typeName = part.getTypeName();
            if (typeName != null) {
                String typeNameLocalPart = typeName.getLocalPart();
                String uri = typeName.getNamespaceURI();
                if (uri != null) {
                    Iterator prefixes = envelope.getNamespacePrefixes();
                    while (prefixes.hasNext()) {
                        String prefix = (String)prefixes.next();
                        String nsURI = envelope.getNamespaceURI(prefix);
                        if (nsURI.equals(typeName.getNamespaceURI())) {
                            typeNameLocalPart = prefix + ":" + typeNameLocalPart;
                        }
                    }
                }
                parameterElement.addAttribute(envelope.createName("xsi:type"), typeNameLocalPart);
                value = typeName.getLocalPart();
            }
            if (messageInputGenerator != null) {
                value = messageInputGenerator.generate(elementName,
                  bindingOperation.getName(),
                  wsdl.getDefinition());
            }
            if (value != null) {
                parameterElement.addTextNode(value);
            }
        }
        return soapMessage;
    }


    /**
     * Process the document style binding operation
     *
     * @param bo       the binding operation
     * @param envelope the soap envelope
     * @param message  the operation message (input or output)
     */
    private void processDocumentStyle(BindingOperation bo, SOAPEnvelope envelope, javax.wsdl.Message message)
      throws SOAPException {
        Operation operation = bo.getOperation();
        List parts = message.getOrderedParts(null);
        if (parts == null || parts.isEmpty()) return;
        Part part = (Part)parts.get(0);

        String operationElementName = "";
        QName elementQName = part.getElementName();
        String value = "value";
        SOAPElement parameterElement = null;

        if (elementQName != null) {
            operationElementName = elementQName.getLocalPart();
            String uri = elementQName.getNamespaceURI();
            String prefix = null;
            if (uri != null) {
                prefix = wsdl.getDefinition().getPrefix(uri);
            }
            Name operationName = envelope.createName(operationElementName, prefix, uri);
            SOAPBody body = envelope.getBody();

            SOAPElement bodyElement = body.addBodyElement(operationName);

            NameTypePair[] npair = getSchemaParameterElements(operationElementName);
            for (int i = 0; i < npair.length; i++) {
                NameTypePair nameTypePair = npair[i];
                String pUri = nameTypePair.nameSpaceUri;
                String pPrefix = wsdl.getDefinition().getPrefix(pUri);
                Name partName = envelope.createName(nameTypePair.name, pPrefix, pUri);
                parameterElement = bodyElement.addChildElement(partName);
                if (nameTypePair.type != null) {
                    value = nameTypePair.type;
                    if (value != null) {
                        int pos = value.indexOf(':');
                        if (pos != -1) {
                            value = value.substring(pos + 1);
                        }
                    }
                }
                if (messageInputGenerator != null) {
                    value = messageInputGenerator.generate(nameTypePair.name, operationElementName, wsdl.getDefinition());
                }
                if (value != null) {
                    parameterElement.addTextNode(value);
                }

            }
        }
    }

    /**
     * extract the parameter names from schema (wsdl Types section)
     *
     * @param elementName the element name to search for
     * @return the array of name, type pairs or empty array if not found
     */
    private NameTypePair[] getSchemaParameterElements(String elementName) {
        Types types = wsdl.getTypes();
        if (types == null) return new NameTypePair[]{};
        List l = types.getExtensibilityElements();
        if (l == null || l.isEmpty()) return new NameTypePair[]{};

        Iterator iter = l.iterator();
        Element elem = null;
        String targetNamespace = wsdl.getDefinition().getTargetNamespace();
        while (iter.hasNext()) {
            ExtensibilityElement el = (ExtensibilityElement)iter.next();
            if (el.getElementType().getLocalPart().equals("schema")) {
                UnknownExtensibilityElement uee = (UnknownExtensibilityElement)el;
                elem = uee.getElement();
                String tns = elem.getAttribute("targetNamespace");
                if (tns != null) {
                    targetNamespace = tns;
                }
                NodeList nl = elem.getChildNodes();
                final int length = nl.getLength();
                for (int i = 0; i < length; i++) {
                    Node child = nl.item(i);
                    if (!(child instanceof Element)) continue;
                    Element element = (Element)child;
                    if ("element".equals(element.getLocalName())) {
                        if (!elementName.equals(element.getAttribute("name"))) {
                            continue;
                        }
                        Document doc = elem.getOwnerDocument();
                        DocumentTraversal traversable = (DocumentTraversal)doc; // tragic! em20040204
                        NodeIterator nodeIterator =
                          traversable.createNodeIterator(child,
                            NodeFilter.SHOW_ELEMENT, new LocalNameFilter("element"), true);
                        List elements = new ArrayList();
                        Node n = nodeIterator.nextNode();
                        if (n == null) return new NameTypePair[]{};
                        n = nodeIterator.nextNode();
                        for (; n != null; n = nodeIterator.nextNode()) {
                            Element e = (Element)n;
                            String nameAttribute = e.getAttribute("name");
                            if (nameAttribute != null && !"".equals(nameAttribute)) {
                                elements.add(new NameTypePair(nameAttribute, targetNamespace, e.getAttribute("type")));
                            }
                        }
                        return (NameTypePair[])elements.toArray(new NameTypePair[]{});
                    }
                }
            }
        }
        return new NameTypePair[]{};
    }

    private class LocalNameFilter implements NodeFilter {
        String nodeName;

        public LocalNameFilter(String attributeName) {
            this.nodeName = attributeName;
        }

        /**
         * Test whether a specified node is visible in the logical view of a
         * <code>TreeWalker</code>
         *
         * @param n The node to check to see if it passes the filter or not.
         * @return A constant to determine whether the node is accepted,
         *         rejected, or skipped, as defined above.
         */
        public short acceptNode(Node n) {
            Element element = (Element)n; // blow on cast
            if (nodeName.equals(n.getLocalName())) return FILTER_ACCEPT;

            return FILTER_REJECT;
        }

    }

    /**
     * internal helper class that holds name type pair for schema based
     * wsdl4j does not give much support for it.
     */
    private static class NameTypePair {
        String name;
        String nameSpaceUri;
        String type;

        public NameTypePair(String name, String uri, String type) {
            this.name = name;
            this.nameSpaceUri = uri;
            this.type = type;
        }

        /**
         * @return a string representation of the name type pair.
         */
        public String toString() {
            return "[ name = " + name + ", type = " + type + " ]";
        }
    }

    /**
     * Represents the soap message and the soap action
     */
    public static class Message {
        private final SOAPMessage soapMessage;
        private final String soapAction;
        private String operation;
        private String binding;

        public Message(SOAPMessage sm, String sa, String bi, String op) {
            if (sm == null) {
                throw new IllegalArgumentException();
            }
            soapMessage = sm;
            soapAction = sa;
            binding = bi;
            operation = op;
        }

        public String getSOAPAction() {
            return soapAction;
        }

        public SOAPMessage getSOAPMessage() {
            return soapMessage;
        }

        public String getOperation() {
            return operation;
        }

        public String getBinding() {
            return binding;
        }

        public String toString() {
            StringBuffer sb = new StringBuffer("[");
            if (operation != null) {
                sb.append(" SOAP operation " + operation);
            }
            sb.append("]");
            if (soapMessage != null) {
                sb.append("the message is\n");
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                try {
                    soapMessage.writeTo(bos);
                    sb.append(bos.toString());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            return sb.toString();
        }
    }

    /**
     * Implementations provide the messgae gener
     */
    public interface MessageInputGenerator {
        /**
         * @param messagePartName the message part name
         * @param operationName   the operation the part name belongs to
         * @param definition      the wsdl definition (context)
         * @return the user provided part value as a string
         */
        String generate(String messagePartName, String operationName, Definition definition);
    }
}
