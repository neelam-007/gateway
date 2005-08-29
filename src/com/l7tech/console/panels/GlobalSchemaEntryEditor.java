/**
 * LAYER 7 TECHNOLOGIES, INC<br/>
 *
 * User: flascell<br/>
 * Date: Aug 8, 2005<br/>
 */
package com.l7tech.console.panels;

import com.japisoft.xmlpad.XMLContainer;
import com.japisoft.xmlpad.PopupModel;
import com.japisoft.xmlpad.UIAccessibility;
import com.japisoft.xmlpad.action.ActionModel;
import com.l7tech.common.xml.schema.SchemaEntry;
import com.l7tech.common.util.XmlUtil;
import com.l7tech.console.action.Actions;
import com.l7tech.console.text.FilterDocument;
import com.l7tech.policy.assertion.xml.SchemaValidation;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

/**
 * Called by the GlobalSchemaDialog, this is used to edit an existing or add a new global schema entry.
 *
 * @author flascelles@layer7-tech.com
 */
public class GlobalSchemaEntryEditor extends JDialog {
    private final Logger logger = Logger.getLogger(GlobalSchemaEntryEditor.class.getName());
    private JPanel xmlDisplayPanel;
    private JTextField schemanametxt;
    private JButton helpbutton;
    private JButton cancelbutton;
    private JButton okbutton;
    private JPanel mainPanel;
    private XMLContainer xmlContainer;
    private UIAccessibility uiAccessibility;
    private SchemaEntry subject;
    private boolean dataloaded = false;
    public boolean success = false;

    public GlobalSchemaEntryEditor(JDialog owner, SchemaEntry subject) {
        super(owner, true);
        initialize();
        this.subject = subject;
    }

    private void initialize() {
        setContentPane(mainPanel);
        setTitle("Edit Global Schema");

        // set xml view
        xmlContainer = new XMLContainer(true);
        uiAccessibility = xmlContainer.getUIAccessibility();
        uiAccessibility.setTreeAvailable(false);
        uiAccessibility.setToolBarAvailable(false);
        xmlContainer.setStatusBarAvailable(false);
        PopupModel popupModel = xmlContainer.getPopupModel();
        // remove the unwanted actions
        popupModel.removeAction(ActionModel.getActionByName(ActionModel.LOAD_ACTION));
        popupModel.removeAction(ActionModel.getActionByName(ActionModel.SAVE_ACTION));
        popupModel.removeAction(ActionModel.getActionByName(ActionModel.SAVEAS_ACTION));
        popupModel.removeAction(ActionModel.getActionByName(ActionModel.NEW_ACTION));
        xmlContainer.getTreePopupModel().removeAction(ActionModel.getActionByName(ActionModel.TREE_SELECTNODE_ACTION));
        xmlContainer.getTreePopupModel().removeAction(ActionModel.getActionByName(ActionModel.TREE_COMMENTNODE_ACTION));
        xmlContainer.getTreePopupModel().removeAction(ActionModel.getActionByName(ActionModel.TREE_COPYNODE_ACTION));
        xmlContainer.getTreePopupModel().removeAction(ActionModel.getActionByName(ActionModel.TREE_CUTNODE_ACTION));
        xmlContainer.getTreePopupModel().removeAction(ActionModel.getActionByName(ActionModel.TREE_EDITNODE_ACTION));
        xmlContainer.getTreePopupModel().removeAction(ActionModel.getActionByName(ActionModel.TREE_CLEANHISTORY_ACTION));
        xmlContainer.getTreePopupModel().removeAction(ActionModel.getActionByName(ActionModel.TREE_ADDHISTORY_ACTION));
        xmlContainer.getTreePopupModel().removeAction(ActionModel.getActionByName(ActionModel.TREE_PREVIOUS_ACTION));
        xmlContainer.getTreePopupModel().removeAction(ActionModel.getActionByName(ActionModel.TREE_NEXT_ACTION));
        xmlDisplayPanel.setLayout(new BorderLayout());
        xmlDisplayPanel.add(xmlContainer.getView(), BorderLayout.CENTER);

        // support Enter and Esc keys
        Actions.setEscKeyStrokeDisposes(this);
        Actions.setEnterAction(this, new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                cancel();
            }
        });

        // button callbacks
        helpbutton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                help();
            }
        });
        cancelbutton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                cancel();
            }
        });
        okbutton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                ok();
            }
        });

        schemanametxt.setDocument(new FilterDocument(128, new FilterDocument.Filter() {
                                                                public boolean accept(String str) {
                                                                    if (str == null) return false;
                                                                    return true;
                                                                }
                                                            }));
    }

    private void help() {
        Actions.invokeHelp(this);
    }

    private void cancel() {
        dispose();
    }

    private boolean docIsSchema(Document doc) {
        if (doc == null) return false;
        Element rootEl = doc.getDocumentElement();

        if (!SchemaValidation.TOP_SCHEMA_ELNAME.equals(rootEl.getLocalName())) {
            logger.log(Level.WARNING, "document is not schema (top element " + rootEl.getLocalName() +
              " is not " + SchemaValidation.TOP_SCHEMA_ELNAME + ")");
            return false;
        }
        if (!SchemaValidation.W3C_XML_SCHEMA.equals(rootEl.getNamespaceURI())) {
            logger.log(Level.WARNING, "document is not schema (namespace is not + " +
              SchemaValidation.W3C_XML_SCHEMA + ")");
            return false;
        }
        return true;
    }

    private void ok() {
        // make sure this is a schema
        String contents = uiAccessibility.getEditor().getText();

        if (contents == null || contents.length() < 1) {
            JOptionPane.showMessageDialog(this, "There is not xml here. Insert a schema or cancel.",
                                                "Empty",
                                                JOptionPane.ERROR_MESSAGE);
            return;
        }
        Document doc = null;
        try {
            doc = XmlUtil.stringToDocument(contents);
        } catch (IOException e) {
            logger.log(Level.INFO, "parsing problem", e);
            JOptionPane.showMessageDialog(this, "Could not parse XML, consult log for more details.",
                                                "Invalid XML",
                                                JOptionPane.ERROR_MESSAGE);
            return;
        } catch (SAXException e) {
            logger.log(Level.INFO, "parsing problem", e);
            JOptionPane.showMessageDialog(this, "Could not parse XML, consult log for more details.",
                                                "Invalid XML",
                                                JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (!docIsSchema(doc)) {
            JOptionPane.showMessageDialog(this, "This is not an xml schema. Consult log for more details",
                                                "Invalid Schema",
                                                JOptionPane.ERROR_MESSAGE);
            return;
        }
        // get the tns
        String tns = doc.getDocumentElement().getAttribute("targetNamespace");
        if (tns == null || tns.length() < 1) {
            JOptionPane.showMessageDialog(this, "This schema does not declare a target namespace.",
                                                "Invalid Schema",
                                                JOptionPane.ERROR_MESSAGE);
            return;
        }
        // make sure there is a name captioned
        String sustemid = schemanametxt.getText();
        if (sustemid == null || sustemid.length() < 1) {
            JOptionPane.showMessageDialog(this, "You must provide a system id (name) for this schema to be referenced by another schema.",
                                                "Invalid Schema",
                                                JOptionPane.ERROR_MESSAGE);
            return;
        }
        // save it
        subject.setName(sustemid);
        subject.setSchema(contents);
        subject.setTns(tns);
        dispose();
        success = true;
    }

    public void show() {
        if (!dataloaded) {
            resetData();
        }
        super.show();
    }

    private void resetData() {
        schemanametxt.setText(subject.getName());
        String xml = subject.getSchema();
        if (xml != null) {
            uiAccessibility.getEditor().setText(xml);
            uiAccessibility.getEditor().setLineNumber(1);
        }
        dataloaded = true;
    }
}
