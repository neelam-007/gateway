package com.l7tech.common.xml;

import javax.wsdl.*;
import javax.wsdl.extensions.ExtensibilityElement;
import javax.wsdl.extensions.soap.SOAPOperation;
import javax.xml.soap.*;
import java.io.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

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
public class SoapRequestGenerator {
    private MessageInputGenerator messageInputGenerator;
    private Wsdl wsdl;

    /**
     * default constructor
     */
    public SoapRequestGenerator() {
        this(null);
    }

    /**
     * constructor specifying the message input generator The generator
     * is used ot obtain the message input values
     * 
     * @param messageInputGenerator 
     * @see MessageInputGenerator
     */
    public SoapRequestGenerator(MessageInputGenerator messageInputGenerator) {
        this.messageInputGenerator = messageInputGenerator;
    }

    /**
     * @param wsdlResource the wsdl resource name
     * @return the array of <code>SoapRequest</code> instances
     *
     * @throws WSDLException on error parsing the wsdl
     * @throws FileNotFoundException if wsdl cannot be found
     */
    public SOAPRequest[] generate(String wsdlResource)
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
        return generate(newWsdl);
    }

    /**
      * @param wsdl the parsed wsdl instance
      * @return the array of <code>SoapRequest</code> instances
      *
      * @throws SOAPException on error generating SOAP messages
      */
     public SOAPRequest[] generate(Wsdl wsdl) throws SOAPException {
        this.wsdl = wsdl;
        List requests = new ArrayList();
        Iterator it = wsdl.getBindings().iterator();
        while (it.hasNext()) {
            Binding binding = (Binding)it.next();
            requests.addAll(generate(binding));
        }
        return (SOAPRequest[])requests.toArray(new SOAPRequest[]{});
    }


    private List generate(Binding binding)
      throws SOAPException {
        List bindingMessages = new ArrayList();
        List boperations = binding.getBindingOperations();
        for (Iterator iterator = boperations.iterator(); iterator.hasNext();) {
            BindingOperation bindingOperation = (BindingOperation)iterator.next();
            String soapAction = getSoapAction(bindingOperation);
            SOAPMessage soapMessage = generate(bindingOperation);
            bindingMessages.add(new SOAPRequest(soapMessage, soapAction, bindingOperation.getName()));
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

    private SOAPMessage generate(BindingOperation bindingOperation)
      throws SOAPException {
        MessageFactory messageFactory = MessageFactory.newInstance();
        SOAPMessage soapMessage = messageFactory.createMessage();
        SOAPPart soapPart = soapMessage.getSOAPPart();
        SOAPEnvelope envelope = soapPart.getEnvelope();

        String ns = wsdl.getDefinition().getTargetNamespace();
        BindingInput bi = bindingOperation.getBindingInput();
        if (bi != null) {
            Iterator eels = bi.getExtensibilityElements().iterator();
            ExtensibilityElement ee;
            while (eels.hasNext()) {
                ee = (ExtensibilityElement)eels.next();
                if (ee instanceof javax.wsdl.extensions.soap.SOAPBody) {
                    javax.wsdl.extensions.soap.SOAPBody body = (javax.wsdl.extensions.soap.SOAPBody)ee;
                    String uri = body.getNamespaceURI();
                    if (uri != null) ns = uri;
                }
            }
        }

        Name name = envelope.createName(bindingOperation.getName(), "ns1", ns);
        SOAPBody body = envelope.getBody();
        SOAPBodyElement bodyElement = body.addBodyElement(name);


        Operation operation = bindingOperation.getOperation();
        Input input = operation.getInput();
        if (input == null) { // nothing more to do here
            return soapMessage;
        }
        Message message = input.getMessage();
        List parts = message.getOrderedParts(null);

        for (Iterator iterator = parts.iterator(); iterator.hasNext();) {
            Part part = (Part)iterator.next();
            Name partName = envelope.createName(part.getName());
            SOAPElement partElement = bodyElement.addChildElement(partName);
            if (messageInputGenerator != null) {
                String value = messageInputGenerator.generate(partName.getLocalName(),
                                                              bindingOperation.getName(),
                                                              wsdl.getDefinition());
                if (value !=null){
                    partElement.addTextNode(value);
                }
            }
        }
        return soapMessage;
    }

    /**
     * Represents the soap message and the soap action
     */
    public static class SOAPRequest {
        private final SOAPMessage soapMessage;
        private final String soapAction;
        private Object soapOperation;

        public SOAPRequest(SOAPMessage sm, String sa, String op) {
            if (sm == null) {
                throw new IllegalArgumentException();
            }
            soapMessage = sm;
            soapAction = sa;
            soapOperation = op;
        }

        public String getSOAPAction() {
            return soapAction;
        }

        public SOAPMessage getSOAPMessage() {
            return soapMessage;
        }

        public Object getSOAPOperation() {
            return soapOperation;
        }

        public String toString() {
            StringBuffer sb = new StringBuffer("[");
            boolean coma = false;
            if (soapOperation !=null) {
                sb.append(" SOAP operation "+soapOperation);
                coma = true;
            }
            sb.append("]");
            if (soapMessage !=null) {
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
         *
         * @param messagePartName the message part name
         * @param operationName the operation the part name belongs to
         * @param definition the wsdl definition (context)
         * @return the user provided part value as a string
         */
        String generate(String messagePartName, String operationName, Definition definition);
    }
}
