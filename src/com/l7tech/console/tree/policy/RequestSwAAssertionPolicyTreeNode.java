package com.l7tech.console.tree.policy;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.RequestSwAAssertion;
import com.l7tech.console.action.RequestSwAAssertionPropertiesAction;
import com.l7tech.console.util.TopComponents;
import com.l7tech.console.panels.WorkSpacePanel;
import com.l7tech.console.poleditor.PolicyEditorPanel;
import com.l7tech.console.tree.ServiceNode;
import com.l7tech.common.xml.Wsdl;
import com.l7tech.common.xml.SoapMessageGenerator;
import com.l7tech.common.xml.XpathEvaluator;
import com.l7tech.common.wsdl.MimePartInfo;
import com.l7tech.common.wsdl.BindingOperationInfo;
import com.l7tech.common.wsdl.BindingInfo;
import com.l7tech.objectmodel.FindException;

import javax.swing.*;
import javax.wsdl.Binding;
import javax.wsdl.BindingOperation;
import javax.wsdl.WSDLException;
import javax.wsdl.extensions.mime.MIMEMultipartRelated;
import javax.wsdl.extensions.mime.MIMEPart;
import javax.wsdl.extensions.mime.MIMEContent;
import javax.wsdl.extensions.soap.SOAPBody;
import javax.xml.soap.SOAPException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.rmi.RemoteException;

import org.apache.axis.message.SOAPBodyElement;

/**
 * <p> Copyright (C) 2004 Layer 7 Technologies Inc.</p>
 * <p> @author fpang </p>
 * $Id$
 */
public class RequestSwAAssertionPolicyTreeNode extends LeafAssertionTreeNode {
    static final Logger log = Logger.getLogger(RequestSwAAssertionPropertiesAction.class.getName());
    private SoapMessageGenerator.Message[] soapMessages;
    private ServiceNode serviceNode;
    private Wsdl serviceWsdl = null;
    private Map bindings = new HashMap();
    private Map namespaces = new HashMap();

    SoapMessageGenerator.Message soapRequest;

    public RequestSwAAssertionPolicyTreeNode(Assertion assertion) {
        super(assertion);
        initialise();
    }

    private void initialise() {
        WorkSpacePanel currentWorkSpace = TopComponents.getInstance().getCurrentWorkspace();
        JComponent currentPanel = currentWorkSpace.getComponent();
        if(currentPanel == null || !(currentPanel instanceof PolicyEditorPanel)) {
            logger.warning("Internal error: current workspace is not a PolicyEditorPanel instance");
        } else {
            serviceNode = ((PolicyEditorPanel)currentPanel).getServiceNode();
            try {
                if (!(serviceNode.getPublishedService().isSoap())) {
                    JOptionPane.showMessageDialog(null, "This assertion is not supported by non-soap services.");
                } else {
                    if(!(getUserObject() instanceof RequestSwAAssertion)) throw new RuntimeException("assertion must be RequestSwAAssertion");
                    RequestSwAAssertion swaAssertion = (RequestSwAAssertion) getUserObject();
                    loadMIMEPartsInfoFromWSDL();
                    initializeXPath();
                    swaAssertion.setBindings(bindings);
                    swaAssertion.setNamespaceMap(namespaces);
                }
            } catch (RemoteException e) {
                log.log(Level.INFO, "Error getting Published Service", e);
            } catch (FindException e) {
                log.log(Level.INFO, "Error getting Published Service", e);
            }
        }
    }
    public String getName() {
        return "SOAP Request with Attachment";
    }

    protected String iconResource(boolean open) {
        return "com/l7tech/console/resources/xmlencryption.gif";
    }

        /**
     * Get the set of actions associated with this node.
     * This may be used e.g. in constructing a context menu.
     *
     * @return actions appropriate to the node
     */
    public Action[] getActions() {
        java.util.List list = new ArrayList();
        Action a = new RequestSwAAssertionPropertiesAction(this);
        list.add(a);
        list.addAll(Arrays.asList(super.getActions()));
        return (Action[])list.toArray(new Action[]{});
    }

    /**
     * Gets the default action for this node.
     *
     * @return <code>null</code> indicating there should be none default action
     */
    public Action getPreferredAction() {
        return new RequestSwAAssertionPropertiesAction(this);
    }

    /**
     * Test if the node can be deleted. Default is <code>true</code>
     *
     * @return true if the node can be deleted, false otherwise
     */
    public boolean canDelete() {
        return true;
    }

    private void loadMIMEPartsInfoFromWSDL() {
        try {
            Wsdl parsedWsdl = serviceNode.getPublishedService().parsedWsdl();

            Collection bindingList = parsedWsdl.getBindings();

            // for each binding in WSDL
            for (Iterator iterator = bindingList.iterator(); iterator.hasNext();) {
                Binding binding = (Binding) iterator.next();

                //todo: should filter out non-SOAP binding
                Collection boList = binding.getBindingOperations();
                HashMap operations = new HashMap();

                // for each operation in WSDL
                for (Iterator iterator1 = boList.iterator(); iterator1.hasNext();) {
                    BindingOperation bo = (BindingOperation) iterator1.next();

                    HashMap partList = new HashMap();
                    Collection elements = parsedWsdl.getInputParameters(bo);

                    // for each input parameter of the operation in WSDL
                    for (Iterator itr = elements.iterator(); itr.hasNext();) {

                        Object o = (Object) itr.next();
                        if (o instanceof MIMEMultipartRelated) {

                            MIMEMultipartRelated multipart = (MIMEMultipartRelated) o;

                            List parts = multipart.getMIMEParts();

                            // for each MIME part of the input parameter of the operation in WSDL
                            for (Iterator partsItr = parts.iterator(); partsItr.hasNext();) {

                                MIMEPart mimePart = (MIMEPart) partsItr.next();
                                Collection mimePartSubElements = parsedWsdl.getMimePartSubElements(mimePart);

                                // for each extensible part of the MIME part of the input parameter of the operation in WSDL
                                for (Iterator subElementItr = mimePartSubElements.iterator(); subElementItr.hasNext();) {
                                    Object subElement = (Object) subElementItr.next();

                                    if (subElement instanceof MIMEContent) {
                                        MIMEContent mimeContent = (MIMEContent) subElement;

                                        //concat the content type if the part alreay exists
                                        MimePartInfo retrievedPart = (MimePartInfo) partList.get(mimeContent.getPart());
                                        if (retrievedPart != null) {
                                            retrievedPart.addContentType(mimeContent.getType());
                                        } else {
                                            MimePartInfo newPart = new MimePartInfo(mimeContent.getPart(), mimeContent.getType());

                                            // default length 1000 Kbytes
                                            newPart.setMaxLength(1000);

                                            // add the new part
                                            partList.put(mimeContent.getPart(), newPart);
                                        }

                                        // add the new part
                                    } else if (subElement instanceof SOAPBody) {
                                        // don't care about soapPart for now
                                        //SOAPBody soapBody = (SOAPBody) subElement;
                                    }
                                }
                            }
                        }
                        // create BindingOperationInfo
                        BindingOperationInfo operation = new BindingOperationInfo(bo.getOperation().getName(), partList);
                        operations.put(bo.getOperation().getName(), operation);
                    }
                }
                BindingInfo bindingInfo = new BindingInfo(binding.getQName().getLocalPart(), operations);
                bindings.put(bindingInfo.getBindingName(), bindingInfo);
            }

        } catch (FindException e) {
            logger.warning("The service not found: " + serviceNode.getName());
        } catch (RemoteException re) {
            logger.severe("Remote exception");
        } catch (WSDLException e) {
            logger.warning("Unable to retrieve parse the WSDL of the service " + serviceNode.getName());
        }
    }

    private void initializeXPath() {

        if(bindings == null) throw new IllegalStateException("bindings is NULL");

        getServiceWsdl().setShowBindings(Wsdl.SOAP_BINDINGS);
        SoapMessageGenerator sg = new SoapMessageGenerator();
        try {
            soapMessages = sg.generateRequests(getServiceWsdl());

            //initializeBlankMessage(soapMessages[0]);
            for (int i = 0; i < soapMessages.length; i++) {
                soapRequest = soapMessages[i];

                String soapEnvLocalName = soapRequest.getSOAPMessage().getSOAPPart().getEnvelope().getElementName().getLocalName();
                String soapEnvNamePrefix = soapRequest.getSOAPMessage().getSOAPPart().getEnvelope().getElementName().getPrefix();
                String soapBodyLocalName = soapRequest.getSOAPMessage().getSOAPPart().getEnvelope().getBody().getElementName().getLocalName();
                String soapBodyNamePrefix = soapRequest.getSOAPMessage().getSOAPPart().getEnvelope().getBody().getElementName().getPrefix();

                if(soapBodyNamePrefix.length() == 0) {
                    soapBodyNamePrefix = soapEnvNamePrefix;
                }
                Iterator soapBodyElements = soapRequest.getSOAPMessage().getSOAPPart().getEnvelope().getBody().getChildElements();

                // get the first element
                SOAPBodyElement operation = (SOAPBodyElement) soapBodyElements.next();
                String operationName = operation.getName();
                String operationPrefix = operation.getPrefix();

                Iterator bindingsItr = bindings.keySet().iterator();

                BindingInfo binding = null;
                while(bindingsItr.hasNext()) {
                    String bindingName = (String) bindingsItr.next();
                    if(bindingName.equals(soapRequest.getBinding())) {
                        binding = (BindingInfo) bindings.get(bindingName);
                        break;
                    }
                }

                BindingOperationInfo bo = null;
                if(binding != null) {
                    Iterator boItr = binding.getBindingOperations().keySet().iterator();
                    while(boItr.hasNext()) {
                        String boName = (String) boItr.next();
                        if(boName.equals(soapRequest.getOperation())) {
                            bo = (BindingOperationInfo) binding.getBindingOperations().get(boName);
                            break;
                        }
                    }
                }

                String xpathExpression = "/" + soapEnvNamePrefix +
                        ":" + soapEnvLocalName +
                        "/" + soapBodyNamePrefix +
                        ":" + soapBodyLocalName +
                        "/" + operationPrefix +
                        ":" + operationName;

                if(bo != null) {
                    bo.setXpath(xpathExpression);
                }

                logger.finest("Xpath for the operation " + "\"" + soapRequest.getOperation() + "\" is " + xpathExpression);

                namespaces.putAll(XpathEvaluator.getNamespaces(soapRequest.getSOAPMessage()));
            }
        } catch (SOAPException e) {
            logger.log(Level.WARNING, "Caught SAXException when retrieving xml document from the generated request", e);
        }
    }

    private SoapMessageGenerator.Message forOperation(BindingOperation bop) {
        String opName = bop.getOperation().getName();
        Binding binding = serviceWsdl.getBinding(bop);
        if (binding == null) {
            throw new IllegalArgumentException("Bindiong operation without binding " + opName);
        }
        String bindingName = binding.getQName().getLocalPart();

        for (int i = 0; i < soapMessages.length; i++) {
            SoapMessageGenerator.Message soapRequest = soapMessages[i];
            if (opName.equals(soapRequest.getOperation()) &&
                    bindingName.equals(soapRequest.getBinding())) {
                return soapRequest;
            }
        }
        return null;
    }

    private Wsdl getServiceWsdl() {
        if(serviceWsdl == null) {
            try {
                serviceWsdl = serviceNode.getPublishedService().parsedWsdl();
            } catch (Exception e) {
                throw new RuntimeException("Unable to parse the service WSDL " + serviceNode.getName(), e);
            }
        }
        return serviceWsdl;
    }
}
