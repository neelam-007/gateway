package com.l7tech.console.panels;

import com.l7tech.console.util.IconManager;
import com.l7tech.console.util.SortedListModel;
import com.l7tech.identity.User;
import org.apache.log4j.Category;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.util.Collection;
import java.util.Comparator;

/**
 * GroupUsersPanel is the panel for administering the
 * <CODE>Group</CODE> membership properties
 * Currently it is invoked by <code>GroupPanel</code>
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 */
class GroupUsersPanel extends JPanel {
    static final Category log = Category.getInstance(GroupUsersPanel.class.getName());

    // the parent panel (main GroupPanel)
    private GroupPanel groupPanel;

    private JPanel mainPanel = null;
    private JPanel groupMemberPanel = null;
    private JPanel groupButtonPanel = null;
    private JLabel groupMemberLabel = null;

    private JScrollPane groupInListJScrollPane;
    private JList groupMemberList = null;

    private SortedListModel listInModel;
    private SortedListModel listOutModel;

    private JButton groupAdd = null;
    private JButton groupRemove = null;

    private boolean dirty = false;


    private final static String USER_GROUP_MEMBER_LABEL = "Group memberships:";


    /**
     * The only constructor
     *
     * @param groupPanel the parent groupPanel
     */
    public GroupUsersPanel(GroupPanel groupPanel) {
        super();
        try {
            this.groupPanel = groupPanel;
            layoutComponents();
            this.addHierarchyListener(hierarchyListener);
            this.setDoubleBuffered(true);
        } catch (Exception e) {
            log.error("GroupUsersPanel()", e);
        }
    }

    /**
     * This is the main initialization code.
     * Compliant with JBuilder
     *
     * @exception Exception
     */
    private void layoutComponents() throws Exception {
        this.setLayout(new GridBagLayout());

        // add the main panel
        add(getMainPanel(),
                new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0,
                        GridBagConstraints.CENTER,
                        GridBagConstraints.BOTH,
                        new Insets(8, 8, 8, 8), 0, 0));
    }

    /** is the panel dirty */
    public boolean isDirty() {
        return dirty;
    }

    /** layout the components */
    private JPanel getMainPanel() {
        // main anel
        if (mainPanel == null) {
            mainPanel = new JPanel();
            mainPanel.setLayout(new GridBagLayout());
            Component hStrut = Box.createHorizontalStrut(8);

            // add components
            mainPanel.add(hStrut,
                    new GridBagConstraints(0, 1, 1, 1, 1.0, 1.0,
                            GridBagConstraints.CENTER,
                            GridBagConstraints.BOTH,
                            new Insets(0, 0, 0, 0), 0, 0));

            mainPanel.add(getGroupMemberPanel(),
                    new GridBagConstraints(0, 1, 1, 1, 1.0, 1.0,
                            GridBagConstraints.WEST,
                            GridBagConstraints.BOTH,
                            new Insets(5, 0, 0, 0), 0, 0));

        }
        return mainPanel;
    }


    /**
     * Create if needed a default list model
     *
     * @return SortedListModel
     */
    private SortedListModel getListInModel() {
        if (listInModel == null) {
            listInModel =
                    new SortedListModel(new Comparator() {
                        /**
                         * Compares group users by login alphabetically.
                         * @param o1 the first object to be compared.
                         * @param o2 the second object to be compared.
                         * @return a negative integer, zero, or a positive integer as the
                         * 	       first argument is less than, equal to, or greater than the
                         *	       second.
                         * @throws ClassCastException if the arguments' types prevent them from
                         * 	       being compared by this Comparator.
                         */
                        public int compare(Object o1, Object o2) {
                            User u1 = (User)o1;
                            User u2 = (User)o2;

                            return u1.getLogin().compareTo(u2.getLogin());
                        }
            });
        }

        return listInModel;
    }


    /**
     * THe panel that contains the Group Membership information
     *
     * @return JPanel
     */
    private JPanel getGroupMemberPanel() {
        if (groupMemberPanel == null) {
            groupMemberPanel = new JPanel();
            groupMemberPanel.setLayout(new GridBagLayout());

            groupMemberPanel.add(getGroupMemberLabel(),
                    new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0,
                            GridBagConstraints.WEST,
                            GridBagConstraints.BOTH,
                            new Insets(0, 0, 0, 0), 0, 0));

            groupMemberPanel.add(getGroupInListJScrollPane(),
                    new GridBagConstraints(0, 1, 1, 1, 1.0, 1.0,
                            GridBagConstraints.WEST,
                            GridBagConstraints.BOTH,
                            new Insets(0, 0, 0, 0), 0, 0));

            groupMemberPanel.add(getGroupButtonPanel(),
                    new GridBagConstraints(0, 2, 1, 1, 1.0, 1.0,
                            GridBagConstraints.EAST,
                            GridBagConstraints.BOTH,
                            new Insets(8, 0, 0, 0), 0, 0));
        }
        return groupMemberPanel;
    }

    /**
     * Create if needed the Group Member label
     *
     * @return JLabel
     */
    private JLabel getGroupMemberLabel() {
        if (groupMemberLabel == null) {
            groupMemberLabel = new JLabel(USER_GROUP_MEMBER_LABEL);
        }
        return groupMemberLabel;
    }

    /**
     * Create if needed the Group in list scroll pane
     *
     * @return JScrollPane
     */
    private JScrollPane getGroupInListJScrollPane() {
        if (groupInListJScrollPane == null) {
            groupInListJScrollPane = new JScrollPane();
            groupInListJScrollPane.setMinimumSize(new Dimension(200, 120));
            groupInListJScrollPane.setPreferredSize(new Dimension(200, 120));
            groupInListJScrollPane.getViewport().setView(getGroupMemberList());
        }
        return groupInListJScrollPane;
    }

    /**
     * Create if needed the Group members list
     *
     * @return JList
     */
    private JList getGroupMemberList() {
        if (groupMemberList == null) {
            groupMemberList = new JList(getListInModel());
            groupMemberList.setCellRenderer(renderer);
            groupMemberList.addListSelectionListener(new ListSelectionListener() {
                /** Called whenever the value of the selection changes.*/
                public void valueChanged(ListSelectionEvent e) {
                    setAddRemoveButtons();
                }
            });
        }
        return groupMemberList;
    }

    /**
     * enable/disable add/remove buttons
     */
    private void setAddRemoveButtons() {

    }

    /**
     * The Add/remove Group button panel
     *
     * @return JPanel
     */
    private JPanel getGroupButtonPanel() {
        if (groupButtonPanel == null) {
            groupButtonPanel = new JPanel();
            groupButtonPanel.setLayout(new GridBagLayout());

            Component hStrut = Box.createHorizontalStrut(8);

            // add components
            groupButtonPanel.add(hStrut,
                    new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0,
                            GridBagConstraints.CENTER,
                            GridBagConstraints.BOTH,
                            new Insets(0, 0, 0, 0), 0, 0));

            groupButtonPanel.add(getGroupAdd(),
                    new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
                            GridBagConstraints.EAST,
                            GridBagConstraints.NONE,
                            new Insets(0, 0, 0, 0), 0, 0));

            groupButtonPanel.add(getGroupRemove(),
                    new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0,
                            GridBagConstraints.EAST,
                            GridBagConstraints.NONE,
                            new Insets(0, 8, 0, 0), 0, 0));

            JButton buttons1[] = new JButton[]
            {
                getGroupAdd(),
                getGroupRemove()
            };
            Utilities.equalizeButtonSizes(buttons1);
        }

        return groupButtonPanel;
    }

    /**
     * Create if needed the Group add button
     *
     * @return JButton
     */
    private JButton getGroupAdd() {
        if (groupAdd == null) {
            groupAdd = new JButton();
            groupAdd.setText("Add..");

            groupAdd.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    // Perform necessary post-updates
                    setAddRemoveButtons();
                }
            });
        }
        return groupAdd;
    }


    /**
     * Create if needed the Group remove button
     *
     * @return JButton
     */
    private JButton getGroupRemove() {
        if (groupRemove == null) {
            groupRemove = new JButton();
            groupRemove.setText("Remove");

            groupRemove.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    ;
                }
            });
        }
        return groupRemove;
    }

    /**
     * Create if needed the default list model for the
     * Non Members
     *
     * @return SortedListModel
     */
    private SortedListModel getListOutModel() {
        if (listOutModel == null) {
            listOutModel = new SortedListModel();
        }
        return listOutModel;
    }


    // hierarchy listener
    private final HierarchyListener hierarchyListener = new HierarchyListener() {
        /** Called when the hierarchy has been changed.*/
        public void hierarchyChanged(HierarchyEvent e) {
            long flags = e.getChangeFlags();
            if ((flags & HierarchyEvent.SHOWING_CHANGED) == HierarchyEvent.SHOWING_CHANGED) {
                if (GroupUsersPanel.this.isShowing()) {
                    loadGroupUsers();
                }
            }
        }
    };

    private void loadGroupUsers() {
        Collection members = groupPanel.getGroup().getMembers();
        if (members !=null) {
            listInModel.addAll(members);
        }

    }


    private final ListCellRenderer renderer = new DefaultListCellRenderer() {
        public Component getListCellRendererComponent(JList list, Object value,
                                                      int index, boolean isSelected, boolean cellHasFocus) {
            if (isSelected) {
                this.setBackground(list.getSelectionBackground());
                this.setForeground(list.getSelectionForeground());
            } else {
                this.setBackground(list.getBackground());
                this.setForeground(list.getForeground());
            }
            this.setFont(new Font("Dialog", Font.PLAIN, 12));

            // Based on value type, determine cell contents
            setIcon(IconManager.getIcon(User.class));
            User u = (User)value;
            setText(u.getLogin() + " - " + u.getFirstName() + " " +u.getFirstName());

            return this;
        }
    };
}

