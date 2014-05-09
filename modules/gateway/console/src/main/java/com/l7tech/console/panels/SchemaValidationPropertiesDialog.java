package com.l7tech.console.panels;

import com.japisoft.xmlpad.XMLContainer;
import com.japisoft.xmlpad.editor.XMLEditor;
import com.l7tech.common.io.*;
import com.l7tech.console.SsmApplication;
import com.l7tech.console.action.Actions;
import com.l7tech.console.action.ManageHttpConfigurationAction;
import com.l7tech.console.tree.policy.AssertionTreeNode;
import com.l7tech.console.util.*;
import com.l7tech.gateway.common.resources.ResourceAdmin;
import com.l7tech.gateway.common.resources.ResourceEntryHeader;
import com.l7tech.gateway.common.resources.ResourceType;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.gateway.common.service.ServiceDocument;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.FileChooserUtil;
import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.widgets.OkCancelDialog;
import com.l7tech.message.ValidationTarget;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.ObjectModelException;
import com.l7tech.policy.AssertionResourceInfo;
import com.l7tech.policy.SingleUrlResourceInfo;
import com.l7tech.policy.StaticResourceInfo;
import com.l7tech.policy.assertion.AssertionResourceType;
import com.l7tech.policy.assertion.GlobalResourceInfo;
import com.l7tech.policy.assertion.MessageTargetableAssertion;
import com.l7tech.policy.assertion.TargetMessageType;
import com.l7tech.policy.assertion.xml.SchemaValidation;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.util.*;
import com.l7tech.wsdl.Wsdl;
import com.l7tech.wsdl.WsdlSchemaAnalizer;
import org.dom4j.DocumentException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
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
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
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
    private static final StringHolder GLOBAL_SCHEMA_NOT_SET = new StringHolder("<not set>", 128);

    // Top-level widgets
    private JComboBox cbSchemaLocation;
    private JRadioButton rbApplyToBody;
    private JRadioButton rbApplyToArgs;
    private JRadioButton rbApplyToEnvelope;
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
    private JTextField specifySystemIdTextField;
    private JPanel xmlDisplayPanel;
    private XMLContainer xmlContainer;

    // Widgets specific to MODE_SPECIFY_URL
    private JTextField specifyUrlField;
    private JButton manageHttpOptionsButton;
    private JPanel globalURLTab;

    private JComboBox globalSchemaCombo;
    private JButton editGlobalXMLSchemasButton;
    private TargetMessagePanel targetMessagePanel;

    // Other fields
    private final EntityResolver schemaEntityResolver;
    private final boolean readOnly;
    private SchemaValidation schemaValidationAssertion;
    private PublishedService service;

    // schemas extracted from the WSDL
    private List<Pair<String,Element>> fullSchemas;
    private List<Pair<String,Element>> inputSchemas;
    private List<Pair<String,Element>> outputSchemas;

    private final Logger log = Logger.getLogger(getClass().getName());

    private static final int CONTROL_SPACING = 5;

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
        this.schemaEntityResolver = new ResourceAdminEntityResolver(getResourceAdmin());
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
        this.schemaEntityResolver = new ResourceAdminEntityResolver(getResourceAdmin());
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
        }},getPreviousAssertion());

        targetMessagePanel.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // They selected something; no point warning anybody
                inferredTargetMessageType = null;
            }
        });

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
                String err = targetMessagePanel.check();
                if (err != null) {
                    DialogDisplayer.showMessageDialog(SchemaValidationPropertiesDialog.this, MessageFormat.format(resources.getString("error.invalid.targetmessage"), err),
                                                  resources.getString("error.invalid.targetmessage.title"),
                                                  JOptionPane.ERROR_MESSAGE, null);
                    return;
                }
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

        extractSchemas();

        readFromWsdlButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                readFromWsdl();
            }
        });
        editGlobalXMLSchemasButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                launchGlobalResourcesDlg();
            }
        });

        manageHttpOptionsButton.setAction( new ManageHttpConfigurationAction( this ) );
        manageHttpOptionsButton.setText(resources.getString("manageHttpOptionsButton.label"));
        manageHttpOptionsButton.setIcon(null);

        readFromWsdlButton.setEnabled(fullSchemas != null && ! fullSchemas.isEmpty());
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
        ValidationTarget target = schemaValidationAssertion.getValidationTarget();
        rbApplyToArgs.setSelected(ValidationTarget.ARGUMENTS == target);
        rbApplyToBody.setSelected(ValidationTarget.BODY == target);
        rbApplyToEnvelope.setSelected(ValidationTarget.ENVELOPE == target);

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
                String uri = sri.getOriginalUrl();
                if ( uri != null && uri.trim().length() > 0 ) {
                    specifySystemIdTextField.setText( uri );
                    specifySystemIdTextField.setCaretPosition( 0 );
                }

                String doc = sri.getDocument();
                if (doc != null && doc.trim().length() > 0) {
                    setEditorText(doc);
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

    private void launchGlobalResourcesDlg() {
        GlobalResourcesDialog dlg = new GlobalResourcesDialog(this);
        DialogDisplayer.display(dlg, new Runnable() {
            @Override
            public void run() {
                reloadGlobalSchemaList();
            }
        });
    }

    private ResourceAdmin getResourceAdmin() {
        final Registry reg = Registry.getDefault();
        if ( reg == null ) {
            throw new RuntimeException("No access to registry. Cannot locate schema admin.");
        }

        final ResourceAdmin resourceAdmin = reg.getResourceAdmin();
        if ( resourceAdmin == null ) {
            throw new RuntimeException("Unable to access schema admin.");
        }

        return resourceAdmin;
    }

    private void reloadGlobalSchemaList() {
        try {
            //before we update the model we need to remember what was the old selection for global schema
            StringHolder previousItem = (StringHolder) globalSchemaCombo.getSelectedItem();

            Collection<ResourceEntryHeader> allSchemas = getResourceAdmin().findResourceHeadersByType(ResourceType.XML_SCHEMA);
            ArrayList<StringHolder> schemaNames = new ArrayList<StringHolder>();
            if (allSchemas != null) {
                for ( final ResourceEntryHeader schemaEntry : allSchemas ) {
                    schemaNames.add(new StringHolder(schemaEntry.getUri(), 128)); //128 is the old limit before it was extended to 4096
                }
            }
            Collections.sort( schemaNames );
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

    private static class StringHolder implements Comparable<StringHolder> {
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

        @Override
        public int compareTo( final StringHolder o ) {
            return value.compareTo( o.value );
        }
    }

    /**
     * Determines whether wsdl extracting is supported, and extracts schemas if so.
     * This is supported for document/literal and rpc/literal style services only.
     * Traverse all the soap bindings, and if all the bindings have literal use - extracts the schemas.
     *
     * Initializes fullSchemas, inputSchemas, and outputSchemas fields, or leaves them uninitialized if schema extraction is not supported.
     */
    private void extractSchemas() {
        if (service == null || !service.isSoap()) return;
        final String wsdlXml = service.getWsdlXml();
        if (wsdlXml == null) return;

        final Map<String,String> documentsToAnalyze = new HashMap<String,String>();
        documentsToAnalyze.put(service.getWsdlUrl(), wsdlXml);
        try {
            //Lookup the import schema from service document
            for(ServiceDocument document : Registry.getDefault().getServiceManager().findServiceDocumentsByServiceID(service.getId())) {
                if ("WSDL-IMPORT".equals(document.getType())) {
                    documentsToAnalyze.put(document.getUri(), document.getContents());
                }
            }
        } catch (FindException e) {
            logger.log(Level.WARNING, "Error retrieving service documents for service id: " + service.getId(), e);
            return;
        }

        // bugzilla #2081, if we know we can't extract a wsdl, then let's disable feature
        if (!isDocumentLiteral(documentsToAnalyze)) {
            return;
        }

        final List<Pair<String,Element>> fullSchemas = new ArrayList<Pair<String,Element>>();
        final List<Pair<String,Element>> inputSchemas = new ArrayList<Pair<String,Element>>();
        final List<Pair<String,Element>> outputSchemas = new ArrayList<Pair<String,Element>>();
        try {
            final Map<String,Document> domToAnalyze = new HashMap<String,Document>();
            //Convert the XML to document object
            for ( final Map.Entry<String,String> docEntry : documentsToAnalyze.entrySet() ) {
                domToAnalyze.put(docEntry.getKey(), XmlUtil.stringToDocument(docEntry.getValue()));
            }
            for ( final Map.Entry<String,Document> docEntry : domToAnalyze.entrySet() ) {
                final WsdlSchemaAnalizer analyzer = new WsdlSchemaAnalizer(docEntry.getValue(), domToAnalyze);
                analyzer.splitInputOutputs();
                fullSchemas.addAll( toPairs( docEntry.getKey(), analyzer.getFullSchemas() ) );
                inputSchemas.addAll( toPairs( docEntry.getKey(), analyzer.getInputSchemas() ) );
                outputSchemas.addAll( toPairs( docEntry.getKey(), analyzer.getOutputSchemas() ) );
            }
        } catch (SAXException e) {
            logger.log(Level.WARNING, "wsdl is not well formed?", e);
            // here we simply return null because if wsdl is not well formed then
            // we know for sure there is no way we can possibly extract a schema from it
            return;
        }

        this.fullSchemas = fullSchemas;
        this.inputSchemas = inputSchemas;
        this.outputSchemas = outputSchemas;
    }

    private Collection<Pair<String,Element>> toPairs( final String uri, final Element[] schemaElements ) {
        final Collection<Pair<String,Element>> pairs = new ArrayList<Pair<String,Element>>();

        if ( schemaElements != null ) {
            for ( final Element schemaElement : schemaElements ) {
                pairs.add( new Pair<String,Element>( uri, schemaElement ) );               
            }
        }

        return pairs;
    }

    /**
     * @return true if the supplied WSDLs are valid and all their SOAP bindings have 'literal' use.
     */
    private boolean isDocumentLiteral(Map<String, String> wsdls) {
        try {
            Wsdl wsdl = Wsdl.newInstance(Wsdl.getWSDLLocator(service.getWsdlUrl(), wsdls, logger));
            wsdl.setShowBindings(Wsdl.SOAP_BINDINGS);

            Collection<Binding> bindings = wsdl.getBindings();
            for (Binding binding : bindings) {
                if ( !Wsdl.USE_LITERAL.equals(wsdl.getSoapUse(binding)) ) {
                    return false;
                }
            }
        } catch (WSDLException e) {
            return false;
        }
        return true;
    }

    /**
     * Check if there are unresolved schemas in the given schema.
     *
     * @param schemaUri The URI for the schema
     * @param schemaDoc The schema content
     *
     * @return true if there is a missing dependency.
     */
    private boolean hasUnresolvedDependencies( final String schemaUri,
                                               final String schemaDoc ) throws ObjectModelException, IOException, SAXException {
        final DependencyInfo info = validateSchemaDependencies( schemaUri, schemaDoc );
        if ( info != null ) {
            StringBuilder messageBuilder = new StringBuilder();
            messageBuilder.append("A dependency of the schema was not found:\n\n");
            messageBuilder.append(info.toString());

            final int importChoice;
            final int manualChoice;
            final Object[] options;
            final Object defaultOption;
            if ( info.matchedByNamespace ) {
                importChoice = Integer.MIN_VALUE;
                manualChoice = JOptionPane.YES_OPTION;
                options = new Object[]{"Update","Cancel"};
                defaultOption = "Update";
                messageBuilder.append("\nA global schema matches by namespace, select 'Update' to access global schemas\nor 'Cancel' to fix the schemaLocation or System ID.");
            } else if ( info.ambiguousNamespace ) {
                importChoice = Integer.MIN_VALUE;
                manualChoice = JOptionPane.YES_OPTION;
                options = new Object[]{"Update","Cancel"};
                defaultOption = "Update";
                messageBuilder.append("\nMultiple global schemas matched by namespace, select 'Update' to access global schemas\nor 'Cancel' to add a schemaLocation for the import.");
            } else {
                importChoice = JOptionPane.YES_OPTION;
                manualChoice = JOptionPane.NO_OPTION;
                options = new Object[]{"Import","Add","Cancel"};
                defaultOption = "Import";
                messageBuilder.append("\nWould you like to import or manually add missing schema dependencies?");
            }
            String msg = messageBuilder.toString();

            final int width = Utilities.computeStringWidth(this.getFontMetrics(this.getFont()), msg);
            final Object object;
            if(width > 600){
                object = Utilities.getTextDisplayComponent( msg, 600, 100, -1, -1 );
            }else{
                object = msg;
            }

            final int choice = JOptionPane.showOptionDialog(
                    this,
                    object,
                    "Schema Dependency Not Found",
                    JOptionPane.YES_NO_CANCEL_OPTION,
                    JOptionPane.WARNING_MESSAGE,
                    null,
                    options,
                    defaultOption );
            if ( choice == importChoice ) {
                final String wsdlUrl = service==null ? "" : service.getWsdlUrl();
                final Collection<ResourceDocumentResolver> resolvers = !wsdlUrl.isEmpty() && schemaUri!=null && schemaUri.startsWith( wsdlUrl ) ?
                        Collections.<ResourceDocumentResolver>singleton( new WsdlSchemaResourceDocumentResolver(fullSchemas) ) :
                        Collections.<ResourceDocumentResolver>emptyList();

                GlobalResourceImportWizard.importDependencies(
                    this,
                    schemaUri!=null ? schemaUri : "urn:uuid:" + UUID.randomUUID().toString(),
                    ResourceType.XML_SCHEMA,
                    schemaDoc,
                    getResourceAdmin(),
                    resolvers,
                    GlobalResourceImportWizard.getUIImportAdvisor( this, true, !getResourceAdmin().allowSchemaDoctype() ),
                    new Functions.UnaryVoid<String>(){
                        @Override
                        public void call( final String content ) {
                            if ( !schemaDoc.equals(content) ) {
                                setEditorText( content );
                            }
                        }
                    },
                    GlobalResourceImportWizard.getUIErrorListener( this ));
            } else if ( choice == manualChoice ) {
                DialogDisplayer.display(new GlobalResourcesDialog(this));
            }

            return true;
        }

        return false;
    }

    /**
     * Resolve the given schemas dependencies from existing global schemas.
     */
    private DependencyInfo validateSchemaDependencies( final String systemId,
                                                       final String schemaDoc ) throws ObjectModelException, IOException, SAXException  {
        Document schemaDocument;
        try {
            schemaDocument = parseSchema(systemId, schemaDoc);
        } catch ( IOException e ) {
            final String errorMessage = ExceptionUtils.getMessage(e);
            if ( errorMessage.startsWith( "Could not resolve '" ) && errorMessage.endsWith("'") ) {
                return new DependencyInfo( null, errorMessage.substring( 19, errorMessage.length()-1 ), false, null, false, false );
            } else {
                throw e;
            }
        }

        final java.util.List<Element> dependencyElements = new ArrayList<Element>();
        final DocumentReferenceProcessor schemaReferenceProcessor = DocumentReferenceProcessor.schemaProcessor();
        schemaReferenceProcessor.processDocumentReferences( schemaDocument, new DocumentReferenceProcessor.ReferenceCustomizer(){
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

        for ( final Element dependencyElement : dependencyElements ) {
            final DependencyInfo info = getDependencyInfoIfInvalid( systemId, dependencyElement, getResourceAdmin() );
            if ( info != null ) return info;
        }

        return null;
    }

    /**
     * Get the dependency information for the dependency if it is invalid.
     */
    private DependencyInfo getDependencyInfoIfInvalid( final String baseUri,
                                                       final Element dependencyEl,
                                                       final ResourceAdmin resourceAdmin ) throws ObjectModelException {
        final String dependencyLocation = dependencyEl.getAttribute( "schemaLocation" );
        final String dependencyNamespace = dependencyEl.hasAttribute( "namespace" ) ? dependencyEl.getAttribute("namespace") : null;
        final boolean dependencyNamespaceSet = "import".equals( dependencyEl.getLocalName() );

        ResourceEntryHeader entryHeader = null;
        if ( !dependencyLocation.isEmpty() ) {
            entryHeader  = resourceAdmin.findResourceHeaderByUriAndType(dependencyLocation, ResourceType.XML_SCHEMA);

            if ( entryHeader == null && baseUri != null ) { // try with resolved URI
                try {
                    final String resolvedUri = new URI(baseUri).resolve(dependencyLocation).toString();
                    entryHeader = resourceAdmin.findResourceHeaderByUriAndType(resolvedUri, ResourceType.XML_SCHEMA);
                } catch ( URISyntaxException e ) {
                    logger.info( "Unable to resolve schema dependency URI '"+dependencyLocation+"' against '"+baseUri+"': " + ExceptionUtils.getMessage(e) );
                    return null;
                } catch ( IllegalArgumentException e ) {
                    logger.info( "Unable to resolve schema dependency URI '"+dependencyLocation+"' against '"+baseUri+"': " + ExceptionUtils.getMessage(e) );
                    return null;
                }
            }
        }

        boolean ambiguousNamespace = false;
        boolean matchedByNamespaceOnly = false;
        if ( entryHeader == null && dependencyNamespaceSet ) {
            final Collection<ResourceEntryHeader> entries = resourceAdmin.findResourceHeadersByTargetNamespace(dependencyNamespace);
            if ( entries != null && !entries.isEmpty() ) {
                // Only resolve by targetNamespace if there is no schemaLocation
                // If there is a schemaLocation but it does not match a global schema we want
                // to fix the reference.
                if ( !dependencyLocation.isEmpty() ) {
                    matchedByNamespaceOnly = true;
                } else {
                    if ( entries.size() == 1 ) {
                        entryHeader = entries.iterator().next();
                    } else {
                        ambiguousNamespace = true;
                    }
                }
            }
        }

        return entryHeader!=null ? null : new DependencyInfo( baseUri, dependencyLocation, dependencyNamespaceSet, dependencyNamespace, matchedByNamespaceOnly, ambiguousNamespace );
    }

    /** @return true iff. info was committed successfully */
    private boolean commitGlobalTab() {
        GlobalResourceInfo gri = new GlobalResourceInfo();
        if (globalSchemaCombo.getSelectedItem() == null) {
            throw new RuntimeException("the combo has nothing selected?");
            // this shouldn't happen (unless bug)
        } else if (globalSchemaCombo.getSelectedItem().equals( GLOBAL_SCHEMA_NOT_SET )) {
            displayError( resources.getString("error.noglobalschema"), null);
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
        String uri = specifySystemIdTextField.getText().trim();
        if ( uri.isEmpty() ) {
            uri = null;            
        }
        String contents = xmlContainer.getUIAccessibility().getEditor().getText();

        if ( !ValidationUtils.isValidUrl(uri, true) ) {
            displayError(resources.getString("error.badurl"), null);
            return false;
        }

        try {
            if ( hasUnresolvedDependencies(uri, contents))
                return false;
        } catch ( IOException e ) {
            log.log(Level.WARNING, "Error checking schema dependencies.", e);
            displayError("Error processing schema : " + e.getMessage(), null);
            return false;
         } catch (SAXException e) {
            log.log(Level.WARNING, "Error checking schema dependencies.", e);
            displayError("The schema is not formatted properly : " + e.getMessage(), null);
            return false;
        } catch ( ObjectModelException e ) {
            String errMsg = "Error processing schema: " + ExceptionUtils.getMessage(e);
            log.log(Level.WARNING, errMsg, ExceptionUtils.getDebugException(e));
            displayError(errMsg, null);
            return false;
        }

        try {
            XmlUtil.getSchemaTNS(uri, contents, schemaEntityResolver);
        } catch (XmlUtil.BadSchemaException e) {
            log.log(Level.WARNING, "Error processing schema.", e);
            String errMsg = ExceptionUtils.getMessage(e);
            if (e.getCause() instanceof SAXException) {
                errMsg = "A schema-parsing error occurred.\n" + errMsg + "\nPlease correct the invalid content in the schema.";
            } else if (e.getCause() instanceof IOException) {
                errMsg = "Error processing schema:\n" + errMsg;
            }
            displayError(errMsg, null);
            return false;
        }

        if ( !getResourceAdmin().allowSchemaDoctype() && XmlUtil.hasDoctype( contents ) ) {
            final int choice = JOptionPane.showOptionDialog(
                    this,
                    "The schema has a document type declaration and support is currently\ndisabled (schema.allowDoctype cluster property)",
                    "Schema Warning",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE,
                    null,
                    new String[]{ "Save", "Cancel" },
                    "Cancel");
            if ( choice == JOptionPane.NO_OPTION ) {
                return false;
            }
        }

        // Checks pass, commit it
        StaticResourceInfo sri = new StaticResourceInfo();
        sri.setOriginalUrl( uri );
        sri.setDocument( contents );
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

        if ( Syntax.getReferencedNames( url, false ).length==0 &&
             !ValidationUtils.isValidUrl(url.trim(), false, CollectionUtils.caseInsensitiveSet( "http", "https" )) ) {
            displayError(resources.getString("error.badhttpurl"), null);
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
        if (rbApplyToArgs.isSelected())
            schemaValidationAssertion.setValidationTarget(ValidationTarget.ARGUMENTS);
        else if (rbApplyToBody.isSelected())
            schemaValidationAssertion.setValidationTarget(ValidationTarget.BODY);
        else if (rbApplyToEnvelope.isSelected())
            schemaValidationAssertion.setValidationTarget(ValidationTarget.ENVELOPE);

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
        if (!XmlUtil.W3C_XML_SCHEMA.equals(rootEl.getNamespaceURI())) {
            log.log(Level.WARNING, "document is not schema (namespace is not + " +
              XmlUtil.W3C_XML_SCHEMA + ")");
            return false;
        }
        return true;
    }

    private Document parseSchema( final String schemaUri,
                                  final String schemaText ) throws SAXException, IOException {
        return parseSchema( schemaUri, schemaText, schemaEntityResolver );
    }

    private Document parseSchema( final String schemaUri,
                                  final String schemaText,
                                  final EntityResolver entityResolver ) throws SAXException, IOException {
        final InputSource inputSource = new InputSource( schemaUri );
        inputSource.setCharacterStream( new StringReader( schemaText ) );
        return XmlUtil.parse( inputSource, entityResolver );
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
        final SelectWsdlSchemaDialog schemaFromWsdlChooser;
        try {
            schemaFromWsdlChooser = new SelectWsdlSchemaDialog(this, right(fullSchemas), right(inputSchemas), right(outputSchemas));
        } catch (DocumentException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (SAXParseException e) {
            throw new RuntimeException(e);
        }
        schemaFromWsdlChooser.pack();
        Utilities.centerOnParentWindow(schemaFromWsdlChooser);
        DialogDisplayer.display(schemaFromWsdlChooser, new Runnable() {
            @Override
            public void run() {
                String result = schemaFromWsdlChooser.getOkedSchema();
                if ( result != null ) {
                    importSchemaContent(
                            getUri(schemaFromWsdlChooser.getOkedSchemaNode(), CollectionUtils.iterable( fullSchemas, inputSchemas, outputSchemas )),
                            result,
                            Collections.<ResourceDocumentResolver>singleton(new WsdlSchemaResourceDocumentResolver(fullSchemas)) );
                }
            }
        });
    }

    private static String getUri( final Node schemaNode,
                                  final Iterable<Pair<String,Element>> uriElementPairs ) {
        String uri = null;

        if ( uriElementPairs != null ) {
            int index = 1;
            for ( final Pair<String,Element> uriElementPair : uriElementPairs ) {
                if ( schemaNode == uriElementPair.right ) {
                    uri = uriElementPair.left;

                    if ( !schemaNode.isSameNode( schemaNode.getOwnerDocument().getDocumentElement() )  ) {
                        // Then this schema is embedded in a WSDL and we need to make up a URI
                        final String uriSuffix = uriElementPair.right.hasAttributeNS( null, "id" ) ?
                                uriElementPair.right.getAttributeNS( null, "id" ) :
                                ".xsd" + index; // a valid ID cannot start with "."
                        uri += "#" + uriSuffix;
                    }

                    break;
                }

                index++;
            }
        }

        return uri;
    }

    private <T,P extends Pair<?,T>> List<T> right( final List<P> pairCollection ) {
        return Functions.map( pairCollection, new Functions.Unary<T,P>(){
            @Override
            public T call( final P tPair ) {
                return tPair.right;
            }
        } );
    }

    private void setEditorText( final String content ) {
        final XMLEditor editor = xmlContainer.getUIAccessibility().getEditor();
        editor.setText(content);
        editor.setLineNumber(1);
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
        final File file = dlg.getSelectedFile();
        final byte[] schemaData;
        FileInputStream fis = null;
        try {
            fis = new FileInputStream( file );
            schemaData = IOUtils.slurpStream( fis );
        } catch ( FileNotFoundException e ) {
            log.log(Level.FINE, "cannot open file" + file.getAbsolutePath(), e);
            return;
        } catch (IOException e) {
            displayError(resources.getString("error.noxmlaturl") + " " + file.getAbsolutePath(), null);
            log.log(Level.FINE, "cannot process file " + file.getAbsolutePath(), e);
            return;
        }finally {
            ResourceUtils.closeQuietly( fis );
        }

        final String encoding = XmlUtil.getEncoding( schemaData );
        final String schemaContent;

        // try to get document
        try {
            schemaContent = new String( schemaData, encoding );
        } catch (IOException e) {
            displayError(resources.getString("error.noxmlaturl") + " " + file.getAbsolutePath(), null);
            log.log(Level.FINE, "cannot parse " + file.getAbsolutePath(), e);
            return;
        }

        importSchemaContent( file.toURI().toString(), schemaContent );
    }

    private void readFromUrl(String url) {
        // get the schema
        final String schema = fetchSchemaFromUrl(url);
        
        // set the schema
        if ( schema != null ) {
            importSchemaContent( url, schema );
        }
    }

    /**
     * Fetch a schema by using the URL of a schema location.
     *
     * @param url the URL of the schema location
     *
     * @return the schema XML content
     */
    private String fetchSchemaFromUrl( final String url ) {
        if (url == null || url.length() < 1) {
            displayError(resources.getString("error.nourl"), null);
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

        final ResourceAdmin resourceAdmin = getResourceAdmin();
        Either<String, String> schemaXml;
        try {
            schemaXml = AdminGuiUtils.doAsyncAdmin(
                    resourceAdmin,
                    SchemaValidationPropertiesDialog.this,
                    resources.getString("urlLoadingDialog.title"),
                    MessageFormat.format(resources.getString("urlLoadingDialog.message"), url),
                    resourceAdmin.resolveResourceAsync(url));
        } catch (InterruptedException e) {
            //do nothing the user cancelled
            return null;
        } catch (InvocationTargetException e) {
            schemaXml = Either.left(ExceptionUtils.getMessage(e));
        }

        if (schemaXml.isLeft()) {
            //An error occurred retrieving the document
            final String errorMsg = "Cannot download document: " + schemaXml.left();
            displayError(errorMsg, "Errors downloading file");
            log.log(Level.FINE, errorMsg, schemaXml.left());
            return null;
        }

        return schemaXml.right();
    }

    private void importSchemaContent( final String uri,
                                      final String content  ) {
        importSchemaContent( uri, content, Collections.<ResourceDocumentResolver>emptyList() );
    }

    private void importSchemaContent( final String uri,
                                      final String content,
                                      final Collection<ResourceDocumentResolver> resolvers ) {

        final Document doc;
        try {
            doc = parseSchema( uri, content, new ResourceAdminEntityResolver(getResourceAdmin(), true));
        } catch (SAXException e) {
            displayError(resources.getString("error.noxmlaturl") + " " + uri, null);
            log.log(Level.FINE, "cannot parse " + uri, e);
            return;
        } catch ( IOException e ) {
            displayError(resources.getString("error.noxmlaturl") + " " + uri, null);
            log.log(Level.FINE, "cannot process " + uri, e);
            return;
        }

        // check if it's a schema
        if (!docIsSchema(doc)) {
            displayError(resources.getString("error.urlnoschema") + " " + uri, null);
            return;
        }

        final String[] contentHolder = new String[]{ content };
        final boolean update = GlobalResourceImportWizard.importDependencies(
                this,
                uri!=null ? uri : "urn:uuid:" + UUID.randomUUID().toString(),
                ResourceType.XML_SCHEMA,
                content,
                getResourceAdmin(),
                resolvers,
                GlobalResourceImportWizard.getUIImportAdvisor( this, false, !getResourceAdmin().allowSchemaDoctype() ),
                new Functions.UnaryVoid<String>(){
                    @Override
                    public void call( final String content ) {
                        contentHolder[0] = content;
                    }
                },
                GlobalResourceImportWizard.getUIErrorListener( this ));

        if ( update ) {
            if ( uri == null ) {
                specifySystemIdTextField.setText( "" );
            } else {
                specifySystemIdTextField.setText( uri );
                specifySystemIdTextField.setCaretPosition( 0 );
            }
            setEditorText( contentHolder[0] );
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
        xmlContainer = XMLContainerFactory.createXmlContainer(true);

        ButtonGroup bg = new ButtonGroup();
        bg.add(rbApplyToEnvelope);
        bg.add(rbApplyToBody);
        bg.add(rbApplyToArgs);
    }

    /** @return true if the Ok button was pressed and the changes committed successfully. */
    public boolean isChangesCommitted() {
        return changesCommitted;
    }

    private static final class DependencyInfo {
        private final String baseUri;
        private final String schemaLocation;
        private final boolean targetNamespaceSet;
        private final String targetNamespace;
        private final boolean matchedByNamespace;
        private final boolean ambiguousNamespace;

        private DependencyInfo( final String baseUri,
                                final String schemaLocation,
                                final boolean targetNamespaceSet,
                                final String targetNamespace,
                                final boolean matchedByNamespace,
                                final boolean ambiguousNamespace ) {
            this.baseUri = baseUri;
            this.schemaLocation = schemaLocation;
            this.targetNamespaceSet = targetNamespaceSet;
            this.targetNamespace = targetNamespace;
            this.matchedByNamespace = matchedByNamespace;
            this.ambiguousNamespace = ambiguousNamespace;
        }

        public String toString() {
            final StringBuilder description = new StringBuilder();

            if ( baseUri != null && !baseUri.isEmpty() ) {
                description.append( "Base URI: " );
                description.append( baseUri );
                description.append( "\n" );
            }

            if ( schemaLocation != null && !schemaLocation.isEmpty() ) {
                description.append( "Location: " );
                description.append( schemaLocation );
                description.append( "\n" );
            }

            if ( targetNamespaceSet ) {
                description.append( "Target Namespace: " );
                if ( targetNamespace != null ) {
                    description.append( targetNamespace );
                } else {
                    description.append( "<no namespace>" );
                }
                description.append( "\n" );
            }

            return description.toString();
        }
    }

    /**
     * Resource document resolver for Schemas embedded in the WSDL.
     */
    private static final class WsdlSchemaResourceDocumentResolver extends ResourceDocumentResolverSupport {
        private final Collection<Pair<String,Element>> schemaElements;

        private WsdlSchemaResourceDocumentResolver( final Collection<Pair<String,Element>> schemaElements ) {
            this.schemaElements = schemaElements;
        }

        @Override
        public ResourceDocument resolveByTargetNamespace( final String uri, final String targetNamespace ) throws IOException {
            ResourceDocument resourceDocument = null;

            Element element = null;
            for ( final Pair<String,Element> schemaElementPair : schemaElements) {
                final String targetNs = schemaElementPair.right.getAttribute("targetNamespace");
                if ( targetNs.equals(targetNamespace) || targetNs.isEmpty() && targetNamespace==null) {
                    element = schemaElementPair.right;
                    break;
                }
            }

            if ( element != null ) {
                try {
                    URI schemaUri = new URI( getUri( element, schemaElements ) );
                    resourceDocument = new URIResourceDocument( schemaUri, XmlUtil.nodeToFormattedString(element), null );
                } catch ( URISyntaxException e ) {
                    throw new IOException(e);
                }
            }


            return resourceDocument;
        }
    }
}
