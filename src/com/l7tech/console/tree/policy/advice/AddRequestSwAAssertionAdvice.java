package com.l7tech.console.tree.policy.advice;

import com.l7tech.common.wsdl.BindingInfo;
import com.l7tech.common.wsdl.BindingOperationInfo;
import com.l7tech.common.wsdl.MimePartInfo;
import com.l7tech.common.xml.SoapMessageGenerator;
import com.l7tech.common.xml.Wsdl;
import com.l7tech.common.xml.XpathEvaluator;
import com.l7tech.console.tree.policy.PolicyChange;
import com.l7tech.console.tree.policy.PolicyException;
import com.l7tech.console.util.TopComponents;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.RequestSwAAssertion;
import com.l7tech.service.PublishedService;

import javax.swing.*;
import javax.wsdl.Binding;
import javax.wsdl.BindingOperation;
import javax.wsdl.WSDLException;
import javax.wsdl.extensions.mime.MIMEContent;
import javax.wsdl.extensions.mime.MIMEMultipartRelated;
import javax.wsdl.extensions.mime.MIMEPart;
import javax.xml.soap.Name;
import javax.xml.soap.SOAPEnvelope;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPElement;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.rmi.RemoteException;
import java.net.MalformedURLException;
import java.io.IOException;

import org.xml.sax.SAXException;

/**
 * The class <code>AddRequestSwAAssertionAdvice</code> intercepts policy
 * SwA assertion add. It verifies whether the target service supports
 * the assertion (is soap).
 * <p/>
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 */
public class AddRequestSwAAssertionAdvice implements Advice {
    private static final Logger logger = Logger.getLogger(AddRequestSwAAssertionAdvice.class.getName());
    static final int DEFAULT_PART_LENGTH = 1000; // (KB)

    public AddRequestSwAAssertionAdvice() {
    }

    /**
     * Intercepts a policy change.
     *
     * @param pc The policy change.
     */
    public void proceed(PolicyChange pc) throws PolicyException {
        Assertion[] assertions = pc.getEvent().getChildren();
        if (assertions == null || assertions.length != 1 ||
          !(assertions[0] instanceof RequestSwAAssertion)) {
            throw new IllegalArgumentException();
        }
        RequestSwAAssertion swaAssertion = (RequestSwAAssertion)assertions[0];
        final PublishedService service = pc.getService();
        if (!(service.isSoap())) {
            JOptionPane.showMessageDialog(TopComponents.getInstance().getMainWindow(), "The 'SOAP Request with Attachment' assertion is not supported by non-SOAP services.");
            return;
        }

        try {
            initializeMimeMessages(service, swaAssertion);
            initializeXPath(service, swaAssertion);
        } catch (WSDLException e) {
            logger.warning("Unable to parse the WSDL of the service " + service.getName());
            throw new RuntimeException(e); // can't happen
        } catch (MalformedURLException e) {
            logger.warning("MalformedURLException caught. Unable to parse the WSDL of the service " + service.getName());
            throw new PolicyException();
        } catch (IOException e) {
            logger.warning("IOException caught. Unable to parse the WSDL of the service " + service.getName());
            throw new PolicyException();
        } catch (SAXException e) {
            logger.warning("SAXException caught. Unable to parse the WSDL of the service " + service.getName());
            throw new PolicyException();
        }

        pc.proceed();
    }


    private void initializeMimeMessages(PublishedService service, RequestSwAAssertion swaAssertion) throws WSDLException {
        Wsdl parsedWsdl = service.parsedWsdl();
        parsedWsdl.setShowBindings(Wsdl.SOAP_BINDINGS);
        Collection bindingList = parsedWsdl.getBindings();

        // for each binding in WSDL
        for (Iterator iterator = bindingList.iterator(); iterator.hasNext();) {
            Binding binding = (Binding)iterator.next();

            Collection boList = binding.getBindingOperations();
            HashMap operations = new HashMap();

            // for each operation in WSDL
            for (Iterator iterator1 = boList.iterator(); iterator1.hasNext();) {
                BindingOperation bo = (BindingOperation)iterator1.next();
                MIMEMultipartRelated multipart = parsedWsdl.getMimeMultipartRelatedInput(bo);
                if (multipart == null) {
                    continue;
                }

                HashMap partList = new HashMap();
                List parts = multipart.getMIMEParts();

                // for each MIME part of the input parameter of the operation in WSDL
                for (Iterator partsItr = parts.iterator(); partsItr.hasNext();) {

                    MIMEPart mimePart = (MIMEPart)partsItr.next();
                    Collection mimePartSubElements = parsedWsdl.getMimePartSubElements(mimePart);

                    // for each extensible part of the MIME part of the input parameter of the operation in WSDL
                    for (Iterator subElementItr = mimePartSubElements.iterator(); subElementItr.hasNext();) {
                        Object subElement = (Object)subElementItr.next();

                        if (subElement instanceof MIMEContent) {
                            MIMEContent mimeContent = (MIMEContent)subElement;

                            //concat the content type if the part alreay exists
                            MimePartInfo retrievedPart = (MimePartInfo)partList.get(mimeContent.getPart());
                            if (retrievedPart != null) {
                                retrievedPart.addContentType(mimeContent.getType());
                            } else {
                                MimePartInfo newPart = new MimePartInfo(mimeContent.getPart(), mimeContent.getType());

                                // default length 1000 Kbytes
                                newPart.setMaxLength(DEFAULT_PART_LENGTH);

                                // add the new part
                                partList.put(mimeContent.getPart(), newPart);
                            }
                        }
                    }
                }
                // create BindingOperationInfo
                BindingOperationInfo operation = new BindingOperationInfo(bo.getOperation().getName(), partList);
                operations.put(bo.getOperation().getName(), operation);
            }
            BindingInfo bindingInfo = new BindingInfo(binding.getQName().getLocalPart(), operations);
            swaAssertion.getBindings().put(bindingInfo.getBindingName(), bindingInfo);
        }
    }

    private void initializeXPath(PublishedService service, RequestSwAAssertion swaAssertion) throws WSDLException, RemoteException, MalformedURLException, IOException, SAXException {

        final Wsdl wsdl = service.parsedWsdl();
        wsdl.setShowBindings(Wsdl.SOAP_BINDINGS);
        SoapMessageGenerator sg = new SoapMessageGenerator();
        try {
            SoapMessageGenerator.Message[] soapMessages = sg.generateRequests(wsdl);

            for (int i = 0; i < soapMessages.length; i++) {
                SoapMessageGenerator.Message soapRequest = soapMessages[i];

                final SOAPEnvelope envelope = soapRequest.getSOAPMessage().getSOAPPart().getEnvelope();
                final Name envelopeName = envelope.getElementName();
                String soapEnvLocalName = envelopeName.getLocalName();
                String soapEnvNamePrefix = envelopeName.getPrefix();
                final javax.xml.soap.SOAPBody soapBody = envelope.getBody();
                final Name bodyName = soapBody.getElementName();
                String soapBodyLocalName = bodyName.getLocalName();
                String soapBodyNamePrefix = bodyName.getPrefix();

                if (soapBodyNamePrefix.length() == 0) {
                    soapBodyNamePrefix = soapEnvNamePrefix;
                }
                Iterator soapBodyElements = soapBody.getChildElements();

               // get the first element
                Object operation = soapBodyElements.next();
                if(!(operation instanceof SOAPElement))
                    throw new RuntimeException("operation must be an instance of SOAPBodyElement class");

                String operationQName = ((SOAPElement) operation).getElementName().getQualifiedName();

                Map bindings = swaAssertion.getBindings();
                Iterator bindingsItr = bindings.keySet().iterator();

                BindingInfo binding = null;
                while (bindingsItr.hasNext()) {
                    String bindingName = (String)bindingsItr.next();
                    if (bindingName.equals(soapRequest.getBinding())) {
                        binding = (BindingInfo)bindings.get(bindingName);
                        break;
                    }
                }

                BindingOperationInfo bo = null;
                if (binding != null) {
                    Iterator boItr = binding.getBindingOperations().keySet().iterator();
                    while (boItr.hasNext()) {
                        String boName = (String)boItr.next();
                        if (boName.equals(soapRequest.getOperation())) {
                            bo = (BindingOperationInfo)binding.getBindingOperations().get(boName);
                            break;
                        }
                    }
                }

                String xpathExpression = "/" + soapEnvNamePrefix +
                  ":" + soapEnvLocalName +
                  "/" + soapBodyNamePrefix +
                  ":" + soapBodyLocalName +
                  "/" + operationQName;

                if (bo != null) {
                    bo.setXpath(xpathExpression);
                }

                logger.finest("Xpath for the operation " + "\"" + soapRequest.getOperation() + "\" is " + xpathExpression);
                swaAssertion.getNamespaceMap().putAll(XpathEvaluator.getNamespaces(soapRequest.getSOAPMessage()));
            }
        } catch (SOAPException e) {
            logger.log(Level.WARNING, "Caught SAXException when retrieving xml document from the generated request", e);
        }
    }
}
