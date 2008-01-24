package com.l7tech.console.panels;

import com.l7tech.policy.assertion.FaultLevel;
import com.l7tech.common.gui.util.Utilities;
import com.l7tech.common.gui.util.DialogDisplayer;
import com.l7tech.common.xml.SoapFaultLevel;
import com.l7tech.common.xml.InvalidDocumentFormatException;
import com.l7tech.common.util.XmlUtil;
import com.l7tech.common.util.SoapUtil;
import com.l7tech.console.action.Actions;
import com.l7tech.console.util.TopComponents;

import com.japisoft.xmlpad.XMLContainer;
import com.japisoft.xmlpad.UIAccessibility;
import com.japisoft.xmlpad.PopupModel;
import com.japisoft.xmlpad.editor.XMLEditor;
import com.japisoft.xmlpad.action.ActionModel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.WindowEvent;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

/**
 * Dialog box to edit the properties of a FaultLevel assertion.
 * <p/>
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: May 4, 2006<br/>
 *
 * @see com.l7tech.policy.assertion.FaultLevel
 */
public class FaultLevelPropertiesDialog extends JDialog {
    private static final String TITLE = "Fault Level Properties";

    private static final String DROP_LEVEL_DESCRIPTION = "<html><p>In the case of a policy violation, the SecureSpan Gateway will " +
                              "drop the connection with the requestor without returning anything.</p></html>";
    private static final String GEN_LEVEL_DESCRIPTION = "<html><p>In the case of a policy violation, the SecureSpan Gateway will " +
                              "return a generic SOAP fault without the details of the reason for " +
                              "the policy violation.</p><p>A sample of such a SOAP fault is displayed " +
                              "below:</p></html>";
    private static final String MEDIUM_LEVEL_DESCRIPTION = "<html><p>In the case of a policy violation, the SecureSpan Gateway will " +
                              "return a SOAP fault which contains information regarding the reasons for " +
                              "the policy violation.</p><p>A sample of such a SOAP fault is displayed " +
                              "below:</p></html>";
    private static final String FULL_LEVEL_DESCRIPTION = "<html><p>In the case of a policy violation, the SecureSpan Gateway will " +
                              "return a SOAP fault which contains a full trace for each assertion " +
                              "evaluation in the policy whether the assertion was a success or " +
                              "failure.</p><p>A sample of such a SOAP fault is displayed below:</p></html>";
    private static final String TEMPLATE_LEVEL_DESCRIPTION = "<html><p>In the case of a policy violation, the SecureSpan Gateway will " +
                              "return a SOAP fault based on a template provided by you. You can use " +
                              "context variables as part of the template.</p><p><b>You must edit the template " +
                              "below:</b></p></html>";
    private static final String DROP_LEVEL_SAMPLE = "<!-- no fault -->";
    private static final String GEN_LEVEL_SAMPLE = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\">\n" +
                        "    <soapenv:Body>\n" +
                        "        <soapenv:Fault>\n" +
                        "            <faultcode>soapenv:Server</faultcode>\n" +
                        "            <faultstring>Assertion Falsified</faultstring>\n" +
                        "            <faultactor>http://soong:8080/xml/blub</faultactor>\n" +
                        "            <detail>\n" +
                        "               <l7:policyResult status=\"Falsified\" xmlns:l7=\"http://www.layer7tech.com/ws/policy/fault\"/>\n" +
                        "            </detail>\n" +
                        "        </soapenv:Fault>\n" +
                        "    </soapenv:Body>\n" +
                        "</soapenv:Envelope>";
    private static final String MEDIUM_LEVEL_SAMPLE = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\">\n" +
                        "    <soapenv:Body>\n" +
                        "        <soapenv:Fault>\n" +
                        "            <faultcode>soapenv:Server</faultcode>\n" +
                        "            <faultstring>Policy Falsified</faultstring>\n" +
                        "            <faultactor>http://soong:8080/xml/blub</faultactor>\n" +
                        "            <detail>\n" +
                        "               <l7:policyResult status=\"Falsified\" xmlns:l7=\"http://www.layer7tech.com/ws/policy/fault\" xmlns:l7p=\"http://www.layer7tech.com/ws/policy\">\n" +
                        "               \t <l7:assertionResult status=\"BAD\" assertion=\"l7p:WssUsernameToken\">\n" +
                        "               \t    <l7:detailMessage id=\"4302\">This request did not contain any WSS level security.</l7:detailMessage>\n" +
                        "               \t    <l7:detailMessage id=\"5204\">Request did not include an encrypted UsernameToken.</l7:detailMessage>\n" +
                        "               \t </l7:assertionResult>\n" +
                        "               </l7:policyResult>\n" +
                        "            </detail>\n" +
                        "        </soapenv:Fault>\n" +
                        "    </soapenv:Body>\n" +
                        "</soapenv:Envelope>";
    private static final String FULL_LEVEL_SAMPLE = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\">\n" +
                        "    <soapenv:Body>\n" +
                        "        <soapenv:Fault>\n" +
                        "            <faultcode>soapenv:Server</faultcode>\n" +
                        "            <faultstring>Policy Falsified</faultstring>\n" +
                        "            <faultactor>http://soong:8080/xml/blub</faultactor>\n" +
                        "            <detail>\n" +
                        "               <l7:policyResult status=\"Falsified\" xmlns:l7=\"http://www.layer7tech.com/ws/policy/fault\" xmlns:l7p=\"http://www.layer7tech.com/ws/policy\">\n" +
                        "               \t <l7:assertionResult status=\"OK\" assertion=\"l7p:RequestXpathAssertion\"/>\n" +
                        "               \t <l7:assertionResult status=\"BAD\" assertion=\"l7p:WssUsernameToken\">\n" +
                        "               \t    <l7:detailMessage id=\"4302\">This request did not contain any WSS level security.</l7:detailMessage>\n" +
                        "               \t    <l7:detailMessage id=\"5204\">Request did not include an encrypted UsernameToken.</l7:detailMessage>\n" +
                        "               \t </l7:assertionResult>\n" +
                        "               </l7:policyResult>\n" +
                        "            </detail>\n" +
                        "        </soapenv:Fault>\n" +
                        "    </soapenv:Body>\n" +
                        "</soapenv:Envelope>";
    private FaultLevel assertion;
    private JPanel mainPanel;
    private JButton okButton;
    private JButton cancelButton;
    private JComboBox levelBox;
    private JTextPane descriptionPane;
    private JPanel xmlEditorScrollPane;
    private XMLContainer xmlContainer;
    private XMLEditor editor;
    private String lastUserEdits = "";
    public boolean oked = false;
    private JCheckBox urlCheckBox;
    private JButton helpButton;

    public FaultLevelPropertiesDialog(Frame owner, FaultLevel subject, boolean readOnly) {
        super(owner, TITLE, true);
        this.assertion = subject;
        initialize(readOnly);
        DialogDisplayer.suppressSheetDisplay(this); // incompatible with xmlpad
    }

    protected void processWindowEvent(WindowEvent e) {
        super.processWindowEvent(e);
        switch(e.getID()) {
            case WindowEvent.WINDOW_OPENED:
                setInitialData();
                onComboSelection();
                break;
        }
    }

    private void initialize(boolean readOnly) {
        setContentPane(mainPanel);
        Utilities.equalizeButtonSizes(new AbstractButton[] {okButton, cancelButton, helpButton});

        // populate the combo box with the possible levels
        levelBox.setModel(new DefaultComboBoxModel(new LevelComboItems[] {
                            new LevelComboItems(SoapFaultLevel.DROP_CONNECTION, "Drop Connection"),
                            new LevelComboItems(SoapFaultLevel.GENERIC_FAULT, "Generic SOAP Fault"),
                            new LevelComboItems(SoapFaultLevel.MEDIUM_DETAIL_FAULT, "Medium Detail"),
                            new LevelComboItems(SoapFaultLevel.FULL_TRACE_FAULT, "Full Detail"),
                            new LevelComboItems(SoapFaultLevel.TEMPLATE_FAULT, "Template Fault")
                          }));

        levelBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onComboSelection();
            }
        });

        okButton.setEnabled( !readOnly );
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

        Utilities.setEnterAction(this, new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                ok();
            }
        });
        Utilities.setEscAction(this, new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                cancel();
            }
        });

        xmlContainer = new XMLContainer(true);
        UIAccessibility uiAccessibility = xmlContainer.getUIAccessibility();
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

        if (TopComponents.getInstance().isApplet()) {
            // Search action tries to get the class loader
            popupModel.removeAction(ActionModel.getActionByName(ActionModel.INSERT_ACTION));
            popupModel.removeAction(ActionModel.getActionByName(ActionModel.SEARCH_ACTION));
            popupModel.removeAction(ActionModel.getActionByName(ActionModel.COMMENT_ACTION));
            popupModel.removeAction(ActionModel.getActionByName(ActionModel.PARSE_ACTION));
        }

        boolean lastWasSeparator = true; // remove trailing separator
        for (int i=popupModel.size()-1; i>=0; i--) {
            boolean isSeparator = popupModel.isSeparator(i);
            if (isSeparator && (i==0 || lastWasSeparator)) {
                popupModel.removeSeparator(i);
            } else {
                lastWasSeparator = isSeparator;
            }
        }

        xmlEditorScrollPane.setLayout(new BorderLayout());
        xmlEditorScrollPane.add(xmlContainer.getView(), BorderLayout.CENTER);
        editor = uiAccessibility.getEditor();
        editor.initErrorProcessing();
        setInitialData();
    }

    public void setInitialData() {
        int initiallevel = assertion.getLevelInfo().getLevel();
        for (int i = 0; i < levelBox.getModel().getSize(); i++) {
            LevelComboItems item = (LevelComboItems)levelBox.getModel().getElementAt(i);
            if (item.level == initiallevel) {
                levelBox.setSelectedItem(item);
            }
        }
        if (initiallevel == SoapFaultLevel.TEMPLATE_FAULT) {
            lastUserEdits = assertion.getLevelInfo().getFaultTemplate();
        }

        urlCheckBox.setSelected(assertion.getLevelInfo().isIncludePolicyDownloadURL());
    }

    private void ok() {
        int newlevel = ((LevelComboItems)levelBox.getSelectedItem()).level;
        assertion.getLevelInfo().setLevel(newlevel);
        if (newlevel == SoapFaultLevel.TEMPLATE_FAULT) {
            String maybeXML = editor.getText();
            try {
                // make sure it's xml
                Document doc = XmlUtil.stringToDocument(maybeXML);
                // make sure there is a soap body
                Element body = SoapUtil.getBodyElement(doc);
                // make sure there is a fault child
                java.util.List res = XmlUtil.findChildElementsByName(body, body.getNamespaceURI(), "Fault");
                if (res.size() < 1) {
                    displayError("The template does not appear to contain a SOAP Fault", "Invalid Template");
                    return;
                }
            } catch (SAXException e) {
                displayError("The template does not appear to contain well formed XML.\n" + e.getMessage(), "Invalid Template");
                return;
            } catch (InvalidDocumentFormatException e) {
                displayError("The template does not appear to contain a SOAP Fault.\n" + e.getMessage(), "Invalid Template");
                return;
            }
            assertion.getLevelInfo().setFaultTemplate(maybeXML);
        }
        assertion.getLevelInfo().setIncludePolicyDownloadURL(urlCheckBox.isSelected());
        oked = true;
        cancel();
    }

    private void cancel() {
        dispose();
    }

    private void help() {
        Actions.invokeHelp(this);
    }

    private void displayError(String msg, String title) {
        JOptionPane.showMessageDialog(this, msg, title, JOptionPane.ERROR_MESSAGE);
    }

    private class LevelComboItems {
        public int level;
        private String levelname;
        public LevelComboItems(int level, String levelname) {
            assert(levelname != null);
            this.level = level;
            this.levelname = levelname;
        }
        public String toString() {
            return levelname;
        }
    }

    private void onComboSelection() {
        LevelComboItems currentselection = (LevelComboItems)levelBox.getSelectedItem();
        String description;
        if (xmlContainer.isEditable()) {
            // remember what the user captured in case he comes back to this mode
            String tmp = editor.getText();
            if (tmp != null && tmp.indexOf("<!--  Your document, created at :") < 0) {
                lastUserEdits = tmp;
            }
        }
        switch (currentselection.level) {
            case SoapFaultLevel.DROP_CONNECTION:
                description = DROP_LEVEL_DESCRIPTION;
                editor.setText(DROP_LEVEL_SAMPLE);
                xmlContainer.setEditable(false);
                urlCheckBox.setEnabled(false);
                break;
            case SoapFaultLevel.GENERIC_FAULT:
                description = GEN_LEVEL_DESCRIPTION;
                editor.setText(GEN_LEVEL_SAMPLE);
                xmlContainer.setEditable(false);
                urlCheckBox.setEnabled(true);
                break;
            case SoapFaultLevel.MEDIUM_DETAIL_FAULT:
                description = MEDIUM_LEVEL_DESCRIPTION;
                editor.setText(MEDIUM_LEVEL_SAMPLE);
                xmlContainer.setEditable(false);
                urlCheckBox.setEnabled(true);
                break;
            case SoapFaultLevel.FULL_TRACE_FAULT:
                description = FULL_LEVEL_DESCRIPTION;
                editor.setText(FULL_LEVEL_SAMPLE);
                xmlContainer.setEditable(false);
                urlCheckBox.setEnabled(true);
                break;
            case SoapFaultLevel.TEMPLATE_FAULT:
                description = TEMPLATE_LEVEL_DESCRIPTION;
                xmlContainer.setEditable(true);
                showCustomFault();
                urlCheckBox.setEnabled(true);
                break;
            default:
                // can't happen (unless bug)
                throw new RuntimeException("Unhandeled SoapFaultLevel");
        }
        descriptionPane.setText(description);
    }

    private void showCustomFault() {
        String toShow = lastUserEdits;
        if (toShow == null || toShow.length() < 1) {
            toShow = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\">\n" +
                        "    <soapenv:Body>\n" +
                        "        <soapenv:Fault>\n" +
                        "            <faultcode>YOUR_FAULT_CODE</faultcode>\n" +
                        "            <faultstring>YOUR_FAULT_STRING</faultstring>\n" +
                        "            <faultactor>YOUR_FAULT_ACTOR</faultactor>\n" +
                        "            <detail>YOUR_FAULT_DETAIL</detail>\n" +
                        "        </soapenv:Fault>\n" +
                        "    </soapenv:Body>\n" +
                        "</soapenv:Envelope>";
        }
        editor.setText(toShow);
    }
}
