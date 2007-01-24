package com.l7tech.console.panels;

import com.l7tech.service.PublishedService;
import com.l7tech.console.action.Actions;
import com.l7tech.common.gui.util.Utilities;
import com.l7tech.common.protocol.SecureSpanConstants;
import com.japisoft.xmlpad.XMLContainer;
import com.japisoft.xmlpad.UIAccessibility;
import com.japisoft.xmlpad.PopupModel;
import com.japisoft.xmlpad.action.ActionModel;
import com.japisoft.xmlpad.editor.XMLEditor;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.KeyListener;
import java.awt.event.KeyEvent;
import java.util.Set;
import java.util.HashSet;
import java.net.URL;
import java.net.MalformedURLException;

/**
 * SSM Dialog for editing properties of a PublishedService object.
 * <p/>
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Jan 22, 2007<br/>
 */
public class ServicePropertiesDialog extends JDialog {
    private PublishedService subject;
    private XMLContainer xmlContainer;
    private boolean wasoked = false;
    private JPanel mainPanel;
    private JTabbedPane tabbedPane1;
    private JTextField nameField;
    private JRadioButton noURIRadio;
    private JRadioButton customURIRadio;
    private JTextField uriField;
    private JLabel routingURL;
    private JCheckBox getCheck;
    private JCheckBox putCheck;
    private JCheckBox postCheck;
    private JCheckBox deleteCheck;
    private JPanel wsdlPanel;
    private JButton resetWSDLButton;
    private JButton helpButton;
    private JButton okButton;
    private JButton cancelButton;
    private JRadioButton enableRadio;
    private JRadioButton diableRadio;
    private String ssgURL;

    public ServicePropertiesDialog(Frame owner, PublishedService svc) {
        super(owner, true);
        subject = svc;
        initialize();
    }

    private void initialize() {
        setContentPane(mainPanel);
        setTitle("Published Service Properties");

        // set initial data
        nameField.setText(subject.getName());
        if (subject.isDisabled()) {
            diableRadio.setSelected(true);
        } else {
            enableRadio.setSelected(true);
        }
        //ssgURL = TopComponents.getInstance().ssgURL();
        ssgURL = "ssg.franco.net";
        if (!ssgURL.startsWith("http://")) {
            ssgURL = "http://" + ssgURL;
        }
        int pos = ssgURL.lastIndexOf(':');
        if (pos > 4) {
            ssgURL = ssgURL.substring(0, pos);
            // todo, we need to be able to query gateway to get port instead of assuming default
            ssgURL = ssgURL + ":8080";
        } else {
            if (ssgURL.endsWith("/") || ssgURL.endsWith("\\")) {
                // todo, we need to be able to query gateway to get port instead of assuming default
                ssgURL = ssgURL.substring(0, ssgURL.length()-1) + ":8080";
            } else {
                // todo, we need to be able to query gateway to get port instead of assuming default
                ssgURL = ssgURL + ":8080";
            }
        }
        String existinguri = subject.getRoutingUri();
        if (existinguri == null) {
            noURIRadio.setSelected(true);
            customURIRadio.setSelected(false);
            uriField.setEnabled(false);
        } else {
            noURIRadio.setSelected(false);
            customURIRadio.setSelected(true);
            uriField.setText(existinguri);
        }
        ActionListener toggleurifield = new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                if (noURIRadio.isSelected()) {
                    uriField.setEnabled(false);
                } else {
                    uriField.setEnabled(true);
                }
            }
        };
        noURIRadio.addActionListener(toggleurifield);
        customURIRadio.addActionListener(toggleurifield);
        Set<String> methods = subject.getHttpMethods();
        if (methods.contains("GET")) {
            getCheck.setSelected(true);
        }
        if (methods.contains("PUT")) {
            putCheck.setSelected(true);
        }
        if (methods.contains("POST")) {
            postCheck.setSelected(true);
        }
        if (methods.contains("DELETE")) {
            deleteCheck.setSelected(true);
        }

        if (!subject.isSoap()) {
            tabbedPane1.setEnabledAt(2, false);
            noURIRadio.setEnabled(false);
            customURIRadio.setSelected(true);
        } else {
            xmlContainer = new XMLContainer(true);
            final UIAccessibility uiAccessibility = xmlContainer.getUIAccessibility();
            XMLEditor editor = uiAccessibility.getEditor();
            editor.setText(subject.getWsdlXml());
            Action reformatAction = ActionModel.getActionByName(ActionModel.FORMAT_ACTION);
            reformatAction.actionPerformed(null);
            uiAccessibility.setTreeAvailable(false);
            uiAccessibility.setTreeToolBarAvailable(false);
            xmlContainer.setEditable(false);
            uiAccessibility.setToolBarAvailable(false);
            xmlContainer.setStatusBarAvailable(false);
            PopupModel popupModel = xmlContainer.getPopupModel();
            // remove the unwanted actions
            popupModel.removeAction(ActionModel.getActionByName(ActionModel.PARSE_ACTION));
            popupModel.removeAction(ActionModel.getActionByName(ActionModel.FORMAT_ACTION));
            popupModel.removeAction(ActionModel.getActionByName(ActionModel.LOAD_ACTION));
            popupModel.removeAction(ActionModel.getActionByName(ActionModel.SAVE_ACTION));
            popupModel.removeAction(ActionModel.getActionByName(ActionModel.SAVEAS_ACTION));
            popupModel.removeAction(ActionModel.getActionByName(ActionModel.NEW_ACTION));
            popupModel.removeAction(ActionModel.getActionByName(ActionModel.INSERT_ACTION));
            popupModel.removeAction(ActionModel.getActionByName(ActionModel.COMMENT_ACTION));
            popupModel.removeAction(ActionModel.getActionByName(ActionModel.TREE_SELECTNODE_ACTION));
            popupModel.removeAction(ActionModel.getActionByName(ActionModel.TREE_COMMENTNODE_ACTION));
            popupModel.removeAction(ActionModel.getActionByName(ActionModel.TREE_COPYNODE_ACTION));
            popupModel.removeAction(ActionModel.getActionByName(ActionModel.TREE_CUTNODE_ACTION));
            popupModel.removeAction(ActionModel.getActionByName(ActionModel.TREE_EDITNODE_ACTION));
            popupModel.removeAction(ActionModel.getActionByName(ActionModel.TREE_CLEANHISTORY_ACTION));
            popupModel.removeAction(ActionModel.getActionByName(ActionModel.TREE_ADDHISTORY_ACTION));
            popupModel.removeAction(ActionModel.getActionByName(ActionModel.TREE_PREVIOUS_ACTION));
            popupModel.removeAction(ActionModel.getActionByName(ActionModel.TREE_NEXT_ACTION));
            boolean lastWasSeparator = true; // remove trailing separator
            for (int i=popupModel.size()-1; i>=0; i--) {
                boolean isSeparator = popupModel.isSeparator(i);
                if (isSeparator && (i==0 || lastWasSeparator)) {
                    popupModel.removeSeparator(i);
                } else {
                    lastWasSeparator = isSeparator;
                }
            }
            wsdlPanel.setLayout(new BorderLayout());
            wsdlPanel.add(xmlContainer.getView(), BorderLayout.CENTER);
        }
        // event handlers
        okButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                ok();
            }
        });

        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                cancel();
            }
        });

        helpButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                help();
            }
        });

        uriField.addKeyListener(new KeyListener() {
            public void keyPressed(KeyEvent e) {}
            public void keyReleased(KeyEvent e) {
                updateURL();
            }
            public void keyTyped(KeyEvent e) {}
        });

        noURIRadio.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent actionEvent) {
                updateURL();
            }
        });
        customURIRadio.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent actionEvent) {
                updateURL();
            }
        });

        Utilities.setEscAction(this, new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                cancel();
            }
        });

        Utilities.setEnterAction(this, new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                ok();
            }
        });

        // todo, wsdl reset thing (see FeedNewWSDLToPublishedServiceAction.java)

        updateURL();
    }

    private void help() {
        Actions.invokeHelp(this);
    }

    private void cancel() {
        this.dispose();
    }

    public boolean wasOKed() {
        return wasoked;
    }

    private void ok() {

        // validate the name
        String name = nameField.getText();
        if (name == null || name.length() < 1) {
            JOptionPane.showMessageDialog(this, "The service must be given a name");
            return;
        }
        // validate the uri
        String newURI = null;
        if (customURIRadio.isSelected()) {
            newURI = uriField.getText();
            // remove leading '/'s
            if (newURI != null) {
                newURI = newURI.trim();
                while (newURI.startsWith("//")) {
                    newURI = newURI.substring(1);
                }
                if (!newURI.startsWith("/")) newURI = "/" + newURI;
                uriField.setText(newURI);
            }
            try {
                new URL(ssgURL + newURI);
            } catch (MalformedURLException e) {
                JOptionPane.showMessageDialog(this, ssgURL + newURI + " is not a valid URL");
                return;
            }
        }

        if (!subject.isSoap()) {
            if (newURI == null || newURI.length() <= 0 || newURI.equals("/")) { // non-soap service cannot have null routing uri
                JOptionPane.showMessageDialog(this, "Cannot set empty uri on non-soap service");
                return;
            } else if (newURI.startsWith(SecureSpanConstants.SSG_RESERVEDURI_PREFIX)) {
                JOptionPane.showMessageDialog(this, "URI cannot start with " + SecureSpanConstants.SSG_RESERVEDURI_PREFIX);
                return;
            }
        } else {
            if (newURI != null && newURI.length() > 0 && newURI.startsWith(SecureSpanConstants.SSG_RESERVEDURI_PREFIX)) {
                JOptionPane.showMessageDialog(this, "URI cannot start with " + SecureSpanConstants.SSG_RESERVEDURI_PREFIX);
                return;
            }
        }

        // set the new data into the edited subject
        subject.setName(name);
        subject.setDisabled(!enableRadio.isSelected());
        subject.setRoutingUri(newURI);
        Set<String> methods = new HashSet<String>();
        if (getCheck.isSelected()) {
            methods.add("GET");
        }
        if (putCheck.isSelected()) {
            methods.add("PUT");
        }
        if (postCheck.isSelected()) {
            methods.add("POST");
        }
        if (deleteCheck.isSelected()) {
            methods.add("DELETE");
        }
        subject.setHttpMethods(methods);

        wasoked = true;
        cancel();
    }

    private void updateURL() {
        String currentValue = null;
        if (customURIRadio.isSelected()) {
            currentValue = uriField.getText();
        }
        String urlvalue;
        if (currentValue == null || currentValue.length() < 1) {
            urlvalue = ssgURL + "/ssg/soap";
        } else {
            if (currentValue.startsWith("/")) {
                urlvalue = ssgURL + currentValue;
            } else {
                urlvalue = ssgURL + "/" + currentValue;
            }
        }

        routingURL.setText("<html><a href=\"" + urlvalue + "\">" + urlvalue + "</a></html>");
    }

    /* todo, erase this main when dev testing is complete
    public static void main(String[] args) {
        PublishedService svc = new PublishedService();
        svc.setName("flows");
        svc.setSoap(false);
        svc.setWsdlXml("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<wsdl:definitions targetNamespace=\"http://www.csapi.org/schema/parlayx/mm/send/v1_0/local\" xmlns:apachesoap=\"http://xml.apache.org/xml-soap\" xmlns:impl=\"http://www.csapi.org/schema/parlayx/mm/send/v1_0/local\" xmlns:intf=\"http://www.csapi.org/schema/parlayx/mm/send/v1_0/local\" xmlns:tns1=\"http://www.csapi.org/schema/parlayx/common/v1_0\" xmlns:tns2=\"http://www.csapi.org/schema/parlayx/mm/v1_0\" xmlns:wsdl=\"http://schemas.xmlsoap.org/wsdl/\" xmlns:wsdlsoap=\"http://schemas.xmlsoap.org/wsdl/soap/\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\">\n" +
                "<!--WSDL created by Apache Axis version: 1.2RC3\n" +
                "Built on Feb 28, 2005 (10:15:14 EST)-->\n" +
                " <wsdl:types>\n" +
                "  <schema elementFormDefault=\"qualified\" targetNamespace=\"http://www.csapi.org/schema/parlayx/mm/send/v1_0/local\" xmlns=\"http://www.w3.org/2001/XMLSchema\">\n" +
                "   <import namespace=\"http://www.csapi.org/schema/parlayx/common/v1_0\"/>\n" +
                "   <import namespace=\"http://www.csapi.org/schema/parlayx/mm/v1_0\"/>\n" +
                "   <element name=\"sendMessage\">\n" +
                "    <complexType>\n" +
                "     <sequence>\n" +
                "      <element name=\"destinationAddress\" type=\"tns1:ArrayOfEndUserIdentifier\"/>\n" +
                "      <element name=\"senderAddress\" type=\"xsd:string\"/>\n" +
                "      <element name=\"subject\" type=\"xsd:string\"/>\n" +
                "      <element name=\"priority\" type=\"tns2:MessagePriority\"/>\n" +
                "      <element name=\"charging\" type=\"xsd:string\"/>\n" +
                "     </sequence>\n" +
                "    </complexType>\n" +
                "   </element>\n" +
                "   <element name=\"sendMessageResponse\">\n" +
                "    <complexType>\n" +
                "     <sequence>\n" +
                "      <element name=\"result\" type=\"xsd:string\"/>\n" +
                "     </sequence>\n" +
                "    </complexType>\n" +
                "   </element>\n" +
                "   <element name=\"getMessageDeliveryStatus\">\n" +
                "    <complexType>\n" +
                "     <sequence>\n" +
                "      <element name=\"requestIdentifier\" type=\"xsd:string\"/>\n" +
                "     </sequence>\n" +
                "    </complexType>\n" +
                "   </element>\n" +
                "   <element name=\"getMessageDeliveryStatusResponse\">\n" +
                "    <complexType>\n" +
                "     <sequence>\n" +
                "      <element name=\"result\" type=\"tns2:ArrayOfDeliveryStatusType\"/>\n" +
                "     </sequence>\n" +
                "    </complexType>\n" +
                "   </element>\n" +
                "  </schema>\n" +
                "  <schema elementFormDefault=\"qualified\" targetNamespace=\"http://www.csapi.org/schema/parlayx/common/v1_0\" xmlns=\"http://www.w3.org/2001/XMLSchema\">\n" +
                "   <import namespace=\"http://www.csapi.org/schema/parlayx/mm/v1_0\"/>\n" +
                "   <complexType name=\"EndUserIdentifier\">\n" +
                "    <sequence>\n" +
                "     <element name=\"value\" type=\"xsd:anyURI\"/>\n" +
                "    </sequence>\n" +
                "   </complexType>\n" +
                "   <complexType name=\"ArrayOfEndUserIdentifier\">\n" +
                "    <sequence>\n" +
                "     <element maxOccurs=\"unbounded\" minOccurs=\"0\" name=\"ArrayOfEndUserIdentifier\" nillable=\"true\" type=\"tns1:EndUserIdentifier\"/>\n" +
                "    </sequence>\n" +
                "   </complexType>\n" +
                "   <complexType name=\"InvalidArgumentException\">\n" +
                "    <sequence>\n" +
                "     <element name=\"InvalidArgumentException\" nillable=\"true\" type=\"xsd:string\"/>\n" +
                "    </sequence>\n" +
                "   </complexType>\n" +
                "   <element name=\"InvalidArgumentException\" type=\"tns1:InvalidArgumentException\"/>\n" +
                "   <complexType name=\"UnknownEndUserException\">\n" +
                "    <sequence>\n" +
                "     <element name=\"UnknownEndUserException\" nillable=\"true\" type=\"xsd:string\"/>\n" +
                "    </sequence>\n" +
                "   </complexType>\n" +
                "   <element name=\"UnknownEndUserException\" type=\"tns1:UnknownEndUserException\"/>\n" +
                "   <complexType name=\"MessageTooLongException\">\n" +
                "    <sequence>\n" +
                "     <element name=\"MessageTooLongException\" nillable=\"true\" type=\"xsd:string\"/>\n" +
                "    </sequence>\n" +
                "   </complexType>\n" +
                "   <element name=\"MessageTooLongException\" type=\"tns1:MessageTooLongException\"/>\n" +
                "   <complexType name=\"PolicyException\">\n" +
                "    <sequence>\n" +
                "     <element name=\"PolicyException\" nillable=\"true\" type=\"xsd:string\"/>\n" +
                "    </sequence>\n" +
                "   </complexType>\n" +
                "   <element name=\"PolicyException\" type=\"tns1:PolicyException\"/>\n" +
                "   <complexType name=\"ServiceException\">\n" +
                "    <sequence>\n" +
                "     <element name=\"ServiceException\" nillable=\"true\" type=\"xsd:string\"/>\n" +
                "    </sequence>\n" +
                "   </complexType>\n" +
                "   <element name=\"ServiceException\" type=\"tns1:ServiceException\"/>\n" +
                "  </schema>\n" +
                "  <schema elementFormDefault=\"qualified\" targetNamespace=\"http://www.csapi.org/schema/parlayx/mm/v1_0\" xmlns=\"http://www.w3.org/2001/XMLSchema\">\n" +
                "   <import namespace=\"http://www.csapi.org/schema/parlayx/common/v1_0\"/>\n" +
                "   <simpleType name=\"MessagePriority\">\n" +
                "    <restriction base=\"xsd:string\">\n" +
                "     <enumeration value=\"Default\"/>\n" +
                "     <enumeration value=\"Low\"/>\n" +
                "     <enumeration value=\"Normal\"/>\n" +
                "     <enumeration value=\"High\"/>\n" +
                "    </restriction>\n" +
                "   </simpleType>\n" +
                "   <simpleType name=\"DeliveryStatus\">\n" +
                "    <restriction base=\"xsd:string\">\n" +
                "     <enumeration value=\"Delivered\"/>\n" +
                "     <enumeration value=\"DeliveryUncertain\"/>\n" +
                "     <enumeration value=\"DeliveryImpossible\"/>\n" +
                "     <enumeration value=\"MessageWaiting\"/>\n" +
                "    </restriction>\n" +
                "   </simpleType>\n" +
                "   <complexType name=\"DeliveryStatusType\">\n" +
                "    <sequence>\n" +
                "     <element name=\"destinationAddress\" type=\"tns1:EndUserIdentifier\"/>\n" +
                "     <element name=\"deliveryStatus\" type=\"tns2:DeliveryStatus\"/>\n" +
                "    </sequence>\n" +
                "   </complexType>\n" +
                "   <complexType name=\"ArrayOfDeliveryStatusType\">\n" +
                "    <sequence>\n" +
                "     <element maxOccurs=\"unbounded\" minOccurs=\"0\" name=\"ArrayOfDeliveryStatusType\" nillable=\"true\" type=\"tns2:DeliveryStatusType\"/>\n" +
                "    </sequence>\n" +
                "   </complexType>\n" +
                "   <complexType name=\"UnknownRequestIdentifierException\">\n" +
                "    <sequence>\n" +
                "     <element name=\"UnknownRequestIdentifierException\" nillable=\"true\" type=\"xsd:string\"/>\n" +
                "    </sequence>\n" +
                "   </complexType>\n" +
                "   <element name=\"UnknownRequestIdentifierException\" type=\"tns2:UnknownRequestIdentifierException\"/>\n" +
                "  </schema>\n" +
                " </wsdl:types>\n" +
                "\n" +
                "   <wsdl:message name=\"getMessageDeliveryStatusRequest\">\n" +
                "\n" +
                "      <wsdl:part element=\"impl:getMessageDeliveryStatus\" name=\"parameters\"/>\n" +
                "\n" +
                "   </wsdl:message>\n" +
                "\n" +
                "   <wsdl:message name=\"sendMessageRequest\">\n" +
                "\n" +
                "      <wsdl:part element=\"impl:sendMessage\" name=\"parameters\"/>\n" +
                "\n" +
                "   </wsdl:message>\n" +
                "\n" +
                "   <wsdl:message name=\"getMessageDeliveryStatusResponse\">\n" +
                "\n" +
                "      <wsdl:part element=\"impl:getMessageDeliveryStatusResponse\" name=\"parameters\"/>\n" +
                "\n" +
                "   </wsdl:message>\n" +
                "\n" +
                "   <wsdl:message name=\"UnknownEndUserException\">\n" +
                "\n" +
                "      <wsdl:part element=\"tns1:UnknownEndUserException\" name=\"UnknownEndUserException\"/>\n" +
                "\n" +
                "   </wsdl:message>\n" +
                "\n" +
                "   <wsdl:message name=\"UnknownRequestIdentifierException\">\n" +
                "\n" +
                "      <wsdl:part element=\"tns2:UnknownRequestIdentifierException\" name=\"UnknownRequestIdentifierException\"/>\n" +
                "\n" +
                "   </wsdl:message>\n" +
                "\n" +
                "   <wsdl:message name=\"MessageTooLongException\">\n" +
                "\n" +
                "      <wsdl:part element=\"tns1:MessageTooLongException\" name=\"MessageTooLongException\"/>\n" +
                "\n" +
                "   </wsdl:message>\n" +
                "\n" +
                "   <wsdl:message name=\"ServiceException\">\n" +
                "\n" +
                "      <wsdl:part element=\"tns1:ServiceException\" name=\"ServiceException\"/>\n" +
                "\n" +
                "   </wsdl:message>\n" +
                "\n" +
                "   <wsdl:message name=\"InvalidArgumentException\">\n" +
                "\n" +
                "      <wsdl:part element=\"tns1:InvalidArgumentException\" name=\"InvalidArgumentException\"/>\n" +
                "\n" +
                "   </wsdl:message>\n" +
                "\n" +
                "   <wsdl:message name=\"sendMessageResponse\">\n" +
                "\n" +
                "      <wsdl:part element=\"impl:sendMessageResponse\" name=\"parameters\"/>\n" +
                "\n" +
                "   </wsdl:message>\n" +
                "\n" +
                "   <wsdl:message name=\"PolicyException\">\n" +
                "\n" +
                "      <wsdl:part element=\"tns1:PolicyException\" name=\"PolicyException\"/>\n" +
                "\n" +
                "   </wsdl:message>\n" +
                "\n" +
                "   <wsdl:portType name=\"SendMessage\">\n" +
                "\n" +
                "      <wsdl:operation name=\"sendMessage\">\n" +
                "\n" +
                "         <wsdl:input message=\"impl:sendMessageRequest\" name=\"sendMessageRequest\"/>\n" +
                "\n" +
                "         <wsdl:output message=\"impl:sendMessageResponse\" name=\"sendMessageResponse\"/>\n" +
                "\n" +
                "         <wsdl:fault message=\"impl:InvalidArgumentException\" name=\"InvalidArgumentException\"/>\n" +
                "\n" +
                "         <wsdl:fault message=\"impl:UnknownEndUserException\" name=\"UnknownEndUserException\"/>\n" +
                "\n" +
                "         <wsdl:fault message=\"impl:MessageTooLongException\" name=\"MessageTooLongException\"/>\n" +
                "\n" +
                "         <wsdl:fault message=\"impl:PolicyException\" name=\"PolicyException\"/>\n" +
                "\n" +
                "         <wsdl:fault message=\"impl:ServiceException\" name=\"ServiceException\"/>\n" +
                "\n" +
                "      </wsdl:operation>\n" +
                "\n" +
                "      <wsdl:operation name=\"getMessageDeliveryStatus\">\n" +
                "\n" +
                "         <wsdl:input message=\"impl:getMessageDeliveryStatusRequest\" name=\"getMessageDeliveryStatusRequest\"/>\n" +
                "\n" +
                "         <wsdl:output message=\"impl:getMessageDeliveryStatusResponse\" name=\"getMessageDeliveryStatusResponse\"/>\n" +
                "\n" +
                "         <wsdl:fault message=\"impl:InvalidArgumentException\" name=\"InvalidArgumentException\"/>\n" +
                "\n" +
                "         <wsdl:fault message=\"impl:UnknownRequestIdentifierException\" name=\"UnknownRequestIdentifierException\"/>\n" +
                "\n" +
                "         <wsdl:fault message=\"impl:PolicyException\" name=\"PolicyException\"/>\n" +
                "\n" +
                "         <wsdl:fault message=\"impl:ServiceException\" name=\"ServiceException\"/>\n" +
                "\n" +
                "      </wsdl:operation>\n" +
                "\n" +
                "   </wsdl:portType>\n" +
                "\n" +
                "   <wsdl:binding name=\"SendMessageSoapBinding\" type=\"impl:SendMessage\">\n" +
                "\n" +
                "      <wsdlsoap:binding style=\"document\" transport=\"http://schemas.xmlsoap.org/soap/http\"/>\n" +
                "\n" +
                "      <wsdl:operation name=\"sendMessage\">\n" +
                "\n" +
                "         <wsdlsoap:operation soapAction=\"\"/>\n" +
                "\n" +
                "         <wsdl:input name=\"sendMessageRequest\">\n" +
                "\n" +
                "            <wsdlsoap:body use=\"literal\"/>\n" +
                "\n" +
                "         </wsdl:input>\n" +
                "\n" +
                "         <wsdl:output name=\"sendMessageResponse\">\n" +
                "\n" +
                "            <wsdlsoap:body use=\"literal\"/>\n" +
                "\n" +
                "         </wsdl:output>\n" +
                "\n" +
                "         <wsdl:fault name=\"InvalidArgumentException\">\n" +
                "\n" +
                "            <wsdlsoap:fault name=\"InvalidArgumentException\" use=\"literal\"/>\n" +
                "\n" +
                "         </wsdl:fault>\n" +
                "\n" +
                "         <wsdl:fault name=\"UnknownEndUserException\">\n" +
                "\n" +
                "            <wsdlsoap:fault name=\"UnknownEndUserException\" use=\"literal\"/>\n" +
                "\n" +
                "         </wsdl:fault>\n" +
                "\n" +
                "         <wsdl:fault name=\"MessageTooLongException\">\n" +
                "\n" +
                "            <wsdlsoap:fault name=\"MessageTooLongException\" use=\"literal\"/>\n" +
                "\n" +
                "         </wsdl:fault>\n" +
                "\n" +
                "         <wsdl:fault name=\"PolicyException\">\n" +
                "\n" +
                "            <wsdlsoap:fault name=\"PolicyException\" use=\"literal\"/>\n" +
                "\n" +
                "         </wsdl:fault>\n" +
                "\n" +
                "         <wsdl:fault name=\"ServiceException\">\n" +
                "\n" +
                "            <wsdlsoap:fault name=\"ServiceException\" use=\"literal\"/>\n" +
                "\n" +
                "         </wsdl:fault>\n" +
                "\n" +
                "      </wsdl:operation>\n" +
                "\n" +
                "      <wsdl:operation name=\"getMessageDeliveryStatus\">\n" +
                "\n" +
                "         <wsdlsoap:operation soapAction=\"\"/>\n" +
                "\n" +
                "         <wsdl:input name=\"getMessageDeliveryStatusRequest\">\n" +
                "\n" +
                "            <wsdlsoap:body use=\"literal\"/>\n" +
                "\n" +
                "         </wsdl:input>\n" +
                "\n" +
                "         <wsdl:output name=\"getMessageDeliveryStatusResponse\">\n" +
                "\n" +
                "            <wsdlsoap:body use=\"literal\"/>\n" +
                "\n" +
                "         </wsdl:output>\n" +
                "\n" +
                "         <wsdl:fault name=\"InvalidArgumentException\">\n" +
                "\n" +
                "            <wsdlsoap:fault name=\"InvalidArgumentException\" use=\"literal\"/>\n" +
                "\n" +
                "         </wsdl:fault>\n" +
                "\n" +
                "         <wsdl:fault name=\"UnknownRequestIdentifierException\">\n" +
                "\n" +
                "            <wsdlsoap:fault name=\"UnknownRequestIdentifierException\" use=\"literal\"/>\n" +
                "\n" +
                "         </wsdl:fault>\n" +
                "\n" +
                "         <wsdl:fault name=\"PolicyException\">\n" +
                "\n" +
                "            <wsdlsoap:fault name=\"PolicyException\" use=\"literal\"/>\n" +
                "\n" +
                "         </wsdl:fault>\n" +
                "\n" +
                "         <wsdl:fault name=\"ServiceException\">\n" +
                "\n" +
                "            <wsdlsoap:fault name=\"ServiceException\" use=\"literal\"/>\n" +
                "\n" +
                "         </wsdl:fault>\n" +
                "\n" +
                "      </wsdl:operation>\n" +
                "\n" +
                "   </wsdl:binding>\n" +
                "\n" +
                "   <wsdl:service name=\"SendMessageService\">\n" +
                "\n" +
                "      <wsdl:port binding=\"impl:SendMessageSoapBinding\" name=\"SendMessage\">\n" +
                "\n" +
                "         <wsdlsoap:address location=\"http://localhost:8080/parlayx/services/SendMessage\"/>\n" +
                "\n" +
                "      </wsdl:port>\n" +
                "\n" +
                "   </wsdl:service>\n" +
                "\n" +
                "</wsdl:definitions>");

        ServicePropertiesDialog me = new ServicePropertiesDialog(null, svc);
        me.pack();
        me.setVisible(true);
    }*/
}
