package com.l7tech.console.panels;

import com.l7tech.console.table.ConsoleTableModel;
import com.l7tech.console.util.FilterConsoleTableModel;
import com.l7tech.console.util.IconManager;
import com.l7tech.identity.User;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.*;
import java.util.List;

/**
 * This class is the New Group member Dialog for adding users to a group.
 */
public class NewGroupMemberDialog extends JDialog {
  /**
   * Create a new NewGroupMemberDialog
   *
   * @param parent the GroupPanel parent.
   */
  public NewGroupMemberDialog(JDialog owner, GroupPanel parent) {
    // Init UI
    super(owner, true);
    this.parent = parent;
    initResources();
    initComponents();
    pack();
    Utilities.centerOnScreen(this);

    // Update add buttons
    updateAddButton();
  }

  /**
   * Loads locale-specific resources: strings  etc
   */
  private void initResources() {
    Locale locale = Locale.getDefault();
    resources = ResourceBundle.getBundle("com.l7tech.console.resources.NewGroupMemberDialog",locale);
  }

  /**
   * This method is called from within the constructor to
   * initialize the dialog.
   */
  private void initComponents() {
    Container contents = getContentPane();
    setTitle(resources.getString("dialog.title"));
    JPanel p = new JPanel();
    p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
    contents.add(p);

    addWindowListener(new WindowAdapter () {
      public void windowClosing(WindowEvent event) {
        // user hit window manager close button
        windowAction(CMD_CANCEL);
        }
      });

    JPanel panel = new JPanel();
    panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
    panel.setMinimumSize(new Dimension(400, 300));
    panel.setPreferredSize(new Dimension(400, 300));
    JPanel panelTitle = new JPanel();
    panelTitle.setLayout(new BoxLayout(panelTitle, BoxLayout.X_AXIS));

    panelTitle.add(Box.createHorizontalGlue());
    panelTitle.add(getUsersNonMemberLabel());
    panelTitle.add(Box.createHorizontalGlue());
    panel.add(panelTitle);
    panel.add(getUsersOutListJScrollPane());
    panel.add(Box.createVerticalStrut(10));
    p.add(panel);

    p.add(createButtonPanel());
    p.add(Box.createVerticalStrut(10));
  } // initComponents()


  /**
   * Creates the panel of buttons that goes along the bottom of the dialog
   */
  private JPanel createButtonPanel() {
    JPanel panel = new JPanel();
    panel.setLayout(new BoxLayout(panel, 0));

    panel.add(getAddButton());

    // space
    panel.add(Box.createRigidArea(new Dimension(5,0)));

    // cancel button
    JButton cancelButton = new JButton();
    cancelButton.setText(resources.getString("cancelButton.label"));
    cancelButton.setActionCommand(CMD_CANCEL);
    cancelButton.addActionListener(new ActionListener() {
                                     public void actionPerformed(ActionEvent event) {
                                       windowAction(event);
                                     }
                                   });
    panel.add(cancelButton);
    // space
    panel.add(Box.createRigidArea(new Dimension(5,0)));

    // equalize buttons
    Utilities.equalizeButtonSizes(new JButton[] {getAddButton(), cancelButton});

    return panel;
  } // createButtonPanel()

  private JLabel getUsersNonMemberLabel() {
    if (usersNonMemberLabel == null) {
      usersNonMemberLabel = new JLabel(NON_MEMBER_LABEL);
      usersNonMemberLabel.setHorizontalAlignment(SwingConstants.CENTER);
    }
    return usersNonMemberLabel;
  }

  /**
   * Create if needed the Users out scroll pane
   *
   * @return JScrollPane
   */
  private JScrollPane getUsersOutListJScrollPane() {
    if (usersOutListJScrollPane == null) {
      usersOutListJScrollPane = new JScrollPane(getUsersNonMemberTable());
      usersOutListJScrollPane.setViewportBorder(BorderFactory.createEtchedBorder());
      usersOutListJScrollPane.setBorder(BorderFactory.createLoweredBevelBorder());
      usersOutListJScrollPane.setMinimumSize(new Dimension(140, 140));
      usersOutListJScrollPane.setPreferredSize(new Dimension(140, 140));
      usersOutListJScrollPane.getViewport().
        setBackground(getUsersNonMemberTable().getBackground());
    }
    return usersOutListJScrollPane;
  }


  private JTable getUsersNonMemberTable() {
    if (null == usersNonMemberTable) {
      // Create a sorted filtered table
      usersFilterTableModel = new FilterConsoleTableModel(getUsersNonMemberTableModel());

      usersNonMemberTable = new JTable(usersFilterTableModel);
      usersNonMemberTable.setShowGrid(false);
      usersNonMemberTable.setDefaultRenderer(Object.class, renderer);
      usersNonMemberTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
        public void valueChanged(ListSelectionEvent e) {
          // Ignore extra messages
          if (e.getValueIsAdjusting()) return;

          ListSelectionModel lsm = (ListSelectionModel)e.getSource();
          if ((0!=usersNonMemberTable.getRowCount()) && (!lsm.isSelectionEmpty())) {
            // Row(s) are selected
            getAddButton().setEnabled(true);
          } else {
            // No rows selected
            getAddButton().setEnabled(false);
          }
        }
      });
    }
    return usersNonMemberTable;
  }


  /** Returns agent Table's model */
  private ConsoleTableModel getUsersNonMemberTableModel() {
    if (usersNonMemberTableModel == null) {
      usersNonMemberTableModel = new ConsoleTableModel(NON_MEMBER_COLUMN_NAMES, null);
    }
    return usersNonMemberTableModel;
  }


  /** Updates Add button */
  private void updateAddButton() {
    getAddButton().setEnabled(0!=getUsersNonMemberTable().getSelectedRows().length);
  }


  private JButton getAddButton() {
    if (null == okButton) {
      okButton = new JButton();
      okButton.setText(resources.getString("okButton.label"));
      okButton.setToolTipText(resources.getString("okButton.tooltip"));
      okButton.setActionCommand(CMD_OK);
      okButton.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent event) {
            windowAction(event);
            }
          });
    }

    return okButton;
  }

  /**
   * The user has selected an option. Here we close and dispose the dialog.
   *
   * If actionCommand is an ActionEvent, getCommandString() is
   * called, otherwise toString() is used to get the action command.
   *
   * @param actionCommand may be null
   */
  private void windowAction(Object actionCommand) {
    String cmd = null;

    if (actionCommand != null) {
      if (actionCommand instanceof ActionEvent) {
        cmd = ((ActionEvent)actionCommand).getActionCommand();
      } else {
        cmd = actionCommand.toString();
      }
    }
    if (cmd == null) {
      // do nothing
    } else if (cmd.equals(CMD_CANCEL)) {
      this.dispose();
    } else if (cmd.equals(CMD_OK)) {
      addSelectedUsers();
    }
  }

  /** Adds selected users into the group */
  private void addSelectedUsers() {
    // Get selected agents
    int[] agentsToAdd = getUsersNonMemberTable().getSelectedRows();
    int size = agentsToAdd.length;
    if (0 == size) {
      return;
    }

    List agents = new ArrayList(size);
    TableModel tableModel = getUsersNonMemberTable().getModel();
    for (int i=0; i<size; i++) {

    }

    dispose();
  }


  private final TableCellRenderer renderer = new DefaultTableCellRenderer() {
   public Component getTableCellRendererComponent(JTable table, Object value,
         boolean selected, boolean hasFocus, int row, int column) {
     if (selected) {
       this.setBackground(table.getSelectionBackground());
       this.setForeground(table.getSelectionForeground());
     } else {
       this.setBackground(table.getBackground());
       this.setForeground(table.getForeground());
     }
     this.setFont(new Font("Dialog", Font.PLAIN, 12));

     // Based on value type, determine cell contents
     setIcon(null);
     setText(null);
     if (value instanceof User) {
       User user = (User) value;
       setIcon(IconManager.getIcon(User.class));
       setText(user.getName());
     }

     return this;
   }
 };


  /** UI Elements */
  private JButton okButton;

  /** Parent GroupPanel where users are to be added to */
  private GroupPanel parent;
  private JScrollPane usersOutListJScrollPane;
  private JLabel usersNonMemberLabel;
  private JTable usersNonMemberTable;
  private ConsoleTableModel usersNonMemberTableModel;
  private FilterConsoleTableModel usersFilterTableModel;

  private ResourceBundle resources;
  private final String CMD_CANCEL = "cmd.cancel";
  private final String CMD_OK = "cmd.ok";
  private static final String NON_MEMBER_LABEL = "Non Members";

  private final ClassLoader cl = getClass().getClassLoader();
  private static final String[] NON_MEMBER_COLUMN_NAMES = {"User"};
}

