package com.l7tech.console.panels;

import static com.l7tech.console.panels.GlobalResourceImportContext.*;
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
import java.util.TreeSet;

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
                final Set<String> dependantUris = getDependants( true, toRemove, resourceHolders );
                for ( final String dependantUri : dependantUris ) {
                    final ResourceHolder resourceHolder = findResourceHolderByUri( resourceHolders, dependantUri );
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
            final boolean transitive = dependenciesComboBox.getSelectedItem()==DependencyScope.TRANSITIVE;
            dependenciesList.setModel( asListModel( getDependencies( transitive, resourceHolder, resourceHolderTableModel.getRows() ) ) );
            dependantsList.setModel( asListModel( getDependants( transitive, resourceHolder, resourceHolderTableModel.getRows() ) ) );
        } else {
            systemIdTextField.setText( "" );
            statusTextField.setText( "" );
            statusDetailTextPane.setText( "" );
            dependenciesList.setModel( new DefaultListModel() );
            dependantsList.setModel( new DefaultListModel() );
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

    private Set<String> getDependencies( final boolean transitive,
                                         final ResourceHolder resourceHolder,
                                         final Collection<ResourceHolder> resourceHolders ) {
        final Set<String> dependencies = new TreeSet<String>();

        final Functions.Binary<Collection<ResourceHolder>,ResourceHolder,Collection<ResourceHolder>> resolver =
                new Functions.Binary<Collection<ResourceHolder>,ResourceHolder,Collection<ResourceHolder>>(){
                    @Override
                    public Collection<ResourceHolder> call( final ResourceHolder resourceHolder, final Collection<ResourceHolder> resourceHolders ) {
                        Collection<ResourceHolder> dependencies = new ArrayList<ResourceHolder>();

                        for ( final String dependencyUri : resourceHolder.getDependencies() ) {
                            final ResourceHolder dependency = findResourceHolderByUri( resourceHolders, dependencyUri );
                            if ( dependency != null ) {
                                dependencies.add( dependency );
                            }
                        }

                        return dependencies;
                    }
                };

        resolveRecursive( dependencies, transitive, resourceHolder, resourceHolders, resolver );

        return dependencies;
    }

    private Set<String> getDependants( final boolean transitive,
                                       final ResourceHolder resourceHolder,
                                       final Collection<ResourceHolder> resourceHolders ) {
        final Set<String> dependants = new TreeSet<String>();

        final Functions.Binary<Collection<ResourceHolder>,ResourceHolder,Collection<ResourceHolder>> resolver =
                new Functions.Binary<Collection<ResourceHolder>,ResourceHolder,Collection<ResourceHolder>>(){
                    @Override
                    public Collection<ResourceHolder> call( final ResourceHolder resourceHolder, final Collection<ResourceHolder> resourceHolders ) {
                        Collection<ResourceHolder> dependants = new ArrayList<ResourceHolder>();

                        for ( final ResourceHolder holder : resourceHolders ) {
                            for ( final String dependencyUri : holder.getDependencies() ) {
                                if ( dependencyUri.equals( resourceHolder.getSystemId() ) ) {
                                    dependants.add( holder );
                                    break;
                                }
                            }
                        }

                        return dependants;
                    }
                };

        resolveRecursive( dependants, transitive, resourceHolder, resourceHolders, resolver );

        return dependants;
    }

    private void resolveRecursive( final Set<String> values,
                                   final boolean recursive,
                                   final ResourceHolder resourceHolder,
                                   final Collection<ResourceHolder> resourceHolders,
                                   final Functions.Binary<Collection<ResourceHolder>,ResourceHolder,Collection<ResourceHolder>> resolver ) {
        for ( final ResourceHolder resolved : resolver.call( resourceHolder, resourceHolders ) ) {
            if ( values.add( resolved.getSystemId() ) && recursive ) {
                resolveRecursive( values, true, resolved, resourceHolders, resolver );   
            }
        }
    }

    private ResourceHolder findResourceHolderByUri( final Collection<ResourceHolder> resourceHolders,
                                                    final String dependencyUri ) {
        ResourceHolder resourceHolder = null;

        for ( final ResourceHolder holder : resourceHolders ) {
            if ( dependencyUri.equals( holder.getSystemId() ) ) {
                resourceHolder = holder;
                break;
            }
        }

        return resourceHolder;
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

    private enum DependencyScope {
        DIRECT,
        TRANSITIVE
    }
}
