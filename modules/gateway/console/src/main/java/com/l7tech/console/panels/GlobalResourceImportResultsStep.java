package com.l7tech.console.panels;

import static com.l7tech.console.panels.GlobalResourceImportContext.*;
import static com.l7tech.console.panels.GlobalResourceImportWizard.*;
import com.l7tech.gateway.common.resources.ResourceType;
import com.l7tech.gui.SimpleTableModel;
import com.l7tech.gui.util.RunOnChangeListener;
import com.l7tech.gui.util.TableUtil;
import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.widgets.TextListCellRenderer;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.util.CollectionUtils;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Functions;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
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
                resourcesFailedLabel.setText( Integer.toString( Functions.reduce( resourceHolderTableModel.getRows(), 0, new Functions.Binary<Integer,Integer,ResourceHolder>(){
                    @Override
                    public Integer call( final Integer integer, final ResourceHolder resourceHolder ) {
                        return integer + (resourceHolder.isError() ? 1 : 0);
                    }
                } )) );
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

        Utilities.setDoubleClickAction( resourcesTable, viewButton );
    }

    private void doView() {
        final int[] selectedRows = resourcesTable.getSelectedRows();
        if ( selectedRows.length == 1 ) {
            final ResourceHolder holder = resourceHolderTableModel.getRowObject(
                    resourcesTable.convertRowIndexToModel( selectedRows[0] ) );
            GlobalResourceImportWizard.viewResourceHolder( getOwner(), holder );
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

    private void enableAndDisableComponents() {
        viewButton.setEnabled( resourcesTable.getSelectedRowCount() == 1 );
        removeButton.setEnabled( resourcesTable.getSelectedRowCount() > 0 );
        updateButton.setEnabled( false ); // TODO [steve] implement update of system identifiers (http -> local, change local prefixes)
    }

    private void showDetails() {
        final int[] rows = resourcesTable.getSelectedRows();
        if ( rows.length == 1 ) {
            final int modelRow = resourcesTable.convertRowIndexToModel( rows[0] );
            final ResourceHolder resourceHolder = resourceHolderTableModel.getRowObject( modelRow );
            systemIdTextField.setText( resourceHolder.getSystemId() );
            systemIdTextField.setCaretPosition( 0 );
            statusTextField.setText( resourceHolder.getStatus() );
            statusTextField.setCaretPosition( 0 );
            statusDetailTextPane.setText( resourceHolder.isError() ? ExceptionUtils.getMessage(resourceHolder.getError()) : "" );
            statusDetailTextPane.setCaretPosition( 0 );
            final DependencyScope scope = (DependencyScope)dependenciesComboBox.getSelectedItem();
            dependenciesList.setModel( asListModel( getDependencies( scope, resourceHolder, resourceHolderTableModel.getRows() ) ) );
            dependantsList.setModel( asListModel( getDependants( scope, resourceHolder, resourceHolderTableModel.getRows() ) ) );
        } else {
            systemIdTextField.setText( "" );
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
            GlobalResourceImportWizard.saveResources( processedResources.values() );
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
