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
import com.l7tech.gateway.common.schema.FetchSchemaFailureException;
import com.l7tech.gateway.common.schema.SchemaAdmin;
import com.l7tech.gateway.common.schema.SchemaEntry;
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
import com.l7tech.util.*;
import com.l7tech.wsdl.Wsdl;
import com.l7tech.wsdl.WsdlSchemaAnalizer;
import com.l7tech.xml.DocumentReferenceProcessor;
import org.dom4j.DocumentException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import javax.wsdl.Binding;
import javax.wsdl.WSDLException;
import javax.xml.XMLConstants;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
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
    private static final String WSDL_NAMESPACE = "http://schemas.xmlsoap.org/wsdl/";
    private static final StringHolder GLOBAL_SCHEMA_NOT_SET = new StringHolder("<not set>", 128);

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
    private JPanel xmlDisplayPanel;
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
        DialogDisplayer.suppressSheetDisplay(this); // incompatible with xml pad

        // create controls
        initControls();

        // do layout stuff
        setContentPane(rootPanel);

        // Surgery on IDEA form -- remove tabs, and hook up the drop-down to switch sub-panes instead
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
        xmlDisplayPanel.removeAll();
        xmlDisplayPanel.setLayout(new BorderLayout(0, CONTROL_SPACING));
        xmlDisplayPanel.add(xmlContainer.getView(), BorderLayout.CENTER);

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
                final OkCancelDialog dlg = new OkCancelDialog<String>(SchemaValidationPropertiesDialog.this,
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
                SingleUrlResourceInfo singleUrlResourceInfo = (SingleUrlResourceInfo)ri;
                specifyUrlField.setText(singleUrlResourceInfo.getUrl());
            }
        } else if (AssertionResourceType.STATIC.equals(rit)) {
            cbSchemaLocation.setSelectedItem(MODE_SPECIFY);

            if (ri instanceof StaticResourceInfo) {
                StaticResourceInfo sri = (StaticResourceInfo)ri;
                String doc = sri.getDocument();
                if (doc != null && doc.trim().length() > 0) {
                    XMLEditor editor = uiAccessibility.getEditor();
                    editor.setText( reformatXml(doc));
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
                    StringHolder comboBoxItem = (StringHolder)model.getElementAt(i);
                    if (gri.getId() != null && gri.getId().equals(comboBoxItem.getValue())) {
                        found = true;
                        globalSchemaCombo.setSelectedIndex(i);
                        break;
                    }
                }
                if (!found) {
                    globalSchemaCombo.setSelectedItem( GLOBAL_SCHEMA_NOT_SET );
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

    private SchemaAdmin getSchemaAdmin() {
        final Registry reg = Registry.getDefault();
        if ( reg == null ) {
            throw new RuntimeException("No access to registry. Cannot locate schema admin.");
        }

        final SchemaAdmin schemaAdmin = reg.getSchemaAdmin();
        if ( schemaAdmin == null ) {
            throw new RuntimeException("Unable to access schema admin.");
        }

        return schemaAdmin;
    }


    private void reloadGlobalSchemaList() {
        try {
            //before we update the model we need to remember what was the old selection for global schema
            StringHolder previousItem = (StringHolder) globalSchemaCombo.getSelectedItem();

            Collection<SchemaEntry> allSchemas = getSchemaAdmin().findAllSchemas();
            ArrayList<StringHolder> schemaNames = new ArrayList<StringHolder>();
            if (allSchemas != null) {
                for ( final SchemaEntry schemaEntry : allSchemas ) {
                    schemaNames.add(new StringHolder(schemaEntry.getName(), 128)); //128 is the old limit before it was extended to 4096
                }
            }
            schemaNames.add( GLOBAL_SCHEMA_NOT_SET );

            globalSchemaCombo.setModel(new DefaultComboBoxModel(schemaNames.toArray(new StringHolder[schemaNames.size()])));

            //set back the selected item if in the new list
            if (previousItem != null && schemaNames.contains(previousItem)) {
                globalSchemaCombo.setSelectedItem(previousItem);
            } else {
                globalSchemaCombo.setSelectedItem( GLOBAL_SCHEMA_NOT_SET );
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Cannot get global schemas", e);
        }
    }

    private static class StringHolder{
        private final String value;
        private final int maxSize;

        private StringHolder(String value, int maxSize) {
            this.value = value;
            this.maxSize = maxSize;
        }

        public String getValue() {
            return value;
        }

        @Override
        public String toString() {
            return TextUtils.truncStringMiddleExact(value, maxSize);
        }

        @SuppressWarnings({ "RedundantIfStatement" })
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            StringHolder that = (StringHolder) o;

            if (!value.equals(that.value)) return false;

            return true;
        }

        @Override
        public int hashCode() {
            return value.hashCode();
        }
    }

    /**
     * Determine whether wsdl extracting is supported. This is supported for 'document'
     * style services only.
     * Traverse all the soap bindings, and if all the bindings are of style 'document'
     * returns true.
     *
     * @return true if 'document' style supported, false otherwise
     */
    private boolean wsdlExtractSupported() {
        if (service == null || !service.isSoap()) return false;
        String wsdlXml = service.getWsdlXml();
        if (wsdlXml == null) return false;
        analyzeWsdl(wsdlXml);
        // bugzilla #2081, if we know we can't extract a wsdl, then let's disable feature

        try {
            NodeList wsdlSchemas = WsdlSchemaAnalizer.extractSchemaElementFromWsdl(XmlUtil.stringToDocument(wsdlXml));
            if (wsdlSchemas == null || wsdlSchemas.getLength() < 1) {
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
     * @return true if there is at least one included schema unable to resolve.
     */
    private boolean checkForUnresolvedDependencies(final Document schemaDoc) throws XmlUtil.BadSchemaException {
        try {
            resolveSchemaDependencies(null, schemaDoc, new HashSet<String>()); // "null" means this is a root schema.  The set is for tracking circular dependencies.
        } catch (FetchSchemaFailureException e) {
            StringBuilder messageBuilder = new StringBuilder(e.getMessage());
            messageBuilder.append("\nWould you like to manually add the schema dependency?");
            String msg = messageBuilder.toString();

            final int width = Utilities.computeStringWidth(this.getFontMetrics(this.getFont()), msg);
            final Object object;
            if(width > 600){
                object = Utilities.getTextDisplayComponent( msg, 600, 100, -1, -1 );
            }else{
                object = msg;
            }

            if (JOptionPane.showConfirmDialog(this, object, "Unresolved Schema Dependency", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
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
     * Recursively resolve all schema dependencies from the bottom to the top and save them into the database.
     *
     * @param systemId: System ID of a dependency schema.  It is null when the schema is a root schema.
     * @param schemaDoc: the w3c document of the dependency schema.  If systemId is null, then the schemaDoc is a root schema document.
     * @param seenSystemIds: to keep tracking circular dependencies.
     *
     * @throws FetchSchemaFailureException: thrown when cannot to fetch an dependency schema due to invalid schema URL or namespace, etc.
     * @throws com.l7tech.common.io.XmlUtil.BadSchemaException if include of an invalid schema was attempted
     */
    private void resolveSchemaDependencies( final String systemId,
                                            final Document schemaDoc,
                                            final HashSet<String> seenSystemIds ) throws FetchSchemaFailureException, XmlUtil.BadSchemaException {
        // Detect if the schema is a circular dependency.
        if (seenSystemIds.contains(systemId)) {
            return;
        } else {
            seenSystemIds.add(systemId);
        }

        final java.util.List<Element> dependencyElements = new ArrayList<Element>();
        final DocumentReferenceProcessor schemaReferenceProcessor = DocumentReferenceProcessor.schemaProcessor();
        schemaReferenceProcessor.processDocumentReferences( schemaDoc, new DocumentReferenceProcessor.ReferenceCustomizer(){
            @Override
            public String customize( final Document document,
                                     final Node node,
                                     final String documentUrl,
                                     final DocumentReferenceProcessor.ReferenceInfo referenceInfo ) {
                assert node instanceof Element;
                //noinspection ConstantConditions
                if ( node instanceof Element ) dependencyElements.add( (Element)node );
                return null;
            }
        } );

        final String tns = schemaDoc.getDocumentElement().getAttribute("targetNamespace");
        for ( final Element dependencyElement : dependencyElements ) {
            try {
                final Pair<String, String> result = fetchDependencySchema(tns, dependencyElement, getSchemaAdmin());
                if (result != null) {
                    // Recursively resolve the schemas dependencies.
                    resolveSchemaDependencies(result.left, XmlUtil.stringToDocument(result.right), seenSystemIds);
                }
            } catch (FindException e) {
                throw new RuntimeException("Error trying to look for dependency schema in global schema");
            } catch (SAXException e) {
                throw new RuntimeException(e.getMessage());
            }
        }

        // After finishing all dependencies, save the parent schema.
        if ( systemId != null ) {
            saveSchemaDependency( systemId, node2String(schemaDoc) );
        }
    }

    /**
     * Fetch a dependency schema by a given import/include/redefine element.
     *
     * @param schemaNamespace Namespace of the parent schema
     * @param dependencyEl the dependency element with attribute(s), schemaLocation and/or namespace.
     * @param schemaAdmin the Schema Admin API
     *
     * @return a result containing systemId and schemaContent of the dependency schema.
     *
     * @throws FindException: thrown when admin cannot find schemas.
     * @throws FetchSchemaFailureException: thrown when cannot fetch the schema due to invalid schema URL or namespace, etc.
     */
    private Pair<String, String> fetchDependencySchema( final String schemaNamespace,
                                                        final Element dependencyEl,
                                                        final SchemaAdmin schemaAdmin ) throws FindException, FetchSchemaFailureException {
        final String dependencyLocation = dependencyEl.getAttribute("schemaLocation");
        final String dependencyNamespace = dependencyEl.hasAttribute( "namespace" ) ? dependencyEl.getAttribute("namespace") : null;

        boolean attemptedFindByLocation = false;
        boolean attemptedFindByNamespace = false;
        String systemId = null;
        boolean updatedContent = false;
        String dependencySchemaContent = null;

        // Find from global schemas
        if ( !dependencyLocation.isEmpty() ) {
            attemptedFindByLocation = true;
            systemId = dependencyLocation;
            Collection<SchemaEntry> entries = schemaAdmin.findByName(systemId);
            if (entries != null) {
                for ( final SchemaEntry entry : entries ) {
                    if ( "import".equals( dependencyEl.getLocalName() ) ) { // namespace from import must match schema target namespace
                        if ( (dependencyNamespace == null && !entry.hasTns()) || (dependencyNamespace != null && dependencyNamespace.equals( entry.getTns() )) ) {
                            dependencySchemaContent = entry.getSchema();
                        }
                    } else { // namespace from parent schema must match schema target namespace
                        if ( !entry.hasTns() || entry.getTns().equals(schemaNamespace) ) {
                            dependencySchemaContent = entry.getSchema();
                        }
                    }
                }
            }
        }

        if ( dependencySchemaContent == null && "import".equals( dependencyEl.getLocalName() ) ) {
            attemptedFindByNamespace = true;
            systemId = generateURN(dependencyNamespace==null ? "" : dependencyNamespace);
            Collection<SchemaEntry> entries = schemaAdmin.findByTNS(dependencyNamespace);
            if ( entries != null && !entries.isEmpty()) {
                dependencySchemaContent = entries.iterator().next().getSchema();
            }
        }

        // Find or refresh from URL or other schemas in WSDL
        if ( !dependencyLocation.isEmpty() ) {
            final String newSchemaContent = fetchSchemaFromUrl(dependencyLocation, dependencySchemaContent==null);
            if ( newSchemaContent != null ) {
                updatedContent = true;
                dependencySchemaContent = newSchemaContent;
            }
        }

        if ( dependencySchemaContent == null && "import".equals( dependencyEl.getLocalName() ) ) {
            final String newSchemaContent = fetchSchemaByTargetNamespace(dependencyNamespace, dependencySchemaContent==null);
            if ( newSchemaContent != null ) {
                updatedContent = true;
                dependencySchemaContent = newSchemaContent;
            }
        }

        if ( dependencySchemaContent == null) {
            String errorMessage;
            if ( attemptedFindByLocation && attemptedFindByNamespace ) {
                errorMessage = "Cannot locate schema dependency using location:\n  " + dependencyLocation + "\nor by namespace:\n  " + (dependencyNamespace==null ? "<None>" : dependencyNamespace);
            } else if ( attemptedFindByLocation ) {
                errorMessage = "Cannot locate schema dependency using location:\n  " + dependencyLocation;
            } else if ( attemptedFindByNamespace ) {
                errorMessage = "Cannot locate schema dependency by namespace:\n  " + dependencyNamespace;
            } else {
                errorMessage = "Invalid schema " + dependencyEl.getLocalName();
            }
            throw new FetchSchemaFailureException(errorMessage);
        }


        if ( systemId != null && updatedContent ) {
            return new Pair<String, String>(systemId, dependencySchemaContent);
        }

        return null;
    }

    /**
     * Generate a URN based on a namespace.
     * 
     * @param namespace: the URL of a namespace
     * @return a URN string
     */
    private String generateURN(String namespace) {
        StringBuilder sb = new StringBuilder();
        sb.append("urn:uuid:");
        sb.append(UUID.nameUUIDFromBytes(namespace.getBytes(Charsets.UTF8)).toString());
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
    private boolean saveSchemaDependency( final String systemId, final String schemaContent ) throws XmlUtil.BadSchemaException {
        // Get current target namespace and check it and systemId
        String tns = XmlUtil.getSchemaTNS(schemaContent);

        if (systemId == null || systemId.trim().isEmpty()) {
            logger.warning("You must provide a system id (name) for this schemaContent to be referenced by another schemaContent.");
            return false;
        }

        // Get Schema Admin
        SchemaAdmin schemaAdmin = getSchemaAdmin();

        // Check if the schema has been in the database, since it isn't able to save a duplicate schema with a same schema name.
        List<SchemaEntry> entries;
        try {
            entries = (List<SchemaEntry>) schemaAdmin.findByName(systemId);
        } catch (FindException e) {
            throw new RuntimeException("Error trying to look for dependency schema in global schema");
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
        } else if (globalSchemaCombo.getSelectedItem().equals( GLOBAL_SCHEMA_NOT_SET )) {
            return false;
        }
        final StringHolder selectedItem = (StringHolder) globalSchemaCombo.getSelectedItem();
        gri.setId(selectedItem.getValue());
        schemaValidationAssertion.setResourceInfo(gri);
        return true;
    }

    /** @return true iff. info was committed successfully */
    private boolean commitSpecifyTab() {
        // check that whatever is captured is an xml document and a schema with or without a tns
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
            if ( checkForUnresolvedDependencies(doc))
                return false;
        } catch (SAXException e) {
            log.log(Level.WARNING, "issue with xml document", e);
            displayError("The schema is not formatted properly. " + e.getMessage(), null);
            return false;
        } catch (XmlUtil.BadSchemaException e) {
            String errMsg = "Error importing schema: " + ExceptionUtils.getMessage(e);
            log.log(Level.WARNING, errMsg, ExceptionUtils.getDebugException(e));
            displayError(errMsg, null);
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

        if ( !ValidationUtils.isValidUrl(url.trim()) ) {
            displayError(resources.getString("error.badurl"), null);
            return false;
        }

        // Checks pass, commit it
        SingleUrlResourceInfo singleUrlResourceInfo = new SingleUrlResourceInfo();
        singleUrlResourceInfo.setUrl(url.trim());
        schemaValidationAssertion.setResourceInfo(singleUrlResourceInfo);
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

    private String reformatXml(String input) {
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

        final SelectWsdlSchemaDialog schemaFromWsdlChooser;
        try {
            schemaFromWsdlChooser = new SelectWsdlSchemaDialog(this, wsdlDoc);
        } catch (DocumentException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (SAXParseException e) {
            throw new RuntimeException(e);
        }
        schemaFromWsdlChooser.pack();
        Utilities.centerOnScreen(schemaFromWsdlChooser);
        DialogDisplayer.display(schemaFromWsdlChooser, new Runnable() {
            @Override
            public void run() {
                String result = schemaFromWsdlChooser.getOkedSchema();
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
        return wsdl==null ? null : stringToDoc(wsdl);
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
            if (printedSchema != null) {
                uiAccessibility.getEditor().setText(printedSchema);
            }
        } else {
            displayError(resources.getString("error.urlnoschema") + " " + filename, null);
        }
    }

    private void readFromUrl(String url) {
        // get the schema
        String schema = fetchSchemaFromUrl(url, true);
        // set the schema
        if (schema != null) {
            uiAccessibility.getEditor().setText(schema);
        }
    }

    /**
     * Fetch a schema by using the URL of a schema location.
     *
     * @param url the URL of the schema location
     * @param reportErrorEnabled a flag indicating if an error dialog pops up if some errors occur.
     *
     * @return the schema XML content
     */
    private String fetchSchemaFromUrl( final String url, final boolean reportErrorEnabled ) {
        if (url == null || url.length() < 1) {
            if (reportErrorEnabled) displayError(resources.getString("error.nourl"), null);
            return null;
        }

        //validate the URL
        try {
            new URL(url);
        } catch (MalformedURLException e) {
            final String errorMsg = url + " " + resources.getString("error.badurl");
            log.log(Level.FINE, errorMsg, e);
            return null;
        }

        final SchemaAdmin schemaAdmin = getSchemaAdmin();
        final String schemaXml;
        try {
            schemaXml = schemaAdmin.resolveSchemaTarget(url);
        } catch (IOException e) {
            //this is likely to be a GenericHttpException
            final String errorMsg = "Cannot download document: " + ExceptionUtils.getMessage(e);
            if (reportErrorEnabled) {
                displayError(errorMsg, "Errors downloading file");
            }
            log.log(Level.FINE, errorMsg, e);
            return null;
        }

        final Document doc;
        try {
            doc = XmlUtil.parse(schemaXml);
        } catch (SAXException e) {
            if (reportErrorEnabled) displayError(resources.getString("error.noxmlaturl") + " " + url, null);
            log.log(Level.FINE, "cannot parse " + url, e);
            return null;
        }
        
        // check if it's a schema
        if (docIsSchema(doc)) {
            // set the new schema
            return node2String(doc);
        } else {
            if (reportErrorEnabled) displayError(resources.getString("error.urlnoschema") + " " + url, null);
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
            return null;
        }

        NodeList typesElementList = wsdlDocument.getElementsByTagNameNS( WSDL_NAMESPACE, "types");
        if (typesElementList.getLength() != 1) {
            if (reportErrorEnabled) displayError(resources.getString("error.incorrect.wsdl.types"), null);
            return null;
        }

        Element typesElement = (Element) typesElementList.item(0);
        List<Element> schemaElementList = XmlUtil.findChildElementsByName(typesElement, XMLConstants.W3C_XML_SCHEMA_NS_URI, "schema");

        for (Element schemaElement: schemaElementList) {
            String targetNs = schemaElement.getAttribute("targetNamespace");
            if ( targetNs.equals(targetNamespace) || targetNs.isEmpty() && targetNamespace==null) {
                return node2String(schemaElement);
            }
        }

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
        final FontMetrics fontMetrics = this.getFontMetrics(this.getFont());
        final int width = Utilities.computeStringWidth(fontMetrics, msg);
        final Object object;
        if(width > 600){
            object = Utilities.getTextDisplayComponent( msg, 600, 100, -1, -1 );
        }else{
            object = msg;
        }
        JOptionPane.showMessageDialog(this, object, title, JOptionPane.ERROR_MESSAGE);
    }

    private void initControls() {
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
