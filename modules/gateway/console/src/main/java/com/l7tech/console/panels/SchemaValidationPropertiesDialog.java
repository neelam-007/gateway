/*
 * Copyright (C) 2004-2007 Layer 7 Technologies Inc.
 */
package com.l7tech.console.panels;

import com.japisoft.xmlpad.PopupModel;
import com.japisoft.xmlpad.UIAccessibility;
import com.japisoft.xmlpad.XMLContainer;
import com.japisoft.xmlpad.action.ActionModel;
import com.japisoft.xmlpad.editor.XMLEditor;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.console.SsmApplication;
import com.l7tech.console.action.Actions;
import com.l7tech.console.tree.policy.AssertionTreeNode;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.TopComponents;
import com.l7tech.gateway.common.schema.SchemaEntry;
import com.l7tech.gateway.common.schema.SchemaAdmin;
import com.l7tech.gateway.common.schema.FetchSchemaFailureException;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.FileChooserUtil;
import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.widgets.OkCancelDialog;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.policy.AssertionResourceInfo;
import com.l7tech.policy.SingleUrlResourceInfo;
import com.l7tech.policy.StaticResourceInfo;
import com.l7tech.policy.assertion.AssertionResourceType;
import com.l7tech.policy.assertion.GlobalResourceInfo;
import com.l7tech.policy.assertion.MessageTargetableAssertion;
import com.l7tech.policy.assertion.TargetMessageType;
import com.l7tech.policy.assertion.xml.SchemaValidation;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Pair;
import com.l7tech.wsdl.Wsdl;
import com.l7tech.wsdl.WsdlSchemaAnalizer;
import org.dom4j.DocumentException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import javax.wsdl.Binding;
import javax.wsdl.WSDLException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.AccessControlException;
import java.text.MessageFormat;
import java.util.*;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * A dialog to view / configure the properties of a schema validation assertion.
 */
public class SchemaValidationPropertiesDialog extends LegacyAssertionPropertyDialog {
    private static final Logger logger = Logger.getLogger(SchemaValidationPropertiesDialog.class.getName());
    private static final ResourceBundle resources =
            ResourceBundle.getBundle("com.l7tech.console.resources.SchemaValidationPropertiesDialog", Locale.getDefault());
    private final String BORDER_TITLE_PREFIX = resources.getString("modeBorderTitlePrefix.text");
    private static final String WSDL_NSURI = "http://schemas.xmlsoap.org/wsdl/";
    private static final String W3C_SCHEMA_NSURI = "http://www.w3.org/2001/XMLSchema";
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
    private TargetMessagePanel targetMessagePanel;

    // Other fields
    private final boolean readOnly;
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

    private volatile TargetMessageType inferredTargetMessageType;

    public SchemaValidationPropertiesDialog(Frame owner, AssertionTreeNode<SchemaValidation> node, TargetMessageType inferredTarget) {
        super(owner, node.asAssertion(), true);
        final SchemaValidation assertion = node.asAssertion();
        if (assertion == null) throw new IllegalArgumentException("Schema Validation Node == null");
        try {
            this.service = node.getService();
        } catch (FindException e) {
            throw new IllegalStateException("Service not found", e);
        }
        this.readOnly = !node.canEdit();
        schemaValidationAssertion = assertion;
        this.inferredTargetMessageType = inferredTarget;
        initialize();
    }

    public SchemaValidationPropertiesDialog(Frame owner, SchemaValidation assertion, PublishedService service) {
        super(owner, assertion, true);
        if (assertion == null) {
            throw new IllegalArgumentException("Schema Validation == null");
        }
        this.readOnly = false;
        schemaValidationAssertion = assertion;
        this.service = service;
        this.inferredTargetMessageType = null;
        initialize();
    }

    private void initialize() {
        DialogDisplayer.suppressSheetDisplay(this); // incompatible with xmlpad

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

        // Set up a fake model for the TMP to avoid mutating the assertion prior to {@link #ok}
        targetMessagePanel.setModel(new MessageTargetableAssertion() {{
            TargetMessageType targetMessageType = schemaValidationAssertion.getTarget();
            if ( targetMessageType == null ) {
                targetMessageType = inferredTargetMessageType;
            }
            if ( targetMessageType != null ) {
                setTarget(targetMessageType);
            } else {
                clearTarget();
            }
            setOtherTargetMessageVariable(schemaValidationAssertion.getOtherTargetMessageVariable());
        }});

        targetMessagePanel.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // They selected something; no point warning anybody
                inferredTargetMessageType = null;
            }
        });

        Utilities.setEscKeyStrokeDisposes(this);

        // create callbacks
        cbSchemaLocation.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateModeComponents();
            }
        });
        okButton.setEnabled( !readOnly );
        okButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ok();
            }
        });
        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                cancel();
            }
        });
        helpButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Actions.invokeHelp(SchemaValidationPropertiesDialog.this);
            }
        });
        readFromWsdlButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                readFromWsdl();
            }
        });
        editGlobalXMLSchemasButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                launchGlobalSchemasDlg();
            }
        });

        readFromWsdlButton.setEnabled(wsdlExtractSupported());
        readFromWsdlButton.setToolTipText("Extract schema from WSDL; available for 'document/literal' style services");
        readUrlButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final OkCancelDialog dlg = new OkCancelDialog(SchemaValidationPropertiesDialog.this,
                                                        resources.getString("urlDialog.title"),
                                                        true,
                                                        new UrlPanel(resources.getString("urlDialog.prompt"), null));
                dlg.pack();
                Utilities.centerOnScreen(dlg);
                DialogDisplayer.display(dlg, new Runnable() {
                    @Override
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
            @Override
            public void actionPerformed(ActionEvent e) {
                readFromFile();
            }
        });

        SwingUtilities.invokeLater(new Runnable() {
            @Override
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
            @Override
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

            //before we update the model we need to remember what was the old selection for global schema
            String previousSchemaSelection;
            previousSchemaSelection = (String) globalSchemaCombo.getSelectedItem();
            globalSchemaCombo.setModel(new DefaultComboBoxModel(schemaNames.toArray(new String[schemaNames.size()])));

            //set back the selected item if in the new list
            if (previousSchemaSelection != null && schemaNames.contains(previousSchemaSelection)) {
                globalSchemaCombo.setSelectedItem(previousSchemaSelection);
            } else {
                globalSchemaCombo.setSelectedItem(NOTSET);
            }
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
            Collection<Binding> bindings = wsdl.getBindings();
            if (bindings.isEmpty()) return;

            try {
                for (Binding binding : bindings) {
                    if (!Wsdl.STYLE_DOCUMENT.equals(wsdl.getBindingStyle(binding))) {
                        break;
                    }
                }
                wsdlBindingSoapUseIsLiteral = true;
                for (Binding binding : bindings) {
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
     * Check if there are unresolved schemas in the given schema.  If so, extract them and save them into the database.
     *
     * @param schemaDoc: a document of a root schema.
     *
     * @return true if there is at least one imported schema unabled to resolve.
     */
    private boolean checkForUnresolvedImports(Document schemaDoc) {
        try {
            resolveImportedSchemas(null, schemaDoc, new HashSet<String>()); // "null" means this is a root schema.  A hashset is for tracking circular imports.
        } catch (FetchSchemaFailureException e) {
            StringBuilder messageBuilder = new StringBuilder("<html>").append(e.getMessage());
            messageBuilder.append("<center>Would you like to manually add the unresolved schema?</center></html>");

            if (JOptionPane.showConfirmDialog(this, messageBuilder, "Unresolved Import", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                GlobalSchemaDialog globalSchemaManager = new GlobalSchemaDialog(this);
                globalSchemaManager.pack();
                Utilities.centerOnScreen(globalSchemaManager);
                DialogDisplayer.display(globalSchemaManager);
            }
            return true;
        }
        return false;
    }

    /**
     * Recursively resolve all imported schemas from the bottom to the top and save them into the database.
     *
     * @param systemId: System ID of an imported schema.  It is null when the schema is a root schema.
     * @param schemaDoc: the w3c document of the imported schema.  If systemId is null, then the schemaDoc is a root schema document.
     * @param seenSystemIds: to keep tracking circular imports.
     *
     * @throws FetchSchemaFailureException: thrown when cannot to fetch an imported schema due to invalid schema URL or namespace, etc.
     */
    private void resolveImportedSchemas(String systemId, Document schemaDoc, HashSet<String> seenSystemIds) throws FetchSchemaFailureException {
        Element schemaElmt = schemaDoc.getDocumentElement();
        List<Element> listofimports = XmlUtil.findChildElementsByName(schemaElmt, schemaElmt.getNamespaceURI(), "import");

        // If the imported schema has no other imports, then save the schema.
        if (listofimports.isEmpty() && systemId != null) {
            saveImportedSchema(systemId, node2String(schemaDoc));
            return;
        }

        // Detect if the schema is a circular imported schema.
        if (seenSystemIds.contains(systemId)) {
            return;
        } else {
            seenSystemIds.add(systemId);
        }

        Registry reg = Registry.getDefault();
        if (reg == null || reg.getSchemaAdmin() == null) {
            throw new RuntimeException("No access to registry. Cannot check for unresolved imports.");
        }

        for (Element importEl : listofimports) {
            try {
                Pair<String, String> result = fetchImportedSchema(importEl, reg.getSchemaAdmin());
                if (result != null) {
                    // Recursively resolved imported schema as long as there exists import elements.
                    resolveImportedSchemas(result.left, XmlUtil.stringToDocument(result.right), seenSystemIds);
                }
            } catch (FindException e) {
                throw new RuntimeException("Error trying to look for import schema in global schema");
            } catch (SAXException e) {
                throw new RuntimeException(e.getMessage());
            }
        }

        // Afer finishing all child imports, then save the parent import.
        if (systemId != null) {
            saveImportedSchema(systemId, node2String(schemaDoc));
        }
    }

    /**
     * Fetch an imported schema by a given import element.
     *
     * @param importEl: the import element with attribute(s), schemaLocation and/or namespace.
     * @param schemaAdmin: the Schema Admin API
     *
     * @return a result containing systemId and schemaContent of the imported schema.
     *
     * @throws FindException: thrown when admin cannot find schemas.
     * @throws FetchSchemaFailureException: thrown when cannot fetch the imported schema due to invalid schema URL or namespace, etc.
     */
    private Pair<String, String> fetchImportedSchema(Element importEl, SchemaAdmin schemaAdmin) throws FindException, FetchSchemaFailureException {
        String importloc = importEl.getAttribute("schemaLocation");
        String importns = importEl.getAttribute("namespace");
        if (importns == null || importloc == null) {
            throw new IllegalStateException("The Element method, getAttribute should never return null.");
        } else {
            importloc = importloc.trim();
            importns = importns.trim();
        }

        String systemId = null;
        String importedSchemaContent = null;
        String errorMessage = null;

        // Case 1: schemaLocation exists
        if (!importloc.isEmpty()) {
            systemId = importloc;
            boolean foundSchemaInDatabase = ! schemaAdmin.findByName(systemId).isEmpty();

            boolean reportErrorEnabled = ! foundSchemaInDatabase;
            importedSchemaContent = fetchSchemaFromUrl(importloc, reportErrorEnabled);

            if (!foundSchemaInDatabase && importedSchemaContent == null) {
                errorMessage = "<center>Cannot fetch the imported schema from the URL:</center><center>" + importloc + "</center>";
            }
        }
        // Case 2: schemaLocation does not exist
        else if (importloc.isEmpty()) {
            systemId = generateURN(importns);
            boolean foundSchemaInDatabase = ! schemaAdmin.findByName(systemId).isEmpty();

            boolean reportErrorEnabled = ! foundSchemaInDatabase;
            importedSchemaContent = fetchSchemaByTargetNamespace(importns, reportErrorEnabled);

            if (!foundSchemaInDatabase && importedSchemaContent == null) {
                errorMessage = "<center>Cannot fetch the imported schema from the target namespace:</center><center>" + importns + "</center>";
            }
        }

        if (errorMessage != null) {
            throw new FetchSchemaFailureException(errorMessage);
        }
        if (systemId != null && importedSchemaContent != null) {
            return new Pair<String, String>(systemId, importedSchemaContent);
        }
        return null;
    }

    /**
     * Generate a URN based on a namespace.
     * @param namespace: the URL of a namespace
     * @return a URN string
     */
    private String generateURN(String namespace) {
        StringBuilder sb = new StringBuilder();
        sb.append("urn:uuid:");
        try {
            sb.append(UUID.nameUUIDFromBytes(namespace.getBytes("UTF-8")).toString());
        } catch (UnsupportedEncodingException e) {
            sb.append(UUID.nameUUIDFromBytes(namespace.getBytes()).toString());
        }
        return sb.toString();
    }

    /**
     * Save the resolved imported schemas into the community_schema table.  If the schema is a new one, then the schema
     * will be created and saved.  If the schemas has been in the database, then the schema will be updated.
     *
     * @param systemId: the schema name.
     * @param schemaContent: the schema XML content.
     *
     * @return true if the schema is successfully saved or updated.
     */
    private boolean saveImportedSchema(String systemId, String schemaContent) {
        // Get current target namespace and precheck it and systemId
        String tns;
        try {
            tns = XmlUtil.getSchemaTNS(schemaContent);
        } catch (XmlUtil.BadSchemaException e) {
            logger.warning("Problem parsing schemaContent");
            return false;
        }
        if (tns == null || tns.isEmpty()) {
            logger.warning("This schemaContent does not declare a target namespace.");
            return false;
        }
        if (systemId == null || systemId.isEmpty()) {
            logger.warning("You must provide a system id (name) for this schemaContent to be referenced by another schemaContent.");
            return false;
        }

        // Get Schema Admin
        SchemaAdmin schemaAdmin;
        Registry reg = Registry.getDefault();
        if (reg == null || reg.getSchemaAdmin() == null) {
            throw new RuntimeException("No access to registry. Cannot check for unresolved imports.");
        } else {
            schemaAdmin = reg.getSchemaAdmin();
        }

        // Check if the schema has been in the database, since it isn't able to save a duplicate schema with a same schema name.
        List<SchemaEntry> entries;
        try {
            entries = (List<SchemaEntry>) schemaAdmin.findByName(systemId);
        } catch (FindException e) {
            throw new RuntimeException("Error trying to look for import schema in global schema");
        }
        SchemaEntry schemaEntry;
        if (entries.isEmpty()) {
            schemaEntry = new SchemaEntry();
        } else if (entries.size() != 1) {
            throw new IllegalStateException("Schema name is a unique key in community_schemas.");
        } else {
            schemaEntry = entries.get(0);
        }

        // Save or update it
        schemaEntry.setName(systemId);
        schemaEntry.setSchema(schemaContent);
        schemaEntry.setTns(tns);
        try {
            schemaAdmin.saveSchemaEntry(schemaEntry);
            return true;
        } catch (SaveException e) {
            logger.warning("Unable to save schema entry.");
            return false;
        } catch (UpdateException e) {
            logger.warning("Unable to update schema entry.");
            return false;
        }
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
            XmlUtil.getSchemaTNS(contents);
        } catch (XmlUtil.BadSchemaException e) {
            log.log(Level.WARNING, "issue with schema at hand", e);
            String errMsg = ExceptionUtils.getMessage(e);
            if (e.getCause() instanceof SAXException) {
                errMsg = "A schema-parsing error occurred.\n" + errMsg + "\nPlease correct the invalid content in the schema.";
            } else if (e.getCause() instanceof IOException) {
                errMsg = "An IO error occurred.\n" + errMsg + "\nPlease try it again.";
            }
            displayError(errMsg, null);
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

        if (inferredTargetMessageType != null) {
            DialogDisplayer.showConfirmDialog(this, MessageFormat.format(resources.getString("warning.inferredTarget.message"), inferredTargetMessageType.name()),
                                              resources.getString("warning.inferredTarget.title"),
                                              JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE,
                                              new DialogDisplayer.OptionListener() {
                @Override
                public void reportResult(int option) {
                    if (option == JOptionPane.OK_OPTION) {
                        done();
                    }
                }
            });
        } else {
            done();
        }
    }

    private void done() {
        targetMessagePanel.updateModel(schemaValidationAssertion);

        // save new schema
        schemaValidationAssertion.setApplyToArguments(rbApplyToArgs.isSelected());

        // exit
        changesCommitted = true;
        this.dispose();
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
        return node2String(doc);
    }

    private void cancel() {
        SchemaValidationPropertiesDialog.this.dispose();
    }

    @Override
    public void dispose() {
        xmlContainer.dispose();
        super.dispose();
    }

    private void readFromWsdl() {
        Document wsdlDoc = getWsdlDocument();
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
            @Override
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

    private Document getWsdlDocument() {
        if (service == null) {
            displayError(resources.getString("error.noaccesstowsdl"), null);
            return null;
        }
        String wsdl = service.getWsdlXml();
        return stringToDoc(wsdl);
    }

    private void readFromFile() {
        SsmApplication.doWithJFileChooser(new FileChooserUtil.FileChooserUser() {
            @Override
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
            String printedSchema = node2String(doc);
            uiAccessibility.getEditor().setText(printedSchema);
        } else {
            displayError(resources.getString("error.urlnoschema") + " " + filename, null);
        }
    }

    private void readFromUrl(String urlstr) {
        // get the schema
        String schema = fetchSchemaFromUrl(urlstr, true);
        // set the schema
        uiAccessibility.getEditor().setText(schema);
    }

    /**
     * Fetch a schema by using the URL of a schema location.
     *
     * @param urlstr: the URL of the schema location
     * @param reportErrorEnabled: a flag indicating if an error dialog pops up if some errors occur.
     *
     * @return the schema XML content
     */
    private String fetchSchemaFromUrl(String urlstr, boolean reportErrorEnabled) {
        if (urlstr == null || urlstr.length() < 1) {
            if (reportErrorEnabled) displayError(resources.getString("error.nourl"), null);
            return null;
        }
        // compose input source
        URL url;
        try {
            url = new URL(urlstr);
        } catch (MalformedURLException e) {
            if (reportErrorEnabled) displayError(urlstr + " " + resources.getString("error.badurl"), null);
            log.log(Level.FINE, "malformed url", e);
            return null;
        }
        // try to get document
        InputStream is;
        try {
            is = url.openStream();
        } catch (IOException e) {
            if (reportErrorEnabled) displayError(resources.getString("error.urlnocontent") + " " + urlstr, null);
            return null;
        }

        Document doc;
        try {
            doc = XmlUtil.parse(is);
        } catch (SAXException e) {
            if (reportErrorEnabled) displayError(resources.getString("error.noxmlaturl") + " " + urlstr, null);
            log.log(Level.FINE, "cannot parse " + urlstr, e);
            return null;
        } catch (IOException e) {
            if (reportErrorEnabled) displayError(resources.getString("error.noxmlaturl") + " " + urlstr, null);
            log.log(Level.FINE, "cannot parse " + urlstr, e);
            return null;
        }
        // check if it's a schema
        if (docIsSchema(doc)) {
            // set the new schema
            return node2String(doc);
        } else {
            if (reportErrorEnabled) displayError(resources.getString("error.urlnoschema") + " " + urlstr, null);
        }
        return null;
    }

    /**
     * Fetch a schema by using the URI of a target namespace
     *
     * @param targetNamespace: the URI of the target namespace
     * @param reportErrorEnabled: a flag indicating if an error dialog pops up if some errors occur.
     *
     * @return the schema XML content
     */
    private String fetchSchemaByTargetNamespace(String targetNamespace, boolean reportErrorEnabled) {
        Document wsdlDocument = getWsdlDocument();
        if (wsdlDocument == null) {
            if (reportErrorEnabled) displayError(resources.getString("error.nowsdl"), null);
            return null;
        }

        NodeList typesElmtList = wsdlDocument.getElementsByTagNameNS(WSDL_NSURI, "types");
        if (typesElmtList.getLength() != 1) {
            if (reportErrorEnabled) displayError(resources.getString("error.incorrect.wsdl.types"), null);
            return null;
        }

        Element typesElmt = (Element) typesElmtList.item(0);
        List<Element> schemaElmtList = XmlUtil.findChildElementsByName(typesElmt, W3C_SCHEMA_NSURI, "schema");

        for (Element schemaElmt: schemaElmtList) {
            String targetNs = schemaElmt.getAttribute("targetNamespace");
            if (targetNs == null) {
                throw new IllegalStateException("The Element method, getAttribute never returns null.");
            } else if (targetNs.equals(targetNamespace)) {
                return node2String(schemaElmt);
            }
        }

        if (reportErrorEnabled) displayError(MessageFormat.format(resources.getString("error.invalid.targetnamespace"), targetNamespace), null);
        return null;
    }

    private String node2String(Node node) {
        StringWriter sw = new StringWriter(1024);
        try {
            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer t = tf.newTransformer();
            t.setOutputProperty(OutputKeys.INDENT, "yes");
            t.transform(new DOMSource(node), new StreamResult(sw));
            return sw.toString();
        } catch (TransformerException e) {
            throw new RuntimeException(e); // Can't happen
        }
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
