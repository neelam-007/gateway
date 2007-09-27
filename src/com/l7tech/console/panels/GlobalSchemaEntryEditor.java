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
import com.l7tech.common.gui.util.Utilities;
import com.l7tech.common.gui.util.DialogDisplayer;
import com.l7tech.common.gui.util.FileChooserUtil;
import com.l7tech.common.gui.widgets.OkCancelDialog;
import com.l7tech.common.gui.widgets.UrlPanel;
import com.l7tech.common.gui.FilterDocument;
import com.l7tech.common.util.XmlUtil;
import com.l7tech.common.util.ExceptionUtils;
import com.l7tech.common.xml.schema.SchemaEntry;
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
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.security.AccessControlException;

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
    private JButton uploadFromURLBut;
    private JButton uploadFromFileBut;
    private boolean canEdit;

    public GlobalSchemaEntryEditor(JDialog owner, SchemaEntry subject, boolean canEditEntry) {
        super(owner, true);
        this.subject = subject;
        this.canEdit = canEditEntry;
        initialize();
        DialogDisplayer.suppressSheetDisplay(this); // incompatible with xmlpad
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

        uploadFromURLBut.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                uploadFromURL();
            }
        });

        uploadFromFileBut.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                uploadFromFile();
            }
        });

        schemanametxt.setDocument(new FilterDocument(128, new FilterDocument.Filter() {
                                                                public boolean accept(String str) {
                                                                    if (str == null) return false;
                                                                    return true;
                                                                }
                                                            }));
        enableReadOnlyIfNeeded();

        this.addWindowListener(new WindowAdapter() {
            public void windowOpened(WindowEvent e) {
                if (!dataloaded) {
                    resetData();
                }
            }
        });
    }

    private void uploadFromURL() {
        final OkCancelDialog dlg = new OkCancelDialog(this, "Load Schema From URL",
                                                true, new UrlPanel("Enter URL to read XML Schema from", null));
        dlg.pack();
        Utilities.centerOnScreen(dlg);
        DialogDisplayer.display(dlg, new Runnable() {
            public void run() {
                String url = (String)dlg.getValue();
                if (url != null) {
                    readFromUrl(url);
                }
            }
        });
    }

    private void readFromUrl(String urlstr) {
        if (urlstr == null || urlstr.length() < 1) {
            displayError("A URL was not provided", null);
            return;
        }
        // compose input source
        URL url;
        try {
            url = new URL(urlstr);
        } catch (MalformedURLException e) {
            displayError(urlstr + " is not a valid url", null);
            return;
        }
        // try to get document
        InputStream is;
        try {
            is = url.openStream();
        } catch (AccessControlException ace) {
            TopComponents.getInstance().showNoPrivilegesErrorMessage();
            return;
        } catch (IOException e) {
            displayError("Could not read from " + " " + urlstr, null);
            return;
        }

        Document doc;
        try {
            doc = XmlUtil.parse(is);
        } catch (SAXException e) {
            displayError("No XML at " + urlstr, null);
            return;
        } catch (IOException e) {
            displayError("No XML at " + urlstr, null);
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
            displayError("No XML Schema could be parsed from " + urlstr, null);
        }
    }

    private boolean docIsSchema(Document doc) {
        Element rootEl = doc.getDocumentElement();

        if (!SchemaValidation.TOP_SCHEMA_ELNAME.equals(rootEl.getLocalName())) {
            return false;
        }
        if (!SchemaValidation.W3C_XML_SCHEMA.equals(rootEl.getNamespaceURI())) {
            return false;
        }
        return true;
    }

    private void displayError(String msg, String title) {
        if (title == null) title = "Error";
        JOptionPane.showMessageDialog(this, msg, title, JOptionPane.ERROR_MESSAGE);
    }

    private void uploadFromFile() {
        SsmApplication.doWithJFileChooser(new FileChooserUtil.FileChooserUser() {
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
            displayError("No XML at " + filename, null);
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
            if (schemanametxt.getText() == null || schemanametxt.getText().length() < 1) {
                schemanametxt.setText(dlg.getSelectedFile().getName());
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
            success = true;
        }
        dispose();
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

    private void enableReadOnlyIfNeeded() {
        schemanametxt.setEditable(canEdit);
        uploadFromFileBut.setEnabled(canEdit);
        uploadFromURLBut.setEnabled(canEdit);
        xmlContainer.setEditable(canEdit);
    }
}
