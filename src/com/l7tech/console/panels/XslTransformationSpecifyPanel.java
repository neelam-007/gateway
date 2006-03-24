/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.console.panels;

import com.intellij.uiDesigner.core.GridConstraints;
import com.japisoft.xmlpad.PopupModel;
import com.japisoft.xmlpad.UIAccessibility;
import com.japisoft.xmlpad.XMLContainer;
import com.japisoft.xmlpad.action.ActionModel;
import com.japisoft.xmlpad.editor.XMLEditor;
import com.l7tech.common.gui.util.Utilities;
import com.l7tech.common.util.XmlUtil;
import com.l7tech.policy.assertion.xml.XslTransformation;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.swing.*;
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
    private static final String XSL_TOPEL_NAME = "transform";
    private static final String XSL_TOPEL_NAME2 = "stylesheet";
    private static final String XSL_NS = "http://www.w3.org/1999/XSL/Transform";

    private JPanel mainPanel;
    private JPanel xsltPanel;
    private JButton fileButton;
    private JButton urlButton;
    private JTextField nameField;
    private JLabel xsltLabel;

    private final XslTransformationPropertiesDialog xslDialog;
    private final UIAccessibility uiAccessibility;

    public XslTransformationSpecifyPanel(final XslTransformationPropertiesDialog parent, final XslTransformation assertion) {
        this.xslDialog = parent;

        XMLContainer xmlContainer = new XMLContainer(true);
        final JComponent xmlEditor = xmlContainer.getView();
        xmlEditor.setPreferredSize(new Dimension(640, 480));
        xmlEditor.setMinimumSize(new Dimension(400, 300));
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
                dlg.pack();
                dlg.setVisible(true);
                Utilities.centerOnScreen(dlg);
                URL url = (URL)dlg.getValue();
                if (url != null) {
                    readFromUrl(url);
                }
            }
        });

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                String src = assertion.getXslSrc();
                if (src != null) {
                    XMLEditor editor = uiAccessibility.getEditor();
                    try {
                        editor.setText(XmlUtil.nodeToFormattedString(XmlUtil.parse(new StringReader(src), false)));
                    } catch (Exception e) {
                        log.log(Level.WARNING, "Couldn't parse initial XSLT", e);
                        editor.setText(src);
                    }
                    editor.setLineNumber(1);
                }
            }
        });

        xsltPanel.add(xmlEditor, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null));

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

        FileInputStream fis;
        String filename = dlg.getSelectedFile().getAbsolutePath();
        try {
            fis = new FileInputStream(dlg.getSelectedFile());
        } catch (FileNotFoundException e) {
            log.log(Level.FINE, "cannot open file" + filename, e);
            return;
        }

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
    }

    private void readFromUrl(URL url) {
        String urlstr = url.toString();

        // try to get document
        InputStream is;
        try {
            is = url.openStream();
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
        // Null all fetch-mode fields
        assertion.setFetchXsltFromMessageUrls(false);
        assertion.setFetchUrlRegexes(new String[0]);
        assertion.setFetchAllowWithoutStylesheet(false);

        // Set specify-mode fields
        assertion.setXslSrc(uiAccessibility.getEditor().getText());
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
        if (doc == null) return false;
        return docIsXsl(doc);
    }

    private static boolean docIsXsl(Document doc) {
        Element rootEl = doc.getDocumentElement();

        if (!XSL_NS.equals(rootEl.getNamespaceURI())) {
            log.log(Level.WARNING, "document is not valid xslt (namespace is not + " + XSL_NS + ")");
            return false;
        }

        if (XSL_TOPEL_NAME.equals(rootEl.getLocalName())) {
            return true;
        } else if (XSL_TOPEL_NAME2.equals(rootEl.getLocalName())) {
            return true;
        }
        log.log(Level.WARNING, "document is not xslt (top element " + rootEl.getLocalName() +
          " is not " + XSL_TOPEL_NAME + " or " + XSL_TOPEL_NAME2 + ")");
        return false;
    }

}
