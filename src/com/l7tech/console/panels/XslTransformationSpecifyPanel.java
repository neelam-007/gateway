/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.console.panels;

import com.japisoft.xmlpad.PopupModel;
import com.japisoft.xmlpad.UIAccessibility;
import com.japisoft.xmlpad.XMLContainer;
import com.japisoft.xmlpad.action.ActionModel;
import com.japisoft.xmlpad.editor.XMLEditor;
import com.l7tech.common.gui.util.Utilities;
import com.l7tech.common.gui.widgets.OkCancelDialog;
import com.l7tech.common.gui.widgets.UrlPanel;
import com.l7tech.common.util.ResourceUtils;
import com.l7tech.common.util.XmlUtil;
import com.l7tech.policy.AssertionResourceInfo;
import com.l7tech.policy.StaticResourceInfo;
import com.l7tech.policy.assertion.xml.XslTransformation;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.swing.*;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Part of {@link XslTransformationPropertiesDialog}.
 * @author alex
 */
public class XslTransformationSpecifyPanel extends JPanel {
    private static final Logger log = Logger.getLogger(XslTransformationSpecifyPanel.class.getName());
    private static final ResourceBundle resources = ResourceBundle.getBundle("com.l7tech.console.resources.XslTransformationPropertiesDialog");

    public static final int CONTROL_SPACING = 5;

    private JPanel mainPanel;
    private JPanel xsltPanel;
    private JButton fileButton;
    private JButton urlButton;
    private JTextField nameField;
    private JLabel xsltLabel;

    private final XslTransformationPropertiesDialog xslDialog;
    private final UIAccessibility uiAccessibility;

    public static final String XSL_IDENTITY_TRANSFORM = "<?xml version=\"1.0\"?>\n" +
           "<xsl:stylesheet version=\"1.0\" xmlns:xsl='http://www.w3.org/1999/XSL/Transform'>\n" +
           "  <!-- Identity transform -->\n" +
           "  <xsl:template match=\"@*|*|processing-instruction()|comment()\" mode=\"recursive-copy\">\n" +
           "    <xsl:copy>\n" +
           "      <xsl:apply-templates select=\"*|@*|text()|processing-instruction()|comment()\" mode=\"recursive-copy\"/>\n" +
           "    </xsl:copy>\n" +
           "  </xsl:template>\n" +
           "</xsl:stylesheet>";

    public XslTransformationSpecifyPanel(final XslTransformationPropertiesDialog parent, final XslTransformation assertion) {
        this.xslDialog = parent;

        XMLContainer xmlContainer = new XMLContainer(true);
        final JComponent xmlEditor = xmlContainer.getView();
        xsltLabel.setLabelFor(xmlEditor);
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

        fileButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                readFromFile();
            }
        });

        urlButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                OkCancelDialog dlg = new OkCancelDialog(parent, resources.getString("urlDialog.title"), true, new UrlPanel(resources.getString("urlDialog.prompt"), null));
                Utilities.centerOnScreen(dlg);
                dlg.pack();
                dlg.setVisible(true);
                String url = (String)dlg.getValue();
                if (url != null) {
                    readFromUrl(url);
                }
            }
        });

        // Initialize the XML editor to the XSL from the assertion, if any, else to an identity transform
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                String xsl = XSL_IDENTITY_TRANSFORM;

                AssertionResourceInfo ri = assertion.getResourceInfo();
                if (ri instanceof StaticResourceInfo) {
                    StaticResourceInfo sri = (StaticResourceInfo)ri;
                    xsl = sri.getDocument();
                }

                assertion.getResourceInfo();
                if (xsl != null) {
                    XMLEditor editor = uiAccessibility.getEditor();
                    try {
                        editor.setText(XmlUtil.nodeToFormattedString(XmlUtil.parse(new StringReader(xsl), false)));
                    } catch (Exception e) {
                        log.log(Level.WARNING, "Couldn't parse initial XSLT", e);
                        editor.setText(xsl);
                    }
                    editor.setLineNumber(1);
                }
            }
        });

        xsltPanel.setLayout(new BorderLayout());
        xsltPanel.add(xmlEditor, BorderLayout.CENTER);

        if (assertion != null && assertion.getTransformName() != null) {
            nameField.setText(assertion.getTransformName());
        }

        setLayout(new BorderLayout());
        add(mainPanel, BorderLayout.CENTER);
    }

    public UIAccessibility getUiAccessibility() {
        return uiAccessibility;
    }

    public JTextField getNameField() {
        return nameField;
    }


    public JButton getFileButton() {
        return fileButton;
    }

    public JButton getUrlButton() {
        return urlButton;
    }

    private void readFromFile() {
        JFileChooser dlg = Utilities.createJFileChooser();

        if (JFileChooser.APPROVE_OPTION != dlg.showOpenDialog(this)) {
            return;
        }

        FileInputStream fis = null;
        String filename = dlg.getSelectedFile().getAbsolutePath();
        try {
            fis = new FileInputStream(dlg.getSelectedFile());

            // try to get document
            Document doc;
            try {
                doc = XmlUtil.parse(fis);
            } catch (SAXException e) {
                xslDialog.displayError(resources.getString("error.noxmlaturl") + " " + filename, null);
                log.log(Level.FINE, "cannot parse " + filename, e);
                return;
            } catch (IOException e) {
                xslDialog.displayError(resources.getString("error.noxmlaturl") + " " + filename, null);
                log.log(Level.FINE, "cannot parse " + filename, e);
                return;
            }
            // check if it's a xslt
            if (docIsXsl(doc)) {
                // set the new xslt
                String printedxml;
                try {
                    printedxml = XmlUtil.nodeToFormattedString(doc);
                } catch (IOException e) {
                    String msg = "error serializing document";
                    xslDialog.displayError(msg, null);
                    log.log(Level.FINE, msg, e);
                    return;
                }
                uiAccessibility.getEditor().setText(printedxml);
                //okButton.setEnabled(true);
            } else {
                xslDialog.displayError(resources.getString("error.urlnoxslt") + " " + filename, null);
            }
        } catch (FileNotFoundException e) {
            log.log(Level.FINE, "cannot open file" + filename, e);
        } finally {
            ResourceUtils.closeQuietly(fis);
        }
    }

    private void readFromUrl(String urlstr) {
        // try to get document
        InputStream is;
        try {
            is = new URL(urlstr).openStream();
        } catch (IOException e) {
            xslDialog.displayError(resources.getString("error.urlnocontent") + " " + urlstr, null);
            return;
        }
        Document doc;
        try {
            doc = XmlUtil.parse(is);
        } catch (SAXException e) {
            xslDialog.displayError(resources.getString("error.noxmlaturl") + " " + urlstr, null);
            log.log(Level.FINE, "cannot parse " + urlstr, e);
            return;
        } catch (IOException e) {
            xslDialog.displayError(resources.getString("error.noxmlaturl") + " " + urlstr, null);
            log.log(Level.FINE, "cannot parse " + urlstr, e);
            return;
        }
        // check if it's a xslt
        if (!docIsXsl(doc)) {
            xslDialog.displayError(resources.getString("error.urlnoxslt") + " " + urlstr, null);
        } else {
            // set the new xslt
            String printedxml;
            try {
                printedxml = XmlUtil.nodeToFormattedString(doc);
            } catch (IOException e) {
                String msg = "error serializing document";
                xslDialog.displayError(msg, null);
                log.log(Level.FINE, msg, e);
                return;
            }
            uiAccessibility.getEditor().setText(printedxml);
            //okButton.setEnabled(true);
        }
    }

    String check() {
        String contents = uiAccessibility.getEditor().getText();
        if (contents == null || contents.length() == 0 || !docIsXsl(contents)) {
            return resources.getString("error.notxslt");
        }

        return null;
    }

    void updateModel(XslTransformation assertion) {
        assertion.setResourceInfo(new StaticResourceInfo(uiAccessibility.getEditor().getText()));
        assertion.setTransformName(nameField.getText());
    }

    private static boolean docIsXsl(String str) {
        if (str == null || str.length() < 1) {
            log.finest("empty doc");
            return false;
        }
        Document doc;
        try {
            doc = XmlUtil.stringToDocument(str);
        } catch (SAXException e) {
            log.log(Level.WARNING, "Couldn't parse XSLT", e);
            return false;
        }
        return doc != null && docIsXsl(doc);
    }

    private static boolean docIsXsl(Document doc) {
        try {
            TransformerFactory.newInstance().newTemplates(new DOMSource(XmlUtil.parse(new StringReader(XmlUtil.nodeToString(doc)), false)));
        } catch (Exception e) {
            log.log(Level.INFO, "document is not valid xslt", e);
            return false;
        }
        return true;
    }

}
