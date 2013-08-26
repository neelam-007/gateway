package com.l7tech.console.panels;

import com.l7tech.console.action.Actions;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.ResourceAdminEntityResolver;
import com.l7tech.gateway.common.resources.ResourceAdmin;
import com.l7tech.gateway.common.resources.ResourceEntry;
import com.l7tech.gateway.common.resources.ResourceEntryHeader;
import com.l7tech.gateway.common.resources.ResourceType;
import com.l7tech.gateway.common.security.rbac.AttemptedUpdate;
import com.l7tech.gui.SimpleTableModel;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.RunOnChangeListener;
import com.l7tech.gui.util.TableUtil;
import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.widgets.TextListCellRenderer;
import com.l7tech.objectmodel.*;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Functions;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Dialog for management of global resources.
 */
public class GlobalResourcesDialog extends JDialog {

    private static final Logger logger = Logger.getLogger( GlobalResourcesDialog.class.getName() );
    private static final ResourceBundle resourceBundle = ResourceBundle.getBundle( GlobalResourcesDialog.class.getName() );

    private static final Object ANY = new Object();

    private JPanel mainPanel;
    private JButton addXmlSchemaButton;
    private JButton addDTDButton;
    private JButton editButton;
    private JButton removeButton;
    private JButton importButton;
    private JButton analyzeButton;
    private JButton helpButton;
    private JButton closeButton;
    private JButton filterButton;
    private JTable resourcesTable;
    private JTextField matchesTextField;
    private JComboBox typeComboBox;
    private JLabel filterStatusLabel;
    private JLabel displayedAndTotalResourcesLabel;

    private final ResourceAdmin resourceAdmin;
    private final PermissionFlags flags;
    private SimpleTableModel<ResourceEntryHeader> resourcesTableModel;

    public GlobalResourcesDialog( final Window parent ) {
        super( parent, resourceBundle.getString( "title" ), GlobalResourcesDialog.DEFAULT_MODALITY_TYPE );
        final Registry registry = Registry.getDefault();
        resourceAdmin = registry.getResourceAdmin();
        flags = PermissionFlags.get( EntityType.RESOURCE_ENTRY );
        init();
    }

    private void init() {
        add(mainPanel);

        final RunOnChangeListener enableDisableListener = new RunOnChangeListener(){
            @Override
            protected void run() {
                enableDisableComponents();
            }
        };

        final TextListCellRenderer<Object> textRenderer = buildResourceTypeRenderer();
        Utilities.setMaxLength( matchesTextField.getDocument(), 8192);
        typeComboBox.setModel( new DefaultComboBoxModel( ResourceType.values() ) );
        ((DefaultComboBoxModel)typeComboBox.getModel()).insertElementAt( ANY, 0 );
        typeComboBox.setRenderer( textRenderer );
        typeComboBox.setSelectedIndex(0);

        resourcesTableModel = buildResourcesTableModel();
        resourcesTable.setModel( resourcesTableModel );
        resourcesTable.getTableHeader().setReorderingAllowed( false );
        resourcesTable.setDefaultRenderer( ResourceType.class, textRenderer.asTableCellRenderer() );
        resourcesTable.setSelectionMode( ListSelectionModel.MULTIPLE_INTERVAL_SELECTION );
        resourcesTable.getSelectionModel().addListSelectionListener( enableDisableListener );
        Utilities.setRowSorter( resourcesTable, resourcesTableModel, new int[]{0}, new boolean[]{true}, new Comparator[]{String.CASE_INSENSITIVE_ORDER} );
        TableRowSorter<SimpleTableModel<ResourceEntryHeader>> rowSorter =
                (TableRowSorter<SimpleTableModel<ResourceEntryHeader>>) resourcesTable.getRowSorter();
        rowSorter.setSortsOnUpdates( true );
        rowSorter.setRowFilter( getFilter() );

        resourcesTable.getModel().addTableModelListener( enableDisableListener );

        registerListeners();

        pack();
        setMinimumSize( getContentPane().getMinimumSize() );        
        Utilities.setDoubleClickAction( resourcesTable, editButton );
        Utilities.centerOnParentWindow( this );
        Utilities.setEnterAction( matchesTextField, filterButton );
        Utilities.setEscAction( this, closeButton );
        Utilities.setButtonAccelerator( this, helpButton, KeyEvent.VK_F1 );
        enableDisableComponents();
        loadResources();
    }

    private void registerListeners() {
        addXmlSchemaButton.addActionListener( new ActionListener(){
            @Override
            public void actionPerformed( final ActionEvent e ) {
                doAdd(ResourceType.XML_SCHEMA);
            }
        } );
        addDTDButton.addActionListener( new ActionListener(){
            @Override
            public void actionPerformed( final ActionEvent e ) {
                doAdd(ResourceType.DTD);
            }
        } );
        editButton.addActionListener( new ActionListener(){
            @Override
            public void actionPerformed( final ActionEvent e ) {
                doEdit();
            }
        } );
        removeButton.addActionListener( new ActionListener(){
            @Override
            public void actionPerformed( final ActionEvent e ) {
                doRemove();
            }
        } );
        importButton.addActionListener( new ActionListener(){
            @Override
            public void actionPerformed( final ActionEvent e ) {
                doImport();
            }
        } );
        analyzeButton.addActionListener( new ActionListener(){
            @Override
            public void actionPerformed( final ActionEvent e ) {
                doAnalyze();
            }
        } );
        helpButton.addActionListener( new ActionListener(){
            @Override
            public void actionPerformed( final ActionEvent e ) {
                Actions.invokeHelp( GlobalResourcesDialog.this);
            }
        } );
        closeButton.addActionListener( new ActionListener(){
            @Override
            public void actionPerformed( final ActionEvent e ) {
                dispose();
            }
        } );
        filterButton.addActionListener( new ActionListener(){
            @Override
            public void actionPerformed( final ActionEvent e ) {
                 doFilter();
            }
        } );
    }

    private SimpleTableModel<ResourceEntryHeader> buildResourcesTableModel() {
        return TableUtil.configureTable(
                resourcesTable,
                TableUtil.column(resourceBundle.getString( "column.system-id" ), 40, 200, 100000, new Functions.Unary<String, ResourceEntryHeader>(){
                    @Override
                    public String call( final ResourceEntryHeader resourceEntryHeader ) {
                        return resourceEntryHeader.getUri();
                    }
                }, String.class),
                TableUtil.column(resourceBundle.getString( "column.detail" ), 40, 200, 100000, new Functions.Unary<String,ResourceEntryHeader>(){
                    @Override
                    public String call( final ResourceEntryHeader resourceEntryHeader ) {
                        String detail = "";
                        if ( resourceEntryHeader.getResourceKey1() != null ) {
                            switch ( resourceEntryHeader.getResourceType() ) {
                                case XML_SCHEMA:
                                    detail = "TNS: " + resourceEntryHeader.getResourceKey1();
                                    break;
                                case DTD:
                                    detail = "Public ID: " + resourceEntryHeader.getResourceKey1();
                                    break;
                            }
                        }
                        return detail;
                    }
                }, String.class),
                TableUtil.<ResourceType,ResourceEntryHeader>column(
                        resourceBundle.getString("column.type"), 40, 100, 180,
                        Functions.<ResourceType,ResourceEntryHeader>propertyTransform(ResourceEntryHeader.class, "resourceType"),
                        ResourceType.class)
        );
    }

    static TextListCellRenderer<Object> buildResourceTypeRenderer() {
        return new TextListCellRenderer<Object>(new Functions.Unary<String,Object>(){
            @Override
            public String call( final Object resourceType ) {
                String label;

                if ( resourceType == ANY ) {
                     label = resourceBundle.getString( "label.resource-type-any" );
                } else {
                    label = getDisplayName( (ResourceType)resourceType );
                }

                return label;
            }
        } );
    }

    private RowFilter<SimpleTableModel<ResourceEntryHeader>, Integer> getFilter() {
        final String filterString = matchesTextField.getText();
        final Pattern pattern = filterString == null ? null : Pattern.compile(filterString, Pattern.CASE_INSENSITIVE);
        final Object resourceType = typeComboBox.getSelectedItem();

        return new RowFilter<SimpleTableModel<ResourceEntryHeader>, Integer>() {
            @Override
            public boolean include(Entry<? extends SimpleTableModel<ResourceEntryHeader>, ? extends Integer> entry) {
                 boolean canBeShown = true;

                if ( resourceType != ANY && resourceType != entry.getValue(2) ) {
                    canBeShown = false;
                }

                if ( canBeShown && filterString != null && !filterString.trim().isEmpty() && pattern != null) {
                    final Matcher systemIdMatcher = pattern.matcher(entry.getStringValue(0));
                    final Matcher detailMatcher = pattern.matcher(entry.getStringValue(1));
                    canBeShown = systemIdMatcher.find() || detailMatcher.find();
                }

                return canBeShown;
            }
        };
    }

    private String getFilterStatus() {
        String filterStatus = resourceBundle.getString( "label.filter-status.none" );
        final String filterString = matchesTextField.getText();
        final Object resourceType = typeComboBox.getSelectedItem();

        if ( filterString != null && !filterString.isEmpty() && resourceType != ANY ) {
            filterStatus = MessageFormat.format(
                    resourceBundle.getString( "label.filter-status.resource-match" ),
                    getDisplayName( (ResourceType) resourceType ),
                    filterString );
        } else if ( filterString != null && !filterString.isEmpty() ) {
            filterStatus = MessageFormat.format(
                    resourceBundle.getString( "label.filter-status.match" ),
                    filterString );
        } else if ( resourceType != ANY ) {
            filterStatus = MessageFormat.format(
                    resourceBundle.getString( "label.filter-status.resource" ), 
                    getDisplayName( (ResourceType) resourceType ) );
        }

        return filterStatus;
    }

    private static String getDisplayName( final ResourceType resourceType ) {
        try {
            return resourceBundle.getString( "label.resource-type." + resourceType );
        } catch ( MissingResourceException e ) {
            return resourceType.name();
        }
    }

    private void enableDisableComponents() {
        final int[] selectedRows = resourcesTable.getSelectedRows();
        importButton.setEnabled( flags.canCreateSome() );
        addXmlSchemaButton.setEnabled( flags.canCreateSome() );
        addDTDButton.setEnabled( flags.canCreateSome() );
        editButton.setEnabled( selectedRows.length == 1 && flags.canUpdateSome() );
        removeButton.setEnabled( selectedRows.length > 0 && flags.canDeleteSome() );
        analyzeButton.setEnabled( selectedRows.length > 0 );
    }

    private void loadResources() {
        try {
            resourcesTableModel.setRows( new ArrayList<ResourceEntryHeader>( resourceAdmin.findAllResources() ) );
        } catch ( FindException e ) {
            logger.log( Level.WARNING, "Error loading resources", e );
            showErrorMessage(
                    "Error Loading Resources",
                    "Unexpected error loading resources:\n" + ExceptionUtils.getMessage(e) );
        }
        updateSummaryDisplay();
    }

    private void doFilter() {
        try {
            ((TableRowSorter<SimpleTableModel<ResourceEntryHeader>>) resourcesTable.getRowSorter()).setRowFilter( getFilter() );
            filterStatusLabel.setText( getFilterStatus() );
        } catch ( PatternSyntaxException e ) {
            showErrorMessage(
                "Global Resources Filtering",
                "Invalid syntax for the regular expression, \"" + matchesTextField.getText() + "\"" );
        }
        updateSummaryDisplay();
    }

    private void updateSummaryDisplay() {
        displayedAndTotalResourcesLabel.setText( resourcesTable.getRowCount() + " / " + resourcesTableModel.getRowCount() );
    }

    private void doAdd( final ResourceType resourceType ) {
        final ResourceEntry resourceEntry = new ResourceEntry();
        resourceEntry.setType( resourceType );
        editEntry( resourceEntry, true) ;
    }

    private void doEdit() {
        final int selectedRow = resourcesTable.getSelectedRow();
        if (selectedRow < 0) return;

        final ResourceEntryHeader header = resourcesTableModel.getRowObject( resourcesTable.convertRowIndexToModel(selectedRow) );        
        final ResourceEntry resourceEntry;
        try {
            resourceEntry = resourceAdmin.findResourceEntryByPrimaryKey( header.getGoid() );
        } catch ( FindException e ) {
            logger.log( Level.WARNING, "Error accessing resource entry.", e );
            showErrorMessage( "Error Accessing Resource", "Unable to access resource due to:\n" + ExceptionUtils.getMessage(e) );
            return;
        }

        if ( resourceEntry != null ) {
            editEntry( resourceEntry, Registry.getDefault().getSecurityProvider().hasPermission(new AttemptedUpdate(EntityType.RESOURCE_ENTRY, resourceEntry)) );
        } else {
            showErrorMessage( "Error Accessing Resource", "The selected resource is no longer available." );

            // pickup all changes from gateway
            loadResources();
        }
    }

    private void editEntry( final ResourceEntry entry, final boolean canEdit ) {
        final ResourceEntryEditor dlg = new ResourceEntryEditor( this, entry, new ResourceAdminEntityResolver(resourceAdmin), false, canEdit, !resourceAdmin.allowSchemaDoctype() );

        DialogDisplayer.display( dlg, new Runnable(){
            @Override
            public void run() {
                if ( dlg.wasOk() ) {
                    boolean reload = false;
                    try {
                        resourceAdmin.saveResourceEntry( entry );
                        reload = true;
                    } catch ( ObjectModelException e) {
                        if ( ExceptionUtils.causedBy(e, DuplicateObjectException.class) ) {
                            showErrorMessage( "Error Saving Resource", "Unable to save resource entry: System ID must be unique" );
                            editEntry( entry, true );
                        } else {
                            logger.log( Level.WARNING, "Error saving resource entry.", e );
                            showErrorMessage( "Error Saving Resource", "Unable to save resource entry:\n" + ExceptionUtils.getMessage(e) );
                            reload = true;
                        }
                    }

                    if ( reload ) {
                        // pickup all changes from gateway
                        loadResources();
                    }
                }
            }
        } );
    }

    private void doRemove() {
        int[] selectedRows = resourcesTable.getSelectedRows();

        final java.util.List<ResourceEntryHeader> toRemove = new ArrayList<ResourceEntryHeader>();
        for ( final int selectedRow : selectedRows ) {
            toRemove.add( resourcesTableModel.getRowObject( resourcesTable.convertRowIndexToModel( selectedRow )) );
        }

        if ( toRemove.size() > 0 ) {
            final String confirmationMessage;
            if ( toRemove.size() == 1 ) {
                confirmationMessage = "Are you sure you want to remove the selected resource?\n" + toRemove.get( 0 ).getUri();
            } else {
                confirmationMessage = "Are you sure you want to remove "+toRemove.size()+" selected resources?";
            }

            doRemoveWithConfirmation( confirmationMessage, false, new Runnable(){
                @Override
                public void run() {
                    try {
                        final Collection<Goid> resourceGoids = new ArrayList<Goid>();
                        for ( final ResourceEntryHeader resourceEntryHeader : toRemove ) {
                            resourceGoids.add( resourceEntryHeader.getGoid() );
                        }

                        final Runnable deletionRunnable = new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    for ( final ResourceEntryHeader resourceEntryHeader : toRemove ) {
                                        resourceAdmin.deleteResourceEntry( resourceEntryHeader.getGoid() );
                                    }
                                } catch ( ObjectModelException e ) {
                                    logger.log( Level.WARNING, "Error deleting resource entry.", e );
                                    showErrorMessage( "Error Deleting Resource", "Unable to delete resource entry:\n" + ExceptionUtils.getMessage(e) );
                                }
                            }
                        };

                        final int useCount = resourceAdmin.countRegisteredSchemas( resourceGoids );
                        if ( useCount > 0 ) {
                            final String usageConfirmationMessage;
                            if ( resourceGoids.size() == 1 ) {
                                usageConfirmationMessage =
                                        "The selected resource is currently used from policies\n" +
                                        "or registered for hardware use.\n\n" + toRemove.get( 0 ).getUri() +
                                        "\n\nReally delete selected resource?";
                            } else {
                                usageConfirmationMessage = useCount + " of " +resourceGoids.size()+
                                        " selected resources are currently used from\npolicies or registered for hardware use." +
                                        "\n\nReally delete selected resources?";
                            }

                            doRemoveWithConfirmation( usageConfirmationMessage, true, deletionRunnable );
                        } else { // do delete without further interaction
                            deletionRunnable.run();
                        }
                    } catch ( ObjectModelException e ) {
                        logger.log( Level.WARNING, "Error deleting resource entry.", e );
                        showErrorMessage( "Error Deleting Resource", "Unable to delete resource entry:\n" + ExceptionUtils.getMessage(e) );
                    }

                    loadResources();
                }
            } );
        }
    }

    private Object getMessageObject( final String message ) {
        final int width = Utilities.computeStringWidth(this.getFontMetrics(this.getFont()), message);
        final Object messageObject;
        if ( width > 600 ) {
            messageObject = Utilities.getTextDisplayComponent(message, 600, 100, -1, -1);
        } else {
            messageObject = message;
        }

        return messageObject;
    }

    private void doRemoveWithConfirmation( final String message, final boolean warning, final Runnable action ) {
        final Object[] options = {"Remove", "Cancel"};

        DialogDisplayer.showOptionDialog(
                this,
                getMessageObject(message),
                "Confirm Resource Deletion",
                JOptionPane.OK_CANCEL_OPTION,
                warning ? JOptionPane.WARNING_MESSAGE : JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[1],
                new  DialogDisplayer.OptionListener(){
            @Override
            public void reportResult( final int option ) {
                if ( option == JOptionPane.OK_OPTION ) {
                    action.run();
                }
            }
        } );
    }

    private void showErrorMessage( final String title, final String message ) {
        DialogDisplayer.showMessageDialog(
                GlobalResourcesDialog.this,
                getMessageObject(message),
                title,
                JOptionPane.ERROR_MESSAGE,
                null);
    }

    private void doImport() {
        int[] selectedRows = resourcesTable.getSelectedRows();

        final java.util.List<ResourceEntryHeader> initialImportSources = new ArrayList<ResourceEntryHeader>();
        for ( final int selectedRow : selectedRows ) {
            initialImportSources.add( resourcesTableModel.getRowObject( resourcesTable.convertRowIndexToModel( selectedRow )) );
        }

        DialogDisplayer.display( new GlobalResourceImportWizard( this, initialImportSources, resourceAdmin ), new Runnable(){
            @Override
            public void run() {
                loadResources();
            }
        } );
    }

    private void doAnalyze() {
        int[] selectedRows = resourcesTable.getSelectedRows();

        final java.util.List<ResourceEntryHeader> toAnalyze = new ArrayList<ResourceEntryHeader>();
        for ( final int selectedRow : selectedRows ) {
            toAnalyze.add( resourcesTableModel.getRowObject( resourcesTable.convertRowIndexToModel( selectedRow )) );
        }

        DialogDisplayer.display( new GlobalResourcesAnalyzeDialog( this, resourceAdmin, toAnalyze ), new Runnable(){
            @Override
            public void run() {
                loadResources();
            }
        } );
    }

    private void createUIComponents() {
        resourcesTable = new JTable(){
            @Override
            public Component prepareRenderer( final TableCellRenderer renderer, final int row, final int column ) {
                final Component component = super.prepareRenderer( renderer, row, column );

                if ( component instanceof JComponent ) {
                    final JComponent jcomponent = (JComponent) component;
                    final int modelRow = resourcesTable.convertRowIndexToModel( row );
                    final int modelCol = resourcesTable.convertColumnIndexToModel( column );

                    String tooltip = null;
                    if ( modelCol < 2 ) {
                        tooltip = resourcesTableModel.getValueAt( modelRow, modelCol ).toString();
                    }

                    jcomponent.setToolTipText( tooltip );
                }

                return component;
            }
        };
    }
}
