package com.l7tech.console.panels;

import static com.l7tech.console.panels.GlobalResourceImportContext.*;
import static com.l7tech.console.panels.GlobalResourceImportWizard.*;

import com.l7tech.common.io.DocumentReferenceProcessor;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.gateway.common.resources.ResourceAdmin;
import com.l7tech.gateway.common.resources.ResourceType;
import com.l7tech.gui.SimpleTableModel;
import com.l7tech.gui.util.RunOnChangeListener;
import com.l7tech.gui.util.TableUtil;
import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.widgets.TextListCellRenderer;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.util.CollectionUtils;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Functions;
import com.l7tech.util.Pair;
import com.l7tech.util.TextUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Final step for the global resources import wizard.
 */
class GlobalResourceImportResultsStep extends GlobalResourceImportWizardStepPanel {

    private JTable resourcesTable;
    private JButton removeButton;
    private JTextField systemIdTextField;
    private JTextField descriptionTextField;
    private JTextField statusTextField;
    private JTextPane statusDetailTextPane;
    private JComboBox dependenciesComboBox;
    private JList dependenciesList;
    private JList dependantsList;
    private JPanel mainPanel;
    private JLabel resourcesTotalLabel;
    private JLabel resourcesFailedLabel;
    private JButton updateButton;
    private JButton viewButton;

    private SimpleTableModel<ResourceHolder> resourceHolderTableModel;
    private Map<String, ResourceHolder> processedResources = Collections.emptyMap();

    GlobalResourceImportResultsStep( final GlobalResourceImportWizardStepPanel next ) {
        super( "results-step", next );
        init();
    }

    @Override
    public boolean canFinish() {
        return getPersistCount() > 0;
    }

    private void init() {
        setLayout( new BorderLayout() );
        add(mainPanel, BorderLayout.CENTER);

        resourceHolderTableModel = TableUtil.configureTable(
                resourcesTable,
                TableUtil.column(resources.getString("column.system-id"), 40, 240, 100000, Functions.<String,ResourceHolder>propertyTransform(ResourceHolder.class, "systemId"), String.class),
                TableUtil.column(resources.getString("column.details"), 40, 120, 100000, Functions.<String,ResourceHolder>propertyTransform(ResourceHolder.class, "details"), String.class),
                TableUtil.column(resources.getString("column.type"), 40, 80, 120, Functions.<ResourceType,ResourceHolder>propertyTransform(ResourceHolder.class, "type"), ResourceType.class),
                TableUtil.column(resources.getString("column.status"), 40, 50, 120, Functions.<String,ResourceHolder>propertyTransform(ResourceHolder.class, "status"), String.class),
                TableUtil.column(resources.getString("column.action"), 40, 50, 120, Functions.<String,ResourceHolder>propertyTransform(ResourceHolder.class, "action"), String.class)
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
        resourcesTable.getModel().addTableModelListener( new TableModelListener(){
            @Override
            public void tableChanged( final TableModelEvent e ) {
                resourcesTotalLabel.setText( Integer.toString( resourceHolderTableModel.getRowCount()) );
                resourcesFailedLabel.setText( Integer.toString( getErrorCount() ) );
                enableAndDisableComponents();
            }
        } );

        dependenciesComboBox.setModel( new DefaultComboBoxModel( DependencyScope.values() ) );
        dependenciesComboBox.setSelectedItem( DependencyScope.DIRECT );
        dependenciesComboBox.setRenderer( new TextListCellRenderer<DependencyScope>( new Functions.Unary<String,DependencyScope>(){
            @Override
            public String call( final DependencyScope dependencyScope ) {
                return resources.getString( "dependencies." + dependencyScope.name() );
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

        removeButton.addActionListener( new ActionListener(){
            @Override
            public void actionPerformed( final ActionEvent e ) {
                doRemove();
            }
        } );

        updateButton.addActionListener( new ActionListener(){
            @Override
            public void actionPerformed( final ActionEvent e ) {
                doUpdate();
            }
        } );

        Utilities.setDoubleClickAction( resourcesTable, viewButton );
    }

    private void doView() {
        final int[] selectedRows = resourcesTable.getSelectedRows();
        if ( selectedRows.length == 1 ) {
            final ResourceHolder holder = resourceHolderTableModel.getRowObject(
                    resourcesTable.convertRowIndexToModel( selectedRows[0] ) );
            GlobalResourceImportWizard.viewResourceHolder( getOwner(), holder, resourceHolderTableModel.getRows() );
        }
    }

    private void doRemove() {
        final int[] rows = resourcesTable.getSelectedRows();
        if ( rows != null ) {
            final Set<ResourceHolder> resourceHoldersToRemove = new HashSet<ResourceHolder>();

            for ( final int row : rows ) {
                final int modelRow = resourcesTable.convertRowIndexToModel( row );
                resourceHoldersToRemove.add( resourceHolderTableModel.getRowObject( modelRow ) );
            }

            final Collection<ResourceHolder> resourceHolders = resourceHolderTableModel.getRows();
            final Set<ResourceHolder> dependants = new HashSet<ResourceHolder>();
            for ( final ResourceHolder toRemove : resourceHoldersToRemove ) {
                final Set<DependencySummary> dependantSummaries = getDependants( DependencyScope.ALL, toRemove, resourceHolders );
                for ( final DependencySummary dependencySummary : dependantSummaries ) {
                    final ResourceHolder resourceHolder = findResourceHolderByUri( resourceHolders, dependencySummary.getUri() );
                    if ( resourceHolder != null ) {
                        dependants.add( resourceHolder );
                    }
                }
            }
            dependants.removeAll( resourceHoldersToRemove );

            if ( confirmRemoval( resourceHoldersToRemove, dependants ) ) {
                for ( final ResourceHolder toRemove : CollectionUtils.iterable( resourceHoldersToRemove, dependants ) ) {
                    resourceHolderTableModel.removeRow( toRemove );
                }
            }
        }
    }

    private void doUpdate() {
        final java.util.List<URI> resourceUris = new ArrayList<URI>( getNewResourceUris() );
        Collections.sort( resourceUris );

        final JPanel systemIdPanel = new JPanel();
        systemIdPanel.setLayout( new BoxLayout( systemIdPanel, BoxLayout.Y_AXIS ) );

        final JLabel optionLabel = new JLabel( "Enter the current and updated System Identifier prefixes:" );
        optionLabel.setAlignmentX( JComponent.LEFT_ALIGNMENT );
        systemIdPanel.add( optionLabel );

        final JComboBox currentUriComboBox = new JComboBox();
        currentUriComboBox.setAlignmentX( JComponent.LEFT_ALIGNMENT );
        currentUriComboBox.setEditable( true );
        currentUriComboBox.setModel( new DefaultComboBoxModel( resourceUris.toArray() ) );

        systemIdPanel.add( Box.createVerticalStrut( 4 ) );
        systemIdPanel.add( currentUriComboBox );
        systemIdPanel.add( Box.createVerticalStrut( 4 ) );

        final JTextField targetUriTextField = new JTextField();
        targetUriTextField.setAlignmentX( JComponent.LEFT_ALIGNMENT );

        systemIdPanel.add( targetUriTextField );
        systemIdPanel.add( Box.createVerticalStrut( 4 ) );

        while ( true ) {
            final int choice = JOptionPane.showOptionDialog(
                    this,
                    systemIdPanel,
                    "Update System Identifiers",
                    JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    new Object[]{"Update","Cancel"},
                    "Cancel" );

            if ( choice == JOptionPane.OK_OPTION ) {
                final String fromUri = currentUriComboBox.getSelectedItem().toString().trim();
                final String toUri = targetUriTextField.getText().trim();

                if ( !fromUri.isEmpty() && !toUri.isEmpty() ) {
                    if ( updateNewResourceUris( fromUri, toUri ) ) {
                        break;
                    }
                } else {
                    GlobalResourceImportWizard.showErrorMessage( this, "Update Error", "Current and updated System Identifiers are required." );
                }
            } else {
                break;
            }
        }
    }

    private boolean updateNewResourceUris( final String uriPrefix,
                                           final String replacementUri ) {
        final ResourceAdmin resourceAdmin = getResourceAdmin();

        // Ensure no uri conflicts or invalid uris
        final Map<String,URI> newUris = new HashMap<String,URI>(); // map of current to updated uris.
        try {
            for ( final ResourceHolder resourceHolder : resourceHolderTableModel.getRows() ) {
                if ( resourceHolder.isPersist() && resourceHolder.isNew() && resourceHolder.getSystemId().startsWith( uriPrefix )) {
                    final URI newUri = asUri(replacementUri + resourceHolder.getSystemId().substring( uriPrefix.length() ) );
                    if ( !newUri.isAbsolute() ) {
                        throw new IOException( "System identifiers must be absolute (relative identifiers not permitted)" );    
                    }
                    newUris.put( resourceHolder.getSystemId(), newUri );
                }
            }
        } catch ( IOException e ) {
            GlobalResourceImportWizard.showErrorMessage( this, "Update Error", "Unable to generate updated system identifiers:\n" + ExceptionUtils.getMessage(e));
            return false;
        }

        String conflictUri = null;
        try {
            final Set<String> importUris = new HashSet<String>(Functions.map( resourceHolderTableModel.getRows(), new Functions.Unary<String,ResourceHolder>(){
                @Override
                public String call( final ResourceHolder resourceHolder ) {
                    return resourceHolder.getSystemId();
                }
            } ) );
            importUris.removeAll( newUris.keySet() ); // ignore anything being renamed
            for ( final URI uri : newUris.values() ) {
                if ( importUris.contains( uri.toString() )) {
                    conflictUri = uri.toString();
                    break;
                }
            }
            if ( conflictUri == null ) {
                for ( final URI uri : newUris.values() ) {
                    if ( resourceAdmin.findResourceHeaderByUriAndType( uri.toString(), null ) != null ) {
                        conflictUri = uri.toString();
                        break;
                    }
                }
            }
        } catch ( FindException e ) {
            GlobalResourceImportWizard.showErrorMessage( this, "Update Error", "Error checking for resource system identifier conflicts:\n" + ExceptionUtils.getMessage(e));
            return false;
        }

        if ( conflictUri != null ) {
            GlobalResourceImportWizard.showErrorMessage( this, "Update Error", "An updated system identifier conflicts with an imported or existing resource:\n" + TextUtils.truncStringMiddleExact( conflictUri, 80 )+"\nSystem identifiers have not been updated.");
            return false;
        } else {
            // Build updated content first, fix any references to the updated system identifiers
            final Collection<ResourceHolder> resourceHolders = resourceHolderTableModel.getRows();
            final Map<String,Pair<String,Set<Pair<String,String>>>> currentUriToUpdatedInfo;
            try {
                currentUriToUpdatedInfo = buildUpdateMap( uriPrefix, replacementUri, resourceHolders );
            } catch ( IOException e ) {
                GlobalResourceImportWizard.showErrorMessage( this, "Update Error", ExceptionUtils.getMessage(e));
                return false;
            }

            for ( final ResourceHolder resourceHolder : resourceHolders ) {
                final String currentUri = resourceHolder.getSystemId();

                final URI newUri = newUris.get( currentUri );
                if ( newUri != null ) {
                    resourceHolder.setSystemId( newUri );
                }

                final Pair<String,Set<Pair<String,String>>> updateInfo = currentUriToUpdatedInfo.get( currentUri );
                if ( updateInfo != null ) {
                    if ( updateInfo.left != null ) {
                        resourceHolder.setContent( updateInfo.left );
                    }
                    if ( updateInfo.right != null ) {
                        resourceHolder.setDependencies( updateInfo.right );
                    }
                }
            }

            resourceHolderTableModel.fireTableDataChanged();
        }

        return true;
    }

    /**
     * Build a map of current resource URI to updated content / dependencies.
     */
    private Map<String,Pair<String,Set<Pair<String,String>>>> buildUpdateMap( final String uriPrefix,
                                                                              final String replacementUri,
                                                                              final Collection<ResourceHolder> resourceHolders ) throws IOException {
        final Map<String,Pair<String,Set<Pair<String,String>>>> currentUriToUpdateInfo = new HashMap<String,Pair<String,Set<Pair<String,String>>>>();
        for ( final ResourceHolder resourceHolder : resourceHolders ) {
            boolean dependenciesUpdated = false;
            final Map<String,String> dependencyChanges = new HashMap<String,String>();
            final Set<Pair<String,String>> updatedDependencies = new HashSet<Pair<String,String>>();
            for ( final Pair<String,String> dependencyUriPair : resourceHolder.getDependencies() ) {
                if ( dependencyUriPair.left==null ) {
                    updatedDependencies.add( dependencyUriPair );
                    continue; // reference by namespace only, etc
                }

                String newDependencyUri = dependencyUriPair.left;
                String newAbsoluteDependencyUri = dependencyUriPair.right;
                final boolean prefixMatch = dependencyUriPair.right.startsWith( uriPrefix );
                if ( prefixMatch ) {
                    final String updatedBaseUri = resourceHolder.getSystemId().startsWith( uriPrefix ) ?
                            replacementUri + resourceHolder.getSystemId().substring( uriPrefix.length() ) :
                            resourceHolder.getSystemId();
                    final String updatedDependencyUri = replacementUri + dependencyUriPair.right.substring( uriPrefix.length() );
                    final URI newRelativeUri = relativizeUri(asUri(updatedBaseUri), asUri(updatedDependencyUri));
                    if ( !newRelativeUri.equals( asUri(dependencyUriPair.left).normalize() ) ) {
                        // ensure this dependency type can be updated
                        final ResourceHolder dependencyHolder =
                                GlobalResourceImportContext.findResourceHolderByUri( resourceHolders, dependencyUriPair.right );
                        if ( dependencyHolder==null || dependencyHolder.getType() != ResourceType.XML_SCHEMA ) {
                            throw new IOException( "Cannot update system identifiers due to missing or non-updatable resource:\n"+TextUtils.truncStringMiddleExact(dependencyUriPair.right,80));
                        }

                        newDependencyUri = newRelativeUri.toString();
                        newAbsoluteDependencyUri = updatedDependencyUri;
                        dependencyChanges.put( dependencyUriPair.left, newDependencyUri );
                        dependenciesUpdated = true;
                    } else if ( !dependencyUriPair.right.equals( updatedDependencyUri ) ) {
                        newAbsoluteDependencyUri = updatedDependencyUri;
                        dependenciesUpdated = true;
                    }
                }

                updatedDependencies.add( new Pair<String,String>( newDependencyUri, newAbsoluteDependencyUri ) );
            }

            String contentUpdate = null;
            if ( !dependencyChanges.isEmpty() ) {
                if ( resourceHolder.isPersist() ) {
                    try {
                        final InputSource source = new InputSource( resourceHolder.getSystemId() );
                        source.setCharacterStream( new StringReader( resourceHolder.getContent() ) );
                        final Document schemaDocument = XmlUtil.parse( source, new ResourceHolderEntityResolver(resourceHolders) );
                        final DocumentReferenceProcessor schemaProcessor = DocumentReferenceProcessor.schemaProcessor();
                        schemaProcessor.processDocumentReferences( schemaDocument, new DocumentReferenceProcessor.ReferenceCustomizer(){
                            @Override
                            public String customize( final Document document,
                                                     final Node node,
                                                     final String documentUrl,
                                                     final DocumentReferenceProcessor.ReferenceInfo referenceInfo ) {
                                String updatedLocation = null;
                                if ( node instanceof Element ) {
                                    final Element referenceElement = (Element) node;
                                    if ( referenceElement.hasAttributeNS( null, "schemaLocation" ) ) {
                                        final String location = referenceElement.getAttributeNS( null, "schemaLocation" );
                                        updatedLocation = dependencyChanges.get( location );
                                    }
                                }
                                return updatedLocation;
                            }
                        } );

                        contentUpdate = XmlUtil.nodeToString( schemaDocument, true );
                    } catch ( SAXException e ) {
                        throw new IOException("Error updating references for resource:\n"+TextUtils.truncStringMiddleExact(resourceHolder.getSystemId(), 80)+"\n"+ExceptionUtils.getMessage(e));
                    } catch ( IOException e ) {
                        throw new IOException("Error updating references for resource:\n"+TextUtils.truncStringMiddleExact(resourceHolder.getSystemId(), 80)+"\n"+ExceptionUtils.getMessage(e));
                    }
                } else { // Fail, we're not creating/updating this resource so can't change the content
                    throw new IOException("Cannot update references for resource:\n"+TextUtils.truncStringMiddleExact(resourceHolder.getSystemId(), 80));
                }
            }

            if ( contentUpdate != null || dependenciesUpdated ) {
                currentUriToUpdateInfo.put( resourceHolder.getSystemId(), new Pair<String,Set<Pair<String,String>>>( contentUpdate, updatedDependencies ) );
            }
        }

        return currentUriToUpdateInfo;
    }

    private URI asUri( final String uri ) throws IOException {
        try {
            return new URI( uri );
        } catch ( URISyntaxException e ) {
            throw new IOException( "Cannot process invalid URI "+TextUtils.truncStringMiddleExact(uri, 80) + ": " + ExceptionUtils.getMessage(e), e );
        }
    }

    private int getErrorCount() {
        return Functions.reduce( resourceHolderTableModel.getRows(), 0, new Functions.Binary<Integer,Integer, ResourceHolder>(){
            @Override
            public Integer call( final Integer integer, final ResourceHolder resourceHolder ) {
                return integer + (resourceHolder.isError() ? 1 : 0);
            }
        } );
    }

    private int getPersistCount() {
        return Functions.reduce( resourceHolderTableModel.getRows(), 0, new Functions.Binary<Integer,Integer, ResourceHolder>(){
            @Override
            public Integer call( final Integer integer, final ResourceHolder resourceHolder ) {
                return integer + (resourceHolder.isPersist() ? 1 : 0);
            }
        } );
    }

    private int getNewCount() {
        return getNewResourceUris().size();
    }

    private Set<URI> getNewResourceUris() {
        return Functions.reduce( resourceHolderTableModel.getRows(), new HashSet<URI>(), new Functions.Binary<Set<URI>,Set<URI>, ResourceHolder>(){
            @Override
            public Set<URI> call( final Set<URI> resourceUris, final ResourceHolder resourceHolder ) {
                if ( resourceHolder.isPersist() && resourceHolder.isNew() ) {
                    resourceUris.add( resourceHolder.asResourceDocument().getUri() );
                }
                return resourceUris;
            }
        } );
    }

    private void enableAndDisableComponents() {
        viewButton.setEnabled( resourcesTable.getSelectedRowCount() == 1 );
        removeButton.setEnabled( resourcesTable.getSelectedRowCount() > 0 );
        updateButton.setEnabled( getNewCount() > 0 );
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
            statusTextField.setText( resourceHolder.getStatus() );
            statusTextField.setCaretPosition( 0 );
            statusDetailTextPane.setText( resourceHolder.isError() ? ExceptionUtils.getMessage(resourceHolder.getError()) : "" );
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

    @Override
    public boolean canAdvance() {
        return true;
    }

    @Override
    public boolean onNextButton() {
        // process schemas
        try {
            GlobalResourceImportWizard.saveResources( getResourceAdmin(), processedResources.values() );
        } catch ( SaveException se ) {
            GlobalResourceImportWizard.handleSaveError( getOwner(), se );
        }

        return true;
    }

    @Override
    public void storeSettings( final GlobalResourceImportContext settings ) {

    }

    @Override
    public void readSettings( final GlobalResourceImportContext settings ) {
        processedResources = settings.getProcessedResources();
        resourceHolderTableModel.setRows( new ArrayList<ResourceHolder>( processedResources.values()) );
    }

    private ListModel asListModel( final Collection<?> elements ) {
        final DefaultListModel model = new DefaultListModel();

        for ( Object element : elements ) {
            model.addElement( element );
        }

        return model;
    }

    private boolean confirmRemoval( final Collection<ResourceHolder> directRemovals,
                                    final Collection<ResourceHolder> dependencyRemovals ) {
        final String message;

        if ( dependencyRemovals.size() == 0 ) {
            message = "Really remove " + directRemovals.size() + " resource(s)?";
        } else {
            message = "Really remove " + directRemovals.size() + " resource(s) and " + dependencyRemovals.size() + " dependant(s)?";
        }

        final int choice = JOptionPane.showOptionDialog(
                this,
                message,
                "Confirm Resource Removal",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                new Object[]{"Remove","Cancel"},
                "Cancel" );
        return choice == JOptionPane.OK_OPTION;        
    }
}
