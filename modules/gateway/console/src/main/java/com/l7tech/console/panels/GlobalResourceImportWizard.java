package com.l7tech.console.panels;

import com.l7tech.common.io.DocumentReferenceProcessor;
import com.l7tech.common.io.DtdUtils;
import com.l7tech.common.io.FileResourceDocumentResolver;
import com.l7tech.common.io.ResourceDocument;
import com.l7tech.common.io.ResourceDocumentResolver;
import com.l7tech.common.io.SchemaUtil;
import com.l7tech.common.io.URIResourceDocument;
import com.l7tech.common.io.XmlUtil;
import static com.l7tech.console.panels.GlobalResourceImportContext.*;

import com.l7tech.console.action.Actions;
import com.l7tech.console.util.ResourceAdminEntityResolver;
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
import com.l7tech.util.Charsets;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Functions;
import com.l7tech.util.Pair;
import com.l7tech.util.ResourceUtils;
import com.l7tech.util.TextUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
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
        this( parent, initialSources, resourceAdmin, getUIErrorListener(parent) );
    }

    /**
     * Create an import wizard with optional starting resources.
     *
     * @param parent The parent window (optional)
     * @param initialSources The initial set of resources to import (optional)
     * @param resourceAdmin The resource admin to use (required)
     */
    public GlobalResourceImportWizard( final Window parent,
                                       final Collection<ResourceEntryHeader> initialSources,
                                       final ResourceAdmin resourceAdmin,
                                       final ErrorListener errorListener ) {
        super( parent, new GlobalResourceImportSearchStep( new GlobalResourceImportOptionsStep( new GlobalResourceImportResultsStep( null ) ) ) );
        this.resourceAdmin = resourceAdmin;
        this.wizardInput = buildContext( parent, initialSources, resourceAdmin, errorListener );
        init();
    }

    /**
     * Import dependencies for the given resource.
     *
     * @param parent The parent window.
     * @param uriString The URI of the resource
     * @param type The type of the resource
     * @param content The content of the resource
     * @param resourceAdmin The resource admin to use
     * @param additionalResolvers Additional resolvers to use for imported resources (may be null)
     * @param importAdvisor Callback for consultation if the import needs confirmation (may be null)
     * @param updatedContentCallback Callback for modification of the main schema content (may be null)
     * @param errorListener listener for errors
     * @return true if the import completed successfully (false if cancelled).
     */
    public static boolean importDependencies( final Window parent,
                                              final String uriString,
                                              final ResourceType type,
                                              final String content,
                                              final ResourceAdmin resourceAdmin,
                                              final Collection<ResourceDocumentResolver> additionalResolvers,
                                              final ImportAdvisor importAdvisor,
                                              final Functions.UnaryVoid<String> updatedContentCallback,
                                              final ErrorListener errorListener ) {
        final GlobalResourceImportContext context = new GlobalResourceImportContext();
        final ChoiceSelector choiceSelector = buildChoiceSelector( parent, context );
        final Functions.UnaryThrows<ResourceEntryHeader,Collection<ResourceEntryHeader>,IOException> entitySelector = buildEntitySelector( parent, choiceSelector );
        final ResourceTherapist manualResourceTherapist = buildManualResourceTherapist( parent, context, resourceAdmin );
        return importDependencies( context, uriString, type, content, resourceAdmin, additionalResolvers, importAdvisor, updatedContentCallback, errorListener, choiceSelector, entitySelector, manualResourceTherapist );
    }

    public static Collection<ResourceHolder> resolveDependencies( final Set<String> uriStrings,
                                                                  final ResourceAdmin resourceAdmin,
                                                                  final ErrorListener errorListener ) {
        final GlobalResourceImportContext context = new GlobalResourceImportContext();
        context.setResourceDocumentResolverForType( null, GlobalResourceImportContext.buildResourceEntryResolver( resourceAdmin, true, null ) );

        final Collection<ResourceInputSource> inputSources = new ArrayList<ResourceInputSource>();
        for ( final String uriString : uriStrings ) {
            try {
                inputSources.add( context.newResourceInputSource( asUri(uriString), (ResourceType)null ) );
            } catch ( IOException e ) {
                errorListener.notifyError(
                    "Error Loading Dependencies",
                    "Error processing resource '"+TextUtils.truncStringMiddleExact(uriString,80)+"':\n" + ExceptionUtils.getMessage( e ) );
            }
        }

        return processResources( context, inputSources ).values();
    }

    /**
     * Build an import advisor with UI callback.
     *
     * @param parent The parent component to use
     * @param importConfirmed True if the import of dependencies is pre-confirmed (so no UI prompt required)
     * @return The import advisor
     */
    public static ImportAdvisor getUIImportAdvisor( final Component parent,
                                                    final boolean importConfirmed,
                                                    final boolean warnForDoctype ) {
        return new ImportAdvisor() {
            @Override
            public DependencyImportChoice confirmImportDependencies() {
                return importConfirmed ? DependencyImportChoice.IMPORT : confirmResourceImport( parent );
            }

            @Override
            public DependencyImportChoice confirmCompleteImport( final Collection<ResourceHolder> resourceHolders ) {
                return confirmResourceSave( parent, warnForDoctype, resourceHolders );
            }
        };
    }

    /**
     * Advice for importing resources.
     */
    public interface ImportAdvisor {

        /**
         * Confirmation callback for importing dependencies (before any import is attempted).
         *
         * @return The import choice.
         */
        DependencyImportChoice confirmImportDependencies();

        /**
         * Confirmation callback for resource changes (when import is complete)
         *
         * @param resourceHolders The resource changes that will be made.
         * @return The import choice
         */
        DependencyImportChoice confirmCompleteImport( Collection<ResourceHolder> resourceHolders );
    }

    /**
     * Enumerated type for an import choice.
     */
    public enum DependencyImportChoice{
        /**
         * Import the dependencies (proceed)
         */
        IMPORT,

        /**
         * Skip import of dependencies (proceed)
         */
        SKIP,

        /**
         * Cancel the current activity.
         */
        CANCEL
    }

    //- PROTECTED

    protected static final ResourceBundle resources = ResourceBundle.getBundle( GlobalResourceImportWizard.class.getName() );

    protected ResourceAdmin getResourceAdmin() {
        return resourceAdmin;
    }

    /**
     * Import dependencies for the given resource.
     */
    protected static boolean importDependencies( final GlobalResourceImportContext context,
                                                 final String uriString,
                                                 final ResourceType type,
                                                 final String content,
                                                 final ResourceAdmin resourceAdmin,
                                                 final Collection<ResourceDocumentResolver> additionalResolvers,
                                                 final ImportAdvisor importAdvisor,
                                                 final Functions.UnaryVoid<String> updatedContentCallback,
                                                 final ErrorListener errorListener,
                                                 final ChoiceSelector choiceSelector,
                                                 final Functions.UnaryThrows<ResourceEntryHeader,Collection<ResourceEntryHeader>,IOException> entitySelector,
                                                 final ResourceTherapist resourceTherapist ) {
        context.setResourceDocumentResolverForType( null, GlobalResourceImportContext.buildResourceEntryResolver( resourceAdmin, false, null ) );

        final URI uri;
        final ResourceDocument mainResource;
        try {
            uri = asUri(uriString);
            mainResource = context.newResourceDocument( uriString, content );
        } catch ( IOException e ) {
            errorListener.notifyError(
                "Error Checking Dependencies",
                "Error processing resource:\n" + ExceptionUtils.getMessage( e ) );
            return true; // user can still use the main resource if they want
        }

        if ( importAdvisor != null ) {
            // 1) See if there are any dependencies that are unknown
            boolean missingDependencies = false;
            try {
                final Map<String,ResourceHolder> processed = new HashMap<String,ResourceHolder>();
                processResource( context, mainResource, type, null, null, DependencyProcessing.ON, processed);
                if ( processed.get(uriString) != null && !content.equals(processed.get(uriString).getContent()) ) {
                    // treat as missing since a relative URI had to be updated to resolve the dependency
                    missingDependencies = true;
                }
            } catch( IOException e ) {
                missingDependencies = true;
            } catch ( SAXException e ) {
                missingDependencies = true;
            }

            // 2) Ask if dependencies should be imported
            if ( missingDependencies ) {
                switch ( importAdvisor.confirmImportDependencies() ) {
                    case SKIP:
                        return true;
                    case CANCEL:
                        return false;
                }
            } else {
                return true;
            }
        }

        // 3) Do import and prompt if options need selecting
        Collection<ResourceDocumentResolver> resolvers = new ArrayList<ResourceDocumentResolver>();
        if ( additionalResolvers!=null ) resolvers.addAll( additionalResolvers );
        resolvers.add( new FileResourceDocumentResolver() );
        resolvers.add( GlobalResourceImportContext.buildDownloadingResolver( resourceAdmin ) );
        resolvers.add( GlobalResourceImportContext.buildManualResolver(resourceTherapist, choiceSelector) );
        context.setResourceDocumentResolverForType( ResourceType.XML_SCHEMA, context.buildSmartResolver(  ResourceType.XML_SCHEMA, resourceAdmin, resolvers, choiceSelector, entitySelector, resourceTherapist ));
        context.setResourceDocumentResolverForType( ResourceType.DTD, context.buildSmartResolver(  ResourceType.DTD, resourceAdmin, resolvers, choiceSelector, entitySelector, resourceTherapist ));
        final Map<String,ResourceHolder> processedResources =
                processResources( context, Collections.singleton( context.newResourceInputSource( uri, content, type ) ) );

        for ( final ResourceHolder resourceHolder : processedResources.values() ) {
            if ( resourceHolder.isError() ) {
                logger.log(
                        Level.WARNING,
                        "Global resource import of schema dependencies failed due to: " + ExceptionUtils.getMessage( resourceHolder.getError() ),
                        ExceptionUtils.getDebugException( resourceHolder.getError() ) );
                errorListener.notifyError(
                    "Error Importing Dependencies",
                    "Dependency import failed:\n" + ExceptionUtils.getMessage( resourceHolder.getError() ) );
                return true;
            }
        }

        final ResourceHolder holder = processedResources.remove( uriString ); // don't import the original resource, only its dependencies.

        // 4) Show summary and save if desired
        final DependencyImportChoice choice = importAdvisor==null ?
                DependencyImportChoice.IMPORT :
                importAdvisor.confirmCompleteImport( processedResources.values() );
        if ( choice == DependencyImportChoice.IMPORT ||
             choice == DependencyImportChoice.SKIP ) {
            // callback with updated content unless import is cancelled
            if ( holder != null && holder.isPersist() && updatedContentCallback!=null ) {
                try {
                    updatedContentCallback.call( holder.getContent() );
                } catch ( IOException e ) {
                    // not expected since the content must be available
                    logger.log( Level.WARNING, "Error updating imported schema content.", e );
                }
            }
        }
        if ( choice == DependencyImportChoice.IMPORT ) {
            try {
                saveResources( resourceAdmin, processedResources.values() );
            } catch ( SaveException e ) {
                handleSaveError( errorListener, e );
            }
            return true;
        } else {
            return choice == DependencyImportChoice.SKIP;
        }
    }
    
    /**
     * Process the given inputs and output all (new and updated) dependencies.
     *
     * @param context The import context to use.
     * @param resourceInputSources The sources for importing.
     * @return The processed resources, a Map of (String) URI to ResourceHolder
     */
    protected static Map<String,ResourceHolder> processResources( final GlobalResourceImportContext context,
                                                                  final Collection<ResourceInputSource> resourceInputSources ) {
        final Map<String, ResourceHolder> processedResources = new LinkedHashMap<String, ResourceHolder>();
        context.setCurrentResourceHolders( processedResources.values() );

        final Map<URI,ResourceDocument> resolvedResources = new HashMap<URI,ResourceDocument>();

        for ( final ResourceInputSource resourceInputSource : resourceInputSources ) {
            processResource( context, resourceInputSource, resolvedResources, ResourceType.XML_SCHEMA, DependencyProcessing.ON, processedResources );
        }

        // DTDs used from schemas will already have been processed, this will process
        // any directly imported DTDs but will not cause and error if the dependencies
        // cannot be processed since we can't tell if the DTD is supposed to be a "full"
        // DTD or not.
        for ( final ResourceInputSource resourceInputSource : resourceInputSources ) {
            processResource( context, resourceInputSource, resolvedResources, ResourceType.DTD, DependencyProcessing.AUTO, processedResources );
        }

        context.setCurrentResourceHolders( Collections.<ResourceHolder>emptyList() );
        return processedResources;
    }

    protected static void saveResources( final ResourceAdmin admin,
                                         final Collection<ResourceHolder> resourceHolders ) throws SaveException {
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
            } catch ( UpdateException e ) {
                throw new SaveException(ExceptionUtils.getMessage( e ), e);
            }
        }
    }
    
    protected static void viewResourceHolder( final Window owner,
                                              final ResourceHolder holder,
                                              final Collection<ResourceHolder> resourceHolders ) {
        try {
            final ResourceEntry entry = new ResourceEntry();
            entry.setUri( holder.getSystemId() );
            entry.setContent( holder.getContent() );
            entry.setType( holder.getType()!=null ? holder.getType() : holder.isXml() ? ResourceType.XML_SCHEMA : ResourceType.DTD );

            DialogDisplayer.display( new ResourceEntryEditor(
                    owner,
                    entry,
                    new ResourceHolderEntityResolver(resourceHolders),
                    true,
                    false,
                    false ) );
        } catch ( IOException e ) {
            handleViewError( owner, e );
        }
    }

    protected static Pair<String,String> editResource( final Window owner,
                                                       final ResourceType type,
                                                       final String uri,
                                                       final String content,
                                                       final EntityResolver entityResolver,
                                                       final boolean warnForDoctype ) {
        Pair<String,String> editedResource = null;

        final ResourceEntry entry = new ResourceEntry();
        entry.setUri( uri );
        entry.setContent( content );
        entry.setType( type );

        final ResourceEntryEditor editor = new ResourceEntryEditor(
                owner,
                entry,
                entityResolver,
                true,
                true,
                warnForDoctype );

        editor.setVisible( true );
        if ( editor.wasOk() ) {
            editedResource = new Pair<String,String>( entry.getUri(), entry.getContent() );
        }

        return editedResource;
    }

    protected static boolean hasDoctype( final Collection<ResourceHolder> resourceHolders,
                                         final boolean persistOnly ) {
        boolean hasDoctype = false;
        for ( final ResourceHolder holder : resourceHolders ) {
            try {
                if ( (!persistOnly || holder.isPersist()) && holder.isXml() && XmlUtil.hasDoctype( holder.getContent() ) ) {
                    hasDoctype = true;
                    break;
                }
            } catch ( IOException e ) {
                // check other resources
            }
        }
        return hasDoctype;
    }
    
    protected static String describe( final String baseUri,
                                      final String uri,
                                      final String publicId,
                                      final boolean hasTargetNamespace,
                                      final String targetNamespace ) {
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

        if ( publicId != null ) {
            if (!first) description.append( "," );
            description.append( "Public ID:" );
            description.append( publicId );
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
        logger.log( Level.WARNING, "Error saving resources", ExceptionUtils.getDebugException(saveException) );
        if (saveException instanceof DuplicateObjectException) {
            showErrorMessage(
                owner,
                "Error Saving Resources",
                "Resource already exists: " + ExceptionUtils.getMessage( saveException ) );
        } else {
            showErrorMessage(
                owner,
                "Error Saving Resources",
                "Unable to save resources:\n" + ExceptionUtils.getMessage( saveException ) );
        }
    }

    protected static void handleSaveError( final ErrorListener listener,
                                           final SaveException saveException ) {
        logger.log( Level.WARNING, "Error saving resources", saveException );
        listener.notifyError(
            "Error Saving Resources",
            "Unable to save resources:\n" + ExceptionUtils.getMessage( saveException ) );
    }

    protected static void showErrorMessage( final Component owner,
                                            final String title,
                                            final String message) {
        DialogDisplayer.showMessageDialog(
                owner,
                Utilities.getTextDisplayComponent(message, 600, 100, -1, -1),
                title,
                JOptionPane.ERROR_MESSAGE,
                null);
    }

    protected static ErrorListener getUIErrorListener( final Component owner ) {
        return new ErrorListener() {
            @Override
            public void notifyError( final String title, final String message ) {
                showErrorMessage( owner, title, message );
            }
        };
    }

    protected static interface ErrorListener {
        void notifyError( String title, String message );
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

    //- PACKAGE

    static GlobalResourceImportContext buildContext( final Window parent,
                                                     final Collection<ResourceEntryHeader> initialSources,
                                                     final ResourceAdmin resourceAdmin,
                                                     final ErrorListener errorListener ) {
        final GlobalResourceImportContext context = new GlobalResourceImportContext();

        final ChoiceSelector choiceSelector = buildChoiceSelector( parent, context );
        final Functions.UnaryThrows<ResourceEntryHeader,Collection<ResourceEntryHeader>, IOException> entitySelector = buildEntitySelector( parent, choiceSelector );
        final ResourceTherapist manualResourceTherapist = buildManualResourceTherapist( parent, context, resourceAdmin );
        final Collection<ResourceDocumentResolver> newResourceResolvers = Arrays.asList(
                new FileResourceDocumentResolver(),
                GlobalResourceImportContext.buildDownloadingResolver( resourceAdmin ),
                GlobalResourceImportContext.buildManualResolver(manualResourceTherapist, choiceSelector)
        );
        context.setResourceDocumentResolverForType( ResourceType.XML_SCHEMA, context.buildSmartResolver( ResourceType.XML_SCHEMA, resourceAdmin, newResourceResolvers, choiceSelector, entitySelector, manualResourceTherapist ));
        context.setResourceDocumentResolverForType( ResourceType.DTD, context.buildSmartResolver( ResourceType.DTD, resourceAdmin, newResourceResolvers, choiceSelector, entitySelector, manualResourceTherapist ));

        if ( initialSources != null ) {
            final Collection<ResourceInputSource> inputSources = new ArrayList<ResourceInputSource>();
            for ( final ResourceEntryHeader header : initialSources ) {
                try {
                    inputSources.add( context.newResourceInputSource( asUri(header.getUri()), header.getResourceType() ) );
                } catch ( IOException e ) {
                    errorListener.notifyError(
                        "Error Processing Resource",
                        "Error processing resource '"+ TextUtils.truncStringMiddleExact(header.getUri(),80)+"':\n" + ExceptionUtils.getMessage( e ) );
                }
            }
            context.setResourceInputSources( inputSources );
        }

        return context;
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger( GlobalResourceImportWizard.class.getName() );

    private final ResourceAdmin resourceAdmin;

    private void init() {
        setTitle( resources.getString( "dialog.title" ));

        getButtonHelp().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Actions.invokeHelp(GlobalResourceImportWizard.this);
            }
        });

        pack();
        Utilities.centerOnParentWindow( this );
    }

    private static DependencyImportChoice confirmResourceImport( final Component parent ) {
        final GlobalResourceImportWizard.DependencyImportChoice importChoice;
        final int choice = JOptionPane.showOptionDialog(
                parent,
                "Do you want to import the schema's dependencies as global resources?",
                "Import Global Resources?",
                JOptionPane.YES_NO_CANCEL_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                new Object[]{"Import","Skip","Cancel"},
                "Import" );
        if ( choice == JOptionPane.YES_OPTION ) {
            importChoice = GlobalResourceImportWizard.DependencyImportChoice.IMPORT;
        } else if ( choice == JOptionPane.NO_OPTION  ) {
            importChoice = GlobalResourceImportWizard.DependencyImportChoice.SKIP;
        } else {
            importChoice = GlobalResourceImportWizard.DependencyImportChoice.CANCEL;
        }
        return importChoice;
    }

    private static DependencyImportChoice confirmResourceSave( final Component parent,
                                                               final boolean warnForDoctype,
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
                    viewResourceHolder( SwingUtilities.getWindowAncestor(parent), resourceHolder, resourceHolderTableModel.getRows() );
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
        String text = "<html>Do you want to import the schema's dependencies as global resources?";
        if ( warnForDoctype && hasDoctype( resourceHolders, true ) ) {
            text += "<br/><br/>Warning! One or more resources use a document type declaration and support is currently<br/>disabled (schema.allowDoctype cluster property)";
        }
        text += "</html>";
        displayPanel.add( new JLabel(text), BorderLayout.NORTH );
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
            return DependencyImportChoice.IMPORT;
        } else if ( "Skip".equals( selectedValue )) {
            return DependencyImportChoice.SKIP;
        } else {
            return DependencyImportChoice.CANCEL;
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
                resourceType = resourceUri.toString().endsWith( ResourceType.DTD.getFilenameSuffix() ) ?
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
                                         final ResourceInputSource resourceInputSource,
                                         final Map<URI,ResourceDocument> resolvedResources,
                                         final ResourceType resourceType,
                                         final DependencyProcessing processDependencies,
                                         final Map<String, ResourceHolder> processedResources) {
        ResourceDocument resourceDocument = null;
        try {
            resourceDocument = resolvedResources.get( resourceInputSource.getUri() );
            if ( resourceDocument == null ) {
                resourceDocument = resourceInputSource.asResourceDocument();
                resolvedResources.put( resourceInputSource.getUri(), resourceDocument );
            }
            processResource( context, resourceDocument, resourceInputSource.getType(), resourceType, null, processDependencies, processedResources );
        } catch ( SAXException e ) {
            handleResourceError( context, resourceInputSource.getUri(), resourceDocument, e, processedResources );
        } catch ( IOException e ) {
            handleResourceError( context, resourceInputSource.getUri(), resourceDocument, e, processedResources );
        }
    }

    private static void processResource( final GlobalResourceImportContext context,
                                         final ResourceDocument resourceDocument,
                                         final ResourceType resourceType,  // resource type if known, else null
                                         final ResourceType resourceTypeMatch, // only process this type of resource , may be null
                                         final String resourceKey, // Public ID for a DTD, Target Namespace for schema, may be null
                                         final DependencyProcessing processDependencies,
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

            if ( resourceTypeMatch!=null && resourceTypeMatch!=contentResourceType ) {
                return;
            }

            Document schemaDoc = null;
            switch ( contentResourceType ) {
                case XML_SCHEMA:
                    processedResources.put( resourceUriStr, context.newSchemaResourceHolder(resourceDocument, resourceKey!=null?resourceKey:XmlUtil.getSchemaTNS(content), null) ); // put early to prevent circular processing
                    schemaDoc = XmlUtil.parse(
                            new InputSource( resourceUriStr ){{setCharacterStream( new StringReader(content) );}},
                            new DefaultHandler2(){
                                @Override
                                public InputSource resolveEntity( final String name, final String publicId, final String baseURI, final String systemId ) throws SAXException, IOException {
                                    final Pair<String,String> resolved =
                                            GlobalResourceImportWizard.resolveEntity( context, baseURI, systemId, publicId, processedResources, dependencies );
                                    final InputSource entitySource = new InputSource( resolved.left );
                                    entitySource.setCharacterStream( new StringReader( resolved.right ) );
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
                    processedResources.put( resourceUriStr, context.newDTDResourceHolder(resourceDocument, resourceKey, null) ); // put early to prevent circular processing
                    if ( processDependencies.isProcess() ) {
                        try {
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
                        } catch ( IOException e ) {
                            handleDependencyProcessingError( processDependencies, e );
                        } catch ( SAXException e ) {
                            handleDependencyProcessingError( processDependencies, e );
                        }
                    }
                    break;
            }

            boolean contentUpdated = false;
            String updatedDoctypeSystemId = null;
            final ResourceHolder resourceHolder = processedResources.get( resourceUriStr );
            for ( final DependencyInfo dependency : dependencies ) {

                final URI depUri = dependency.uri==null ? null : asUri(dependency.uri);
                final boolean absoluteLocation = depUri != null && depUri.isAbsolute();

                final URI absoluteUri;
                if ( absoluteLocation ) {
                    absoluteUri = depUri;
                } else {
                    absoluteUri = dependency.uri==null ? null : resolveUri( resourceDocument.getUri().toString(), dependency.uri );
                }

                if ( dependency.processed ) {
                    resourceHolder.addDependency( dependency.uri, dependency.uri );

                    if ( processDependencies.isProcess() && schemaDoc!=null && schemaDoc.getDoctype()!=null && schemaDoc.getDoctype().getSystemId()!=null &&
                         absoluteUri!=null && dependency.originalReferenceUri!=null && dependency.originalReferenceUri.equals(schemaDoc.getDoctype().getSystemId()) &&
                         !absoluteUri.toString().equals(schemaDoc.getDoctype().getSystemId())) {

                        // Update the reference to point to the resolved document
                        final String dependencyReference = ResourceUtils.relativizeUri( resourceDocument.getUri(), absoluteUri ).toString();
                        if ( !dependencyReference.equals( schemaDoc.getDoctype().getSystemId() ) ) {
                            updatedDoctypeSystemId = dependencyReference;
                        }
                    }

                    continue;
                }

                if ( dependency.hasTargetNamespace ) {
                    final ResourceDocument dependencyDocument =
                            context.getResourceDocumentResolverForType(ResourceType.XML_SCHEMA).resolveByTargetNamespace( absoluteUri==null?null:absoluteUri.toString(), dependency.targetNamespace );

                    if ( dependencyDocument != null && dependencyDocument.exists() ) {
                        resourceHolder.addDependency( dependency.uri, dependencyDocument.getUri().toString() );

                        if ( schemaDoc!=null && dependency.targetNamespace!=null && absoluteUri!=null && !absoluteUri.equals( dependencyDocument.getUri() ) ) {
                            // Update the import to point to the resolved document
                            if ( updateReference( resourceDocument.getUri(), schemaDoc, true, dependency.targetNamespace, dependency.uri, dependencyDocument.getUri() ) ) {
                                contentUpdated = true;
                            }
                        }

                        // Recursively resolve the schemas dependencies.
                        processResource( context, dependencyDocument, ResourceType.XML_SCHEMA, null, dependency.targetNamespace, DependencyProcessing.ON, processedResources );
                        continue;
                    }
                } else if ( dependency.uri != null && !dependency.uri.isEmpty() ) {
                    final ResourceDocument dependencyDocument = absoluteLocation ?
                            context.newResourceInputSource( asUri(dependency.uri), dependency.resourceType ).asResourceDocument():
                            resourceDocument.relative( dependency.uri, context.getResourceDocumentResolverForType(dependency.resourceType) );

                    if ( dependencyDocument.exists() ) {
                        resourceHolder.addDependency( dependency.uri, dependencyDocument.getUri().toString() );

                        if ( schemaDoc!=null &&  absoluteUri!=null && !absoluteUri.equals( dependencyDocument.getUri() ) ) {
                            // Update the reference to point to the resolved document
                            if ( updateReference( resourceDocument.getUri(), schemaDoc, false, null, dependency.uri, dependencyDocument.getUri() ) ) {
                                contentUpdated = true;
                            }
                        }

                        // Recursively resolve the dependencies.
                        processResource( context, dependencyDocument, ResourceType.XML_SCHEMA, null, null, DependencyProcessing.ON, processedResources );
                        continue;
                    }
                }

                throw new IOException( "Resource not found : " + dependency );
            }

            if ( contentUpdated ) {
                resourceHolder.setContent( XmlUtil.nodeToString( schemaDoc, true ) );
            }
            if ( updatedDoctypeSystemId != null ) {
                resourceHolder.setContent( DtdUtils.updateExternalSystemId( resourceHolder.getContent(), updatedDoctypeSystemId ) );
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
                throw new IOException( ExceptionUtils.getMessage( e ), e );
            }
            throw ExceptionUtils.wrap(e);
        }

    }

    private static <E extends Throwable> void handleDependencyProcessingError( final DependencyProcessing processDependencies, final E e ) throws E {
        if ( processDependencies.isSuppressError() ) {
            logger.log( Level.FINE, "Suppressing dependency processing error : " + ExceptionUtils.getMessage( e ), ExceptionUtils.getDebugException(e) );
        } else {
            throw e;
        }
    }

    private static boolean updateReference( final URI schemaUri,
                                            final Document schemaXml,
                                            final boolean hasTargetNamespace,
                                            final String targetNamespace,
                                            final String currentReference,
                                            final URI dependencyAbsoluteUri ) {
        final boolean updated[] = { false };

        final String dependencyReference = ResourceUtils.relativizeUri( schemaUri, dependencyAbsoluteUri ).toString();
        final DocumentReferenceProcessor schemaReferenceProcessor = DocumentReferenceProcessor.schemaProcessor();
        schemaReferenceProcessor.processDocumentReferences( schemaXml, new DocumentReferenceProcessor.ReferenceCustomizer(){
            @SuppressWarnings({ "ConstantConditions" })
            @Override
            public String customize( final Document document,
                                     final Node node,
                                     final String documentUrl,
                                     final DocumentReferenceProcessor.ReferenceInfo referenceInfo ) {
                String updatedLocation = null;
                assert node instanceof Element;
                if ( node instanceof Element ) {
                    final DependencyInfo info = DependencyInfo.fromSchemaDependencyElement( schemaUri.toString(), (Element) node );

                    if ( info.hasTargetNamespace==hasTargetNamespace &&
                         (!hasTargetNamespace || targetNamespace.equals( info.targetNamespace )) &&
                         currentReference.equals( info.uri )  ) {
                        updatedLocation = dependencyReference;
                        updated[0] = true;
                    }
                }
                return updatedLocation;
            }
        } );


        return updated[0];
    }

    private static Pair<String,String> resolveEntity( final GlobalResourceImportContext context,
                                                      final String baseURI,
                                                      final String systemId,
                                                      final String publicId,
                                                      final Map<String, ResourceHolder> processedResources,
                                                      final java.util.List<DependencyInfo> dependencies ) throws IOException, SAXException {
        Pair<String,String> entity = null;

        // resolve uri
        final URI absoluteSystemId = resolveUri( baseURI, systemId );

        // check for existing resource
        final ResourceHolder holder = processedResources.get( absoluteSystemId.toString() );
        if ( holder != null && !holder.isError() ) {
            if ( dependencies != null ) {
                dependencies.add( DependencyInfo.fromResourceDocument( holder.getType(), holder.asResourceDocument(), null ) );
            }
            entity = new Pair<String,String>( absoluteSystemId.toString(), holder.getContent());
        }

        // resolve
        if ( entity == null ) {
            final ResourceDocument dtdResourceDocument = resolveDtdResourceDocument( context, publicId, absoluteSystemId );

            if ( dtdResourceDocument != null ) {
                entity = new Pair<String,String>( dtdResourceDocument.getUri().toString(), dtdResourceDocument.getContent());
                if ( dependencies != null ) {
                    dependencies.add( DependencyInfo.fromResourceDocument( ResourceType.DTD, dtdResourceDocument, systemId ) );
                }

                processResource( context, dtdResourceDocument, ResourceType.DTD, null, publicId, DependencyProcessing.OFF, processedResources );
            } else {
                throw new IOException("Resource not found for resource "+describe( baseURI, systemId, publicId, false, null ));
            }
        }

        return entity;
    }

    private static ResourceDocument resolveDtdResourceDocument( final GlobalResourceImportContext context,
                                                                final String publicId,
                                                                final URI absoluteSystemId ) throws IOException {
        final ResourceDocument dtdResourceDocument;
        
        if ( publicId != null ) {
            dtdResourceDocument = context.getResourceDocumentResolverForType(ResourceType.DTD).resolveByPublicId( absoluteSystemId.toString(), publicId );
        } else {
            dtdResourceDocument = context.newResourceInputSource( absoluteSystemId, ResourceType.DTD ).asResourceDocument();
        }

        return dtdResourceDocument;
    }

    private static URI resolveUri( final String baseUri, final String uri ) throws IOException {
        final URI absoluteUri;

        if ( baseUri != null ) {
            try {
                absoluteUri = uri.isEmpty() ? new URI(baseUri) : new URI(baseUri).resolve( uri );
            } catch ( URISyntaxException e ) {
                throw new IOException( "Error resolving uri '"+uri+"' against '"+baseUri+"'" );
            } catch ( IllegalArgumentException e ) {
                throw new IOException( "Error resolving uri '"+uri+"' against '"+baseUri+"'" );
            }
        } else {
            absoluteUri = absoluteUri(uri);
        }

        return absoluteUri;
    }

    private static ResourceType getResourceType( final GlobalResourceImportContext context,
                                                 final URI location,
                                                 final String content,
                                                 final ResourceType resourceType,
                                                 final Map<String, ResourceHolder> processedResources,
                                                 final java.util.List<DependencyInfo> dependencies  ) throws IOException {
        ResourceType processedResourceType = resourceType;

        if ( processedResourceType == null ) {
            final String locationPath = location.getPath();
            if ( locationPath != null ) {
                for ( final ResourceType type : ResourceType.values() ) {
                    if ( locationPath.endsWith( "." + type.getFilenameSuffix() )) {
                        processedResourceType = type;
                        break;
                    }
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

    private static ResourceTherapist buildManualResourceTherapist( final Window parent,
                                                                   final GlobalResourceImportContext context,
                                                                   final ResourceAdmin resourceAdmin ) {
        return new ResourceTherapist(){
            @Override
            public ResourceDocument consult( final ResourceType resourceType,
                                             final String resourceDescription,
                                             final ResourceDocument invalidResource,
                                             final String invalidDetail ) {
                ResourceDocument resourceDocument = null;

                while ( resourceDocument == null ) {
                    final StringBuilder builder = new StringBuilder();
                    builder.append( "Manually import " );
                    if ( invalidResource == null ) {
                        builder.append( "missing " );
                    } else {
                        builder.append( "invalid " );   
                    }
                    builder.append( "resource:\n\n" );
                    builder.append( resourceDescription );
                    builder.append( "\n\n" );

                    if ( invalidDetail != null ) {
                        builder.append( "error is:\n\n" );
                        builder.append( invalidDetail );
                        builder.append( "\n\n" );
                    }

                    final int choice = JOptionPane.showOptionDialog(
                            parent,
                            Utilities.getTextDisplayComponent(builder.toString(), 600, 100, -1, -1),
                            "Manual Resource Import",
                            JOptionPane.YES_NO_OPTION,
                            JOptionPane.QUESTION_MESSAGE,
                            null,
                            new Object[]{"Import","Cancel"},
                            "Cancel" );

                    if ( choice == JOptionPane.YES_OPTION ) {
                        String uri = null;
                        String content = null;
                        if ( invalidResource != null ) {
                            uri = invalidResource.getUri().toString();
                            if ( invalidResource.available() ) {
                                try {
                                    content = invalidResource.getContent();
                                } catch ( IOException e ) {
                                    // this is not expected since we check if content is available
                                    logger.log( Level.WARNING, "Error getting resource content for manual edit", e );
                                }
                            }
                        }

                        final Pair<String,String> resourceUriAndContent =
                                editResource( parent,
                                              resourceType!=null ? resourceType : ResourceType.DTD,
                                              uri,
                                              content,
                                              new ResourceHolderEntityResolver( context.getCurrentResourceHolders(), new ResourceAdminEntityResolver(resourceAdmin,true), null),
                                              !resourceAdmin.allowSchemaDoctype() );
                        if ( resourceUriAndContent == null ) {
                            break; // manual import cancelled
                        }

                        try {
                            resourceDocument = context.newResourceDocument( resourceUriAndContent.left, resourceUriAndContent.right );
                        } catch ( IOException e ) {
                                showErrorMessage( parent,
                                        "Resource Import Error",
                                        "Unable to import resource:\n " + TextUtils.truncStringMiddleExact( resourceUriAndContent.left, 80 ) + "\ndue to:\n" + ExceptionUtils.getMessage(e));
                        }
                    } else if ( choice == JOptionPane.NO_OPTION || choice == JOptionPane.CLOSED_OPTION) {
                        break; // manual import cancelled
                    }
                }

                return resourceDocument;
            }
        };
    }

    private static Functions.UnaryThrows<ResourceEntryHeader,Collection<ResourceEntryHeader>,IOException> buildEntitySelector(
            final Window parent,
            final ChoiceSelector choiceSelector ) {
        return new Functions.UnaryThrows<ResourceEntryHeader,Collection<ResourceEntryHeader>,IOException>(){
            @Override
            public ResourceEntryHeader call( final Collection<ResourceEntryHeader> resourceEntryHeaders ) throws IOException {
                ResourceEntryHeader selected = null;

                // determine the type of resource ambiguity
                ResourceType type = null;
                String detail = null;
                if ( !resourceEntryHeaders.isEmpty() ) {
                    final ResourceEntryHeader sampleHeader = resourceEntryHeaders.iterator().next();
                    type = sampleHeader.getResourceType();
                    detail = sampleHeader.getResourceKey1();
                }

                ImportChoice choice = null;
                if ( type != null ) {
                    final ImportOption option = type==ResourceType.DTD ? ImportOption.AMBIGUOUS_PUBLIC_ID : ImportOption.AMBIGUOUS_TARGET_NAMESPACE;
                    choice = choiceSelector.selectChoice( option, null, ImportChoice.EXISTING, detail, null, null );
                }

                if ( choice != null ) {
                    switch ( choice ) {
                        case EXISTING:
                            final ResourceEntryHeaderSelectionDialog dialog = new ResourceEntryHeaderSelectionDialog( parent, type, detail, resourceEntryHeaders );
                            dialog.setVisible( true );
                            if  ( dialog.wasOk() ) {
                                selected = dialog.getSelection();
                            } else {
                                throw new IOException("Ambiguous resource skipped (resource selection cancelled).");
                            }
                            break;
                        case SKIP:
                            throw new IOException("Ambiguous resource skipped.");
                    }
                }

                return selected;
            }
        };
    }

    private static ChoiceSelector buildChoiceSelector( final Window parent,
                                                       final GlobalResourceImportContext context ) {
        return new ChoiceSelector(){
            @Override
            public ImportChoice selectChoice( final ImportOption importOption,
                                              final String optionDetail,
                                              final ImportChoice importChoice,
                                              final String choiceResourceDetail,
                                              final String uri,
                                              final String description ) {
                ImportChoice result = context.getImportOptions().get( importOption );
                if ( result == ImportChoice.NONE ) {
                    final StringBuilder messageBuilder = new StringBuilder();
                    messageBuilder.append( "<html>" );
                    switch ( importOption ) {
                        case AMBIGUOUS_TARGET_NAMESPACE:
                            messageBuilder.append( "The target namespace of a dependency matches multiple existing XML Schemas:" );
                            break;
                        case AMBIGUOUS_PUBLIC_ID:
                            messageBuilder.append( "The public identifier of a dependency matches multiple existing resources:" );
                            break;
                        case CONFLICTING_URI:
                            messageBuilder.append( "The System ID of an imported resource conflicts with an existing resource:" );
                            break;
                        case CONFLICTING_TARGET_NAMESPACE:
                            messageBuilder.append( "The target namespace of an imported resource matches an existing XML Schema:" );
                            break;
                        case CONFLICTING_PUBLIC_ID:
                            messageBuilder.append( "The public identifier of an imported resource is a duplicate of an existing resource:" );
                            break;
                        case MISSING_RESOURCE:
                            messageBuilder.append( "A dependency of an imported resource is " ).append( optionDetail ).append( ':' );
                            break;
                    }

                    messageBuilder.append( "<br/><br/>" );

                    if ( choiceResourceDetail != null ) {
                        messageBuilder.append( TextUtils.escapeHtmlSpecialCharacters( choiceResourceDetail ).replace( "\n", "<br/>" ));
                        messageBuilder.append( "<br/><br/>" );
                    }

                    if ( uri != null ) {
                        messageBuilder.append( "URI: " );
                        messageBuilder.append( TextUtils.escapeHtmlSpecialCharacters( TextUtils.truncStringMiddleExact( uri, 80 ) ) );
                        messageBuilder.append( "<br/><br/>" );
                    }

                    if ( description != null ) {
                        messageBuilder.append( "<i>" );
                        messageBuilder.append( TextUtils.breakOnMultipleLines( TextUtils.escapeHtmlSpecialCharacters(description), 80, "<br/>" ) );
                        messageBuilder.append( "</i><br/><br/>" );
                    }
                    
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

        throw new IOException(throwable);
    }

    @SuppressWarnings({"unchecked"})
    private static <T extends Throwable> void throwAsType( final Throwable throwable ) throws T {
        throw (T) throwable;
    }

    private enum DependencyProcessing {
        ON(true,false),  // process dependencies
        OFF(false,false), // don't process dependencies
        AUTO(true,true); // process dependencies if possible, ignore dependencies on error

        private final boolean process;
        private final boolean suppressErrors;

        private DependencyProcessing( final boolean process,
                                      final boolean suppressErrors ) {
            this.process = process;
            this.suppressErrors = suppressErrors;
        }

        public boolean isProcess() {
            return process;
        }

        public boolean isSuppressError() {
            return suppressErrors;
        }
    }

    private static final class DependencyInfo {
        private final ResourceType resourceType;
        private final String baseUri;
        private final String uri;
        private final boolean hasTargetNamespace;
        private final String targetNamespace;
        private final boolean processed;
        private final String originalReferenceUri; // the original URI which can be different from URI when processed

        private DependencyInfo( final ResourceType resourceType,
                                final String baseUri,
                                final String uri,
                                final boolean hasTargetNamespace,
                                final String targetNamespace,
                                final boolean processed,
                                final String originalReferenceUri ) {
            this.resourceType = resourceType;
            this.baseUri = baseUri;
            this.uri = uri;
            this.hasTargetNamespace = hasTargetNamespace;
            this.targetNamespace = targetNamespace;
            this.processed = processed;
            this.originalReferenceUri = originalReferenceUri;
        }

        private static DependencyInfo fromResourceDocument( final ResourceType type,
                                                            final ResourceDocument resourceDocument,
                                                            final String originalReferenceUri ) {
            return new DependencyInfo( type, null, resourceDocument.getUri().toString(), false, null, true, originalReferenceUri );
        }

        private static DependencyInfo fromSchemaDependencyElement( final String baseUri,
                                                                   final Element dependencyEl ) {
            final String dependencyLocation = dependencyEl.hasAttributeNS( null, "schemaLocation" ) ? dependencyEl.getAttributeNS( null, "schemaLocation" ) : null;
            final String dependencyNamespace = dependencyEl.hasAttributeNS( null, "namespace" ) ? dependencyEl.getAttributeNS(null, "namespace") : null;
            final boolean dependencyNamespaceSet = "import".equals( dependencyEl.getLocalName() );

            return new DependencyInfo( ResourceType.XML_SCHEMA, baseUri, dependencyLocation, dependencyNamespaceSet, dependencyNamespace, false, null );
        }

        public String toString() {
            return describe( baseUri, uri, null, hasTargetNamespace, targetNamespace );
        }
    }
}
