package com.l7tech.console.panels;

import com.l7tech.console.panels.*;
import com.l7tech.console.table.DynamicTableModel;
import com.l7tech.console.tree.EntityTreeNode;
import com.l7tech.console.tree.EntityHeaderNode;
import com.l7tech.console.MainWindow;
import org.apache.log4j.Category;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.TreeMap;
import java.util.Vector;

/**
 * Find Dialog
 * @author <a href="mailto:emarceta@layer7-tech.com>Emil Marceta</a>
 * @version 1.2
 */
public class FindDialog extends JDialog {
  private final ClassLoader cl = getClass().getClassLoader();
  final Category log = Category.getInstance(FindDialog.class.getName());

  static Map oTypes = new TreeMap(); // sorted
  static {
  }
  /**
   * Resource bundle with default locale
   */
  private ResourceBundle resources = null;

  /** Command string for a cancel action (e.g., a button or menu item). */
  private String CMD_CANCEL = "cmd.cancel";

  /** Command string for a close action (e.g., a button or menu item). */
  private String CMD_CLOSE = "cmd.close";

  /** Command string for find action (e.g.,a button or menu item). */
  private String CMD_FIND = "cmd.find";
  /** name search options */
  private static final
  String[] NAME_SEARCH_OPTIONS = {"equals", "contains"};


  private JPanel mainPanel = null;
  private JButton fromButton = null;
  private JTextField nameField = null;
  private JTextField fromField = null;
  private JCheckBox fqNamesCheckBox = null;
  private JComboBox searchNameOptions = null;
  private JComboBox searchType = null;

  private JTable jTable =  new JTable();
  private JPanel searchResultPanel = new JPanel();
  private PanelListener pListener;

  /** cancle search */
  private JButton cancelSearchButton = null;

  /** result counter label */
  final JLabel resultCounter = new JLabel();


  private DynamicTableModel tableModel =  null;
  private JTabbedPane searchTabs = new JTabbedPane();
  private Dimension origDimension = null;

  /** the search context (where the search begins) */
  private EntityHeaderNode entry = null;
  /** the search info with search expression and parameters */
  private SearchInfo searchInfo = new SearchInfo();

  /**
   * Creates new FindDialog for a given context
   *
   * @param parent the Frame from which the dialog is displayed
   * @param modal  true for a modal dialog, false for one that
   *               allows others windows to be active at the
   *               same time
   * @param n      the directory tree node (start context)
   * @param broker the panel listener broker
   *
   * @see javax.swing.JDialog
   */
  public FindDialog(Frame parent, boolean modal,
                    EntityTreeNode n, PanelListenerBroker broker) {
    super(parent, modal);
    if (n == null) {
      throw new NullPointerException("node");
    }
    Object o = n.getUserObject();
    if (o instanceof EntityHeaderNode) {
      this.entry = (EntityHeaderNode)o;
    } else {
      throw new
              IllegalArgumentException("node must contain Entry. received "+o.getClass());
    }

    // Add our panel listener to the broker
    broker.addPanelListener(new ObjectListener());
    pListener = broker;

    initResources();
    initComponents();
    pack();
  }

  /**
   * Loads locale-specific resources: strings, images, etc
   */
  private void initResources() {
    Locale locale = Locale.getDefault();
    resources = ResourceBundle.getBundle("com.l7tech.console.resources.FindDialog", locale);
  }

  /**
   *
   * Called from the constructor to initialize this window
   *
   */
  private void initComponents() {

    // Set properties on the dialog, itself
    setTitle(resources.getString("dialog.label"));
    addWindowListener (new WindowAdapter () {
                         public void windowClosing(WindowEvent event) {
                           // user hit window manager close button
                           windowAction(CMD_CANCEL);
                         }
                       });

    addComponentListener(new ComponentAdapter() {
                           /** Invoked when the component has been made visible.*/
                           public void componentShown(ComponentEvent e) {
                             origDimension = FindDialog.this.getSize();
                           }
                         });

    // accessibility - all frames, dialogs, and applets should have
    // a description
    getAccessibleContext().
    setAccessibleDescription(resources.getString("dialog.description"));

    // table properties
    jTable.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    jTable.setDefaultRenderer(Object.class, tableRenderer);
    jTable.setShowGrid(false);
    jTable.sizeColumnsToFit(0);
    mainPanel = new JPanel();
    Container contents  = mainPanel;
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
    constraints.insets = new Insets(12,12,0,0);
    contents.add(descLabel, constraints);

    searchTabs.addTab("General",getGeneralSearchPanel());
    constraints = new GridBagConstraints();
    constraints.gridx = 0;
    constraints.gridy = 1;
    constraints.weightx = 1.0;
    constraints.gridheight = 2;
    constraints.gridwidth = 4;
    constraints.fill = GridBagConstraints.HORIZONTAL;
    constraints.anchor = GridBagConstraints.WEST;
    constraints.insets = new Insets(12,12,5,0);
    contents.add(searchTabs, constraints);


    constraints = new GridBagConstraints();
    constraints.gridx = 4;
    constraints.gridy = 2;
    constraints.gridwidth = GridBagConstraints.REMAINDER;
    constraints.fill = GridBagConstraints.NONE;
    constraints.anchor = GridBagConstraints.EAST;
    constraints.insets = new Insets(12,12,0,11);
    contents.add(getSearchButtonPanel(), constraints);

    constraints = new GridBagConstraints();
    constraints.gridx = 0;
    constraints.gridy = 4;
    constraints.weightx = 1.0;
    constraints.weighty = 1.0;
    constraints.gridwidth =5;
    constraints.fill = GridBagConstraints.BOTH;
    constraints.anchor = GridBagConstraints.CENTER;
    constraints.insets = new Insets(12,12,5,12);

    JPanel p = getSearchResultTable();
    p.setVisible(false);
    contents.add(p, constraints);
    getContentPane().add(mainPanel);
  } // initComponents()


  /**
   * Returns the general search panel
   */
  private JPanel getGeneralSearchPanel() {
    // Build the contents
    JPanel gSearchPanel = new JPanel();
    Container contents  = gSearchPanel;
    contents.setLayout(new GridBagLayout());

    // "from" label
    JLabel fromLabel = new JLabel();
    fromLabel.setDisplayedMnemonic(
                                  resources.getString("fromText.mnemonic").charAt(0));
    fromLabel.setText(resources.getString("fromText.label"));

    GridBagConstraints constraints = new GridBagConstraints();
    constraints.gridx = 0;
    constraints.gridy = 1;
    constraints.fill = GridBagConstraints.NONE;
    constraints.anchor = GridBagConstraints.EAST;
    constraints.insets = new Insets(12,12,0,0);
    contents.add(fromLabel, constraints);

    // text field for context (from)
    fromField = new JTextField(15);
    fromField.setText(entry.getName());
    fromField.setToolTipText(resources.getString("fromText.tooltip"));
    constraints = new GridBagConstraints();
    constraints.gridx = 1;
    constraints.gridy = 1;
    constraints.weightx = 1.0;
    constraints.gridwidth = 3;
    constraints.fill = GridBagConstraints.HORIZONTAL;
    constraints.anchor = GridBagConstraints.WEST;
    constraints.insets = new Insets(12,12,0,0);
    contents.add(fromField, constraints);
    fromButton =
    new JButton(new ImageIcon(cl.getResource(MainWindow.RESOURCE_PATH+"/Tree16.gif")));
    fromButton.setMargin(new Insets(0, 0, 0, 0));

    fromButton.
    addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent e) {
                          // selectSearchBase(entry);
                        }
                      });

    fromButton.setToolTipText(resources.getString("fromText.tooltip"));
    constraints = new GridBagConstraints();
    constraints.gridx = 4;
    constraints.gridy = 1;
    constraints.fill = GridBagConstraints.NONE;
    constraints.anchor = GridBagConstraints.WEST;
    constraints.insets = new Insets(12,12,0,0);
    contents.add(fromButton, constraints);

    // "type" label
    JLabel typeLabel = new JLabel();
    typeLabel.setDisplayedMnemonic(
                                  resources.getString("findType.mnemonic").charAt(0));
    typeLabel.setText(resources.getString("findType.label"));

    constraints = new GridBagConstraints();
    constraints.gridx = 0;
    constraints.gridy = 2;
    constraints.anchor = GridBagConstraints.EAST;
    constraints.insets = new Insets(12,12,0,0);
    contents.add(typeLabel, constraints);

    searchType = new JComboBox(new Vector(oTypes.keySet()));
    searchType.setToolTipText(resources.getString("findType.tooltip"));
    constraints = new GridBagConstraints();
    constraints.gridx = 1;
    constraints.gridy = 2;
    constraints.anchor = GridBagConstraints.WEST;
    constraints.fill = GridBagConstraints.NONE;
    constraints.insets = new Insets(12,12,0,0);
    contents.add(searchType, constraints);



    // "find name" label
    JLabel findLabel = new JLabel();
    findLabel.setDisplayedMnemonic(
                                  resources.getString("findText.mnemonic").charAt(0));
    findLabel.setText(resources.getString("findText.label"));

    constraints = new GridBagConstraints();
    constraints.gridx = 0;
    constraints.gridy = 3;
    constraints.anchor = GridBagConstraints.EAST;
    constraints.insets = new Insets(12,12,5,0);
    contents.add(findLabel, constraints);

    searchNameOptions = new JComboBox(NAME_SEARCH_OPTIONS);
    searchNameOptions.
    addActionListener( new ActionListener() {
                         /**Invoked when an action occurs.*/
                         public void actionPerformed(ActionEvent e) {
                           JComboBox c = (JComboBox)e.getSource();
                           String option =  (String)c.getSelectedItem();
                           fqNamesCheckBox.setEnabled("equals".equals(option));
                         }
                       });

    constraints = new GridBagConstraints();
    constraints.gridx = 1;
    constraints.gridy = 3;
    constraints.anchor = GridBagConstraints.WEST;
    constraints.fill = GridBagConstraints.HORIZONTAL;
    constraints.insets = new Insets(12,12,5,0);
    contents.add(searchNameOptions, constraints);


    // text field for name to be found
    nameField = new JTextField(15);
    nameField.setToolTipText(resources.getString("findText.tooltip"));

    constraints = new GridBagConstraints();
    constraints.gridx = 2;
    constraints.gridy = 3;
    constraints.gridwidth =2;
    constraints.weightx = 1.0;
    constraints.fill = GridBagConstraints.HORIZONTAL;
    constraints.anchor = GridBagConstraints.WEST;
    constraints.insets = new Insets(12,12,5,0);

    contents.add(nameField, constraints);

    fqNamesCheckBox = new JCheckBox();
    fqNamesCheckBox.setMnemonic(
                               resources.getString("fqNamesCheckBox.mnemonic").charAt(0));
    fqNamesCheckBox.setMargin(new Insets(0, 2, 0, 2));
    fqNamesCheckBox.setToolTipText(
                                  resources.getString("fqNamesCheckBox.tooltip"));
    fqNamesCheckBox.setText(
                           resources.getString("fqNamesCheckBox.label"));

    constraints = new GridBagConstraints();
    constraints.gridx = 4;
    constraints.gridy = 3;
    constraints.fill = GridBagConstraints.HORIZONTAL;
    constraints.anchor = GridBagConstraints.WEST;
    constraints.insets = new Insets(12,12,5,11);

    contents.add(fqNamesCheckBox, constraints);


    return gSearchPanel;
  }

  /**
   * @return the panel containing the main search
   *         button controls
   */
  private JPanel getSearchButtonPanel() {
    JButton findButton = null;
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
    buttonPanel.add(Box.createRigidArea(new Dimension(5,5)));

    // cancel search button
    cancelSearchButton = new JButton();
    cancelSearchButton.setText(resources.getString("cancelSearchButton.label"));
    cancelSearchButton.setToolTipText(resources.getString("cancelSearchButton.tooltip"));

    cancelSearchButton.
    addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent event) {
                          stopLoadingTableModel();
                        }
                      });
    buttonPanel.add(cancelSearchButton);

    // space
    buttonPanel.add(Box.createRigidArea(new Dimension(5,5)));

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
    = new JButton[] { findButton, cancelSearchButton, closeButton};

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
      return;
    }
    setVisible(false);
    // in canse closed and still searching
    stopLoadingTableModel();
    dispose();
  }


  /**
   * show the result table. Display and layout the
   * table if required.
   */
  private void showSearchResult(SearchInfo info) {

    if (!searchResultPanel.isVisible()) {
      SwingUtilities.invokeLater(
                                new Runnable() {
                                  public void run() {
                                    searchResultPanel.setVisible(true);
                                    Dimension d = getSize();
                                    setSize(d.width, (int)(origDimension.height*3));
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
  private JPanel getSearchResultTable() {
    searchResultPanel.setLayout(new BorderLayout());
    searchResultPanel.add(new JLabel("Search results"), BorderLayout.NORTH);
    JScrollPane scrollPane = new JScrollPane(jTable);
    scrollPane.getViewport().setBackground(jTable.getBackground());

    searchResultPanel.add(scrollPane, BorderLayout.CENTER);

    // Button panel at the bottom
    JPanel buttonPanel = new JPanel();
    buttonPanel.setBorder(BorderFactory.createEmptyBorder(5,0,5,0));
    buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
    // result counter
    buttonPanel.add(resultCounter);

    buttonPanel.add(Box.createGlue());

    // edit button
    final JButton editButton = new JButton();
    editButton.setText(resources.getString("editButton.label"));
    editButton.addActionListener(new ActionListener() {
                                   public void actionPerformed(ActionEvent event) {
                                     int row = jTable.getSelectedRow();
                                     if (row == -1) return;
                                     Object o =
                                     ((DynamicTableModel)jTable.getModel()).getValueAt(row);
                                     if (o == null) return;
                                     showEntryDialog(o);
                                   }
                                 });
    buttonPanel.add(editButton);
    editButton.setEnabled(false);

    jTable.getSelectionModel().
    addListSelectionListener( new ListSelectionListener () {
                                /**
                                 * Called whenever the value of the selection changes.
                                 * @param e the event that characterizes the change.
                                 */
                                public void valueChanged(ListSelectionEvent e) {
                                  int row = jTable.getSelectedRow();
                                  if (row == -1) return;

                                  editButton.setEnabled(true);
                                }
                              });

    jTable.
    addMouseListener(new MouseAdapter() {
                       public void mouseClicked(MouseEvent e) {
                         if (e.getClickCount() == 2) {
                           int row = jTable.getSelectedRow();
                           if (row == -1) return;
                           Object o = jTable.getModel().getValueAt(row,0);
                           if (o == null) return;
                           showEntryDialog(o);
                         }
                       }
                     });

    jTable.
    addKeyListener(new KeyAdapter() {
                     /** Invoked when a key has been pressed.*/
                     public void keyPressed(KeyEvent e) {
                       int row = jTable.getSelectedRow();
                       if (row == -1) return;
                       Object o = jTable.getValueAt(row, 0);

                       int keyCode = e.getKeyCode();
                       if (keyCode == KeyEvent.VK_ENTER) {
                         showEntryDialog(o);
                       }
                     }
                   });




    // space
    buttonPanel.add(Box.createRigidArea(new Dimension(5,0)));

    // cancel button
    JButton newSearchButton = new JButton();
    newSearchButton.setText(resources.getString("newSearchButton.label"));
    newSearchButton.addActionListener(new ActionListener() {
                                        public void actionPerformed(ActionEvent event) {
                                          searchResultPanel.setVisible(false);
                                          FindDialog.this.setSize(origDimension);
                                          // reset search info
                                          searchInfo = new SearchInfo();
                                          resultCounter.setText("");
                                        }
                                      });
    buttonPanel.add(newSearchButton);
    buttonPanel.add(Box.createGlue());

    searchResultPanel.add(buttonPanel, BorderLayout.SOUTH);

    JButton[] buttons
    = new JButton[] { newSearchButton, editButton};

    Utilities.equalizeButtonSizes(buttons);

    return searchResultPanel;
  }

  /**
   * stop loading the table model. The method does nothing
   * if the model has not been created yet.
   */
  private void stopLoadingTableModel() {
    if (tableModel !=null) {
      tableModel.stop();
    }
  }

  /**
   * instantiate the dialog for given AbstractTreeNode
   *
   * @param o   the <CODE>AbstractTreeNode</CODE> instance
   */
  private void showEntryDialog(Object o) {
    JPanel panel = null; // PanelFactory.getPanel(o, pListener);
    if (panel == null) return;

    JFrame f = (JFrame)SwingUtilities.windowForComponent(this);
    EditorDialog dialog = new EditorDialog(f, panel);
    dialog.pack();
    Utilities.centerOnScreen(dialog);
    dialog.show();
  }

  /**
   * the <CODE>TableCellRenderer</CODE> instance
   * for the result table
   */
  private final TableCellRenderer
  tableRenderer = new DefaultTableCellRenderer() {
    /**  Returns the default table cell renderer.*/
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
      // based on value type and column, determine cell contents
      setIcon(null);
      return this;
    }
  };

  /** the class instances keep the search info. */
  private class SearchInfo {
    String searchName;
    boolean exactName;
    boolean fqNames;
    String type = "ALL";
    String searchDesc;
    boolean exactDesc;

    /** set the search type */
    void setObjectType(String t) {
      type = t;
    }

    /** FQ names toggle (ignored if not exact) */
    void setFqNames(boolean b) {
      fqNames = b;
    }

    /** set the search by name and equals/contains. */
    void setSearchName(String name, boolean b) {
      searchName = name;
      exactName = b;
    }

    /** set the search by desc and equals/contains. */
    void setSearchDescription(String desc, boolean b) {
      searchDesc = desc;
      exactDesc = b;
    }

    /** Returns whether all types are searched. */
    boolean isAllTypes() {
      return type== null || "ALL".equals(type);
    }

    /**
     * Returns the search expression (SQL expression).
     * sequence must match parameters.
     */
    String getSearchExpression() {
      StringBuffer sb = new StringBuffer();

      return sb.toString();
    }

    /** Returns the array of additional attributes requested. */
    String[] getAdditionalAttributes() {
      return null;
    }

    private boolean isEmpty(String s) {
      return "".equals(s) || s==null;
    }
  }


  private class ObjectListener extends PanelListenerAdapter {
    /**
     * invoked after insert
     *
     * @param object an arbitrary object set by the Panel
     */
    public void onInsert(Object object) {
      ;
    }

    /**
     * invoked after update
     *
     * @param object an arbitrary object set by the Panel
     */
    public void onUpdate(Object object) {
      ;
    }

    /**
     * invoked on object delete
     *
     * @param object an arbitrary object set by the Panel
     */
    public void onDelete(Object object) {
      // Find the row that corresponds to the deleted object
      int row = tableModel.getRow(object);

      // If found
      if (-1 != row) {
        // Delete the object's row
        tableModel.removeRow(row);
      }
    }
  }
}



