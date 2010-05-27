/**
 * LAYER 7 TECHNOLOGIES, INC<br/>
 *
 * User: flascell<br/>
 * Date: Aug 8, 2005<br/>
 */
package com.l7tech.console.panels;

import com.japisoft.xmlpad.PopupModel;
import com.japisoft.xmlpad.UIAccessibility;
import com.japisoft.xmlpad.XMLContainer;
import com.japisoft.xmlpad.action.ActionModel;
import com.l7tech.console.util.Registry;
import com.l7tech.gateway.common.schema.SchemaAdmin;
import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.FileChooserUtil;
import com.l7tech.gui.widgets.OkCancelDialog;
import com.l7tech.gui.FilterDocument;
import com.l7tech.objectmodel.EntityUtil;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.gateway.common.schema.SchemaEntry;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.console.action.Actions;
import com.l7tech.console.util.TopComponents;
import com.l7tech.console.SsmApplication;
import com.l7tech.policy.assertion.xml.SchemaValidation;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Called by the GlobalSchemaDialog, this is used to edit an existing or add a new global schema entry.
 *
 * @author flascelles@layer7-tech.com
 */
public class GlobalSchemaEntryEditor extends JDialog {
    private final Logger logger = Logger.getLogger(GlobalSchemaEntryEditor.class.getName());
    private JPanel xmlDisplayPanel;
    private JTextField schemaNameTextField;
    private JButton helpButton;
    private JButton cancelButton;
    private JButton okButton;
    private JPanel mainPanel;
    private XMLContainer xmlContainer;
    private UIAccessibility uiAccessibility;
    private SchemaEntry subject;
    private boolean dataLoaded = false;
    public boolean success = false;
    private JButton uploadFromURLBut;
    private JButton uploadFromFileBut;
    private boolean canEdit;

    public GlobalSchemaEntryEditor(JDialog owner, SchemaEntry subject, boolean canEditEntry) {
        super(owner, true);
        this.subject = subject;
        this.canEdit = canEditEntry;
        initialize();
        DialogDisplayer.suppressSheetDisplay(this); // incompatible with xml pad
    }

    private void initialize() {
        setContentPane(mainPanel);
        setTitle(canEdit?"Edit Global Schema":"View Global Schema");

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
        popupModel.removeAction(ActionModel.getActionByName(ActionModel.PARSE_ACTION));
        xmlContainer.getTreePopupModel().removeAction(ActionModel.getActionByName(ActionModel.TREE_SELECTNODE_ACTION));
        xmlContainer.getTreePopupModel().removeAction(ActionModel.getActionByName(ActionModel.TREE_COMMENTNODE_ACTION));
        xmlContainer.getTreePopupModel().removeAction(ActionModel.getActionByName(ActionModel.TREE_COPYNODE_ACTION));
        xmlContainer.getTreePopupModel().removeAction(ActionModel.getActionByName(ActionModel.TREE_CUTNODE_ACTION));
        xmlContainer.getTreePopupModel().removeAction(ActionModel.getActionByName(ActionModel.TREE_EDITNODE_ACTION));
        xmlContainer.getTreePopupModel().removeAction(ActionModel.getActionByName(ActionModel.TREE_CLEANHISTORY_ACTION));
        xmlContainer.getTreePopupModel().removeAction(ActionModel.getActionByName(ActionModel.TREE_ADDHISTORY_ACTION));
        xmlContainer.getTreePopupModel().removeAction(ActionModel.getActionByName(ActionModel.TREE_PREVIOUS_ACTION));
        xmlContainer.getTreePopupModel().removeAction(ActionModel.getActionByName(ActionModel.TREE_NEXT_ACTION));

        if (TopComponents.getInstance().isApplet()) {
            // Search action tries to get the class loader
            popupModel.removeAction(ActionModel.getActionByName(ActionModel.INSERT_ACTION));
            popupModel.removeAction(ActionModel.getActionByName(ActionModel.SEARCH_ACTION));
            popupModel.removeAction(ActionModel.getActionByName(ActionModel.COMMENT_ACTION));
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

        xmlDisplayPanel.setLayout(new BorderLayout());
        xmlDisplayPanel.add(xmlContainer.getView(), BorderLayout.CENTER);

        // support Enter and Esc keys
        Utilities.setEscKeyStrokeDisposes(this);
        Utilities.setEnterAction(this, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ok();
            }
        });

        // button callbacks
        helpButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                help();
            }
        });
        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                cancel();
            }
        });
        okButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ok();
            }
        });

        uploadFromURLBut.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                uploadFromURL();
            }
        });

        uploadFromFileBut.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                uploadFromFile();
            }
        });

        schemaNameTextField.setDocument(new FilterDocument(EntityUtil.getMaxFieldLength(SchemaEntry.class, "getName", 4096), new FilterDocument.Filter() {
                                                                @Override
                                                                public boolean accept(String str) {
                                                                    return str != null;
                                                                }
                                                            }));
        enableReadOnlyIfNeeded();

        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent e) {
                if (!dataLoaded ) {
                    resetData();
                }
            }
        });
    }

    private void uploadFromURL() {
        final OkCancelDialog dlg = new OkCancelDialog<String>(this, "Load Schema From URL",
                                                true, new UrlPanel("Enter URL to read XML Schema from", null));
        dlg.pack();
        Utilities.centerOnScreen(dlg);
        DialogDisplayer.display(dlg, new Runnable() {
            @Override
            public void run() {
                String url = (String)dlg.getValue();
                if (url != null) {
                    readFromUrl(url);
                }
            }
        });
    }

    private void readFromUrl(String url) {
        if (url == null || url.length() < 1) {
            displayError("A URL was not provided", null);
            return;
        }
        // compose input source
        try {
            new URL(url);
        } catch (MalformedURLException e) {
            displayError(url + " is not a valid url", null);
            return;
        }

        final SchemaAdmin schemaAdmin;
        final Registry reg = Registry.getDefault();
        if (reg == null || reg.getServiceManager() == null) {
            throw new RuntimeException("No access to registry. Cannot download document.");
        } else {
            schemaAdmin = reg.getSchemaAdmin();
        }

        final String schemaXml;
        try {
            schemaXml = schemaAdmin.resolveSchemaTarget(url);
        } catch (IOException e) {
            //this is likely to be a GenericHttpException
            final String errorMsg = "Cannot download document: " + ExceptionUtils.getMessage(e);
            displayError(errorMsg, "Errors downloading file");
            return;
        }

        Document doc;
        try {
            doc = XmlUtil.parse(schemaXml);
        } catch (SAXException e) {
            displayError("No XML at " + url, null);
            return;
        }

        // check if it's a schema
        if (docIsSchema(doc)) {
            // set the new schema
            String printedSchema;
            try {
                printedSchema = XmlUtil.nodeToFormattedString(doc);
            } catch (IOException e) {
                String msg = "error serializing document";
                displayError(msg, null);
                return;
            }
            uiAccessibility.getEditor().setText(printedSchema);
        } else {
            displayError("No XML Schema could be parsed from " + url, null);
        }
    }

    private boolean docIsSchema(Document doc) {
        Element rootEl = doc.getDocumentElement();

        return SchemaValidation.TOP_SCHEMA_ELNAME.equals( rootEl.getLocalName() ) &&
               XmlUtil.W3C_XML_SCHEMA.equals( rootEl.getNamespaceURI() );
    }

    private void displayError(String msg, String title) {
        if (title == null) title = "Error";
        JOptionPane.showMessageDialog(this, msg, title, JOptionPane.ERROR_MESSAGE);
    }

    private void uploadFromFile() {
        SsmApplication.doWithJFileChooser(new FileChooserUtil.FileChooserUser() {
            @Override
            public void useFileChooser(JFileChooser fc) {
                doUpload(fc);
            }
        });
    }

    private void doUpload(final JFileChooser dlg) {
        if (JFileChooser.APPROVE_OPTION != dlg.showOpenDialog(this)) {
            return;
        }
        FileInputStream fis;
        String filename = dlg.getSelectedFile().getAbsolutePath();
        try {
            fis = new FileInputStream(dlg.getSelectedFile());
        } catch (FileNotFoundException e) {
            logger.log(Level.FINE, "cannot open file" + filename, e);
            return;
        }

        // try to get document
        Document doc;
        try {
            doc = XmlUtil.parse(fis);
        } catch (SAXException e) {
            displayError("Error parsing schema from " + filename + " : " + ExceptionUtils.getMessage( e ), null);
            logger.log(Level.FINE, "cannot parse " + filename, e);
            return;
        } catch (IOException e) {
            displayError("No XML at" + filename, null);
            logger.log(Level.FINE, "cannot parse " + filename, e);
            return;
        }
        // check if it's a schema
        if (docIsSchema(doc)) {
            // set the new schema
            String printedSchema;
            try {
                printedSchema = XmlUtil.nodeToFormattedString(doc);
            } catch (IOException e) {
                String msg = "error serializing document";
                displayError(msg, null);
                logger.log(Level.FINE, msg, e);
                return;
            }
            uiAccessibility.getEditor().setText(printedSchema);
            if ( schemaNameTextField.getText() == null || schemaNameTextField.getText().length() < 1) {
                schemaNameTextField.setText(dlg.getSelectedFile().getName());
            }
        } else {
            displayError("An XML Schema could not be read from " + filename, null);
        }
    }

    private void help() {
        Actions.invokeHelp(this);
    }

    private void cancel() {
        dispose();
    }

    private void ok() {
        if (canEdit) {
            // make sure this is a schema
            String contents = uiAccessibility.getEditor().getText();
            String tns;
            try {
                tns = XmlUtil.getSchemaTNS(contents);
            } catch (XmlUtil.BadSchemaException e) {
                logger.log(Level.WARNING, "problem parsing schema", e);
                JOptionPane.showMessageDialog(this, "This is not a legal xml schema: " + ExceptionUtils.getMessage(e),
                                                    "Illegal Schema",
                                                    JOptionPane.ERROR_MESSAGE);
                return;
            }

            // make sure there is a name captioned
            String systemId = schemaNameTextField.getText();
            if (systemId == null || systemId.trim().length() < 1) {
                JOptionPane.showMessageDialog(this, "You must provide a system id (name) for this schema to be referenced by another schema.",
                                                    "Invalid Schema",
                                                    JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            // save it
            subject.setName(systemId.trim());
            subject.setSchema(contents);
            subject.setTns(tns);
            success = true;
        }
        dispose();
    }

    private void resetData() {
        schemaNameTextField.setText(subject.getName());
        String xml = subject.getSchema();
        if (xml != null) {
            uiAccessibility.getEditor().setText(xml);
            uiAccessibility.getEditor().setLineNumber(1);
        }
        dataLoaded = true;
    }

    private void enableReadOnlyIfNeeded() {
        schemaNameTextField.setEditable(canEdit);
        uploadFromFileBut.setEnabled(canEdit);
        uploadFromURLBut.setEnabled(canEdit);
        xmlContainer.setEditable(canEdit);
    }
}
