package com.l7tech.console.panels;

import com.l7tech.common.gui.util.Utilities;
import com.l7tech.console.action.*;
import com.l7tech.console.event.EntityEvent;
import com.l7tech.console.event.EntityListenerAdapter;
import com.l7tech.console.logging.ErrorManager;
import com.l7tech.console.table.DynamicTableModel;
import com.l7tech.console.tree.AbstractTreeNode;
import com.l7tech.console.tree.EntityHeaderNode;
import com.l7tech.console.tree.TreeNodeFactory;
import com.l7tech.console.util.Registry;
import com.l7tech.identity.GroupBean;
import com.l7tech.identity.IdentityProvider;
import com.l7tech.identity.IdentityProviderConfigManager;
import com.l7tech.identity.UserBean;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.*;
import java.security.Principal;
import java.util.*;
import java.util.List;
import java.util.logging.Level;

/**
 * Find Dialog
 * 
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.2
 */
public class FindIdentitiesDialog extends JDialog {
    static Map oTypes = new TreeMap(); // sorted

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
    private static final String[] NAME_SEARCH_OPTIONS = {"equals", "starts with"};


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
    Principal[] selections = new Principal[]{};

    private Options options = new Options();

    /**
     * The <code>FindIdentitiesDialog</code> Options
     * Default options are
     * <ul>
     * <li>delete disabled
     * <li>allow multiple elements selection
     * <li>initial provider id
     * </ul>
     */
    public static class Options {

        /**
         * Enable the delete action
         */
        public void enableDeleteAction() {
            enableDeleteAction = true;
        }

        /**
         * Disable the properties opening for the selected item
         * Enabled by default.
         */
        public void disableOpenProperties() {
            enableOpenProperties = false;
        }

        /**
         * Dispose on select Disabled by default.
         */
        public void disposeOnSelect() {
            disposeOnSelect = true;
        }


        /**
         * Set the selection mode. Use one of the values from
         * <code>ListSelectionModel</code>
         *
         * @param selectionMode
         * @see ListSelectionModel
         */
        public void setSelectionMode(int selectionMode) {
            this.selectionMode = selectionMode;
        }

        /**
         * Set the intial provider id whed the dialog is displayed
         *
         * @param id the provider id
         */
        public void setInitialProvider(long id) {
            this.initialProviderOid = id;
        }

        boolean enableDeleteAction = false;
        boolean enableOpenProperties = true;
        private boolean disposeOnSelect = false;
        int selectionMode = ListSelectionModel.MULTIPLE_INTERVAL_SELECTION;
        long initialProviderOid = IdentityProviderConfigManager.INTERNALPROVIDER_SPECIAL_OID;
    }


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
     *
     * @param parent the Frame from which the dialog is displayed
     * @param modal  true for a modal dialog, false for one that
     *               allows others windows to be active at the
     *               same time
     * @see javax.swing.JDialog
     */
    public FindIdentitiesDialog(Frame parent, boolean modal, Options options) {
        super(parent, modal);
        if (options == null) {
            throw new IllegalArgumentException();
        }
        this.options = options;
        initResources();
        initComponents();
    }


    /**
     * bring up the find form and return the selected
     *
     * @return
     */
    public Principal[] showDialog() {
        show();
        return selections;
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
        Actions.setEscKeyStrokeDisposes(this);
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
        Container contents = gSearchPanel;
        contents.setLayout(new GridBagLayout());

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
        contents.add(selectProviderLabel, constraints);

        // provider to search

        providersComboBox = new JComboBox();
        providersComboBox.setModel(getProvidersComboBoxModel());
        providersComboBox.setRenderer(new DefaultListCellRenderer() {
            public Component getListCellRendererComponent(JList list,
                                                          Object value,
                                                          int index,
                                                          boolean isSelected,
                                                          boolean cellHasFocus) {
                Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                IdentityProvider ip = (IdentityProvider)value;
                setText(ip.getConfig().getName());
                return c;
            }
        });
        final ComboBoxModel cbModel = providersComboBox.getModel();
        int size = cbModel.getSize();
        for (int i = 0; i < size; i++) {
            IdentityProvider ip = (IdentityProvider)cbModel.getElementAt(i);
            if (options.initialProviderOid == ip.getConfig().getOid()) {
                cbModel.setSelectedItem(ip);
                break;
            }
        }

        providersComboBox.setToolTipText(resources.getString("selectProviderText.tooltip"));
        constraints = new GridBagConstraints();
        constraints.gridx = 1;
        constraints.gridy = 1;
        constraints.gridwidth = 2;
        constraints.fill = GridBagConstraints.NONE;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(12, 12, 0, 0);
        contents.add(providersComboBox, constraints);

        constraints = new GridBagConstraints();
        constraints.gridx = 2;
        constraints.gridy = 1;
        constraints.gridwidth = 1;
        constraints.weightx = 1.0;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.anchor = GridBagConstraints.WEST;
        contents.add(Box.createHorizontalGlue(), constraints);

        // "type" label
        JLabel typeLabel = new JLabel();
        typeLabel.setDisplayedMnemonic(resources.getString("findType.mnemonic").charAt(0));
        typeLabel.setText(resources.getString("findType.label"));

        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 2;
        constraints.anchor = GridBagConstraints.EAST;
        constraints.insets = new Insets(12, 12, 0, 0);
        contents.add(typeLabel, constraints);

        searchType = new JComboBox(new Vector(oTypes.keySet()));
        searchType.setToolTipText(resources.getString("findType.tooltip"));
        constraints = new GridBagConstraints();
        constraints.gridx = 1;
        constraints.gridy = 2;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.NONE;
        constraints.insets = new Insets(12, 12, 0, 0);
        contents.add(searchType, constraints);



        // "find name" label
        JLabel findLabel = new JLabel();
        findLabel.setDisplayedMnemonic(resources.getString("findText.mnemonic").charAt(0));
        findLabel.setText(resources.getString("findText.label"));

        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 3;
        constraints.anchor = GridBagConstraints.EAST;
        constraints.insets = new Insets(12, 12, 5, 0);
        contents.add(findLabel, constraints);

        searchNameOptions = new JComboBox(NAME_SEARCH_OPTIONS);
        constraints = new GridBagConstraints();
        constraints.gridx = 1;
        constraints.gridy = 3;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.NONE;
        constraints.insets = new Insets(12, 12, 5, 0);
        contents.add(searchNameOptions, constraints);

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
        contents.add(nameField, constraints);


        return gSearchPanel;
    }

    /**
     * @return the panel containing the main search
     *         button controls
     */
    private JPanel getSearchButtonPanel() {
        JButton closeButton = null;
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
            ;
        } else if (actionCommand.equals(CMD_CLOSE)) {
            ;
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
        searchInfo.setSearchName(name, "equals".equals(option));
        String key = (String)searchType.getSelectedItem();
        searchInfo.setSearchType((SearchType)oTypes.get(key));
        searchInfo.setProvider((IdentityProvider)providersComboBox.getSelectedItem());

        return true;
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
            setTableModel(Collections.enumeration(info.getProvider().search(types, searchName)));
        } catch (Exception e) {
            setTableModel(Collections.enumeration(Collections.EMPTY_LIST));
            ErrorManager.getDefault().
              notify(Level.WARNING, e, "The system reported an error during search.");
        }

        if (!searchResultPanel.isVisible()) {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    searchResultPanel.setVisible(true);
                    Dimension d = getSize();
                    setSize(d.width, (int)(origDimension.height * 3));
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
                List principals = new ArrayList();
                for (int i = 0; rows != null && i < rows.length; i++) {
                    int row = rows[i];
                    EntityHeader eh = (EntityHeader)searchResultTable.getModel().getValueAt(row, 0);
                    principals.add(headerToPrincipal(eh));
                }
                if (options.disposeOnSelect) {
                    selections = (Principal[])principals.toArray(new Principal[]{});
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

        final JButton deleteButton = new JButton();
        deleteButton.setText(resources.getString("deleteButton.label"));
        deleteButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                int rows[] = searchResultTable.getSelectedRows();
                for (int i = 0; rows != null && i < rows.length; i++) {
                    int row = rows[i];
                    EntityHeader eh = (EntityHeader)searchResultTable.getModel().getValueAt(row, 0);
                    deleteEntity(eh, row);
                }
            }
        });

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
                  if (row == -1) {
                      selectButton.setEnabled(false);
                      deleteButton.setEnabled(false);
                  } else {
                      selectButton.setEnabled(true);
                      IdentityProvider ip = (IdentityProvider)providersComboBox.getSelectedItem();
                      deleteButton.setEnabled(!ip.isReadOnly());
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
                      deleteEntity(eh, row);
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
    private void setTableModel(Enumeration enum) {
        stopLoadingTableModel();

        DynamicTableModel.
          ObjectRowAdapter oa =
          new DynamicTableModel.ObjectRowAdapter() {
              public Object getValue(Object o, int col) {
                  String text = "";
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

        tableModel = new DynamicTableModel(enum, columns.length, columns, oa);
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
            AbstractTreeNode an = TreeNodeFactory.asTreeNode(eh);
            final BaseAction a = (BaseAction)an.getPreferredAction();
            if (a == null) {
                return;
            }
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    if (a instanceof UserPropertiesAction) {
                        ((UserPropertiesAction)a).setIdProvider(searchInfo.getProvider());
                    } else if (a instanceof GroupPropertiesAction) {
                        ((GroupPropertiesAction)a).setIdProvider(searchInfo.getProvider());
                    }
                    a.performAction();
                }
            });
        }

        if (panel == null) return;
        panel.edit(o);
        JFrame f = (JFrame)SwingUtilities.windowForComponent(this);
        EditorDialog dialog = new EditorDialog(f, panel);
        dialog.pack();
        Utilities.centerOnScreen(dialog);
        dialog.show();
    }

    /**
     * delete the given entity
     *
     * @param eh the <CODE>EntityHeader</CODE> instance
     */
    private void deleteEntity(EntityHeader eh, final int row) {
        AbstractTreeNode an = TreeNodeFactory.asTreeNode(eh);
        final IdentityProvider ip = (IdentityProvider)providersComboBox.getSelectedItem();
        DeleteEntityAction da = new DeleteEntityAction((EntityHeaderNode)an, ip);
        final EntityListenerAdapter listener = new EntityListenerAdapter() {
            public void entityRemoved(EntityEvent ev) {
                tableModel.removeRow(row);
            }
        };
        da.addEntityListener(listener);
        da.performAction();
    }


    /**
     * the <CODE>TableCellRenderer</CODE> instance
     * for the result table
     */
    private final TableCellRenderer
      tableRenderer = new DefaultTableCellRenderer() {
          Icon groupIcon = new ImageIcon(Utilities.loadImage(GroupPanel.GROUP_ICON_RESOURCE));
          Icon userIcon = new ImageIcon(Utilities.loadImage(UserPanel.USER_ICON_RESOURCE));

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
            Iterator providers =
              Registry.getDefault().getProviderConfigManager().findAllIdentityProviders().iterator();
            while (providers.hasNext()) {
                Object o = providers.next();
                providersComboBoxModel.addElement(o);
            }
        } catch (FindException e) {
            e.printStackTrace();
        }
        return providersComboBoxModel;
    }


    /**
     * the class instances keep the search info.
     */
    private class SearchInfo {
        IdentityProvider provider;
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

        public IdentityProvider getProvider() {
            return provider;
        }

        public void setProvider(IdentityProvider provider) {
            this.provider = provider;
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

    private static class SearchType {
        public static final
        SearchType USER = new SearchType("Users", new EntityType[]{EntityType.USER});
        public static final
        SearchType GROUP = new SearchType("Groups", new EntityType[]{EntityType.GROUP});
        public static final
        SearchType ALL = new SearchType("ALL", new EntityType[]{EntityType.GROUP, EntityType.USER});

        private SearchType(String name, EntityType[] t) {
            this.name = name;
            this.types = t;
        }

        public String getName() {
            return name;
        }

        public EntityType[] getTypes() {
            return types;
        }

        EntityType[] types;
        String name;
    }

    private Principal headerToPrincipal(EntityHeader eh) {
        IdentityProvider ip = (IdentityProvider)providersComboBoxModel.getSelectedItem();
        if (eh.getType() == EntityType.USER) {
            UserBean u = new UserBean();
            u.setName(eh.getName());
            u.setLogin(eh.getName());
            u.setProviderId(ip.getConfig().getOid());
            u.setUniqueIdentifier(eh.getStrId());
            return u;
        } else if (eh.getType() == EntityType.GROUP) {
            GroupBean g = new GroupBean();
            g.setName(eh.getName());
            g.setProviderId(ip.getConfig().getOid());
            g.setUniqueIdentifier(eh.getStrId());
            return g;
        }
        throw new IllegalArgumentException("Don't know anything about " + eh.getType());
    }
}



