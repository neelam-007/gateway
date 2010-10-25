package com.l7tech.console.panels;

import com.l7tech.console.action.Actions;
import com.l7tech.gateway.common.resources.ResourceAdmin;
import com.l7tech.gateway.common.resources.ResourceEntryHeader;
import com.l7tech.gateway.common.resources.ResourceType;
import static com.l7tech.console.panels.GlobalResourceImportContext.*;
import static com.l7tech.console.panels.GlobalResourceImportWizard.*;
import com.l7tech.gui.SimpleTableModel;
import com.l7tech.gui.util.RunOnChangeListener;
import com.l7tech.gui.util.TableUtil;
import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.widgets.TextListCellRenderer;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Functions;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.ResourceBundle;

/**
 * Global resource analysis dialog.
 */
public class GlobalResourcesAnalyze extends JDialog {

    private static final ResourceBundle resources = ResourceBundle.getBundle( GlobalResourcesAnalyze.class.getName() );
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
    private JTextField statusTextField;
    private JTextPane statusDetailTextPane;
    private JComboBox dependenciesComboBox;
    private JList dependenciesList;
    private JList dependantsList;

    private final ResourceAdmin resourceAdmin;
    private SimpleTableModel<ResourceHolder> resourceHolderTableModel;

    public GlobalResourcesAnalyze( final Window parent,
                                   final ResourceAdmin resourceAdmin,
                                   final Collection<ResourceEntryHeader> resourceHeaders ) {
        super( parent, JDialog.DEFAULT_MODALITY_TYPE );
        this.resourceAdmin = resourceAdmin;
        init();
        initAnalyze( resourceHeaders );
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
                updateSummaries( false );
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
                Actions.invokeHelp( GlobalResourcesAnalyze.this);
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
        updateSummaries(false);
        enableAndDisableComponents();
    }

    private void initAnalyze( final Collection<ResourceEntryHeader> resourceHeaders ) {
        final Collection<ResourceHolder> resourceHolders = GlobalResourceImportWizard.resolveDependencies(
                this,
                new HashSet<String>(Functions.map( resourceHeaders, Functions.<String,ResourceEntryHeader>propertyTransform( ResourceEntryHeader.class, "uri" ))),
                resourceAdmin );

        resourceHolderTableModel.setRows( new ArrayList<ResourceHolder>(resourceHolders) );
    }

    private void enableAndDisableComponents() {
        viewButton.setEnabled( resourcesTable.getSelectedRowCount()==1 );
        validateButton.setEnabled( false ); //TODO [steve] enable when implemented
        resetButton.setEnabled( false ); // TODO [steve] enable if write permitted and resource can be reset
    }

    private void doView() {
        final int[] selectedRows = resourcesTable.getSelectedRows();
        if ( selectedRows.length == 1 ) {
            final ResourceHolder holder = resourceHolderTableModel.getRowObject(
                    resourcesTable.convertRowIndexToModel( selectedRows[0] ) );
            GlobalResourceImportWizard.viewResourceHolder( this, holder );
        }
    }

    private void doValidate() {
        //TODO [steve] implement
    }

    private void doReset() {
        //TODO [steve] implement
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

    private void updateSummaries( final boolean validated ) {
        analyzedResourceCountLabel.setText( Integer.toString( resourceHolderTableModel.getRowCount() ));

        final Integer invalidCount = Functions.reduce( resourceHolderTableModel.getRows(), 0, new Functions.Binary<Integer,Integer,ResourceHolder>(){
            @Override
            public Integer call( final Integer count, final ResourceHolder resourceHolder ) {
                return count + (resourceHolder.isError() ? 1 : 0);
            }
        } );
        final String suffix = validated ? "" : resources.getString("suffix.not-validated");
        validationFailureCountLabel.setText( invalidCount + " " + suffix);                
    }
}
