/*
 * Copyright (C) 2004-2007 Layer 7 Technologies Inc.
 */
package com.l7tech.console.panels;

import com.japisoft.xmlpad.PopupModel;
import com.japisoft.xmlpad.UIAccessibility;
import com.japisoft.xmlpad.XMLContainer;
import com.japisoft.xmlpad.action.ActionModel;
import com.japisoft.xmlpad.editor.XMLEditor;
import com.l7tech.common.gui.util.DialogDisplayer;
import com.l7tech.common.gui.util.Utilities;
import com.l7tech.common.gui.util.FileChooserUtil;
import com.l7tech.common.gui.widgets.OkCancelDialog;
import com.l7tech.common.gui.widgets.UrlPanel;
import com.l7tech.common.util.XmlUtil;
import com.l7tech.common.xml.Wsdl;
import com.l7tech.common.xml.WsdlSchemaAnalizer;
import com.l7tech.common.xml.schema.SchemaEntry;
import com.l7tech.console.action.Actions;
import com.l7tech.console.tree.policy.SchemaValidationTreeNode;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.TopComponents;
import com.l7tech.console.SsmApplication;
import com.l7tech.objectmodel.ObjectModelException;
import com.l7tech.policy.AssertionResourceInfo;
import com.l7tech.policy.SingleUrlResourceInfo;
import com.l7tech.policy.StaticResourceInfo;
import com.l7tech.policy.assertion.AssertionResourceType;
import com.l7tech.policy.assertion.GlobalResourceInfo;
import com.l7tech.policy.assertion.xml.SchemaValidation;
import com.l7tech.service.PublishedService;
import org.apache.xml.serialize.OutputFormat;
import org.apache.xml.serialize.XMLSerializer;
import org.dom4j.DocumentException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import javax.wsdl.Binding;
import javax.wsdl.WSDLException;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.AccessControlException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * A dialog to view / configure the properties of a schema validation assertion.
 */
public class SchemaValidationPropertiesDialog extends JDialog {
    private static final Logger logger = Logger.getLogger(SchemaValidationPropertiesDialog.class.getName());
    private static final ResourceBundle resources =
            ResourceBundle.getBundle("com.l7tech.console.resources.SchemaValidationPropertiesDialog", Locale.getDefault());
    private final String BORDER_TITLE_PREFIX = resources.getString("modeBorderTitlePrefix.text");
    public static final String NOTSET = "<not set>";

    // Top-level widgets
    private JComboBox cbSchemaLocation;
    private JButton helpButton;
    private JButton okButton;
    private JButton cancelButton;

    // Cut points where we will be doing surgery on the IDEA form
    private JPanel rootPanel;
    private JPanel innerPanel;
    private JPanel borderPanel;
    private JTabbedPane innerTabHolder;
    private JPanel specifyTab;
    private JPanel specifyUrlTab;

    // Widgets specific to MODE_SPECIFY_SCHEMA
    private JButton readFromWsdlButton;
    private JButton readUrlButton;
    private JButton readFileButton;
    private JRadioButton rbApplyToBody;
    private JRadioButton rbApplyToArgs;
    private JPanel xmldisplayPanel;
    private XMLContainer xmlContainer;

    // Widgets specific to MODE_SPECIFY_URL
    private JTextField specifyUrlField;
    private JPanel globalURLTab;
    private JComboBox globalSchemaCombo;
    private JButton editGlobalXMLSchemasButton;

    // Other fields
    private UIAccessibility uiAccessibility;
    private SchemaValidation schemaValidationAssertion;
    private PublishedService service;

    private final Logger log = Logger.getLogger(getClass().getName());

    // cached values
    private boolean wsdlBindingSoapUseIsLiteral;

    private static int CONTROL_SPACING = 5;

    // Combo box strings that also serve as mode identifiers
    private final String MODE_SPECIFY = resources.getString("specifyItem.label");
    private final String MODE_SPECIFY_URL = resources.getString("specifyUrlItem.label");
    private final String MODE_GLOBAL = resources.getString("globalItem.label");
    //private final String MODE_FETCH_XSI_URL = resources.getString("messageUrlItem.label");
    private final String[] MODES = new String[] {
            MODE_SPECIFY,
            MODE_SPECIFY_URL,
            MODE_GLOBAL
            //MODE_FETCH_XSI_URL,  // TODO implement later on
    };

    /** Set to true if the Ok button completes. */
    private boolean changesCommitted = false;

    public SchemaValidationPropertiesDialog(Frame owner, SchemaValidationTreeNode node, PublishedService service) {
        super(owner, true);
        final SchemaValidation assertion = node.asAssertion();
        if (assertion == null) throw new IllegalArgumentException("Schema Validation Node == null");
        schemaValidationAssertion = assertion;
        this.service = service;
        initialize();
    }

    public SchemaValidationPropertiesDialog(Frame owner, SchemaValidation assertion, PublishedService service) {
        super(owner, true);
        if (assertion == null) {
            throw new IllegalArgumentException("Schema Validation == null");
        }
        schemaValidationAssertion = assertion;
        this.service = service;
        initialize();
    }

    private void initialize() {
        DialogDisplayer.suppressSheetDisplay(this); // incompatible with xmlpad

        setTitle(resources.getString("window.title"));

        // create controls
        allocControls();

        // do layout stuff
        setContentPane(rootPanel);

        // Surgery on IDEA form -- remove tabs, and hook up the drop-down to switch subpanes instead
        innerTabHolder.remove(specifyTab);
        innerTabHolder.remove(specifyUrlTab);
        innerTabHolder.remove(globalURLTab);
        innerPanel.removeAll();
        innerPanel.setLayout(new BoxLayout(innerPanel, BoxLayout.Y_AXIS));
        innerPanel.add(specifyTab);
        innerPanel.add(specifyUrlTab);
        innerPanel.add(globalURLTab);
        specifyUrlTab.setVisible(false);

        // Attach XML editor
        xmldisplayPanel.removeAll();
        xmldisplayPanel.setLayout(new BorderLayout(0, CONTROL_SPACING));
        xmldisplayPanel.add(xmlContainer.getView(), BorderLayout.CENTER);

        Utilities.setEscKeyStrokeDisposes(this);

        // create callbacks
        cbSchemaLocation.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                updateModeComponents();
            }
        });
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
                Actions.invokeHelp(SchemaValidationPropertiesDialog.this);
            }
        });
        readFromWsdlButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                readFromWsdl();
            }
        });
        editGlobalXMLSchemasButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                launchGlobalSchemasDlg();
            }
        });

        readFromWsdlButton.setEnabled(wsdlExtractSupported());
        readFromWsdlButton.setToolTipText("Extract schema from WSDL; available for 'document/literal' style services");
        readUrlButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                final OkCancelDialog dlg = new OkCancelDialog(SchemaValidationPropertiesDialog.this,
                                                        resources.getString("urlDialog.title"),
                                                        true,
                                                        new UrlPanel(resources.getString("urlDialog.prompt"), null));
                dlg.pack();
                Utilities.centerOnScreen(dlg);
                DialogDisplayer.display(dlg, new Runnable() {
                    public void run() {
                        String url = (String)dlg.getValue();
                        if (url != null) {
                            try {
                                readFromUrl(url);
                            } catch (AccessControlException ace) {
                                TopComponents.getInstance().showNoPrivilegesErrorMessage();
                            }
                        }
                    }
                });
            }
        });

        readFileButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                readFromFile();
            }
        });

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                modelToView();
                updateModeComponents();
            }
        });

        reloadGlobalSchemaList();
    }

    private void modelToView() {
        if (schemaValidationAssertion.isApplyToArguments()) {
            rbApplyToArgs.setSelected(true);
        } else {
            rbApplyToBody.setSelected(true);
        }

        AssertionResourceInfo ri = schemaValidationAssertion.getResourceInfo();
        AssertionResourceType rit = ri.getType();
        if (AssertionResourceType.SINGLE_URL.equals(rit)) {
            cbSchemaLocation.setSelectedItem(MODE_SPECIFY_URL);

            if (ri instanceof SingleUrlResourceInfo) {
                SingleUrlResourceInfo suri = (SingleUrlResourceInfo)ri;
                specifyUrlField.setText(suri.getUrl());
            }
        } else if (AssertionResourceType.STATIC.equals(rit)) {
            cbSchemaLocation.setSelectedItem(MODE_SPECIFY);

            if (ri instanceof StaticResourceInfo) {
                StaticResourceInfo sri = (StaticResourceInfo)ri;
                String doc = sri.getDocument();
                if (doc != null && doc.trim().length() > 0) {
                    XMLEditor editor = uiAccessibility.getEditor();
                    editor.setText(reformatxml(doc));
                    editor.setLineNumber(1);
                }
            }
        } else if (AssertionResourceType.GLOBAL_RESOURCE.equals(rit)) {
            cbSchemaLocation.setSelectedItem(MODE_GLOBAL);

            if (ri instanceof GlobalResourceInfo) {
                GlobalResourceInfo gri = (GlobalResourceInfo)ri;
                DefaultComboBoxModel model = (DefaultComboBoxModel)globalSchemaCombo.getModel();
                boolean found = false;
                for (int i = 0; i < model.getSize(); i++) {
                    String comboBoxItem = (String)model.getElementAt(i);
                    if (gri.getId() != null && gri.getId().equals(comboBoxItem)) {
                        found = true;
                        globalSchemaCombo.setSelectedIndex(i);
                        break;
                    }
                }
                if (!found) {
                    globalSchemaCombo.setSelectedItem(NOTSET);
                }
            }
        } else {
            throw new RuntimeException("Unhandled AssertionResourceType " + rit);
        }
    }

    private String getCurrentFetchMode() {
        return (String)cbSchemaLocation.getSelectedItem();
    }

    private void updateModeComponents() {
        String mode = getCurrentFetchMode();
        if (MODE_SPECIFY_URL.equals(mode)) {
            specifyUrlTab.setVisible(true);
            specifyTab.setVisible(false);
            globalURLTab.setVisible(false);
        } else if (MODE_SPECIFY.equals(mode)) {
            specifyTab.setVisible(true);
            specifyUrlTab.setVisible(false);
            globalURLTab.setVisible(false);
        } else if (MODE_GLOBAL.equals(mode)) {
            globalURLTab.setVisible(true);
            specifyUrlTab.setVisible(false);
            specifyTab.setVisible(false);
        } else {
            throw new RuntimeException("unhandled schema validation mode: " + mode);
        }
        Border border = borderPanel.getBorder();
        if (border instanceof TitledBorder) {
            TitledBorder tb = (TitledBorder)border;
            tb.setTitle(BORDER_TITLE_PREFIX + " " + mode);
        }
        innerPanel.revalidate();
    }

    private void launchGlobalSchemasDlg() {
        GlobalSchemaDialog dlg = new GlobalSchemaDialog(this);
        dlg.pack();
        Utilities.centerOnScreen(dlg);
        DialogDisplayer.display(dlg, new Runnable() {
            public void run() {
                reloadGlobalSchemaList();
            }
        });
    }

    private void reloadGlobalSchemaList() {
        Registry reg = Registry.getDefault();
        if (reg == null || reg.getSchemaAdmin() == null) {
            throw new RuntimeException("No access to registry. Cannot check for unresolved imports.");
        }
        try {
            Collection<SchemaEntry> allschemas = reg.getSchemaAdmin().findAllSchemas();
            ArrayList<String> schemaNames = new ArrayList<String>();
            if (allschemas != null) {
                for (SchemaEntry s : allschemas) {
                    schemaNames.add(s.getName());
                }
            }
            schemaNames.add(NOTSET);
            globalSchemaCombo.setModel(new DefaultComboBoxModel(schemaNames.toArray(new String[]{})));
            globalSchemaCombo.setSelectedItem(NOTSET);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Cannot get global schemas", e);
        }
    }

    /**
     * Determine whether wsdl extracting is supported. This is supported for 'document'
     * style services only.
     * Traverse all the soap bindings, and if all the bindings are of style 'document'
     * returns true.
     *
     * @return true if 'document' stle supported, false otherwise
     */
    private boolean wsdlExtractSupported() {
        if (service == null || !service.isSoap()) return false;
        String wsdlXml = service.getWsdlXml();
        if (wsdlXml == null) return false;
        analyzeWsdl(wsdlXml);
        // bugzilla #2081, if we know we can't extract a wsdl, then let's disable feature

        try {
            NodeList wsdlschemas = WsdlSchemaAnalizer.extractSchemaElementFromWsdl(XmlUtil.stringToDocument(wsdlXml));
            if (wsdlschemas == null || wsdlschemas.getLength() < 1) {
                return false;
            }
        } catch (SAXException e) {
            logger.log(Level.WARNING, "wsdl is not well formed?", e);
            // here we simply return false because if wsdl is not well formed then
            // we know for sure there is no way we can possibly extract a schema from it
            return false;
        }
        return wsdlBindingSoapUseIsLiteral;
    }

    /**
     * Determine what kind of service is defined in the wsdl (doc/literal, rpc/encoded, rpc/literal)
     */
    private void analyzeWsdl(String wsdlXml) {
        try {
            Wsdl wsdl = Wsdl.newInstance(null, new StringReader(wsdlXml));
            wsdl.setShowBindings(Wsdl.SOAP_BINDINGS);
            Collection bindings = wsdl.getBindings();
            if (bindings.isEmpty()) return;

            try {
                for (Iterator iterator = bindings.iterator(); iterator.hasNext();) {
                    Binding binding = (Binding)iterator.next();
                    if (!Wsdl.STYLE_DOCUMENT.equals(wsdl.getBindingStyle(binding))) {
                        break;
                    }
                }
                wsdlBindingSoapUseIsLiteral = true;
                for (Iterator iterator = bindings.iterator(); iterator.hasNext();) {
                    Binding binding = (Binding)iterator.next();
                    if (!Wsdl.USE_LITERAL.equals(wsdl.getSoapUse(binding))) {
                        wsdlBindingSoapUseIsLiteral = false;
                        break;
                    }
                }
            } catch (WSDLException e) {
                log.log(Level.WARNING, "Could not determine soap use", e);
            }
        } catch (WSDLException e) {
            log.log(Level.WARNING, "Wsdl parsing error", e);
        }
    }

    /**
     * returns true if there was at least one unresolved import
     *
     * NOTE this code is reused (copy/paste) in GlobalSchemaDialog
     */
    private boolean checkForUnresolvedImports(Document schemaDoc) {
        Element schemael = schemaDoc.getDocumentElement();
        java.util.List listofimports = XmlUtil.findChildElementsByName(schemael, schemael.getNamespaceURI(), "import");
        if (listofimports.isEmpty()) return false;
        ArrayList unresolvedImportsList = new ArrayList();
        ArrayList resolutionSuggestionList = new ArrayList();
        Registry reg = Registry.getDefault();
        if (reg == null || reg.getSchemaAdmin() == null) {
            throw new RuntimeException("No access to registry. Cannot check for unresolved imports.");
        }
        for (Iterator iterator = listofimports.iterator(); iterator.hasNext();) {
            Element importEl = (Element) iterator.next();
            String importns = importEl.getAttribute("namespace");
            String importloc = importEl.getAttribute("schemaLocation");
            try {
                if (importloc == null || reg.getSchemaAdmin().findByName(importloc).isEmpty()) {
                    //if (importns == null || reg.getSchemaAdmin().findByTNS(importns).isEmpty()) {
                        if (importloc != null) {
                            unresolvedImportsList.add(importloc);

                            // Check for the desired namespace with different location
                            if (importns != null) {
                                for (SchemaEntry entry : reg.getSchemaAdmin().findByTNS(importns)) {
                                    if (entry.getName() != null && entry.getName().length() > 0)
                                        resolutionSuggestionList.add(entry.getName());
                                }
                            }
                        } else {
                            unresolvedImportsList.add(importns);
                        }
                    //}
                }
            } catch (ObjectModelException e) {
                throw new RuntimeException("Error trying to look for import schema in global schema");
            }
        }
        if (!unresolvedImportsList.isEmpty()) {
            StringBuffer msg = new StringBuffer("The assertion cannot be saved because the schema\n" +
                                                "contains the following unresolved imported schemas:\n");
            for (Iterator iterator = unresolvedImportsList.iterator(); iterator.hasNext();) {
                msg.append("  " + iterator.next());
                msg.append("\n");
            }
            if (!resolutionSuggestionList.isEmpty()) {
                msg.append("Note that these schemas are imported but the locations do not match:\n");
                for (Iterator iterator = resolutionSuggestionList.iterator(); iterator.hasNext();) {
                    msg.append("  " + iterator.next());
                    msg.append("\n");
                }
            }
            msg.append("Would you like to import those unresolved schemas now?");
            if (JOptionPane.showConfirmDialog(this, msg, "Unresolved Imports", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                GlobalSchemaDialog globalSchemaManager = new GlobalSchemaDialog(this);
                globalSchemaManager.pack();
                Utilities.centerOnScreen(globalSchemaManager);
                globalSchemaManager.setVisible(true); // TODO change to use DialogDisplayer
            }
            return true;
        }
        return false;
    }

    /** @return true iff. info was committed successfully */
    private boolean commitGlobalTab() {
        GlobalResourceInfo gri = new GlobalResourceInfo();
        if (globalSchemaCombo.getSelectedItem() == null) {
            throw new RuntimeException("the combo has nothing selected?");
            // this shouldn't happen (unless bug)
        } else if (globalSchemaCombo.getSelectedItem().equals(NOTSET)) {
            return false;
        }
        gri.setId(globalSchemaCombo.getSelectedItem().toString());
        schemaValidationAssertion.setResourceInfo(gri);
        return true;
    }

    /** @return true iff. info was committed successfully */
    private boolean commitSpecifyTab() {
        // check that whatever is captured is an xml document, a schema and has a tns
        String contents = uiAccessibility.getEditor().getText();

        try {
            String tns = XmlUtil.getSchemaTNS(contents);
            if (tns == null) {
                displayError("The schema must declare a targetNamespace", null);
                return false;
            }
        } catch (XmlUtil.BadSchemaException e) {
            log.log(Level.WARNING, "issue with schema at hand", e);
            displayError("This does not appear to be a legal schema. Consult log for more information", null);
            return false;
        }
        try {
            Document doc = XmlUtil.stringToDocument(contents);
            if (checkForUnresolvedImports(doc))
                return false;
        } catch (SAXException e) {
            log.log(Level.WARNING, "issue with xml document", e);
            displayError("The schema is not formatted properly. " + e.getMessage(), null);
            return false;
        }

        // Checks pass, commit it
        StaticResourceInfo sri = new StaticResourceInfo();
        sri.setDocument(contents);
        schemaValidationAssertion.setResourceInfo(sri);
        return true;
    }

    /** @return true iff. info was committed successfully */
    private boolean commitSpecifyUrlTab() {
        String url = specifyUrlField.getText();

        if (url == null || url.trim().length() < 1) {
            displayError(resources.getString("error.nourl"), null);
            return false;
        }

        try {
            new URL(url);
        } catch (MalformedURLException e) {
            displayError(resources.getString("error.badurl"), null);
            return false;
        }

        // Checks pass, commit it
        SingleUrlResourceInfo suri = new SingleUrlResourceInfo();
        suri.setUrl(url);
        schemaValidationAssertion.setResourceInfo(suri);
        return true;
    }

    private void ok() {
        String mode = getCurrentFetchMode();

        if (MODE_SPECIFY_URL.equals(mode)) {
            if (!commitSpecifyUrlTab())
                return;
        } else if (MODE_SPECIFY.equals(mode)) {
            if (!commitSpecifyTab())
                return;
        } else if (MODE_GLOBAL.equals(mode)) {
            if (!commitGlobalTab())
                return;
        } else {
            throw new RuntimeException("Unhandled mode " + mode);
        }

        // save new schema
        schemaValidationAssertion.setApplyToArguments(rbApplyToArgs.isSelected());

        // exit
        changesCommitted = true;
        SchemaValidationPropertiesDialog.this.dispose();
    }

    private boolean docIsSchema(Document doc) {
        Element rootEl = doc.getDocumentElement();

        if (!SchemaValidation.TOP_SCHEMA_ELNAME.equals(rootEl.getLocalName())) {
            log.log(Level.WARNING, "document is not schema (top element " + rootEl.getLocalName() +
              " is not " + SchemaValidation.TOP_SCHEMA_ELNAME + ")");
            return false;
        }
        if (!SchemaValidation.W3C_XML_SCHEMA.equals(rootEl.getNamespaceURI())) {
            log.log(Level.WARNING, "document is not schema (namespace is not + " +
              SchemaValidation.W3C_XML_SCHEMA + ")");
            return false;
        }
        return true;
    }

    private Document stringToDoc(String str) {
        Document doc;
        try {
            doc = XmlUtil.stringToDocument(str);
        } catch (SAXException e) {
            log.log(Level.WARNING, "cannot parse doc", e);
            return null;
        }
        return doc;
    }

    private String reformatxml(String input) {
        Document doc = stringToDoc(input);
        try {
            return doc2String(doc);
        } catch (IOException e) {
            log.log(Level.INFO, "reformat could not serialize", e);
            return null;
        }
    }

    private void cancel() {
        SchemaValidationPropertiesDialog.this.dispose();
    }
    public void dispose() {
        xmlContainer.dispose();
        super.dispose();
    }

    private void readFromWsdl() {
        if (service == null) {
            displayError(resources.getString("error.noaccesstowsdl"), null);
            return;
        }
        String wsdl;
        wsdl = service.getWsdlXml();

        Document wsdlDoc = stringToDoc(wsdl);
        if (wsdlDoc == null) {
            displayError(resources.getString("error.nowsdl"), null);
            return;
        }

        final SelectWsdlSchemaDialog schemafromwsdlchooser;
        try {
            schemafromwsdlchooser = new SelectWsdlSchemaDialog(this, wsdlDoc);
        } catch (DocumentException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (SAXParseException e) {
            throw new RuntimeException(e);
        }
        schemafromwsdlchooser.pack();
        Utilities.centerOnScreen(schemafromwsdlchooser);
        DialogDisplayer.display(schemafromwsdlchooser, new Runnable() {
            public void run() {
                String result = schemafromwsdlchooser.getOkedSchema();
                if (result != null) {
                    final XMLEditor editor = uiAccessibility.getEditor();
                    editor.setText(result);
                    editor.setLineNumber(1);
                }
            }
        });
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
            displayError(resources.getString("error.noxmlaturl") + " " + filename, null);
            log.log(Level.FINE, "cannot parse " + filename, e);
            return;
        } catch (IOException e) {
            displayError(resources.getString("error.noxmlaturl") + " " + filename, null);
            log.log(Level.FINE, "cannot parse " + filename, e);
            return;
        }
        // check if it's a schema
        if (docIsSchema(doc)) {
            // set the new schema
            String printedSchema;
            try {
                printedSchema = doc2String(doc);
            } catch (IOException e) {
                String msg = "error serializing document";
                displayError(msg, null);
                log.log(Level.FINE, msg, e);
                return;
            }
            uiAccessibility.getEditor().setText(printedSchema);
            //okButton.setEnabled(true);
        } else {
            displayError(resources.getString("error.urlnoschema") + " " + filename, null);
        }
    }

    private void readFromUrl(String urlstr) {
        if (urlstr == null || urlstr.length() < 1) {
            displayError(resources.getString("error.nourl"), null);
            return;
        }
        // compose input source
        URL url;
        try {
            url = new URL(urlstr);
        } catch (MalformedURLException e) {
            displayError(urlstr + " " + resources.getString("error.badurl"), null);
            log.log(Level.FINE, "malformed url", e);
            return;
        }
        // try to get document
        InputStream is;
        try {
            is = url.openStream();
        } catch (IOException e) {
            displayError(resources.getString("error.urlnocontent") + " " + urlstr, null);
            return;
        }

        Document doc;
        try {
            doc = XmlUtil.parse(is);
        } catch (SAXException e) {
            displayError(resources.getString("error.noxmlaturl") + " " + urlstr, null);
            log.log(Level.FINE, "cannot parse " + urlstr, e);
            return;
        } catch (IOException e) {
            displayError(resources.getString("error.noxmlaturl") + " " + urlstr, null);
            log.log(Level.FINE, "cannot parse " + urlstr, e);
            return;
        }
        // check if it's a schema
        if (docIsSchema(doc)) {
            // set the new schema
            String printedSchema;
            try {
                printedSchema = doc2String(doc);
            } catch (IOException e) {
                String msg = "error serializing document";
                displayError(msg, null);
                log.log(Level.FINE, msg, e);
                return;
            }
            uiAccessibility.getEditor().setText(printedSchema);
            //okButton.setEnabled(true);
        } else {
            displayError(resources.getString("error.urlnoschema") + " " + urlstr, null);
        }
    }

    private String doc2String(Document doc) throws IOException {
        final StringWriter sw = new StringWriter(512);
        XMLSerializer xmlSerializer = new XMLSerializer();
        xmlSerializer.setOutputCharStream(sw);
        OutputFormat of = new OutputFormat();
        of.setIndent(4);
        xmlSerializer.setOutputFormat(of);
        xmlSerializer.serialize(doc);
        return sw.toString();
    }

    private void displayError(String msg, String title) {
        if (title == null) title = resources.getString("error.window.title");
        JOptionPane.showMessageDialog(this, msg, title, JOptionPane.ERROR_MESSAGE);
    }

    private void allocControls() {
        // configure xml editing widget
        cbSchemaLocation.setModel(new DefaultComboBoxModel(MODES));
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
        popupModel.removeAction(ActionModel.getActionByName(ActionModel.TREE_SELECTNODE_ACTION));
        popupModel.removeAction(ActionModel.getActionByName(ActionModel.TREE_COMMENTNODE_ACTION));
        popupModel.removeAction(ActionModel.getActionByName(ActionModel.TREE_COPYNODE_ACTION));
        popupModel.removeAction(ActionModel.getActionByName(ActionModel.TREE_CUTNODE_ACTION));
        popupModel.removeAction(ActionModel.getActionByName(ActionModel.TREE_EDITNODE_ACTION));
        popupModel.removeAction(ActionModel.getActionByName(ActionModel.TREE_CLEANHISTORY_ACTION));
        popupModel.removeAction(ActionModel.getActionByName(ActionModel.TREE_ADDHISTORY_ACTION));
        popupModel.removeAction(ActionModel.getActionByName(ActionModel.TREE_PREVIOUS_ACTION));
        popupModel.removeAction(ActionModel.getActionByName(ActionModel.TREE_NEXT_ACTION));
        popupModel.removeAction(ActionModel.getActionByName(ActionModel.PARSE_ACTION));

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

        ButtonGroup bg = new ButtonGroup();
        bg.add(rbApplyToBody);
        bg.add(rbApplyToArgs);
    }

    /** @return true if the Ok button was pressed and the changes committed successfully. */
    public boolean isChangesCommitted() {
        return changesCommitted;
    }
}
