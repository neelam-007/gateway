package com.l7tech.console.panels.identity.finder;

import com.l7tech.console.action.*;
import com.l7tech.console.event.EntityEvent;
import com.l7tech.console.event.EntityListenerAdapter;
import com.l7tech.console.logging.ErrorManager;
import com.l7tech.console.panels.EditorDialog;
import com.l7tech.console.panels.EntityEditorPanel;
import com.l7tech.console.panels.GroupPanel;
import com.l7tech.console.panels.UserPanel;
import com.l7tech.console.table.DynamicTableModel;
import com.l7tech.console.tree.AbstractTreeNode;
import com.l7tech.console.tree.EntityHeaderNode;
import com.l7tech.console.tree.TreeNodeFactory;
import com.l7tech.console.tree.UserNode;
import com.l7tech.console.util.LimitExceededMarkerIdentityHeader;
import com.l7tech.console.util.Registry;
import com.l7tech.gateway.common.admin.IdentityAdmin;
import com.l7tech.gateway.common.security.rbac.AttemptedDeleteSpecific;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.Utilities;
import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.identity.IdentityProviderType;
import com.l7tech.objectmodel.*;
import com.l7tech.util.Functions.Unary;
import com.l7tech.util.Option;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.*;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.l7tech.util.Functions.map;
import static com.l7tech.util.Option.none;
import static com.l7tech.util.Option.some;

/**
 * Find Dialog
 *
 * @author Emil Marceta
 */
public class FindIdentitiesDialog extends JDialog {
    final Logger logger = Logger.getLogger(FindIdentitiesDialog.class.getName());
    static Map<String, SearchType> oTypes = new TreeMap<String, SearchType>(); // sorted

    static {
        oTypes.put(SearchType.USER.getName(), SearchType.USER);
        oTypes.put(SearchType.GROUP.getName(), SearchType.GROUP);
        oTypes.put(SearchType.ALL.getName(), SearchType.ALL);
    }

    /**
     * Resource bundle with default locale
     */
    private ResourceBundle resources = null;

    /**
     * Command string for a cancel action (e.g., a button or menu item).
     */
    private String CMD_CANCEL = "cmd.cancel";

    /**
     * Command string for a close action (e.g., a button or menu item).
     */
    private String CMD_CLOSE = "cmd.close";

    /**
     * Command string for find action (e.g.,a button or menu item).
     */
    private String CMD_FIND = "cmd.find";
    /**
     * name search options
     */
    private static final String NAME_SEARCH_OPTION_EQUALS = "Equals";
    private static final String NAME_SEARCH_OPTION_STARTS = "Starts with";

    private JPanel mainPanel;
    private JTextField nameField;
    private JComboBox providersComboBox;
    private JComboBox searchNameOptions;
    private JComboBox searchType;
    private JTable searchResultTable;
    private JButton findButton;
    private JButton stopSearchButton;
    private JButton closeButton;
    private JButton selectButton;
    private JButton deleteButton;
    private JButton newSearchButton;
    private JLabel resultCounter;
    private JScrollPane scrollPane;
    private JPanel searchDetailsPanel;
    private JPanel searchResultsPanel;

    private DynamicTableModel tableModel = null;

    /**
     * the search info with search expression and parameters
     */
    private Option<SearchInfo> searchInfo = none();
    private ComboBoxModel providersComboBoxModel;
    private IdentityHeader[] selections = new IdentityHeader[]{};

    private Options options = new Options();
    private DeleteEntityAction deleteIdAction;

    /**
     * Creates new FindDialog for a given context
     *
     * @param parent the Frame from which the dialog is displayed
     * @param modal  true for a modal dialog, false for one that
     *               allows others windows to be active at the
     *               same time
     * @see javax.swing.JDialog
     */
    public FindIdentitiesDialog(Window parent, boolean modal) {
        this(parent, modal, new Options());
    }

    /**
     * Creates new FindDialog for a given context
     *                                                                         `
     * @param parent the Frame from which the dialog is displayed
     * @param modal  true for a modal dialog, false for one that
     *               allows others windows to be active at the
     *               same time
     * @see javax.swing.JDialog
     */
    public FindIdentitiesDialog(Window parent, boolean modal, Options options) {
        super(parent, modal ? ModalityType.APPLICATION_MODAL : ModalityType.MODELESS);
        if (options == null) {
            throw new IllegalArgumentException();
        }
        this.options = options;
        initResources();
        initComponents();
    }

    /**
     * Get identity search helper.
     *
     * @param options the options to use
     * @return The FindIdentities helper
     */
    public static FindIdentities getFindIdentities( final Options options,
                                                    final Collection<EntityHeader> identityProviderHeaders,
                                                    final Runnable selectionCallback ) {
        return new FindIdentities(options, identityProviderHeaders, selectionCallback);
    }

    /**
     * bring up the find form and return the selected
     */
    public FindResult showDialog() {
        setVisible(true);
        ProviderEntry providerEntry = (ProviderEntry)providersComboBoxModel.getSelectedItem();
        return providerEntry == null ? null : new FindResult(providerEntry.getOid(), selections);
    }

    public static class FindResult {
        public FindResult(long ipc, IdentityHeader[] headers) {
            this.providerConfigOid = ipc;
            this.entityHeaders = headers;
        }

        public final long providerConfigOid;
        public final IdentityHeader[] entityHeaders;
    }

    /**
     * Loads locale-specific resources: strings, images, etc
     */
    private void initResources() {
        Locale locale = Locale.getDefault();
        resources = ResourceBundle.getBundle("com.l7tech.console.resources.FindDialog", locale);
    }

    /**
     * Called from the constructor to initialize this window
     */
    private void initComponents() {
        // Set properties on the dialog, itself
        setTitle( resources.getString( "dialog.label" ) );
        addWindowListener( new WindowAdapter() {
            @Override
            public void windowClosing( WindowEvent event ) {
                // user hit window manager close button
                windowAction( CMD_CANCEL );
            }
        } );

        // accessibility - all frames, dialogs, and applets should have
        // a description
        getAccessibleContext().
          setAccessibleDescription(resources.getString("dialog.description"));

        initSearchPanel();
        initSearchButtons();
        initSearchResultPanel();

        // Set up the default button for the dialog
        getRootPane().setDefaultButton(findButton);
        setContentPane( mainPanel );
        Utilities.setEscKeyStrokeDisposes( this );
        Utilities.setMinimumSize(this);
    } // initComponents()


    /**
     * Initializes the general search panel
     */
    private void initSearchPanel() {
        providersComboBox.setModel( getProvidersComboBoxModel() );
        providersComboBox.addItemListener( new ItemListener() {
            @Override
            public void itemStateChanged( ItemEvent e ) {
                if ( searchResultTable.getModel() != null && searchResultTable.getModel() instanceof DynamicTableModel ) {
                    DynamicTableModel model = (DynamicTableModel) searchResultTable.getModel();
                    try {
                        model.clear();
                    } catch ( InterruptedException e1 ) {
                        logger.log( Level.SEVERE, "unexpected interruption", e );
                    }
                }
            }
        } );
        final ComboBoxModel cbModel = providersComboBox.getModel();
        int size = cbModel.getSize();
        for (int i = 0; i < size; i++) {
            ProviderEntry providerEntry = (ProviderEntry)cbModel.getElementAt(i);
            if (options.getInitialProviderOid() == providerEntry.getOid()) {
                cbModel.setSelectedItem(providerEntry);
                break;
            }
        }

        if ( options.getSearchType() != SearchType.ALL ) {
            searchType.setModel(new DefaultComboBoxModel(new Object[]{options.getSearchType().getName()}));
            searchType.setEditable(false);
        } else {
            searchType.setModel((Utilities.comboBoxModel(oTypes.keySet())));
        }

        searchNameOptions.setModel(new DefaultComboBoxModel(new String[] {NAME_SEARCH_OPTION_EQUALS, NAME_SEARCH_OPTION_STARTS}));
    }

    /**
     * @return the panel containing the main search
     *         button controls
     */
    private void initSearchButtons() {
        // find button
        findButton.setActionCommand(CMD_FIND);
        findButton.addActionListener( new ActionListener() {
            @Override
            public void actionPerformed( ActionEvent event ) {
                windowAction( event.getActionCommand() );
            }
        } );

        // cancel search button
        stopSearchButton.setEnabled( false );
        stopSearchButton.
          addActionListener( new ActionListener() {
              @Override
              public void actionPerformed( ActionEvent event ) {
                  stopLoadingTableModel();
              }
          } );

        // close button
        closeButton.setActionCommand(CMD_CLOSE);
        closeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                windowAction(event.getActionCommand());
            }
        });

        JButton[] buttons
          = new JButton[]{findButton, stopSearchButton, closeButton};

        Utilities.equalizeButtonSizes(buttons);
    }


    /**
     * The user has selected an option. Here we close and
     * dispose the dialog too.
     *
     * @param actionCommand may be null
     */
    private void windowAction(String actionCommand) {

        if (actionCommand == null) {
            // do nothing
        } else if (actionCommand.equals(CMD_CANCEL)) {
        } else if (actionCommand.equals(CMD_CLOSE)) {
        } else if (actionCommand.equals(CMD_FIND)) {
            doSearch();
            return;
        }
        setVisible(false);
        // in canse closed and still searching
        stopLoadingTableModel();
        dispose();
    }

    private void doSearch() {
        showSearchResult( collectFormData() );
    }

    private SearchInfo collectFormData() {
        final String name = nameField.getText();
        final String option = (String)searchNameOptions.getSelectedItem();
        final String key = (String)searchType.getSelectedItem();

        final SearchInfo info = new SearchInfo(
                (ProviderEntry)providersComboBox.getSelectedItem(),
                oTypes.get(key),
                name,
                NAME_SEARCH_OPTION_EQUALS.equals(option)
        );

        searchInfo = some(info);

        return info;
    }

    private IdentityAdmin getIdentityAdmin() {
        return Registry.getDefault().getIdentityAdmin();
    }

    /**
     * show the result table. Display and layout the
     * table if required.
     */
    private void showSearchResult( final SearchInfo info ) {
        final EntityType[] types = info.getSearchType().getTypes();
        String searchName = info.getSearchName();
        if (!info.isExactName() && !searchName.endsWith("*")) {
            searchName += "*";
        }
        if ("".equals(searchName)) {
            searchName = "*";
        }
        try {
            final Set<IdentityHeader> tableModelHeaders;
            final EntityHeaderSet<IdentityHeader> headers =
                    info.getProviderEntry() == null ? new EntityHeaderSet<IdentityHeader>() :
                    getIdentityAdmin().searchIdentities( info.getProviderEntry().getOid(), types, searchName);
            if (headers.isMaxExceeded()) {
                tableModelHeaders = new LinkedHashSet<IdentityHeader>();
                tableModelHeaders.add(new LimitExceededMarkerIdentityHeader());
                tableModelHeaders.addAll(headers);
            } else {
                tableModelHeaders = headers;
            }
            setTableModel(Collections.enumeration(tableModelHeaders), headers.size());

            //Set up tool tips for the Description cells.
            DefaultTableCellRenderer renderer = new DefaultTableCellRenderer(){
                public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                    Component comp = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                    if((comp instanceof JComponent) && (value instanceof String)) {
                       ((JComponent)comp).setToolTipText((String) value);
                    }
                    return comp;
                }
            };
            searchResultTable.getColumnModel().getColumn(2).setCellRenderer(renderer);

        } catch (Exception e) {
            setTableModel(Collections.enumeration(Collections.emptyList()), 0);
            if (e instanceof FindException && e.getCause()==null) {
                JOptionPane.showMessageDialog(this,
                  "There was an error while seaching the provider:\n" + e.getMessage(),
                  "Error searching provider",
                  JOptionPane.ERROR_MESSAGE);
            } else {
                ErrorManager.getDefault().
                    notify(Level.WARNING, e, "The system reported an error during search.");
            }
        }
    }

    /**
     * display and layout the search result table.
     * The table is displayed after the first search.
     */
    private void initSearchResultPanel() {
        selectButton.addActionListener( new ActionListener() {
            @Override
            public void actionPerformed( ActionEvent event ) {
                List<IdentityHeader> principals = getSelectedIdentities();
                if ( options.isDisposeOnSelect() ) {
                    selections = principals.toArray( new IdentityHeader[principals.size()] );
                    FindIdentitiesDialog.this.dispose();
                } else {
                    int row = searchResultTable.getSelectedRow();
                    if ( row == -1 ) return;
                    Object o = searchResultTable.getModel().getValueAt( row, 0 );
                    if ( o == null ) return;
                    if ( options.isEnableOpenProperties() ) {
                        showEntityDialog( o );
                    }
                }
            }
        } );
        selectButton.setEnabled(false);

        deleteButton.setAction( getDeleteAction() );
        deleteButton.setText(resources.getString("deleteButton.label"));
        deleteButton.setToolTipText( null );
        deleteButton.setVisible(options.isEnableDeleteAction());
        deleteButton.setEnabled(false);

        scrollPane.getViewport().setBackground(searchResultTable.getBackground());

        // table properties
        searchResultTable.setDefaultRenderer(Object.class, tableRenderer);
        searchResultTable.setShowGrid( false );
        searchResultTable.sizeColumnsToFit( 0 );
        searchResultTable.getTableHeader().setReorderingAllowed( false );
        searchResultTable.setSelectionMode( options.getSelectionMode() );
        searchResultTable.getSelectionModel().addListSelectionListener( new ListSelectionListener() {
            @Override
            public void valueChanged( ListSelectionEvent e ) {
                onSearchResultsSelectionChange();
            }
        } );

        initSearchResultActions();

        // cancel button
        newSearchButton.addActionListener( new ActionListener() {
            @Override
            public void actionPerformed( ActionEvent event ) {
                // reset search info
                if ( tableModel != null ) {
                    try {
                        tableModel.clear();
                    } catch ( InterruptedException e ) {
                        //swallow
                    }
                }
                searchInfo = none();
                resultCounter.setText( "" );
            }
        } );

        JButton[] buttons
          = new JButton[]{newSearchButton, deleteButton, selectButton};

        Utilities.equalizeButtonSizes(buttons);

        resultCounter.setText( "" );
    }

    private List<IdentityHeader> getSelectedIdentities() {
        int rows[] = searchResultTable.getSelectedRows();
        List<IdentityHeader> principals = new ArrayList<IdentityHeader>();
        for ( int i = 0; rows != null && i < rows.length; i++ ) {
            int row = rows[i];
            IdentityHeader eh = (IdentityHeader) searchResultTable.getModel().getValueAt( row, 0 );
            if ( !shouldIgnoreHeader(eh) )
                principals.add( eh );
        }
        return principals;
    }

    /**
     * Add "advanced" options, not suitable for a basic user/group search.
     */
    protected void initSearchResultActions() {
        searchResultTable.addMouseListener( new MouseAdapter() {
            @Override
            public void mouseClicked( MouseEvent e ) {
                if ( e.getClickCount() == 2 ) {
                    int row = searchResultTable.getSelectedRow();
                    if ( row == -1 ) return;
                    Object o = searchResultTable.getModel().getValueAt( row, 0 );
                    if ( o == null ) return;
                    if ( options.isEnableOpenProperties() ) {
                        showEntityDialog( o );
                    }
                }
            }
        } );

        searchResultTable.addKeyListener( new KeyAdapter() {
            @Override
            public void keyPressed( KeyEvent e ) {
                int row = searchResultTable.getSelectedRow();
                if ( row == -1 ) return;
                EntityHeader eh = (EntityHeader) searchResultTable.getValueAt( row, 0 );

                int keyCode = e.getKeyCode();
                if ( keyCode == KeyEvent.VK_ENTER && options.isEnableOpenProperties() ) {
                    showEntityDialog( eh );
                } else if ( keyCode == KeyEvent.VK_DELETE && options.isEnableDeleteAction() ) {
                    if ( getDeleteAction().isAuthorized() ) {
                        getDeleteAction().invoke();
                    }
                }
            }
        } );
    }

    protected void onSearchResultsSelectionChange() {
        int row = searchResultTable.getSelectedRow();

        if (row == -1) {
            selectButton.setEnabled(false);
            deleteButton.setEnabled(false);
        } else {
            EntityHeader eh = (EntityHeader)searchResultTable.getModel().getValueAt(row, 0);
            if ( shouldIgnoreHeader(eh) ) {
              selectButton.setEnabled(false);
              deleteButton.setEnabled(false);

            } else {
                selectButton.setEnabled(true);
                ProviderEntry providerEntry = (ProviderEntry)providersComboBox.getSelectedItem();

                deleteButton.setEnabled(
                        getDeleteAction().isAuthorized() &&
                        providerEntry.isWritable() );
            }
        }
    }

    private boolean shouldIgnoreHeader( final EntityHeader  eh ) {
        return eh instanceof LimitExceededMarkerIdentityHeader ||
                (eh.getType() != EntityType.USER && eh.getType() !=EntityType.GROUP &&
                 eh.getType() != EntityType.ID_PROVIDER_CONFIG);
    }

    /**
     * set the <CODE>TableModel</CODE> that is used by this
     * object browser instance.
     */
    private void setTableModel(Enumeration e, final int numEntries) {
        stopLoadingTableModel();

        DynamicTableModel.ObjectRowAdapter oa =
          new DynamicTableModel.ObjectRowAdapter() {
              @Override
              public Object getValue(Object o, int col) {
                  String text;
                  if(o instanceof IdentityHeader){
                      IdentityHeader ih = (IdentityHeader)o;
                      if(col == 2){
                          text = ih.getDescription();
                      }else if(col == 1){
                          if(EntityType.USER.equals(ih.getType())){
                           text = ih.getName();
                          }else{
                              text = ""; //userid/login doesn't make sense for group
                          }
                      }else{
                          return ih;
                      }
                  }
                  else {
                      throw new
                        IllegalArgumentException("Invalid argument type: "
                        + "\nExpected: IdentityHeader"
                        + "\nReceived: " + o.getClass().getName());
                  }
                  if (text == null) {
                      text = "";
                  }
                  return text;
              }
          };

        String columns[] = new String[]{"Name", "Login", "Description"};

        tableModel = new DynamicTableModel(e, columns.length, columns, oa);
        searchResultTable.setModel( tableModel );

        tableModel.
          addTableModelListener( new TableModelListener() {
              int counter = 0;

              /**
               * This fine grain notification tells listeners the exact range
               * of cells, rows, or columns that changed.
               */
              @Override
              public void tableChanged( TableModelEvent e ) {
                  if ( e.getType() == TableModelEvent.INSERT ) {
                      counter = numEntries;
                      resultCounter.setText( "[ " + counter + " objects found]" );
                      findButton.setEnabled( true );
                      stopSearchButton.setEnabled( false );
                  }
              }
          } );
        searchInProgress( true );
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                try {
                    tableModel.start();
                } catch (InterruptedException e) {
                    // swallow
                }
            }
        });
    }

    protected void searchInProgress( final boolean searching ) {
        findButton.setEnabled(!searching);
        stopSearchButton.setEnabled(searching);
    }

    /**
     * stop loading the table model. The method does nothing
     * if the model has not been created yet.
     */
    private void stopLoadingTableModel() {
        if (tableModel != null) {
            try {
                tableModel.stop();
            } catch (InterruptedException e) {
                // swallow
            }
            searchInProgress( false );
        }
    }

    /**
     * instantiate the dialog for given AbstractTreeNode
     *
     * @param o the <CODE>Object</CODE> Expected entity header
     */
    private void showEntityDialog(Object o) {
        EntityEditorPanel panel = null;

        if (o instanceof EntityHeader) {
            EntityHeader eh = (EntityHeader)o;
            AbstractTreeNode an;
            try {
                an = TreeNodeFactory.asTreeNode(eh, null);
            } catch (IllegalArgumentException e) {
                logger.log(Level.FINE, "entity does not resolve an AbstractTreeNode", e);
                return;
            }
            BaseAction action = (BaseAction)an.getPreferredAction();

            // null in the case of UserNode
            if (action == null) {
                if (eh.getType() == EntityType.USER) {
                    if ( searchInfo.isSome() && searchInfo.some().getProviderEntry().getConfig().type() == IdentityProviderType.FEDERATED ) {
                        action = new FederatedUserPropertiesAction((UserNode) an);
                    } else {
                        action = new GenericUserPropertiesAction((UserNode) an);
                    }
                }
            }

            if (action == null) {
                return;
            }

            final BaseAction a = action;
            a.addPropertyChangeListener(new PropertyChangeListener() {
                @Override
                public void propertyChange(PropertyChangeEvent evt) {
                    searchResultTable.repaint();
                }
            });
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    if ( searchInfo.isSome() ) {
                        if (a instanceof UserPropertiesAction) {
                            ((UserPropertiesAction)a).setIdProviderConfig( searchInfo.some().getProviderEntry().getConfig() );
                        } else if (a instanceof GroupPropertiesAction) {
                            ((GroupPropertiesAction)a).setIdProviderConfig(searchInfo.some().getProviderEntry().getConfig() );
                        }
                    }
                    a.invoke();
                }
            });
        }

        if (panel == null) return;
        panel.edit(o);
        JFrame f = (JFrame)SwingUtilities.windowForComponent(this);
        EditorDialog dialog = new EditorDialog(f, panel);
        dialog.pack();
        Utilities.centerOnScreen(dialog);
        DialogDisplayer.display(dialog);
    }

    private DeleteEntityAction getDeleteAction() {
        if (deleteIdAction == null) {
            deleteIdAction = new DeleteEntityAction(null, null) {
                /**
                 * @return true if the current user is authorized to delete every selected item
                 */
                @Override
                public synchronized boolean isAuthorized() {
                    final int rows[] = searchResultTable.getSelectedRows();
                    final ProviderEntry providerEntry = (ProviderEntry) providersComboBox.getSelectedItem();
                    if (providerEntry == null)
                        return false;
                    config = providerEntry.getConfig();
                    for (int i = 0; rows != null && i < rows.length; i++) {
                        final int row = rows[i];
                        EntityHeader eh = (EntityHeader)searchResultTable.getModel().getValueAt(row, 0);
                        AttemptedDeleteSpecific delete = getAttemptedDelete(eh);
                        if (!canAttemptOperation(delete)) return false;
                    }
                    return true;
                }

                @Override
                protected void performAction() {
                    final ProviderEntry providerEntry = (ProviderEntry)providersComboBox.getSelectedItem();
                    final int rows[] = searchResultTable.getSelectedRows();
                    for (int i = 0; rows != null && i < rows.length; i++) {
                        final int row = rows[i];
                        EntityHeader eh = (EntityHeader)searchResultTable.getModel().getValueAt(row, 0);
                        EntityHeaderNode an = (EntityHeaderNode)TreeNodeFactory.asTreeNode(eh, null);
                        setNode(an);
                        setConfig(providerEntry.getConfig());
                        final EntityListenerAdapter listener = new EntityListenerAdapter() {
                            @Override
                            public void entityRemoved(EntityEvent ev) {
                                tableModel.removeRow(row);
                            }
                        };
                        deleteIdAction.addEntityListener(listener);
                        super.performAction();
                        super.removeEntityListener(listener);
                    }
                }
            };

        }
        return deleteIdAction;
    }

    /**
     * the <CODE>TableCellRenderer</CODE> instance
     * for the result table
     */
    private static final TableCellRenderer
      tableRenderer = new DefaultTableCellRenderer() {
          Icon groupIcon = new ImageIcon(Utilities.loadImage(GroupPanel.GROUP_ICON_RESOURCE));
          Icon userIcon = new ImageIcon(Utilities.loadImage(UserPanel.USER_ICON_RESOURCE));
          Icon stopIcon = new ImageIcon(Utilities.loadImage("com/l7tech/console/resources/Stop16.gif"));

          /**
           * Returns the default table cell renderer.
           */
          @Override
          public Component
            getTableCellRendererComponent(JTable table,
                                          Object value,
                                          boolean iss,
                                          boolean hasFocus,
                                          int row, int column) {
              this.setFont(new Font("Dialog", Font.PLAIN, 12));
              if (iss) {
                  this.setBackground(table.getSelectionBackground());
                  this.setForeground(table.getSelectionForeground());
              } else {
                  this.setBackground(table.getBackground());
                  this.setForeground(table.getForeground());
              }
              //if (value instanceof EntityHeader) {
              if (value instanceof LimitExceededMarkerIdentityHeader) {
                  setIcon(stopIcon);
                  setText(((LimitExceededMarkerIdentityHeader)value).getName());
              } else if (value instanceof IdentityHeader) {
                  //EntityHeader ih = (EntityHeader)value;
                  IdentityHeader ih = (IdentityHeader)value;
                  if (EntityType.USER.equals(ih.getType())){
                      setIcon(userIcon);
                      String cn = ih.getCommonName();
                      if(cn == null || cn.trim().length() < 1) {
                          //if this IdentityHeader was created by casting and EntityHeader it won't have a value for cn
                          cn = ih.getName();
                      }
                      setText(cn);
                  } else if (EntityType.GROUP.equals(ih.getType())) {
                      setIcon(groupIcon);
                      setText(ih.getName());
                  }
              } else {
                  setIcon(null);
                  setText(value.toString());
              }
              return this;
          }
      };

    /**
     * @return the list of providers combo box model
     */
    private ComboBoxModel getProvidersComboBoxModel() {
        if (providersComboBoxModel != null)
            return providersComboBoxModel;

        final List<ProviderEntry> providerEntries = new ArrayList<ProviderEntry>(getIdentityProviderEntries());
        Collections.sort( providerEntries );
        providersComboBoxModel = Utilities.comboBoxModel( providerEntries );

        return providersComboBoxModel;
    }

    protected Collection<ProviderEntry> getIdentityProviderEntries() {
        Collection<ProviderEntry> identityProviderEntries = Collections.emptyList();
        try {
            final IdentityAdmin admin = Registry.getDefault().getIdentityAdmin();
            final EntityHeader[] headers = admin.findAllIdentityProviderConfig();

            if ( headers != null ) {
                identityProviderEntries = new ArrayList<ProviderEntry>();
                for ( final EntityHeader header : headers ) {
                    final IdentityProviderConfig config = admin.findIdentityProviderConfigByID(header.getOid());
                    if ( config == null ) {
                        logger.warning("IdentityProviderConfig #" + header.getOid() + " no longer exists");
                    } else {
                        if ( config.isAdminEnabled() || !options.isAdminOnly() ) {
                            identityProviderEntries.add( new ProviderEntry( config ) );
                        }
                    }
                }
            }
        } catch ( FindException fe ) {
            throw new RuntimeException("Error while loading identity provider list.", fe);
        }
        return identityProviderEntries;
    }

    private static class ProviderEntry implements Comparable<ProviderEntry> {
        private final long oid;
        private final String name;
        private final boolean writable;
        private final IdentityProviderConfig config;

        private ProviderEntry( final EntityHeader header ) {
            this.oid = header.getOid();
            this.name = header.getName();
            this.writable = false;
            this.config = null;
        }

        private ProviderEntry( final IdentityProviderConfig config ) {
            this.oid = config.getOid();
            this.name = config.getName();
            this.writable = config.isWritable();
            this.config = config;
        }

        public long getOid() {
            return oid;
        }

        public String getName() {
            return name;
        }

        public boolean isWritable() {
            return writable;
        }

        public IdentityProviderConfig getConfig() {
            return config;
        }

        public String toString() {
            return getName();
        }

        @Override
        public int compareTo( final ProviderEntry o ) {
            return getName().toLowerCase().compareTo( o.getName().toLowerCase() );
        }

        @SuppressWarnings({ "RedundantIfStatement" })
        @Override
        public boolean equals( final Object o ) {
            if ( this == o ) return true;
            if ( o == null || getClass() != o.getClass() ) return false;

            final ProviderEntry that = (ProviderEntry) o;

            if ( oid != that.oid ) return false;

            return true;
        }

        @Override
        public int hashCode() {
            return (int) (oid ^ (oid >>> 32));
        }

        private static Unary<ProviderEntry,EntityHeader> fromEntity() {
            return new Unary<ProviderEntry,EntityHeader>(){
                @Override
                public ProviderEntry call( final EntityHeader entityHeader ) {
                    return new ProviderEntry( entityHeader );
                }
            };
        }
    }

    /**
     * the class instances keep the search info.
     */
    private static class SearchInfo {
        private final ProviderEntry providerEntry;
        private final SearchType searchType;
        private final String searchName;
        private final boolean exactName;

        private SearchInfo( final ProviderEntry providerConfig,
                            final SearchType searchType,
                            final String searchName,
                            final boolean exactName ) {
            this.providerEntry = providerConfig;
            this.searchType = searchType;
            this.searchName = searchName;
            this.exactName = exactName;
        }

        public ProviderEntry getProviderEntry() {
            return providerEntry;
        }

        public SearchType getSearchType() {
            return searchType;
        }

        public String getSearchName() {
            return searchName;
        }

        public boolean isExactName() {
            return exactName;
        }
    }

    public static class FindIdentities {
        private final FindIdentitiesDialog dialog;

        private FindIdentities( final Options options,
                                final Collection<EntityHeader> identityProviderHeaders,
                                final Runnable selectionCallback ) {
            this.dialog = new FindIdentitiesDialog( null, false, options ) {
                @Override
                protected void initSearchResultActions() {
                    // disable delete, view, etc
                }
                @Override
                protected void onSearchResultsSelectionChange() {
                    selectionCallback.run();
                }

                @Override
                protected Collection<ProviderEntry> getIdentityProviderEntries() {
                    return identityProviderHeaders == null ?
                            super.getIdentityProviderEntries() :
                            map( identityProviderHeaders, ProviderEntry.fromEntity() );
                }
            };
            this.dialog.closeButton.setVisible( false );
        }

        public JPanel getSearchDetailsPanel() {
            return dialog.searchDetailsPanel;
        }

        public JPanel getSearchResultsPanel() {
            return dialog.searchResultsPanel;
        }

        public JComponent getSearchStatusComponent() {
            return dialog.resultCounter;
        }

        public Collection<IdentityHeader> getSelections() {
            return dialog.getSelectedIdentities();
        }
    }

}



