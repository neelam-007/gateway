package com.l7tech.console.panels;

import com.l7tech.common.io.IOExceptionThrowingReader;
import com.l7tech.common.io.ResourceDocument;
import com.l7tech.common.io.URIResourceDocument;
import com.l7tech.console.action.Actions;
import com.l7tech.gateway.common.resources.ResourceAdmin;
import com.l7tech.gateway.common.resources.ResourceEntry;
import com.l7tech.gateway.common.resources.ResourceEntryHeader;
import com.l7tech.gateway.common.resources.ResourceType;
import com.l7tech.gui.SimpleTableModel;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.RunOnChangeListener;
import com.l7tech.gui.util.TableUtil;
import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.widgets.TextListCellRenderer;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.util.*;
import org.w3c.dom.ls.LSInput;
import org.w3c.dom.ls.LSResourceResolver;
import org.xml.sax.SAXException;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.SchemaFactory;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.l7tech.console.panels.GlobalResourceImportContext.*;
import static com.l7tech.console.panels.GlobalResourceImportWizard.DependencySummaryListCellRenderer;
import static com.l7tech.console.panels.GlobalResourceImportWizard.describe;

/**
 * Global resource analysis dialog.
 */
public class GlobalResourcesAnalyzeDialog extends JDialog {

    private static final Logger logger = Logger.getLogger( GlobalResourcesAnalyzeDialog.class.getName() );

    private static final ResourceBundle resources = ResourceBundle.getBundle( GlobalResourcesAnalyzeDialog.class.getName() );
    private static final ResourceBundle resourcesWizard = ResourceBundle.getBundle( GlobalResourceImportWizard.class.getName() );

    private JPanel mainPanel;
    private JButton closeButton;
    private JButton helpButton;
    private JButton viewButton;
    private JButton validateButton;
    private JButton resetButton;
    private JTable resourcesTable;
    private JLabel analyzedResourceCountLabel;
    private JLabel validationFailureCountLabel;
    private JTextField systemIdTextField;
    private JTextField descriptionTextField;
    private JTextField statusTextField;
    private JTextPane statusDetailTextPane;
    private JComboBox dependenciesComboBox;
    private JList dependenciesList;
    private JList dependantsList;

    private final ResourceAdmin resourceAdmin;
    private final PermissionFlags flags;
    private final java.util.List<ResourceEntryHeader> resourceHeaders;
    private SimpleTableModel<ResourceHolder> resourceHolderTableModel;
    private Collection<ResourceHolder> defaultResources = Collections.emptyList();

    public GlobalResourcesAnalyzeDialog( final Window parent,
                                         final ResourceAdmin resourceAdmin,
                                         final Collection<ResourceEntryHeader> resourceHeaders ) {
        super( parent, JDialog.DEFAULT_MODALITY_TYPE );
        this.resourceAdmin = resourceAdmin;
        this.resourceHeaders = new ArrayList<ResourceEntryHeader>( resourceHeaders );
        this.flags = PermissionFlags.get( EntityType.RESOURCE_ENTRY );
        init();
        initAnalyze( resourceHeaders );
        initDefaults();
        updateSummaries(false);
    }

    private void init() {
        setTitle( resources.getString("title.analyze") );
        add( mainPanel );

        resourceHolderTableModel = TableUtil.configureTable(
                resourcesTable,
                TableUtil.column(resourcesWizard.getString("column.system-id"), 40, 240, 100000, Functions.<String, ResourceHolder>propertyTransform( ResourceHolder.class, "systemId"), String.class),
                TableUtil.column(resourcesWizard.getString("column.details"), 40, 120, 100000, Functions.<String, ResourceHolder>propertyTransform( ResourceHolder.class, "details"), String.class),
                TableUtil.column(resourcesWizard.getString("column.type"), 40, 80, 120, Functions.<ResourceType, ResourceHolder>propertyTransform( ResourceHolder.class, "type"), ResourceType.class),
                TableUtil.column(resourcesWizard.getString("column.status"), 40, 50, 120, Functions.<String, ResourceHolder>propertyTransform( ResourceHolder.class, "status"), String.class)
        );
        resourcesTable.setModel( resourceHolderTableModel );
        resourcesTable.getTableHeader().setReorderingAllowed( false );
        resourcesTable.setDefaultRenderer( ResourceType.class, GlobalResourcesDialog.buildResourceTypeRenderer().asTableCellRenderer() );
        resourcesTable.setSelectionMode( ListSelectionModel.MULTIPLE_INTERVAL_SELECTION );
        resourcesTable.getSelectionModel().addListSelectionListener( new RunOnChangeListener(){
            @Override
            protected void run() {
                enableAndDisableComponents();
                showDetails();
            }
        } );
        Utilities.setRowSorter( resourcesTable, resourceHolderTableModel, new int[]{0}, new boolean[]{true}, new Comparator[]{String.CASE_INSENSITIVE_ORDER} );
        resourceHolderTableModel.addTableModelListener( new TableModelListener(){
            @Override
            public void tableChanged( final TableModelEvent e ) {
                enableAndDisableComponents();
            }
        } );

        dependenciesComboBox.setModel( new DefaultComboBoxModel( DependencyScope.values() ) );
        dependenciesComboBox.setSelectedItem( DependencyScope.DIRECT );
        dependenciesComboBox.setRenderer( new TextListCellRenderer<DependencyScope>( new Functions.Unary<String,DependencyScope>(){
            @Override
            public String call( final DependencyScope dependencyScope ) {
                return resourcesWizard.getString( "dependencies." + dependencyScope.name() );
            }
        } ) );
        dependenciesComboBox.addActionListener( new ActionListener(){
            @Override
            public void actionPerformed( final ActionEvent e ) {
                showDetails();
            }
        } );
        final ActionListener dependencySummaryNavigationListener = new ActionListener() {
            @Override
            public void actionPerformed( final ActionEvent e ) {
                navigateDependencySummary( (JList) e.getSource() );
            }
        };
        dependenciesList.setCellRenderer( new DependencySummaryListCellRenderer() );
        Utilities.setDoubleClickAction( dependenciesList, dependencySummaryNavigationListener, dependenciesList, "click");
        dependantsList.setCellRenderer( new DependencySummaryListCellRenderer() );
        Utilities.setDoubleClickAction( dependantsList, dependencySummaryNavigationListener, dependantsList, "click");

        viewButton.addActionListener( new ActionListener(){
            @Override
            public void actionPerformed( final ActionEvent e ) {
                doView();
            }
        } );
        validateButton.addActionListener( new ActionListener(){
            @Override
            public void actionPerformed( final ActionEvent e ) {
                doValidate();
            }
        } );
        resetButton.addActionListener( new ActionListener(){
            @Override
            public void actionPerformed( final ActionEvent e ) {
                doReset();
            }
        } );
        helpButton.addActionListener( new ActionListener(){
            @Override
            public void actionPerformed( final ActionEvent e ) {
                Actions.invokeHelp( GlobalResourcesAnalyzeDialog.this );
            }
        } );
        closeButton.addActionListener( new ActionListener(){
            @Override
            public void actionPerformed( final ActionEvent e ) {
                dispose();
            }
        } );

        pack();
        setMinimumSize( getContentPane().getMinimumSize() );
        Utilities.setEscKeyStrokeDisposes( this );
        Utilities.centerOnParentWindow( this );
        Utilities.setDoubleClickAction( resourcesTable, viewButton );
        Utilities.setButtonAccelerator( this, helpButton, KeyEvent.VK_F1 );
        enableAndDisableComponents();
    }

    private void initAnalyze( final Collection<ResourceEntryHeader> resourceHeaders ) {
        final Collection<ResourceHolder> resourceHolders = GlobalResourceImportWizard.resolveDependencies(
                new HashSet<String>(Functions.map( resourceHeaders, Functions.<String,ResourceEntryHeader>propertyTransform( ResourceEntryHeader.class, "uri" ))),
                resourceAdmin,
                GlobalResourceImportWizard.getUIErrorListener( this ));

        final int[] selectedRows = resourcesTable.getSelectedRows();
        final Set<String> selectedSystemIds = new HashSet<String>();
        for ( final int selectedRow : selectedRows ) {
            final int modelRow = resourcesTable.convertRowIndexToModel( selectedRow );
            selectedSystemIds.add( resourceHolderTableModel.getRowObject( modelRow ).getSystemId() );
        }

        resourceHolderTableModel.setRows( new ArrayList<ResourceHolder>(resourceHolders) );

        // restore users selections
        for ( final String selectedSystemId : selectedSystemIds ) {
            for ( final ResourceHolder holder : resourceHolders  ) {
                if ( selectedSystemId.equals( holder.getSystemId() )) {
                    final int tableRow = resourcesTable.convertRowIndexToView( resourceHolderTableModel.getRowIndex( holder ) );
                    resourcesTable.getSelectionModel().addSelectionInterval( tableRow, tableRow );
                    break;
                }
            }
        }
    }

    private void initDefaults() {
        try {
            final Collection<ResourceEntryHeader> headers = resourceAdmin.findDefaultResources();
            if ( headers != null ) {
                final Collection<ResourceHolder> defaults = new ArrayList<ResourceHolder>();
                for ( final ResourceEntryHeader resourceEntryHeader : headers ) {
                    try {
                        final ResourceEntry resourceEntry = resourceAdmin.findDefaultResourceByUri( resourceEntryHeader.getUri() );
                        if ( resourceEntry == null ) {
                            throw new IOException("Default resource not found for URI '"+resourceEntryHeader.getUri() +"'");
                        }
                        final ResourceDocument resourceDocument = GlobalResourceImportContext.newResourceDocument( resourceEntryHeader, resourceEntry );
                        defaults.add( GlobalResourceImportContext.newResourceHolder( resourceDocument, resourceEntry.getType() ) );
                    } catch ( IOException e ) {
                        logger.log(
                                Level.WARNING,
                                "Error processing default resource '"+resourceEntryHeader.getUri()+"': " + ExceptionUtils.getMessage(e),
                                ExceptionUtils.getDebugException(e) );
                    }
                }

                defaultResources = Collections.unmodifiableCollection( defaults );
            }
        } catch ( FindException e ) {
            logger.log(
                    Level.WARNING,
                    "Unable to load default resources '"+ExceptionUtils.getMessage(e)+"'",
                    ExceptionUtils.getDebugException(e) );
        }
    }

    private void enableAndDisableComponents() {
        viewButton.setEnabled( resourcesTable.getSelectedRowCount()==1 );
        final Pair<Boolean,Boolean> defaultUriAndContent = isDefaultUriAndContent( getSelectedResourceHolder() );
        resetButton.setEnabled( flags.canUpdateAll() && (!defaultUriAndContent.left || !defaultUriAndContent.right) );
    }

    private Pair<Boolean,Boolean> isDefaultUriAndContent( final ResourceHolder resourceHolder ) {
        boolean defaultUri = true;
        boolean defaultContent = true;

        if ( resourceHolder != null ) {
            final ResourceHolder defaultResource = resolve(
                       defaultResources,
                       resourceHolder.getType(),
                       null,
                       resourceHolder.getSystemId(),
                       resourceHolder.getPublicId(),
                       resourceHolder.getTargetNamespace() );
            if ( defaultResource != null ) {
                defaultUri = defaultResource.getSystemId().equals( resourceHolder.getSystemId() );
                
                try {
                    defaultContent = defaultResource.getContent().equals( resourceHolder.getContent() );
                } catch ( IOException e ) {
                    logger.log(
                            Level.WARNING,
                            "Error checking resource against defaults '"+resourceHolder.getSystemId()+"': " + ExceptionUtils.getMessage(e),
                            ExceptionUtils.getDebugException(e) );
                }
            }
        }

        return new Pair<Boolean,Boolean>( defaultUri, defaultContent );
    }

    private ResourceHolder getSelectedResourceHolder() {
        ResourceHolder resourceHolder = null;

        final int[] selectedRows = resourcesTable.getSelectedRows();
        if ( selectedRows.length == 1 ) {
            resourceHolder = resourceHolderTableModel.getRowObject(
                    resourcesTable.convertRowIndexToModel( selectedRows[0] ) );
        }

        return resourceHolder;
    }

    private void doView() {
        final ResourceHolder resourceHolder = getSelectedResourceHolder();
        if ( resourceHolder != null ) {
            GlobalResourceImportWizard.viewResourceHolder( this, resourceHolder, resourceHolderTableModel.getRows() );
        }
    }

    private void doValidate() {
        final Collection<ResourceHolder> resources = resourceHolderTableModel.getRows();
        final Set<String> validatedResourceUris = new HashSet<String>();
        for ( final ResourceHolder resourceHolder : resources ) {
            if ( !validatedResourceUris.contains( resourceHolder.getSystemId() ) &&
                 !resourceHolder.isError() &&
                 ResourceType.DTD!=resourceHolder.getType() ) {
                try {
                    validateResource( resourceHolder, resources, validatedResourceUris );
                } catch ( SAXException e ) {
                    resourceHolder.setError( e );
                } catch ( IOException e ) {
                    resourceHolder.setError( e );
                }
            }
        }
        final int[] selections = resourcesTable.getSelectedRows();
        resourceHolderTableModel.fireTableDataChanged();
        for ( final int selection : selections ) resourcesTable.getSelectionModel().addSelectionInterval( selection, selection );
        updateSummaries( true );

        if ( !resourceAdmin.allowSchemaDoctype() && GlobalResourceImportWizard.hasDoctype( resources, false ) ) {
            JOptionPane.showMessageDialog(
                    this,
                    "One or more resources use a document type declaration and support is currently\ndisabled (schema.allowDoctype cluster property)",
                    "Schema Warning",
                    JOptionPane.WARNING_MESSAGE );
        } else {
            final int errors = countErrors( resources );
            if ( errors == 0 ) {
                JOptionPane.showMessageDialog(
                        this,
                        "Validation completed successfully.",
                        "Validation Successful",
                        JOptionPane.INFORMATION_MESSAGE );
            } else {
                JOptionPane.showMessageDialog(
                        this,
                        "Validation completed with " + errors + " error(s).\nSelect each 'Failed' resource to view error details.",
                        "Validation Failed",
                        JOptionPane.WARNING_MESSAGE );
            }
        }
    }

    private void validateResource( final ResourceHolder resource,
                                   final Collection<ResourceHolder> resources,
                                   final Set<String> validatedResourceUris ) throws IOException, SAXException {
        validatedResourceUris.add( resource.getSystemId() );

        final Set<String> resolvedResourceUris = new HashSet<String>();
        SchemaFactory factory = SchemaFactory.newInstance( XMLConstants.W3C_XML_SCHEMA_NS_URI );
        factory.setResourceResolver( new LSResourceResolver() {
            @SuppressWarnings({ "ThrowableInstanceNeverThrown" })
            @Override
            public LSInput resolveResource( final String type,
                                            final String namespaceURI,
                                            final String publicId,
                                            final String systemId,
                                            final String baseURI ) {
                LSInput input = null;

                final ResourceHolder resourceHolder;
                final boolean hasTargetNamespace;
                if ( XMLConstants.XML_DTD_NS_URI.equals( type )) {
                    resourceHolder = resolve( resources, ResourceType.DTD, baseURI, systemId, publicId, null );
                    hasTargetNamespace = false;
                } else {
                    resourceHolder = resolve( resources, ResourceType.XML_SCHEMA, baseURI, systemId, publicId, namespaceURI );
                    hasTargetNamespace = true;
                }

                if ( resourceHolder != null ) {
                    input = new LSInputImpl();
                    input.setSystemId( resourceHolder.getSystemId() );
                    try {
                        input.setCharacterStream( new StringReader( resourceHolder.getContent() ) );
                    } catch ( IOException e ) {
                        input.setCharacterStream( new IOExceptionThrowingReader( e ) );
                    }
                }

                if ( input == null ) {
                    final String resourceDescription = describe( baseURI, systemId, publicId, hasTargetNamespace, namespaceURI );
                    input = new LSInputImpl();
                    input.setSystemId( systemId );
                    input.setCharacterStream( new IOExceptionThrowingReader( new IOException("Resource not found: " + resourceDescription), false) );
                }

                resolvedResourceUris.add( input.getSystemId() );

                return input;
            }
        });

        final StreamSource source = new StreamSource();
        source.setSystemId( resource.getSystemId() );
        source.setReader( new StringReader(resource.getContent()) );
        factory.newSchema( source );
        validatedResourceUris.addAll( resolvedResourceUris );
    }

    private ResourceHolder resolve( final Collection<ResourceHolder> resources,
                                    final ResourceType resourceType,
                                    final String baseUri,
                                    final String uri,
                                    final String publicId,
                                    final String namespace ) {
        ResourceHolder resource = null;

        // check for exact URI match
        if ( uri != null ) {
            for ( final ResourceHolder resourceHolder : resources ) {
                if ( resourceHolder.getType()==resourceType && resourceHolder.getSystemId().equals( uri ) ) {
                    resource = resourceHolder;
                    break;
                }
            }
        }

        // check for resolved URI match
        if ( resource == null && baseUri != null && uri != null ) {
            try {
                final String resolvedUri = new URI(baseUri).resolve( uri ).toString();
                for ( final ResourceHolder resourceHolder : resources ) {
                    if ( resourceHolder.getType()==resourceType && resourceHolder.getSystemId().equals( resolvedUri ) ) {
                        resource = resourceHolder;
                        break;
                    }
                }
            } catch ( URISyntaxException e ) {
                logger.warning( "Unable to resolve URI '"+uri+"' against '"+baseUri+"': " + ExceptionUtils.getMessage(e) );
            } catch ( IllegalArgumentException e ) {
                logger.warning( "Unable to resolve URI '"+uri+"' against '"+baseUri+"': " + ExceptionUtils.getMessage(e) );  
            }
        }

        // check for public identifier match
        if ( resource == null && publicId != null && resourceType == ResourceType.DTD ) {
            for ( final ResourceHolder resourceHolder : resources ) {
                if ( resourceHolder.getType()==ResourceType.DTD && publicId.equalsIgnoreCase( resourceHolder.getPublicId() ) ) {
                    resource = resourceHolder;
                    break;
                }
            }
        }

        // check for target namespace match
        if ( resource == null && resourceType == ResourceType.XML_SCHEMA ) {
            for ( final ResourceHolder resourceHolder : resources ) {
                if ( resourceHolder.getType()==ResourceType.XML_SCHEMA &&
                     ((namespace==null && resourceHolder.getTargetNamespace()==null ) ||
                      (namespace!=null && namespace.equals( resourceHolder.getTargetNamespace() ) ) ) ) {
                    resource = resourceHolder;
                    break;
                }
            }
        }

        return resource;
    }

    private void doReset() {
        final ResourceHolder resourceHolder = getSelectedResourceHolder();
        if ( resourceHolder != null ) {
            final ResourceHolder defaultResource = resolve(
                       defaultResources,
                       resourceHolder.getType(),
                       null,
                       resourceHolder.getSystemId(),
                       resourceHolder.getPublicId(),
                       resourceHolder.getTargetNamespace() );
            if ( defaultResource != null ) {
                final Pair<Boolean,Boolean> defaultUriAndContent = isDefaultUriAndContent( resourceHolder );
                String originalUri = null;
                String originalContent = null;
                boolean updateUri = false;
                boolean updateContent = false;

                final String displayUri = TextUtils.truncStringMiddleExact( resourceHolder.getSystemId(), 64 );
                if ( !defaultUriAndContent.left && !defaultUriAndContent.right ) {
                    final String message = "Reset resource to use the default system identifier and content?\n\n" + displayUri;
                    Pair<Boolean,Boolean> resetOptions = confirmReset( message, true );
                    updateUri = resetOptions.left;
                    updateContent = resetOptions.right;
                } else if ( !defaultUriAndContent.left ) {
                    final String message = "Reset resource to use the default system identifier?\n\n" + displayUri;
                    if ( confirmReset( message, false ).left ) {
                        updateUri = true;
                    }
                } else if ( !defaultUriAndContent.right ) {
                    final String message = "Reset resource to use the default content?\n\n" + displayUri;
                    if ( confirmReset( message, false ).left ) {
                        updateContent = true;
                    }
                }

                try {
                    if ( updateContent ) {
                        originalContent = resourceHolder.getContent();
                        resourceHolder.updateContentFrom( defaultResource.asResourceDocument() );
                    } else { // dummy update so the resource is persisted on save
                        resourceHolder.updateContentFrom( resourceHolder.asResourceDocument() );
                    }
                    if ( updateUri ) {
                        originalUri = resourceHolder.getSystemId();
                        resourceHolder.setSystemId( new URI( defaultResource.getSystemId() ) );
                    }
                    if ( updateUri || updateContent ) {
                        resourceHolder.setError( null ); // clear any error to allow save
                        GlobalResourceImportWizard.saveResources( resourceAdmin, Collections.singleton( resourceHolder ) );
                    }
                    if ( updateUri && originalUri != null) { // if update succeeds, we may need to update the initial resource headers
                        updateResourceHeaders( resourceHeaders, originalUri, resourceHolder.getSystemId() );
                    }
                } catch ( SaveException e ) {
                    resetResource( resourceHolder, originalUri, originalContent );
                    logger.log(
                            Level.WARNING,
                            "Error resetting resource '"+resourceHolder.getSystemId()+": " + ExceptionUtils.getMessage(e),
                            ExceptionUtils.getDebugException(e));
                    showErrorMessage( "Resource Reset Error", "Unable to reset resource to default value:\n" + ExceptionUtils.getMessage(e) );
                } catch ( IOException e ) {
                    resetResource( resourceHolder, originalUri, originalContent );
                    logger.log(
                            Level.WARNING,
                            "Error resetting resource '"+resourceHolder.getSystemId()+": " + ExceptionUtils.getMessage(e),
                            ExceptionUtils.getDebugException(e));
                    showErrorMessage( "Resource Reset Error", "Unable to reset resource to default value:\n" + ExceptionUtils.getMessage(e) );
                } catch ( URISyntaxException e ) {
                    resetResource( resourceHolder, originalUri, originalContent );
                    showErrorMessage( "Resource Reset Error", "Unable to reset resource to default value:\n" + ExceptionUtils.getMessage(e) );
                }
            }

            initAnalyze( resourceHeaders );
            updateSummaries(false);
        }
    }

    private void resetResource( final ResourceHolder resourceHolder,
                                final String uri,
                                final String content ) {
        if ( uri != null ) {
            try {
                resourceHolder.setSystemId( new URI( uri ) );
            } catch ( URISyntaxException e ) {
                logger.warning( "Unable to reset resource URI : " + ExceptionUtils.getMessage(e) );
            }
        }

        if ( content != null ) {
            try {
                resourceHolder.updateContentFrom( new URIResourceDocument( URI.create("content.dat"), content, null ) );
            } catch ( IOException e ) {
                logger.warning( "Unable to reset resource content : " + ExceptionUtils.getMessage(e) );
            }
        }
    }


    private Pair<Boolean,Boolean> confirmReset( final String message,
                                                final boolean uriAndContent ) {
        Pair<Boolean,Boolean> confirmed = new Pair<Boolean,Boolean>( false, false );

        final int optionType;
        final Object[] options;
        if ( uriAndContent ) {
            optionType = JOptionPane.YES_NO_CANCEL_OPTION;
            options = new Object[]{"Reset", "Reset Content Only", "Cancel"};
        } else {
            optionType = JOptionPane.YES_NO_OPTION;
            options = new Object[]{"Reset","Cancel"};
        }

        final int choice = JOptionPane.showOptionDialog(
                this,
                message,
                "Confirm Resource Reset",
                optionType,
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                "Cancel" );
        if ( choice == JOptionPane.YES_OPTION ) {
            confirmed = new Pair<Boolean,Boolean>( true, true );
        } else if ( choice == JOptionPane.NO_OPTION ) {
            confirmed = new Pair<Boolean,Boolean>( false, true );            
        }

        return confirmed;
    }

    private void showErrorMessage( final String title,
                                   final String message) {
        DialogDisplayer.showMessageDialog(
                this,
                Utilities.getTextDisplayComponent(message, 600, 100, -1, -1),
                title,
                JOptionPane.ERROR_MESSAGE,
                null);
    }

    private ListModel asListModel( final Collection<?> elements ) {
        final DefaultListModel model = new DefaultListModel();

        for ( Object element : elements ) {
            model.addElement( element );
        }

        return model;
    }

    private void showDetails() {
        final int[] rows = resourcesTable.getSelectedRows();
        if ( rows.length == 1 ) {
            final int modelRow = resourcesTable.convertRowIndexToModel( rows[0] );
            final ResourceHolder resourceHolder = resourceHolderTableModel.getRowObject( modelRow );
            systemIdTextField.setText( resourceHolder.getSystemId() );
            systemIdTextField.setCaretPosition( 0 );
            descriptionTextField.setText( resourceHolder.getDescription() );
            descriptionTextField.setCaretPosition( 0 );
            statusTextField.setText( getStatus(resourceHolder) );
            statusTextField.setCaretPosition( 0 );
            statusDetailTextPane.setText( getStatusDetail(resourceHolder) );
            statusDetailTextPane.setCaretPosition( 0 );
            final DependencyScope scope = (DependencyScope)dependenciesComboBox.getSelectedItem();
            dependenciesList.setModel( asListModel( getDependencies( scope, resourceHolder, resourceHolderTableModel.getRows() ) ) );
            dependantsList.setModel( asListModel( getDependants( scope, resourceHolder, resourceHolderTableModel.getRows() ) ) );
        } else {
            systemIdTextField.setText( "" );
            descriptionTextField.setText( "" );
            statusTextField.setText( "" );
            statusDetailTextPane.setText( "" );
            dependenciesList.setModel( new DefaultListModel() );
            dependantsList.setModel( new DefaultListModel() );
        }
    }

    private String getStatus( final ResourceHolder resourceHolder ) {
        final StringBuilder statusBuilder = new StringBuilder();

        statusBuilder.append( resourceHolder.getStatus() );

        final Pair<Boolean,Boolean> defaultUriAndContent = isDefaultUriAndContent( resourceHolder );
        if ( !defaultUriAndContent.left || !defaultUriAndContent.right ) {
            statusBuilder.append( " (" );
            if ( !defaultUriAndContent.left ) {
                statusBuilder.append( "System ID " );
            }
            if ( !defaultUriAndContent.right ) {
                if ( !defaultUriAndContent.left ) {
                    statusBuilder.append( "and " );
                }
                statusBuilder.append( "content " );
            }
            statusBuilder.append( "modified from default value)" );
        }

        return statusBuilder.toString();
    }

    private String getStatusDetail( final ResourceHolder resourceHolder ) {
        String statusDetail = resourceHolder.isError() ? ExceptionUtils.getMessage(resourceHolder.getError()) : "";

        final ResourceHolder defaultResource = resolve(
                   defaultResources,
                   resourceHolder.getType(),
                   null,
                   resourceHolder.getSystemId(),
                   resourceHolder.getPublicId(),
                   resourceHolder.getTargetNamespace() );
        if ( defaultResource != null && !defaultResource.getSystemId().equals( resourceHolder.getSystemId() ) ) {
            if ( !statusDetail.isEmpty() ) statusDetail += "\n\n";

            statusDetail += "The default URI for this resource is:\n" + defaultResource.getSystemId();
        }

        return statusDetail;
    }

    private void updateResourceHeaders( final java.util.List<ResourceEntryHeader> headers,
                                        final String originalUri,
                                        final String updatedUri ) {
        ResourceEntryHeader oldHeader = null;
        ResourceEntryHeader newHeader = null;
        for ( final ResourceEntryHeader header : headers ) {
            if ( originalUri.equals( header.getUri() ) ) {
                oldHeader = header;
                newHeader = new ResourceEntryHeader(
                        header.getStrId(),
                        updatedUri,
                        header.getDescription(),
                        header.getResourceType(),
                        header.getResourceKey1(),
                        header.getResourceKey2(),
                        header.getResourceKey3(),
                        header.getVersion(),
                        header.getSecurityZoneId()
                );

                break;
            }
        }

        if ( oldHeader != null ) {
            final int index = headers.indexOf( oldHeader );
            headers.remove( index );
            headers.add( index, newHeader );
        }
    }

    private void navigateDependencySummary( final JList list ) {
        final Object selected = list.getSelectedValue();        
        if ( selected instanceof DependencySummary ) {
            final DependencySummary dependencySummary = (DependencySummary) selected;
            final ResourceHolder resourceHolder =
                    findResourceHolderByUri( resourceHolderTableModel.getRows(), dependencySummary.getUri() );
            if ( resourceHolder != null ) {
                final int modelRow = resourceHolderTableModel.getRowIndex( resourceHolder );
                final int row = resourcesTable.convertRowIndexToView( modelRow );
                resourcesTable.getSelectionModel().setSelectionInterval( row, row );
            }
        }
    }

    private void updateSummaries( final boolean validated ) {
        final Collection<ResourceHolder> resourceHolders = resourceHolderTableModel.getRows();
        final int invalidCount = countErrors( resourceHolders );
        final String suffix = validated ? "" : resources.getString("suffix.not-validated");
        
        analyzedResourceCountLabel.setText( Integer.toString( resourceHolders.size() ));
        validationFailureCountLabel.setText( invalidCount + " " + suffix);
    }

    private int countErrors( final Collection<ResourceHolder> resourceHolders ) {
        return Functions.reduce( resourceHolders, 0, new Functions.Binary<Integer,Integer,ResourceHolder>(){
            @Override
            public Integer call( final Integer count, final ResourceHolder resourceHolder ) {
                return count + (resourceHolder.isError() ? 1 : 0);
            }
        } );
    }
}
