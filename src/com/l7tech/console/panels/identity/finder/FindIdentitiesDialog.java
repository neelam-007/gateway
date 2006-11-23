package com.l7tech.console.panels.identity.finder;

import com.l7tech.common.gui.util.Utilities;
import com.l7tech.common.gui.util.DialogDisplayer;
import com.l7tech.common.security.rbac.AttemptedDeleteSpecific;
import static com.l7tech.common.security.rbac.EntityType.ID_PROVIDER_CONFIG;
import com.l7tech.console.action.*;
import com.l7tech.console.event.EntityEvent;
import com.l7tech.console.event.EntityListenerAdapter;
import com.l7tech.console.logging.ErrorManager;
import com.l7tech.console.panels.*;
import com.l7tech.console.table.DynamicTableModel;
import com.l7tech.console.tree.AbstractTreeNode;
import com.l7tech.console.tree.EntityHeaderNode;
import com.l7tech.console.tree.TreeNodeFactory;
import com.l7tech.console.tree.UserNode;
import com.l7tech.console.util.Registry;
import com.l7tech.identity.IdentityAdmin;
import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.identity.IdentityProviderType;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.IdentityHeader;

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

/**
 * Find Dialog
 * 
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.2
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


    private JPanel mainPanel = null;
    private JTextField nameField = null;
    private JComboBox providersComboBox = null;
    private JComboBox searchNameOptions = null;
    private JComboBox searchType = null;

    private JTable searchResultTable = new JTable();
    private JPanel searchResultPanel = new JPanel();

    /**
     * cancle search
     */
    private JButton stopSearchButton = null;
    /**
     * find button
     */
    private JButton findButton = null;

    /**
     * result counter label
     */
    final JLabel resultCounter = new JLabel();


    private DynamicTableModel tableModel = null;
    private JTabbedPane searchTabs = new JTabbedPane();
    private Dimension origDimension = null;

    /**
     * the search info with search expression and parameters
     */
    private SearchInfo searchInfo = new SearchInfo();
    private DefaultComboBoxModel providersComboBoxModel;
    IdentityHeader[] selections = new IdentityHeader[]{};

    private Options options = new Options();
    private DeleteEntityAction deleteIdAction;
    private final PermissionFlags idpFlags;


    /**
     * Creates new FindDialog for a given context
     * 
     * @param parent the Frame from which the dialog is displayed
     * @param modal  true for a modal dialog, false for one that
     *               allows others windows to be active at the
     *               same time
     * @see javax.swing.JDialog
     */
    public FindIdentitiesDialog(Frame parent, boolean modal) {
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
    public FindIdentitiesDialog(Frame parent, boolean modal, Options options) {
        super(parent, modal);
        idpFlags = PermissionFlags.get(ID_PROVIDER_CONFIG);
        if (options == null) {
            throw new IllegalArgumentException();
        }
        this.options = options;
        initResources();
        initComponents();
    }


    /**
     * bring up the find form and return the selected
     */
    public FindResult showDialog() {
        setVisible(true);
        IdentityProviderConfig ipc = (IdentityProviderConfig)providersComboBoxModel.getSelectedItem();
        return ipc == null ? null : new FindResult(ipc.getOid(), selections);
    }

    public static class FindResult {
        public FindResult(long ipc, IdentityHeader[] headers) {
            this.providerConfigOid = ipc;
            this.entityHeaders = headers;
        }

        public long providerConfigOid;
        public IdentityHeader[] entityHeaders;
    }

    /**
     * Get/access the search result table. This is to allow
     * access to set certain table properties, such as single/mutliple
     * selection model etc.
     * <strong>note that the form table instance is accessed directly</strong>
     *
     * @return the search result yable
     */
    public JTable getSearchResultTable() {
        return searchResultTable;
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
        searchResultTable.setSelectionMode(options.selectionMode);
        // Set properties on the dialog, itself
        setTitle(resources.getString("dialog.label"));
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent event) {
                // user hit window manager close button
                windowAction(CMD_CANCEL);
            }
        });
        Utilities.setEscKeyStrokeDisposes(this);
        addComponentListener(new ComponentAdapter() {
            /**
             * Invoked when the component has been made visible.
             */
            public void componentShown(ComponentEvent e) {
                origDimension = FindIdentitiesDialog.this.getSize();
            }
        });

        // accessibility - all frames, dialogs, and applets should have
        // a description
        getAccessibleContext().
          setAccessibleDescription(resources.getString("dialog.description"));

        // table properties
        searchResultTable.setDefaultRenderer(Object.class, tableRenderer);
        searchResultTable.setShowGrid(false);
        searchResultTable.sizeColumnsToFit(0);
        mainPanel = new JPanel();
        Container contents = mainPanel;
        contents.setLayout(new GridBagLayout());

        // "description" label
        JLabel descLabel = new JLabel();
        descLabel.setText(resources.getString("dialog.description"));
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
//    constraints.weightx = 1.0;
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.fill = GridBagConstraints.NONE;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(12, 12, 0, 0);
        contents.add(descLabel, constraints);

        searchTabs.addTab("Details", getGeneralSearchPanel());
        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 1;
        constraints.weightx = 1.0;
        constraints.gridheight = 2;
        constraints.gridwidth = 4;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(12, 12, 5, 0);
        contents.add(searchTabs, constraints);


        constraints = new GridBagConstraints();
        constraints.gridx = 4;
        constraints.gridy = 2;
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.fill = GridBagConstraints.NONE;
        constraints.anchor = GridBagConstraints.EAST;
        constraints.insets = new Insets(12, 12, 0, 11);
        contents.add(getSearchButtonPanel(), constraints);

        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 4;
        constraints.weightx = 1.0;
        constraints.weighty = 1.0;
        constraints.gridwidth = 5;
        constraints.fill = GridBagConstraints.BOTH;
        constraints.anchor = GridBagConstraints.CENTER;
        constraints.insets = new Insets(12, 12, 5, 12);

        JPanel p = getSearchResultPanel();
        contents.add(p, constraints);
        getContentPane().add(mainPanel);
    } // initComponents()


    /**
     * Returns the general search panel
     */
    private JPanel getGeneralSearchPanel() {
        // Build the contents
        JPanel gSearchPanel = new JPanel();
        gSearchPanel.setLayout(new GridBagLayout());

        // "from" label
        JLabel selectProviderLabel = new JLabel();
        selectProviderLabel.setDisplayedMnemonic(resources.getString("selectProviderText.mnemonic").charAt(0));
        selectProviderLabel.setText(resources.getString("selectProviderText.label"));

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 1;
        constraints.fill = GridBagConstraints.NONE;
        constraints.anchor = GridBagConstraints.EAST;
        constraints.insets = new Insets(12, 12, 0, 0);
        gSearchPanel.add(selectProviderLabel, constraints);

        // provider to search

        providersComboBox = new JComboBox();
        providersComboBox.setModel(getProvidersComboBoxModel());
        providersComboBox.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                if (searchResultTable.getModel() != null && searchResultTable.getModel() instanceof DynamicTableModel) {
                    DynamicTableModel model = (DynamicTableModel)searchResultTable.getModel();
                    try {
                        model.clear();
                    } catch (InterruptedException e1) {
                        logger.log(Level.SEVERE, "unexpected interruption", e);
                    }
                }
            }
        });
        providersComboBox.setRenderer(new DefaultListCellRenderer() {
            public Component getListCellRendererComponent(JList list,
                                                          Object value,
                                                          int index,
                                                          boolean isSelected,
                                                          boolean cellHasFocus) {
                Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                IdentityProviderConfig ipc = (IdentityProviderConfig)value;
                if (ipc == null) {
                    logger.severe("null IdentityProviderConfig in list");
                    setText("<null Identity Provider>");
                } else {
                    setText(ipc.getName());
                }
                return c;
            }
        });
        final ComboBoxModel cbModel = providersComboBox.getModel();
        int size = cbModel.getSize();
        for (int i = 0; i < size; i++) {
            IdentityProviderConfig ipc = (IdentityProviderConfig)cbModel.getElementAt(i);
            if (options.initialProviderOid == ipc.getOid()) {
                cbModel.setSelectedItem(ipc);
                break;
            }
        }

        providersComboBox.setToolTipText(resources.getString("selectProviderText.tooltip"));
        constraints = new GridBagConstraints();
        constraints.gridx = 1;
        constraints.gridy = 1;
        constraints.gridwidth = 2;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(12, 12, 0, 12);
        gSearchPanel.add(providersComboBox, constraints);

        constraints = new GridBagConstraints();
        constraints.gridx = 2;
        constraints.gridy = 1;
        constraints.gridwidth = 1;
        constraints.weightx = 1.0;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.anchor = GridBagConstraints.WEST;
        gSearchPanel.add(Box.createHorizontalGlue(), constraints);

        // "type" label
        JLabel typeLabel = new JLabel();
        typeLabel.setDisplayedMnemonic(resources.getString("findType.mnemonic").charAt(0));
        typeLabel.setText(resources.getString("findType.label"));

        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 2;
        constraints.anchor = GridBagConstraints.EAST;
        constraints.insets = new Insets(12, 12, 0, 0);
        gSearchPanel.add(typeLabel, constraints);

        if (options.getSearchType() != SearchType.ALL) {
            searchType = new JComboBox(new Object[]{options.getSearchType().getName()});
            searchType.setEditable(false);
        }
        else
            searchType = new JComboBox(new Vector<String>(oTypes.keySet()));

        searchType.setToolTipText(resources.getString("findType.tooltip"));
        constraints = new GridBagConstraints();
        constraints.gridx = 1;
        constraints.gridy = 2;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.NONE;
        constraints.insets = new Insets(12, 12, 0, 0);
        gSearchPanel.add(searchType, constraints);



        // "find name" label
        JLabel findLabel = new JLabel();
        findLabel.setDisplayedMnemonic(resources.getString("findText.mnemonic").charAt(0));
        findLabel.setText(resources.getString("findText.label"));

        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 3;
        constraints.anchor = GridBagConstraints.EAST;
        constraints.insets = new Insets(12, 12, 5, 0);
        gSearchPanel.add(findLabel, constraints);

        searchNameOptions = new JComboBox(new String[] {NAME_SEARCH_OPTION_EQUALS, NAME_SEARCH_OPTION_STARTS});
        constraints = new GridBagConstraints();
        constraints.gridx = 1;
        constraints.gridy = 3;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.NONE;
        constraints.insets = new Insets(12, 12, 5, 0);
        gSearchPanel.add(searchNameOptions, constraints);

        // text field for name to be found
        nameField = new JTextField(15);
        nameField.setToolTipText(resources.getString("findText.tooltip"));

        constraints = new GridBagConstraints();
        constraints.gridx = 2;
        constraints.gridy = 3;
        constraints.gridwidth = 2;
        constraints.weightx = 1.0;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(12, 12, 5, 12);
        gSearchPanel.add(nameField, constraints);


        return gSearchPanel;
    }

    /**
     * @return the panel containing the main search
     *         button controls
     */
    private JPanel getSearchButtonPanel() {
        JButton closeButton;
        // Button panel at the bottom
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.Y_AXIS));
        // find button
        findButton = new JButton();
        findButton.setText(resources.getString("findButton.label"));
        findButton.setActionCommand(CMD_FIND);
        findButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                windowAction(event.getActionCommand());
            }
        });
        buttonPanel.add(findButton);
        // space
        buttonPanel.add(Box.createRigidArea(new Dimension(5, 5)));

        // cancel search button
        stopSearchButton = new JButton();
        stopSearchButton.setText(resources.getString("stopSearchButton.label"));
        stopSearchButton.setToolTipText(resources.getString("stopSearchButton.tooltip"));
        stopSearchButton.setEnabled(false);

        stopSearchButton.
          addActionListener(new ActionListener() {
              public void actionPerformed(ActionEvent event) {
                  stopLoadingTableModel();
              }
          });
        buttonPanel.add(stopSearchButton);

        // space
        buttonPanel.add(Box.createRigidArea(new Dimension(5, 5)));

        // close button
        closeButton = new JButton();
        closeButton.setText(resources.getString("closeButton.label"));
        closeButton.setActionCommand(CMD_CLOSE);
        closeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                windowAction(event.getActionCommand());
            }
        });

        buttonPanel.add(closeButton);


        JButton[] buttons
          = new JButton[]{findButton, stopSearchButton, closeButton};

        Utilities.equalizeButtonSizes(buttons);

        // Set up the default button for the dialog
        getRootPane().setDefaultButton(findButton);

        return buttonPanel;

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
            if (collectFormData()) {
                showSearchResult(searchInfo);
            }

            return;
        }
        setVisible(false);
        // in canse closed and still searching
        stopLoadingTableModel();
        dispose();
    }

    private boolean collectFormData() {
        String name = nameField.getText();
        String option = (String)searchNameOptions.getSelectedItem();
        searchInfo.setSearchName(name, NAME_SEARCH_OPTION_EQUALS.equals(option));
        String key = (String)searchType.getSelectedItem();
        searchInfo.setSearchType(oTypes.get(key));
        searchInfo.setProviderConfig((IdentityProviderConfig)providersComboBox.getSelectedItem());

        return true;
    }


    private IdentityAdmin getIdentityAdmin() {
        return Registry.getDefault().getIdentityAdmin();
    }

    /**
     * show the result table. Display and layout the
     * table if required.
     */
    private void showSearchResult(SearchInfo info) {
        final EntityType[] types = info.getSearchType().getTypes();
        String searchName = info.getSearchName();
        if (!info.isExactName()) {
            searchName += "*";
        }
        if ("".equals(searchName)) {
            searchName = "*";
        }
        try {
            final EntityHeader[] headers =
                    getIdentityAdmin().searchIdentities( searchInfo.getProviderConfig().getOid(),
                                                         types, searchName);
            setTableModel(Collections.enumeration(Arrays.asList(headers)));
        } catch (Exception e) {
            setTableModel(Collections.enumeration(Collections.emptyList()));
            ErrorManager.getDefault().
              notify(Level.WARNING, e, "The system reported an error during search.");
        }

        if (!searchResultPanel.isVisible()) {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    searchResultPanel.setVisible(true);
                    Dimension d = getSize();
                    setSize(d.width, origDimension.height * 3);
                    mainPanel.revalidate();
                    mainPanel.repaint();
                }
            });
        }
    }

    /**
     * display and layout the search result table.
     * The table is displayed after the first search.
     */
    private JPanel getSearchResultPanel() {
        searchResultPanel.setLayout(new BorderLayout());
        searchResultPanel.add(new JLabel("Search Results:"), BorderLayout.NORTH);
        JScrollPane scrollPane = new JScrollPane(searchResultTable);
        scrollPane.getViewport().setBackground(searchResultTable.getBackground());

        searchResultPanel.add(scrollPane, BorderLayout.CENTER);

        // Button panel at the bottom
        JPanel buttonPanel = new JPanel();
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
        // result counter
        buttonPanel.add(resultCounter);

        buttonPanel.add(Box.createGlue());

        final JButton selectButton = new JButton();
        selectButton.setText(resources.getString("selectButton.label"));
        selectButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                int rows[] = searchResultTable.getSelectedRows();
                List<EntityHeader> principals = new ArrayList<EntityHeader>();
                for (int i = 0; rows != null && i < rows.length; i++) {
                    int row = rows[i];
                    EntityHeader eh = (EntityHeader)searchResultTable.getModel().getValueAt(row, 0);
                    principals.add(eh);
                }
                if (options.isDisposeOnSelect()) {
                    selections = principals.toArray(new IdentityHeader[]{});
                    FindIdentitiesDialog.this.dispose();
                } else {
                    int row = searchResultTable.getSelectedRow();
                    if (row == -1) return;
                    Object o = searchResultTable.getModel().getValueAt(row, 0);
                    if (o == null) return;
                    if (options.enableOpenProperties) {
                      showEntityDialog(o);
                    }
                }
            }
        });
        buttonPanel.add(selectButton);
        selectButton.setEnabled(false);

        final JButton deleteButton = new JButton(getDeleteAction());
        deleteButton.setText(resources.getString("deleteButton.label"));
        /*deleteButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                int rows[] = searchResultTable.getSelectedRows();
                for (int i = 0; rows != null && i < rows.length; i++) {
                    int row = rows[i];
                    EntityHeader eh = (EntityHeader)searchResultTable.getModel().getValueAt(row, 0);
                    deleteEntity(eh, row);
                }
            }
        });*/

        if (options.enableDeleteAction) {
            buttonPanel.add(deleteButton);
        }
        deleteButton.setEnabled(false);


        searchResultTable.getSelectionModel().
          addListSelectionListener(new ListSelectionListener() {
              /**
               * Called whenever the value of the selection changes.
               * 
               * @param e the event that characterizes the change.
               */
              public void valueChanged(ListSelectionEvent e) {
                  int row = searchResultTable.getSelectedRow();
                  boolean hasUpdateIpPermissions = idpFlags.canUpdateSome();

                  if (row == -1) {
                      selectButton.setEnabled(false);
                      deleteButton.setEnabled(false);
                  } else {
                      EntityHeader eh = (EntityHeader)searchResultTable.getModel().getValueAt(row, 0);
                      if (eh.getType() != EntityType.USER && eh.getType() !=EntityType.GROUP &&
                          eh.getType() != EntityType.ID_PROVIDER_CONFIG) {
                        selectButton.setEnabled(false);
                        deleteButton.setEnabled(false);

                      } else {
                          selectButton.setEnabled(true);
                          IdentityProviderConfig ipc = (IdentityProviderConfig)providersComboBox.getSelectedItem();

                          deleteButton.setEnabled(
                                    getDeleteAction().isAuthorized() &&
                                    ipc.isWritable() &&
                                    hasUpdateIpPermissions);
                      }
                  }
              }
          });

        searchResultTable.
          addMouseListener(new MouseAdapter() {
              public void mouseClicked(MouseEvent e) {
                  if (e.getClickCount() == 2) {
                      int row = searchResultTable.getSelectedRow();
                      if (row == -1) return;
                      Object o = searchResultTable.getModel().getValueAt(row, 0);
                      if (o == null) return;
                      if (options.enableOpenProperties) {
                        showEntityDialog(o);
                      }
                  }
              }
          });

        searchResultTable.
          addKeyListener(new KeyAdapter() {
              /**
               * Invoked when a key has been pressed.
               */
              public void keyPressed(KeyEvent e) {
                  int row = searchResultTable.getSelectedRow();
                  if (row == -1) return;
                  EntityHeader eh = (EntityHeader)searchResultTable.getValueAt(row, 0);

                  int keyCode = e.getKeyCode();
                  if (keyCode == KeyEvent.VK_ENTER && options.enableOpenProperties) {
                      showEntityDialog(eh);
                  } else if (keyCode == KeyEvent.VK_DELETE && options.enableDeleteAction) {
                      if (getDeleteAction().isAuthorized()) {
                          getDeleteAction().invoke();
                      }
                      //deleteEntity(eh, row);
                  }
              }
          });
        // space
        buttonPanel.add(Box.createRigidArea(new Dimension(5, 0)));

        // cancel button
        JButton newSearchButton = new JButton();
        newSearchButton.setText(resources.getString("newSearchButton.label"));
        newSearchButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                // reset search info
                if (tableModel != null) {
                    try {
                        tableModel.clear();
                    } catch (InterruptedException e) {
                        //swallow
                    }
                }
                searchInfo = new SearchInfo();
                resultCounter.setText("");
            }
        });
        buttonPanel.add(newSearchButton);
        buttonPanel.add(Box.createGlue());

        searchResultPanel.add(buttonPanel, BorderLayout.SOUTH);

        JButton[] buttons
          = new JButton[]{newSearchButton, deleteButton, selectButton};

        Utilities.equalizeButtonSizes(buttons);

        return searchResultPanel;
    }

    /**
     * set the <CODE>TableModel</CODE> that is used by this
     * object browser instance.
     */
    private void setTableModel(Enumeration e) {
        stopLoadingTableModel();

        DynamicTableModel.
          ObjectRowAdapter oa =
          new DynamicTableModel.ObjectRowAdapter() {
              public Object getValue(Object o, int col) {
                  String text;
                  if (o instanceof EntityHeader) {
                      EntityHeader eh = (EntityHeader)o;
                      if (col == 1) {
                          text = eh.getDescription();
                      } else {
                          return eh;
                      }
                  } else {
                      throw new
                        IllegalArgumentException("Invalid argument type: "
                        + "\nExpected: EntityHeader"
                        + "\nReceived: " + o.getClass().getName());
                  }
                  if (text == null) {
                      text = "";
                  }
                  return text;
              }
          };

        String columns[] =
          new String[]{"Name", "Description"};

        tableModel = new DynamicTableModel(e, columns.length, columns, oa);
        searchResultTable.setModel(tableModel);

        tableModel.
          addTableModelListener(new TableModelListener() {
              int counter = 0;

              /**
               * This fine grain notification tells listeners the exact range
               * of cells, rows, or columns that changed.
               */
              public void tableChanged(TableModelEvent e) {
                  if (e.getType() == TableModelEvent.INSERT) {
                      counter += e.getLastRow() - e.getFirstRow();
                      resultCounter.setText("[ " + counter + " objects found]");
                      findButton.setEnabled(true);
                      stopSearchButton.setEnabled(false);
                  }
              }
          });
        findButton.setEnabled(false);
        stopSearchButton.setEnabled(true);
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                try {
                    tableModel.start();
                } catch (InterruptedException e) {
                    // swallow
                }
            }
        });
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
            findButton.setEnabled(true);
            stopSearchButton.setEnabled(false);
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
                an = TreeNodeFactory.asTreeNode(eh);
            } catch (IllegalArgumentException e) {
                logger.log(Level.FINE, "entity does not resolve an AbstractTreeNode", e);
                return;
            }
            BaseAction action = (BaseAction)an.getPreferredAction();

            // null in the case of UserNode
            if (action == null) {
                if (eh.getType() == EntityType.USER) {
                    if (searchInfo.getProviderConfig().type() == IdentityProviderType.FEDERATED) {
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
                public void propertyChange(PropertyChangeEvent evt) {
                    searchResultTable.repaint();
                }
            });
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    if (a instanceof UserPropertiesAction) {
                        ((UserPropertiesAction)a).setIdProviderConfig(searchInfo.getProviderConfig());
                    } else if (a instanceof GroupPropertiesAction) {
                        ((GroupPropertiesAction)a).setIdProviderConfig(searchInfo.getProviderConfig());
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
                    config = (IdentityProviderConfig)providersComboBox.getSelectedItem();
                    for (int i = 0; rows != null && i < rows.length; i++) {
                        final int row = rows[i];
                        EntityHeader eh = (EntityHeader)searchResultTable.getModel().getValueAt(row, 0);
                        AttemptedDeleteSpecific delete = getAttemptedDelete(eh);
                        if (!canAttemptOperation(delete)) return false;
                    }
                    return true;
                }

                protected void performAction() {
                    final IdentityProviderConfig ip = (IdentityProviderConfig)providersComboBox.getSelectedItem();
                    final int rows[] = searchResultTable.getSelectedRows();
                    for (int i = 0; rows != null && i < rows.length; i++) {
                        final int row = rows[i];
                        EntityHeader eh = (EntityHeader)searchResultTable.getModel().getValueAt(row, 0);
                        EntityHeaderNode an = (EntityHeaderNode)TreeNodeFactory.asTreeNode(eh);
                        setNode(an);
                        setConfig(ip);
                        final EntityListenerAdapter listener = new EntityListenerAdapter() {
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
    private final TableCellRenderer
      tableRenderer = new DefaultTableCellRenderer() {
          Icon groupIcon = new ImageIcon(Utilities.loadImage(GroupPanel.GROUP_ICON_RESOURCE));
          Icon userIcon = new ImageIcon(Utilities.loadImage(UserPanel.USER_ICON_RESOURCE));
          Icon stopIcon = new ImageIcon(Utilities.loadImage("com/l7tech/console/resources/Stop16.gif"));

          /**
           * Returns the default table cell renderer.
           */
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
              if (value instanceof EntityHeader) {
                  EntityHeader eh = (EntityHeader)value;
                  if (EntityType.USER.equals(eh.getType()))
                      setIcon(userIcon);
                  else if (EntityType.GROUP.equals(eh.getType())) {
                      setIcon(groupIcon);
                  } else if (EntityType.MAXED_OUT_SEARCH_RESULT.equals(eh.getType())) {
                      setIcon(stopIcon);
                  }
                  setText(eh.getName());
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

        providersComboBoxModel = new DefaultComboBoxModel();
        try {
            final IdentityAdmin admin = Registry.getDefault().getIdentityAdmin();
            EntityHeader[] headers =
              admin.findAllIdentityProviderConfig();
            for (EntityHeader header : headers) {
                IdentityProviderConfig config = admin.findIdentityProviderConfigByID(header.getOid());
                if (config == null) {
                    logger.warning("IdentityProviderConfig #" + header.getOid() + " no longer exists");
                } else {
                    if (config.isAdminEnabled() || !options.isAdminOnly()) {
                        providersComboBoxModel.addElement(config);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return providersComboBoxModel;
    }


    /**
     * the class instances keep the search info.
     */
    private class SearchInfo {
        IdentityProviderConfig providerConfig;
        String searchName;
        boolean exactName;
        SearchType searchType;

        /**
         * set the search by name and equals/contains.
         */
        void setSearchName(String name, boolean b) {
            searchName = name;
            exactName = b;
        }

        public IdentityProviderConfig getProviderConfig() {
            return providerConfig;
        }

        public void setProviderConfig(IdentityProviderConfig config) {
            this.providerConfig = config;
        }

        public String getSearchName() {
            return searchName;
        }

        public void setSearchName(String searchName) {
            this.searchName = searchName;
        }

        public boolean isExactName() {
            return exactName;
        }

        public SearchType getSearchType() {
            return searchType;
        }

        public void setSearchType(SearchType searchType) {
            this.searchType = searchType;
        }
    }

}



