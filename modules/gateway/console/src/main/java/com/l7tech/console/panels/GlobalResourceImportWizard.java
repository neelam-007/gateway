package com.l7tech.console.panels;

import com.l7tech.common.io.DtdUtils;
import com.l7tech.common.io.FileResourceDocumentResolver;
import com.l7tech.common.io.ResourceDocument;
import com.l7tech.common.io.ResourceDocumentResolver;
import com.l7tech.common.io.URIResourceDocument;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.console.util.Registry;
import com.l7tech.console.xmlviewer.Viewer;
import static com.l7tech.console.panels.GlobalResourceImportContext.*;
import com.l7tech.gateway.common.resources.ResourceAdmin;
import com.l7tech.gateway.common.resources.ResourceEntry;
import com.l7tech.gateway.common.resources.ResourceEntryBag;
import com.l7tech.gateway.common.resources.ResourceEntryHeader;
import com.l7tech.gateway.common.resources.ResourceType;

import com.l7tech.gui.SimpleTableModel;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.RunOnChangeListener;
import com.l7tech.gui.util.TableUtil;
import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.widgets.TextListCellRenderer;
import com.l7tech.objectmodel.DuplicateObjectException;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.util.CausedIOException;
import com.l7tech.util.Charsets;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Functions;
import com.l7tech.util.Pair;
import com.l7tech.util.SchemaUtil;
import com.l7tech.util.TextUtils;
import com.l7tech.xml.DocumentReferenceProcessor;
import org.dom4j.DocumentException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.ext.DefaultHandler2;

import javax.swing.*;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLResolver;
import javax.xml.stream.XMLStreamException;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Wizard for importing global resources.
 */
public class GlobalResourceImportWizard extends Wizard<GlobalResourceImportContext> {

    //- PUBLIC

    /**
     * Create an import wizard with optional starting resources.
     *
     * @param parent The parent window (optional)
     * @param initialSources The initial set of resources to import (optional)
     * @param resourceAdmin The resource admin to use (required)
     */
    public GlobalResourceImportWizard( final Window parent,
                                       final Collection<ResourceEntryHeader> initialSources,
                                       final ResourceAdmin resourceAdmin ) {
        super( parent, new GlobalResourceImportSearchStep( new GlobalResourceImportOptionsStep( new GlobalResourceImportResultsStep( null ) ) ) );
        wizardInput = new GlobalResourceImportContext();
        final Collection<ResourceDocumentResolver> newResourceResolvers = Arrays.asList(
                new FileResourceDocumentResolver(),
                GlobalResourceImportContext.buildDownloadingResolver( resourceAdmin )
        );
        final Functions.Ternary<ImportChoice,ImportOption,ImportChoice,String> choiceResolver = buildChoiceResolver( parent, wizardInput );
        wizardInput.setResourceDocumentResolverForType( ResourceType.XML_SCHEMA, wizardInput.buildSmartResourceEntryResolver( ResourceType.XML_SCHEMA, resourceAdmin, newResourceResolvers, choiceResolver ));
        wizardInput.setResourceDocumentResolverForType( ResourceType.DTD, wizardInput.buildSmartResourceEntryResolver( ResourceType.DTD, resourceAdmin, newResourceResolvers, choiceResolver ));

        if ( initialSources != null ) {
            final Collection<ResourceInputSource> inputSources = new ArrayList<ResourceInputSource>();
            for ( final ResourceEntryHeader header : initialSources ) {
                try {
                    inputSources.add( wizardInput.newResourceInputSource( asUri(header.getUri()), header.getResourceType() ) );
                } catch ( IOException e ) {
                    showErrorMessage(
                        parent,
                        "Error Processing Resource",
                        "Error processing resource '"+TextUtils.truncStringMiddleExact(header.getUri(),80)+"':\n" + ExceptionUtils.getMessage( e ) );
                }
            }
            wizardInput.setResourceInputSources( inputSources );
        }

        init();
    }
                       
    /**
     * Import dependencies for the given resource.
     *
     * @param parent The parent window.
     * @param uriString The URI of the resource
     * @param type The type of the resource
     * @param content The content of the resource
     * @param confirmed True if the import of dependencies is confirmed
     * @return true if the import completed successfully (false if cancelled).
     */
    public static boolean importDependencies( final Window parent,
                                              final String uriString,
                                              final ResourceType type,
                                              final String content,
                                              final boolean confirmed,
                                              final ResourceAdmin resourceAdmin,
                                              final Collection<ResourceDocumentResolver> additionalResolvers ) {
        final GlobalResourceImportContext context = new GlobalResourceImportContext();
        context.setResourceDocumentResolverForType( null, GlobalResourceImportContext.buildResourceEntryResolver( resourceAdmin ) );

        final URI uri;
        final ResourceDocument mainResource;
        try {
            uri = asUri(uriString);
            mainResource = context.newResourceDocument( uriString, content );
        } catch ( IOException e ) {
            showErrorMessage(
                parent,
                "Error Checking Dependencies",
                "Error processing resource:\n" + ExceptionUtils.getMessage( e ) );
            return true; // user can still use the main resource if they want
        }


        if ( !confirmed ) {
            // 1) See if there are any dependencies that are unknown
            boolean missingDependencies = false;
            try {
                //TODO [steve] only process one level of dependency, no need to traverse the entire tree
                processResource( context, mainResource, type, new HashMap<String,ResourceHolder>() );
            } catch( IOException e ) {
                missingDependencies = true;
            } catch ( SAXException e ) {
                missingDependencies = true;
            }

            // 2) Ask if dependencies should be imported
            if ( missingDependencies ) {
                final int choice = JOptionPane.showOptionDialog(
                        parent,
                        "Do you want to import the schemas dependencies as global resources?",
                        "Import Global Resources?",
                        JOptionPane.YES_NO_CANCEL_OPTION,
                        JOptionPane.QUESTION_MESSAGE,
                        null,
                        new Object[]{"Import","Skip","Cancel"},
                        "Import" );
                if ( choice != JOptionPane.YES_OPTION ) {
                    return choice == JOptionPane.NO_OPTION;
                }
            } else {
                return true;
            }
        }

        // 3) Do import and prompt if options need selecting
        final Functions.Ternary<ImportChoice,ImportOption,ImportChoice,String> choiceResolver = buildChoiceResolver( parent, context );
        Collection<ResourceDocumentResolver> resolvers = new ArrayList<ResourceDocumentResolver>();
        resolvers.addAll( additionalResolvers );
        resolvers.add( new FileResourceDocumentResolver() );
        resolvers.add( GlobalResourceImportContext.buildDownloadingResolver( resourceAdmin ) );
        context.setResourceDocumentResolverForType( ResourceType.XML_SCHEMA, context.buildSmartResourceEntryResolver(  ResourceType.XML_SCHEMA, resourceAdmin, resolvers, choiceResolver ));
        context.setResourceDocumentResolverForType( ResourceType.DTD, context.buildSmartResourceEntryResolver(  ResourceType.DTD, resourceAdmin, resolvers, choiceResolver ));
        final Map<String,ResourceHolder> processedResources =
                processResources( context, Collections.singleton( context.newResourceInputSource( uri, content ) ) );

        for ( final ResourceHolder resourceHolder : processedResources.values() ) {
            if ( resourceHolder.isError() ) {
                logger.log(
                        Level.WARNING,
                        "Global resource import of schema dependencies failed due to: " + ExceptionUtils.getMessage( resourceHolder.getError() ),
                        ExceptionUtils.getDebugException( resourceHolder.getError() ) );
                showErrorMessage(
                    parent,
                    "Error Importing Dependencies",
                    "Dependency import failed:\n" + ExceptionUtils.getMessage( resourceHolder.getError() ) );
                return true;
            }
        }

        processedResources.remove( uriString ); // don't import the original resource, only its dependencies.

        // 4) Show summary and save if desired
        final int choice = confirmResourceSave( parent, processedResources.values() );
        if ( choice == JOptionPane.YES_OPTION ) {
            try {
                saveResources( processedResources.values() );
            } catch ( SaveException e ) {
                handleSaveError( parent, e );
            }
            return true;
        } else {
            return choice == JOptionPane.NO_OPTION;
        }
    }

    public static Collection<ResourceHolder> resolveDependencies( final Window parent,
                                                                  final Set<String> uriStrings,
                                                                  final ResourceAdmin resourceAdmin ) {
        final GlobalResourceImportContext context = new GlobalResourceImportContext();
        context.setResourceDocumentResolverForType( null, GlobalResourceImportContext.buildResourceEntryResolver( resourceAdmin ) );

        final Collection<ResourceInputSource> inputSources = new ArrayList<ResourceInputSource>();
        for ( final String uriString : uriStrings ) {
            try {
                inputSources.add( context.newResourceInputSource( asUri(uriString), (ResourceType)null ) );
            } catch ( IOException e ) {
                showErrorMessage(
                    parent,
                    "Error Loading Dependencies",
                    "Error processing resource '"+TextUtils.truncStringMiddleExact(uriString,80)+"':\n" + ExceptionUtils.getMessage( e ) );
            }
        }

        return processResources( context, inputSources ).values();
    }

    //- PROTECTED

    protected static final ResourceBundle resources = ResourceBundle.getBundle( GlobalResourceImportWizard.class.getName() );


    /**
     * Process the given inputs and output all (new and updated) dependencies.
     *
     * @param context The import context to use.
     * @param resourceInputSources The sources for importing.
     * @return The processed resources, a Map of (String) URI to ResourceHolder
     */
    protected static Map<String,ResourceHolder> processResources( final GlobalResourceImportContext context,
                                                                  final Collection<ResourceInputSource> resourceInputSources ) {
        Map<String, ResourceHolder> processedResources = new LinkedHashMap<String, ResourceHolder>();

        for ( final ResourceInputSource resourceInputSource : resourceInputSources ) {
            ResourceDocument resourceDocument = null;
            try {
                resourceDocument = resourceInputSource.asResourceDocument();
                processResource( context, resourceDocument, null, processedResources );
            } catch ( SAXException e ) {
                handleResourceError( context, resourceInputSource.getUri(), resourceDocument, e, processedResources );
            } catch ( IOException e ) {
                handleResourceError( context, resourceInputSource.getUri(), resourceDocument, e, processedResources );
            }
        }

        return processedResources;
    }

    protected static void saveResources( final Collection<ResourceHolder> resourceHolders ) throws SaveException {
        final ResourceAdmin admin = Registry.getDefault().getResourceAdmin();
        final Collection<ResourceEntry> resourceEntries = new ArrayList<ResourceEntry>();

        for ( final ResourceHolder resourceHolder : resourceHolders ) {
            if ( !resourceHolder.isPersist() ) {
                continue;
            }

            try {
                resourceEntries.add( resourceHolder.asResourceEntry() );
            } catch ( IOException e ) {
                throw new SaveException(e);
            }
        }

        if ( !resourceEntries.isEmpty() ) {
            try {
                admin.saveResourceEntryBag( new ResourceEntryBag( resourceEntries ) );
            } catch ( DuplicateObjectException e ) {
                throw new SaveException(e);
            } catch ( UpdateException e ) {
                throw new SaveException(e);
            }
        }
    }
    
    protected static void viewResourceHolder( final Window owner,
                                              final ResourceHolder holder ) {
        try {
            // Don't display resources with errors as XML since the error
            // could be due to XML parsing of the resource
            DialogDisplayer.display( new ViewResourceDialog(
                    owner,
                    "View Resource",
                    holder.getContent(),
                    holder.isXml() && !holder.isError() ) );
        } catch ( IOException e ) {
            handleViewError( owner, e );
        } catch ( DocumentException e ) {
            handleViewError( owner, e );
        } catch ( SAXParseException e ) {
            handleViewError( owner, e );
        }
    }

    protected static void handleViewError( final Component owner,
                                           final Throwable error ) {
        logger.log( Level.WARNING, "Error viewing resource", error );
        showErrorMessage(
                owner,
            "Error Displaying Resource",
            "Unable to display resource:\n" + ExceptionUtils.getMessage( error ) );
    }

    protected static void handleSaveError( final Component owner,
                                           final SaveException saveException ) {
        logger.log( Level.WARNING, "Error saving resources", saveException );
        showErrorMessage(
            owner,
            "Error Saving Resources",
            "Unable to save resources:\n" + ExceptionUtils.getMessage( saveException ) );
    }

    protected static void showErrorMessage( final Component owner,
                                            final String title, final String message) {
        DialogDisplayer.showMessageDialog(
                owner,
                Utilities.getTextDisplayComponent(message, 600, 100, -1, -1),
                title,
                JOptionPane.ERROR_MESSAGE,
                null);
    }

    protected static class DependencySummaryListCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent( final JList list,
                                                       final Object value,
                                                       final int index,
                                                       final boolean isSelected,
                                                       final boolean cellHasFocus ) {
            final Component component =
                    super.getListCellRendererComponent( list, value, index, isSelected, cellHasFocus );

            if ( value instanceof DependencySummary && ((DependencySummary)value).isTransitive() ) {
                component.setFont( component.getFont().deriveFont( Font.ITALIC ) );
            }

            return component;
        }
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger( GlobalResourceImportWizard.class.getName() );

    private void init() {
        setTitle( resources.getString( "dialog.title" ));
        pack();
        Utilities.centerOnParentWindow( this );
    }

    private static int confirmResourceSave( final Window parent,
                                            final Collection<ResourceHolder> resourceHolders ) {

        final JButton viewButton = new JButton( resources.getString("button.view").replace( "&", "" ));
        viewButton.setMnemonic( 'V' );
        viewButton.setEnabled( false );
        final JTable resourcesTable = new JTable();
        final SimpleTableModel<ResourceHolder> resourceHolderTableModel = TableUtil.configureTable(
                resourcesTable,
                TableUtil.column(resources.getString("column.system-id"), 40, 240, 100000, Functions.<String,ResourceHolder>propertyTransform(ResourceHolder.class, "systemId"), String.class),
                TableUtil.column(resources.getString("column.details"), 40, 120, 100000, Functions.<String,ResourceHolder>propertyTransform(ResourceHolder.class, "details"), String.class),
                TableUtil.column(resources.getString("column.type"), 40, 80, 120, Functions.<ResourceType,ResourceHolder>propertyTransform(ResourceHolder.class, "type"), ResourceType.class),
                TableUtil.column(resources.getString("column.action"), 40, 50, 120, Functions.<String,ResourceHolder>propertyTransform(ResourceHolder.class, "action"), String.class)
        );
        resourcesTable.setModel( resourceHolderTableModel );
        resourcesTable.getTableHeader().setReorderingAllowed( false );
        resourcesTable.setDefaultRenderer( ResourceType.class, GlobalResourcesDialog.buildResourceTypeRenderer().asTableCellRenderer() );
        resourcesTable.setSelectionMode( ListSelectionModel.SINGLE_SELECTION );
        resourcesTable.getSelectionModel().addListSelectionListener( new RunOnChangeListener(){
            @Override
            protected void run() {
                viewButton.setEnabled( resourcesTable.getSelectedRow() >= 0 );
            }
        } );
        Utilities.setRowSorter( resourcesTable, resourceHolderTableModel, new int[]{0}, new boolean[]{true}, new Comparator[]{String.CASE_INSENSITIVE_ORDER} );
        resourceHolderTableModel.setRows( new ArrayList<ResourceHolder>(resourceHolders) );
        viewButton.addActionListener( new ActionListener(){
            @Override
            public void actionPerformed( final ActionEvent e ) {
                final int selectedRow = resourcesTable.getSelectedRow();
                if ( selectedRow >= 0 ) {
                    final ResourceHolder resourceHolder = resourceHolderTableModel.getRowObject( resourcesTable.convertRowIndexToModel( selectedRow ));
                    viewResourceHolder( parent, resourceHolder );
                }
            }
        } );
        Utilities.setDoubleClickAction( resourcesTable, viewButton );

        final JScrollPane tableScrollPane = new JScrollPane( resourcesTable );
        tableScrollPane.setPreferredSize( new Dimension( 540, 200 ) );

        final JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout( new BoxLayout( buttonPanel, BoxLayout.Y_AXIS ) );
        buttonPanel.add( viewButton );

        final JPanel displayPanel = new JPanel();
        displayPanel.setLayout( new BorderLayout( 4, 4) );
        displayPanel.add( new JLabel("Do you want to import the schemas dependencies as global resources?"), BorderLayout.NORTH );
        displayPanel.add( tableScrollPane, BorderLayout.CENTER );
        displayPanel.add( buttonPanel, BorderLayout.EAST );
        displayPanel.add( new JLabel("Total Resources: " + resourceHolders.size()), BorderLayout.SOUTH );

        final JOptionPane pane = new JOptionPane(
                displayPanel,
                JOptionPane.QUESTION_MESSAGE,
                JOptionPane.YES_NO_CANCEL_OPTION,
                null,
                new Object[]{"Import","Skip","Cancel"},
                "Import");

        final JDialog dialog = pane.createDialog( parent, "Confirm Global Resource Import" );
        dialog.setDefaultCloseOperation( JDialog.DISPOSE_ON_CLOSE );
        dialog.setResizable(true);
        dialog.setMinimumSize( dialog.getContentPane().getMinimumSize() );
        pane.selectInitialValue();
        dialog.setVisible( true );

        final Object selectedValue = pane.getValue();

        if( "Import".equals( selectedValue )) {
            return JOptionPane.YES_OPTION;
        } else if ( "Skip".equals( selectedValue )) {
            return JOptionPane.NO_OPTION;
        } else {
            return JOptionPane.CLOSED_OPTION;
        }
    }

    private static void handleResourceError(
                                      final GlobalResourceImportContext context,
                                      final URI resourceUri,
                                      final ResourceDocument resourceDocument,
                                      final Throwable error,
                                      final Map<String, ResourceHolder> processedResources ) {
        boolean handled = false;

        String tns = null;
        ResourceType resourceType = null;
        if ( resourceDocument != null ) {
            try {
                tns = resourceDocument.available() ? XmlUtil.getSchemaTNS( resourceDocument.getContent() ) : null;
            } catch ( XmlUtil.BadSchemaException e ) {
                tns = null;
            } catch ( IOException e ) {
                tns = null;
            }

            try {
                resourceType = getResourceType( context, resourceUri, resourceDocument.available() ? resourceDocument.getContent() : null, null, processedResources, null );
                switch ( resourceType ) {
                    case XML_SCHEMA:
                        processedResources.put(
                                resourceUri.toString(),
                                context.newSchemaResourceHolder(resourceDocument, tns, error) );
                        handled = true;
                        break;
                    case DTD:
                        processedResources.put(
                                resourceUri.toString(),
                                context.newDTDResourceHolder(resourceDocument, null, error) );
                        handled = true;
                        break;
                }
            } catch ( IOException e ) {
                // handle below
            }
        }

        if ( !handled ) {
            if ( resourceType == null ) {
                resourceType = resourceUri!=null && resourceUri.toString().endsWith( ResourceType.DTD.getFilenameSuffix() ) ?
                        ResourceType.DTD :
                        ResourceType.XML_SCHEMA;
            }

            ResourceDocument errorResourceDocument = resourceDocument;
            if ( errorResourceDocument == null ) {
                errorResourceDocument = new URIResourceDocument( resourceUri, null, null );
            }

            processedResources.put(
                    resourceUri.toString(),
                    resourceType==ResourceType.XML_SCHEMA ?
                            context.newSchemaResourceHolder(errorResourceDocument, tns, error) :
                            context.newDTDResourceHolder(errorResourceDocument, null, error));
        }
    }

    private static void processResource( final GlobalResourceImportContext context,
                                         final ResourceDocument resourceDocument,
                                         final ResourceType resourceType,  // resource type if known, else null
                                         final Map<String, ResourceHolder> processedResources ) throws SAXException, IOException {

        final URI resourceUri = resourceDocument.getUri();
        final String resourceUriStr = resourceDocument.getUri().toString();
        final ResourceHolder existingResource = processedResources.get( resourceUriStr );
        if ( existingResource != null ) {
            if ( existingResource.isError() ) throwException(existingResource.getError(), IOException.class, SAXException.class);
            return;
        }

        logger.finer( "Processing resource '" + resourceUriStr + "'." );
        try {
            final java.util.List<DependencyInfo> dependencies = new ArrayList<DependencyInfo>();
            final String content = resourceDocument.getContent();
            final ResourceType contentResourceType = getResourceType( context, resourceUri, content, resourceType, processedResources, dependencies );

            switch ( contentResourceType ) {
                case XML_SCHEMA:
                    processedResources.put( resourceUriStr, context.newSchemaResourceHolder(resourceDocument, XmlUtil.getSchemaTNS(content), null) ); // put early to prevent circular processing
                    final Document schemaDoc = XmlUtil.parse(
                            new InputSource( resourceUriStr ){{setCharacterStream( new StringReader(content) );}},
                            new DefaultHandler2(){
                                @Override
                                public InputSource resolveEntity( final String name, final String publicId, final String baseURI, final String systemId ) throws SAXException, IOException {
                                    final Pair<String,String> resolved =
                                            GlobalResourceImportWizard.resolveEntity( context, baseURI, systemId, publicId, processedResources, dependencies );
                                    final InputSource entitySource = new InputSource( resolved.left );
                                    entitySource.setCharacterStream( new StringReader(content) );
                                    return entitySource;
                                }
                            } );
                    final DocumentReferenceProcessor schemaReferenceProcessor = DocumentReferenceProcessor.schemaProcessor();
                    schemaReferenceProcessor.processDocumentReferences( schemaDoc, new DocumentReferenceProcessor.ReferenceCustomizer(){
                        @Override
                        public String customize( final Document document,
                                                 final Node node,
                                                 final String documentUrl,
                                                 final DocumentReferenceProcessor.ReferenceInfo referenceInfo ) {
                            assert node instanceof Element;
                            if ( node instanceof Element ) {
                                dependencies.add( DependencyInfo.fromSchemaDependencyElement( resourceUriStr, (Element)node ) );
                            }
                            return null;
                        }
                    } );
                    break;
                case DTD:
                    processedResources.put( resourceUriStr, context.newDTDResourceHolder(resourceDocument, null, null) ); // put early to prevent circular processing
                    DtdUtils.processReferences( resourceUriStr, content, new DtdUtils.Resolver(){
                        @Override
                        public Pair<String, String> call( final String publicId, final String baseUri, final String systemId ) throws IOException {
                            try {
                                return GlobalResourceImportWizard.resolveEntity( context, baseUri, systemId, publicId, processedResources, dependencies );
                            } catch ( SAXException e ) {
                                throw new IOException(e);
                            }
                        }
                    } );
                    break;
            }

            final ResourceHolder resourceHolder = processedResources.get( resourceUriStr );
            for ( final DependencyInfo dependency : dependencies ) {

                if ( dependency.processed ) {
                    resourceHolder.addDependency( dependency.uri );
                    continue;
                }

                if ( dependency.uri != null && !dependency.uri.isEmpty() ) {
                    final URI depUri = asUri(dependency.uri);
                    final boolean absoluteLocation = depUri.isAbsolute();
                    final ResourceDocument dependencyDocument = absoluteLocation ?
                            context.newResourceInputSource( asUri(dependency.uri), dependency.resourceType ).asResourceDocument():
                            resourceDocument.relative( dependency.uri, context.getResourceDocumentResolverForType(dependency.resourceType) );

                    // TODO [steve] if this is a new resource, check if we have an existing resource with the same target namespace and have the user pick one
                    if ( dependencyDocument.exists() ) {
                        resourceHolder.addDependency( dependencyDocument.getUri().toString() );

                        // Recursively resolve the dependencies.
                        processResource( context, dependencyDocument, null, processedResources );
                        continue;
                    }
                }

                if ( dependency.hasTargetNamespace ) {
                    final ResourceDocument dependencyDocument =
                            context.getResourceDocumentResolverForType(ResourceType.XML_SCHEMA).resolveByTargetNamespace( dependency.targetNamespace );

                    if ( dependencyDocument != null && dependencyDocument.exists() ) {
                        resourceHolder.addDependency( dependencyDocument.getUri().toString() );

                        // Recursively resolve the schemas dependencies.
                        processResource( context, dependencyDocument, ResourceType.XML_SCHEMA, processedResources );
                        continue;
                    }
                }

                throw new CausedIOException( "Resource not found : " + dependency );
            }
        } catch ( Exception e ) {
            processedResources.remove( resourceUriStr );
            if ( e instanceof IOException ) {
                throw (IOException) e;
            }
            if ( e instanceof SAXException ) {
                throw (SAXException) e;
            }
            if ( e instanceof XmlUtil.BadSchemaException ) {
                throw new CausedIOException( ExceptionUtils.getMessage( e ), e );
            }
            throw ExceptionUtils.wrap(e);
        }

    }

    private static Pair<String,String> resolveEntity( final GlobalResourceImportContext context,
                                                      final String baseURI,
                                                      final String systemId,
                                                      final String publicId,
                                                      final Map<String, ResourceHolder> processedResources,
                                                      final java.util.List<DependencyInfo> dependencies ) throws IOException, SAXException {
        Pair<String,String> entity = null;

        if ( publicId != null ) {
            final ResourceDocument dtdResourceDocument = context.getResourceDocumentResolverForType(ResourceType.DTD).resolveByPublicId( publicId );
            if ( dtdResourceDocument != null ) {
                entity = new Pair<String,String>( dtdResourceDocument.getUri().toString(), dtdResourceDocument.getContent());
                if ( dependencies != null ) {
                    dependencies.add( DependencyInfo.fromResourceDocument( ResourceType.DTD, dtdResourceDocument ) );
                }
                processResource( context, dtdResourceDocument, ResourceType.DTD, processedResources );
            }
        }

        if ( entity == null ) {
            final URI absoluteSystemId;
            if ( baseURI != null ) {
                try {
                    absoluteSystemId = new URI(baseURI).resolve( systemId );
                } catch ( URISyntaxException e ) {
                    throw new IOException( "Error resolving system identifier '"+systemId+"' against '"+baseURI+"'" );
                }
            } else {
                absoluteSystemId = absoluteUri(systemId);
            }

            final ResourceHolder holder = processedResources.get( absoluteSystemId.toString() );
            if ( holder != null && !holder.isError() ) {
                if ( dependencies != null ) {
                    dependencies.add( DependencyInfo.fromResourceDocument( holder.getType(), holder.asResourceDocument() ) );
                }
                entity = new Pair<String,String>( absoluteSystemId.toString(), holder.getContent());
            } else {
                final ResourceDocument dtdResourceDocument = context.newResourceInputSource( absoluteSystemId, ResourceType.DTD ).asResourceDocument();
                entity = new Pair<String,String>( absoluteSystemId.toString(), dtdResourceDocument.getContent());
                if ( dependencies != null ) {
                    dependencies.add( DependencyInfo.fromResourceDocument( ResourceType.DTD, dtdResourceDocument ) );
                }
                processResource( context, dtdResourceDocument, ResourceType.DTD, processedResources );
            }
        }

        return entity;
    }

    private static ResourceType getResourceType( final GlobalResourceImportContext context,
                                                 final URI location,
                                                 final String content,
                                                 final ResourceType resourceType,
                                                 final Map<String, ResourceHolder> processedResources,
                                                 final java.util.List<DependencyInfo> dependencies  ) throws IOException {
        ResourceType processedResourceType = resourceType;

        if ( processedResourceType == null ) {
            for ( final ResourceType type : ResourceType.values() ) {
                if ( location.getPath().endsWith( "." + type.getFilenameSuffix() )) {
                    processedResourceType = type;
                    break;
                }
            }

            if ( processedResourceType == null && content != null) {
                try {
                    final QName type = XmlUtil.getDocumentQName( location.toString(), content, new XMLResolver(){
                        @Override
                        public Object resolveEntity( final String publicID, final String systemID, final String baseURI, final String namespace ) throws XMLStreamException {
                            try {
                                final Pair<String,String> resolved = GlobalResourceImportWizard.resolveEntity( context, baseURI, systemID, publicID, processedResources, dependencies );
                                return new ByteArrayInputStream( resolved.right.getBytes( Charsets.UTF8 ));
                            } catch ( SAXException e ) {
                                throw new XMLStreamException(e);
                            } catch ( IOException e ) {
                                throw new XMLStreamException(e);
                            }
                        }
                    } );
                    if ( SchemaUtil.isSchema( type ) ) {
                        processedResourceType = ResourceType.XML_SCHEMA;
                    }
                } catch ( SAXException e ) {
                    // not XML?
                }
            }

            if ( processedResourceType == null ) {
                processedResourceType = ResourceType.DTD;
            }
        } 

        return processedResourceType;
    }

    private static Functions.Ternary<ImportChoice,ImportOption,ImportChoice,String>
            buildChoiceResolver( final Window parent,
                                 final GlobalResourceImportContext context ) {
        return new Functions.Ternary<ImportChoice,ImportOption,ImportChoice,String>(){
            @Override
            public ImportChoice call( final ImportOption importOption,
                                      final ImportChoice importChoice,
                                      final String resourceDetail ) {
                final StringBuilder messageBuilder = new StringBuilder();
                messageBuilder.append( "<html>" );
                switch ( importOption ) {
                    case CONFLICTING_URI:
                        messageBuilder.append( "The System ID of an imported resource conflicts with an existing resource:" );
                        break;
                    case CONFLICTING_TARGET_NAMESPACE:
                        messageBuilder.append( "The target namespace of an imported resource matches an existing resource:" );
                        break;
                    case CONFLICTING_PUBLIC_ID:
                        messageBuilder.append( "The public identifier of an imported resource is a duplicate of an existing resource:" );
                        break;
                    case MISSING_RESOURCE:
                        messageBuilder.append( "A dependency of an imported resource is missing or invalid:" );
                        //TODO [steve] which is it? missing or invalid resource
                        break;
                }

                //TODO [steve] display resource entry description?

                messageBuilder.append( "<br/><br/>" );
                messageBuilder.append( TextUtils.truncStringMiddleExact( resourceDetail, 80 ) );
                messageBuilder.append( "<br/><br/>" );
                messageBuilder.append( "Do you want to:</html>" );

                String msg = messageBuilder.toString();

                final JPanel optionsPanel = new JPanel();
                optionsPanel.setLayout( new BoxLayout( optionsPanel, BoxLayout.Y_AXIS ) );

                final JLabel optionLabel = new JLabel( msg );
                optionLabel.setAlignmentX( JComponent.LEFT_ALIGNMENT );
                optionsPanel.add( optionLabel );

                final JComboBox optionComboBox = new JComboBox();
                optionComboBox.setAlignmentX( JComponent.LEFT_ALIGNMENT );
                optionComboBox.setModel( new DefaultComboBoxModel( importOption.getImportChoices().toArray() ) );
                optionComboBox.setRenderer( new TextListCellRenderer<ImportChoice>( new Functions.Unary<String,ImportChoice>(){
                    @Override
                    public String call( final ImportChoice importChoice ) {
                        String text;
                        final String optionChoiceKey = "option." + importOption.name() + "." + importChoice.name();
                        if ( resources.containsKey( optionChoiceKey ) ) {
                            text = resources.getString( optionChoiceKey );
                        } else {
                            text = resources.getString( "choice." + importChoice.name() );
                        }
                        return text;
                    }
                } ) );

                optionsPanel.add( Box.createVerticalStrut( 4 ) );
                optionsPanel.add( optionComboBox );
                optionsPanel.add( Box.createVerticalStrut( 4 ) );

                final int choice = JOptionPane.showOptionDialog(
                        parent,
                        optionsPanel,
                        "Select Import Option",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.QUESTION_MESSAGE,
                        null,
                        new Object[]{"This Time Only","Always"},
                        "This Time Only" );

                ImportChoice result = null;
                if ( choice == JOptionPane.YES_OPTION ) {
                    result = (ImportChoice) optionComboBox.getSelectedItem();
                } else if ( choice == JOptionPane.NO_OPTION ) {
                    result = (ImportChoice) optionComboBox.getSelectedItem();

                    if ( result != null ) {
                        // save this choice for later
                        Map<ImportOption, ImportChoice> options = new HashMap<ImportOption, ImportChoice>(context.getImportOptions());
                        options.put( importOption, result );
                        context.setImportOptions( options );
                    }
                }

                if ( result == null ) {
                    result = importChoice;
                }

                return result;
            }
        };
    }

    private static URI asUri( final String uri ) throws IOException {
        try {
            return new URI(uri);
        } catch ( URISyntaxException e ) {
            throw new IOException( e );
        }
    }

    private static URI absoluteUri( final String uriString ) throws IOException {
        final URI uri = asUri(uriString);

        if ( !uri.isAbsolute() ) {
            throw new IOException("Cannot resolve relative URI : " + uri);            
        }

        return uri;
    }

    private static void throwException( final Throwable throwable,
                                        final Class<?>... checkedExceptionTypes ) throws IOException {

        for ( Class<?> exceptionClass : checkedExceptionTypes ) {
            if ( exceptionClass.isInstance(throwable) ) {
                GlobalResourceImportWizard.<RuntimeException>throwAsType( throwable );
            }
        }

        throw new CausedIOException(throwable);
    }

    @SuppressWarnings({"unchecked"})
    private static <T extends Throwable> void throwAsType( final Throwable throwable ) throws T {
        throw (T) throwable;
    }

    private static final class DependencyInfo {
        private final ResourceType resourceType;
        private final String baseUri;
        private final String uri;
        private final boolean hasTargetNamespace;
        private final String targetNamespace;
        private final boolean processed;

        private DependencyInfo( final ResourceType resourceType,
                                final String baseUri,
                                final String uri,
                                final boolean hasTargetNamespace,
                                final String targetNamespace,
                                final boolean processed ) {
            this.resourceType = resourceType;
            this.baseUri = baseUri;
            this.uri = uri;
            this.hasTargetNamespace = hasTargetNamespace;
            this.targetNamespace = targetNamespace;
            this.processed = processed;
        }

        private static DependencyInfo fromResourceDocument( final ResourceType type, final ResourceDocument resourceDocument ) {
            return new DependencyInfo( type, null, resourceDocument.getUri().toString(), false, null, true );
        }

        private static DependencyInfo fromSchemaDependencyElement( final String baseUri,
                                                                   final Element dependencyEl ) {
            final String dependencyLocation = dependencyEl.getAttribute( "schemaLocation" );
            final String dependencyNamespace = dependencyEl.hasAttribute( "namespace" ) ? dependencyEl.getAttribute("namespace") : null;
            final boolean dependencyNamespaceSet = "import".equals( dependencyEl.getLocalName() );

            return new DependencyInfo( ResourceType.XML_SCHEMA, baseUri, dependencyLocation, dependencyNamespaceSet, dependencyNamespace, false );
        }

        public String toString() {
            final StringBuilder description = new StringBuilder();
            boolean first = true;

            if ( baseUri != null && !baseUri.isEmpty() ) {
                description.append( "Base URI:" );
                description.append( baseUri );
                first = false;
            }

            if ( uri != null && !uri.isEmpty() ) {
                if (!first) description.append( "," );
                description.append( "URI:" );
                description.append( uri );
                first = false;
            }

            if ( hasTargetNamespace ) {
                if (!first) description.append( "," );
                description.append( "Target Namespace:" );
                if ( targetNamespace != null ) {
                    description.append( targetNamespace );
                } else {
                    description.append( "<no namespace>" );
                }
            }

            return description.toString();
        }
    }

    private static final class ViewResourceDialog extends JDialog {
        private ViewResourceDialog( final Window parent,
                                    final String title,
                                    final String resource,
                                    final boolean xml ) throws DocumentException, SAXParseException, IOException {

            super( parent, JDialog.DEFAULT_MODALITY_TYPE );
            setTitle( title );
            setDefaultCloseOperation( JDialog.DISPOSE_ON_CLOSE );

            final JPanel panel = new JPanel( new BorderLayout() );
            final JComponent viewComponent;
            if ( xml ) {
                viewComponent = Viewer.createMessageViewer( resource );
            } else {
                final JScrollPane scrollPane = new JScrollPane();
                final JTextArea textArea = new JTextArea();
                textArea.setEditable( false );
                textArea.setText( resource );
                textArea.setCaretPosition( 0 );
                scrollPane.setViewportView( textArea );
                viewComponent = scrollPane;
            }
            viewComponent.setPreferredSize( new Dimension(580, 640) );
            panel.add( viewComponent, BorderLayout.CENTER );

            getContentPane().add(panel, BorderLayout.CENTER);

            pack();
            setMinimumSize( getContentPane().getMinimumSize() );
            Utilities.setEscKeyStrokeDisposes( this );
            Utilities.centerOnParentWindow( this );
        }
    }
}
