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
import com.l7tech.common.gui.util.DialogDisplayer;
import com.l7tech.common.gui.util.FileChooserUtil;
import com.l7tech.common.gui.widgets.OkCancelDialog;
import com.l7tech.common.gui.widgets.UrlPanel;
import com.l7tech.common.util.ResourceUtils;
import com.l7tech.common.util.XmlUtil;
import com.l7tech.common.util.ExceptionUtils;
import com.l7tech.common.util.HexUtils;
import com.l7tech.common.io.ByteOrderMarkInputStream;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.policy.AssertionResourceInfo;
import com.l7tech.policy.StaticResourceInfo;
import com.l7tech.policy.assertion.xml.XslTransformation;
import com.l7tech.console.util.TopComponents;
import com.l7tech.console.SsmApplication;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.swing.*;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.URIResolver;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamSource;
import javax.xml.transform.dom.DOMSource;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.security.AccessControlException;

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
           "  <xsl:template match=\"@*|*|processing-instruction()|comment()\">\n" +
           "    <xsl:copy>\n" +
           "      <xsl:apply-templates select=\"*|@*|text()|processing-instruction()|comment()\"/>\n" +
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
        popupModel.removeAction(ActionModel.getActionByName(ActionModel.PARSE_ACTION));
        
        if (TopComponents.getInstance().isApplet()) {
            // Search action tries to get the class loader
            popupModel.removeAction(ActionModel.getActionByName(ActionModel.INSERT_ACTION));
            popupModel.removeAction(ActionModel.getActionByName(ActionModel.SEARCH_ACTION));
            popupModel.removeAction(ActionModel.getActionByName(ActionModel.COMMENT_ACTION));
        }

        xmlContainer.getTreePopupModel().removeAction(ActionModel.getActionByName(ActionModel.TREE_SELECTNODE_ACTION));
        xmlContainer.getTreePopupModel().removeAction(ActionModel.getActionByName(ActionModel.TREE_COMMENTNODE_ACTION));
        xmlContainer.getTreePopupModel().removeAction(ActionModel.getActionByName(ActionModel.TREE_COPYNODE_ACTION));
        xmlContainer.getTreePopupModel().removeAction(ActionModel.getActionByName(ActionModel.TREE_CUTNODE_ACTION));
        xmlContainer.getTreePopupModel().removeAction(ActionModel.getActionByName(ActionModel.TREE_EDITNODE_ACTION));
        xmlContainer.getTreePopupModel().removeAction(ActionModel.getActionByName(ActionModel.TREE_CLEANHISTORY_ACTION));
        xmlContainer.getTreePopupModel().removeAction(ActionModel.getActionByName(ActionModel.TREE_ADDHISTORY_ACTION));
        xmlContainer.getTreePopupModel().removeAction(ActionModel.getActionByName(ActionModel.TREE_PREVIOUS_ACTION));
        xmlContainer.getTreePopupModel().removeAction(ActionModel.getActionByName(ActionModel.TREE_NEXT_ACTION));

        boolean lastWasSeparator = true; // remove trailing separator
        for (int i=popupModel.size()-1; i>=0; i--) {
            boolean isSeparator = popupModel.isSeparator(i);
            if (isSeparator && (i==0 || lastWasSeparator)) {
                popupModel.removeSeparator(i);
            } else {
                lastWasSeparator = isSeparator;
            }
        }

        fileButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                readFromFile();
            }
        });

        urlButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                final OkCancelDialog dlg = new OkCancelDialog(parent, resources.getString("urlDialog.title"), true, new UrlPanel(resources.getString("urlDialog.prompt"), null));
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
        });

        // Initialize the XML editor to the XSL from the assertion, if any, else to an identity transform
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                String xsl = null;

                AssertionResourceInfo ri = assertion.getResourceInfo();
                if (ri instanceof StaticResourceInfo) {
                    StaticResourceInfo sri = (StaticResourceInfo)ri;
                    xsl = sri.getDocument();
                }

                // Default to identity transform
                if (xsl == null || xsl.trim().length() < 1)
                    xsl = XSL_IDENTITY_TRANSFORM;

                XMLEditor editor = uiAccessibility.getEditor();
                try {
                    editor.setText(XmlUtil.nodeToString(XmlUtil.parse(new StringReader(xsl), false)));
                } catch (Exception e) {
                    log.log(Level.WARNING, "Couldn't parse initial XSLT", e);
                    editor.setText(xsl);
                }
                editor.setLineNumber(1);
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
        SsmApplication.doWithJFileChooser(new FileChooserUtil.FileChooserUser() {
            public void useFileChooser(JFileChooser fc) {
                doRead(fc);
            }
        });
    }

    private void doRead(JFileChooser dlg) {
        if (JFileChooser.APPROVE_OPTION != dlg.showOpenDialog(this)) {
            return;
        }

        ByteOrderMarkInputStream bomis = null;
        String filename = dlg.getSelectedFile().getAbsolutePath();
        try {
            String encoding;
            try {
                bomis = new ByteOrderMarkInputStream(new FileInputStream(dlg.getSelectedFile()));
                encoding = bomis.getEncoding();
            } catch (FileNotFoundException e) {
                log.log(Level.FINE, "cannot open file" + filename, e);
                return;
            } catch (IOException e) {
                log.log(Level.FINE, "cannot parse" + filename, e);
                return;
            }

            // try to get document
            Document doc;
            byte[] bytes;
            try {
                bytes = HexUtils.slurpStreamLocalBuffer(bomis);
                doc = XmlUtil.parse(new ByteArrayInputStream(bytes));
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

            try {
                docIsXsl(doc);
                // set the new xslt
                String printedxml;
                try {
                    if (encoding == null) {
                        log.log(Level.FINE, "Unable to detect character encoding for " + filename + "; will use platform default encoding");
                        printedxml = new String(bytes);
                    } else {
                        printedxml = new String(bytes, encoding);
                    }
                } catch (IOException e) {
                    String msg = "error serializing document";
                    xslDialog.displayError(msg, null);
                    log.log(Level.FINE, msg, e);
                    return;
                }
                uiAccessibility.getEditor().setText(printedxml);
                //okButton.setEnabled(true);
            } catch (SAXException e) {
                xslDialog.displayError(resources.getString("error.urlnoxslt") + " " + filename + ": " + ExceptionUtils.getMessage(e), null);
            }
        } finally {
            ResourceUtils.closeQuietly(bomis);
        }
    }

    private void readFromUrl(String urlstr) {
        // try to get document
        byte[] bytes;
        String encoding;
        ByteOrderMarkInputStream bomis = null;
        InputStream httpStream = null;
        try {
            URLConnection conn = new URL(urlstr).openConnection();
            String ctype = conn.getContentType();
            encoding = ctype == null ? null : ContentTypeHeader.parseValue(ctype).getEncoding();
            httpStream = conn.getInputStream();
            bytes = HexUtils.slurpStreamLocalBuffer(httpStream);
            bomis = new ByteOrderMarkInputStream(new ByteArrayInputStream(bytes));
            if (encoding == null) encoding = bomis.getEncoding();
        } catch (AccessControlException ace) {
            TopComponents.getInstance().showNoPrivilegesErrorMessage();
            return;
        } catch (IOException e) {
            xslDialog.displayError(resources.getString("error.urlnocontent") + " " + urlstr, null);
            return;
        } finally {
            ResourceUtils.closeQuietly(httpStream);
            ResourceUtils.closeQuietly(bomis);
        }

        Document doc;
        try {
            doc = XmlUtil.parse(new ByteArrayInputStream(bytes));
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
        try {
            docIsXsl(doc);
            // set the new xslt
            String printedxml;

            try {
                if (encoding == null) {
                    log.log(Level.FINE, "Unable to determine character encoding for " + urlstr + "; using platform default");
                    printedxml = new String(bytes);
                } else {
                    printedxml = new String(bytes, encoding);
                }
            } catch (IOException e) {
                String msg = "error reading document";
                xslDialog.displayError(msg, null);
                log.log(Level.FINE, msg, e);
                return;
            }
            uiAccessibility.getEditor().setText(printedxml);
            //okButton.setEnabled(true);
        } catch (SAXException e) {
            xslDialog.displayError(resources.getString("error.urlnoxslt") + " " + urlstr + ": " + ExceptionUtils.getMessage(e), null);
        }
    }

    String check() {
        String contents = uiAccessibility.getEditor().getText();
        try {
            docIsXsl(contents);
        } catch (SAXException e) {
            return resources.getString("error.notxslt") + ": " + ExceptionUtils.getMessage(e);
        }

        return null;
    }

    void updateModel(XslTransformation assertion) {
        assertion.setResourceInfo(new StaticResourceInfo(uiAccessibility.getEditor().getText()));
        assertion.setTransformName(nameField.getText());
    }

    private static void docIsXsl(String str) throws SAXException {
        if (str == null || str.length() < 1) {
            throw new SAXException("empty document");
        }
        Document doc = XmlUtil.stringToDocument(str);
        if (doc == null) throw new SAXException("null document");
        docIsXsl(doc);
    }

    private static void docIsXsl(Document doc) throws SAXException {
        try {
            TransformerFactory tf = TransformerFactory.newInstance();
            tf.setURIResolver( new URIResolver(){
                public Source resolve( String href, String base ) throws TransformerException {
                    return new StreamSource(new StringReader("<a xsl:version=\"1.0\" xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\"/>"));
                }
            } );            
            tf.newTemplates(new DOMSource(XmlUtil.parse(new StringReader(XmlUtil.nodeToString(doc)), false)));
        } catch (Exception e) {
            throw new SAXException("Document is not valid XSLT: " + ExceptionUtils.getMessage(e), e);
        }
    }

}
