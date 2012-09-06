/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.console.panels;

import com.japisoft.xmlpad.UIAccessibility;
import com.japisoft.xmlpad.XMLContainer;
import com.japisoft.xmlpad.editor.XMLEditor;
import com.l7tech.common.io.ByteOrderMarkInputStream;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.console.SsmApplication;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.XMLContainerFactory;
import com.l7tech.gateway.common.service.ServiceAdmin;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.FileChooserUtil;
import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.widgets.OkCancelDialog;
import com.l7tech.policy.AssertionResourceInfo;
import com.l7tech.policy.StaticResourceInfo;
import com.l7tech.policy.assertion.xml.XslTransformation;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.IOUtils;
import com.l7tech.util.ResourceUtils;
import com.l7tech.util.TextUtils;
import com.l7tech.xml.xslt.XsltUtil;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.swing.*;
import javax.xml.transform.ErrorListener;
import javax.xml.transform.TransformerException;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Part of {@link XslTransformationPropertiesDialog}.
 *
 * <p>This panel must be disposed after use to ensure UI resources are freed.</p>
 *
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

    private final XMLContainer xmlContainer;
    private final XslTransformationPropertiesDialog xslDialog;
    private final UIAccessibility uiAccessibility;
    private String xmlText;

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

        xmlContainer = XMLContainerFactory.createXmlContainer(true);
        final JComponent xmlEditor = xmlContainer.getView();
        xsltLabel.setLabelFor(xmlEditor);
        uiAccessibility = xmlContainer.getUIAccessibility();

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


        xsltPanel.setLayout(new BorderLayout());
        xsltPanel.add(xmlEditor, BorderLayout.CENTER);

        setLayout(new BorderLayout());
        add(mainPanel, BorderLayout.CENTER);

        setModel(assertion);
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
            Charset encoding;
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
                bytes = IOUtils.slurpStream(bomis);
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
                docIsXsl(doc, "2.0");
                // set the new xslt
                String printedxml;
                if (encoding == null) {
                    log.log(Level.FINE, "Unable to detect character encoding for " + filename + "; will use platform default encoding");
                    printedxml = new String(bytes);
                } else {
                    printedxml = new String(bytes, encoding);
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
        final ServiceAdmin serviceAdmin;
        final Registry reg = Registry.getDefault();
        if (reg == null || reg.getServiceManager() == null) {
            throw new RuntimeException("No access to registry. Cannot download document.");
        } else {
            serviceAdmin = reg.getServiceManager();
        }

        final String xslString;
        try {
            xslString = serviceAdmin.resolveUrlTarget(urlstr, ServiceAdmin.DownloadDocumentType.XSL);
        } catch (IOException e) {
            //this is likely to be a GenericHttpException
            xslDialog.displayError(resources.getString("error.urlnocontent") + " '" + urlstr+"'. " +
                    "Due to: " + ExceptionUtils.getMessage(e), null);
            return;
        }

        Document doc;
        try {
            doc = XmlUtil.parse(xslString);
        } catch (SAXException e) {
            xslDialog.displayError(resources.getString("error.noxmlaturl") + " " + urlstr, null);
            log.log(Level.FINE, "cannot parse " + urlstr, e);
            return;
        }

        // check if it's a xslt
        try {
            docIsXsl(doc, "2.0");
            uiAccessibility.getEditor().setText(xslString);
            //okButton.setEnabled(true);
        } catch (SAXException e) {
            xslDialog.displayError(resources.getString("error.urlnoxslt") + " " + urlstr + ": " + ExceptionUtils.getMessage(e), null);
        }
    }

    String check(String xsltVersion) {
        String contents = getEditorText();
        try {
            docIsXsl(contents, xsltVersion);
        } catch (SAXException e) {
            return resources.getString("error.notxslt") + " " + ExceptionUtils.getMessage(e);
        }

        return null;
    }

    void setModel(XslTransformation assertion){
        // Initialize the XML editor to the XSL from the assertion, if any, else to an identity transform

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
            editor.setText( XmlUtil.nodeToString(XmlUtil.parse(new StringReader(xsl), false)));
        } catch (Exception e) {
            log.log(Level.WARNING, "Couldn't parse initial XSLT", e);
            editor.setText(xsl);
        }
        editor.setLineNumber(1);
  
        if (assertion.getTransformName() != null) {
            nameField.setText(assertion.getTransformName());
        }
    }

    void updateModel(XslTransformation assertion) {
        assertion.setResourceInfo(new StaticResourceInfo(getEditorText()));
        assertion.setTransformName(nameField.getText());
    }

    /**
     * Dispose of any UI resources.
     */
    public void dispose() {
        // The editor cannot be used after it is disposed so record the final text.
        xmlText = uiAccessibility.getEditor().getText();
        xmlContainer.dispose();
    }

    /**
     * Get the editor text, this method is safe to use after disposal of UI resources.
     */
    private String getEditorText() {
        return xmlText != null ? xmlText : uiAccessibility.getEditor().getText();
    }

    private static void docIsXsl(String str, String xsltVersion) throws SAXException {
        if (str == null || str.length() < 1) {
            throw new SAXException("empty document");
        }
        Document doc = XmlUtil.stringToDocument(str);
        if (doc == null) throw new SAXException("null document");
        docIsXsl(doc, xsltVersion);
    }

    private synchronized static void docIsXsl(Document doc, String xsltVersion) throws SAXException {
        final java.util.List<String> errors = new ArrayList<>();
        try {
            final ErrorListener errorListener = new ErrorListener() {
                @Override
                public void warning(TransformerException exception) throws TransformerException {
                    errors.add("Warning: " + ExceptionUtils.getMessage(exception));
                }

                @Override
                public void error(TransformerException exception) throws TransformerException {
                    errors.add("Error: " + ExceptionUtils.getMessage(exception));
                }

                @Override
                public void fatalError(TransformerException exception) throws TransformerException {
                    errors.add("Fatal: " + ExceptionUtils.getMessage(exception));
                }
            };
            XsltUtil.checkXsltSyntax(doc, xsltVersion, errorListener);
        } catch (Exception e) {
            throw new SAXException(ExceptionUtils.getMessage(e) + "\n" + TextUtils.join("\n", errors), e);
        }
    }
}
