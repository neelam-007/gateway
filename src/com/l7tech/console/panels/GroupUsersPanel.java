package com.l7tech.console.panels;

import com.l7tech.common.gui.util.ImageCache;
import com.l7tech.common.gui.util.Utilities;
import com.l7tech.console.util.SortedListModel;
import com.l7tech.identity.Group;
import com.l7tech.objectmodel.EntityHeader;

import javax.swing.*;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * GroupUsersPanel is the panel for administering the
 * <CODE>Group</CODE> membership properties
 * Currently it is invoked by <code>GroupPanel</code>
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 */
class GroupUsersPanel extends JPanel {
    static Logger log = Logger.getLogger(GroupUsersPanel.class.getName());

    // the parent panel (main GroupPanel)
    private GroupPanel groupPanel;

    private JPanel mainPanel = null;
    private JPanel groupMemberPanel = null;

    private JPanel buttonsPanel = null;
    private JLabel groupMemberLabel = null;

    private JScrollPane groupInListJScrollPane;
    private JList groupMemberList = null;

    SortedListModel listInModel;

    private JButton groupAdd = null;
    private JButton groupRemove = null;
    private boolean isLoading = false;

    private final static String USER_GROUP_MEMBER_LABEL = "Group Membership:";


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
            log.log(Level.SEVERE, "GroupUsersPanel()", e);
        }
    }

    /**
     * package private method, allows adding users
     */
    void addUsers(Set userHeaders) {
        listInModel.addAll(userHeaders);
    }

    /**
     * package private method, allows adding users
     */
    Set getCurrentUsers() {
        return new HashSet(Arrays.asList(listInModel.toArray()));
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


    /** layout the components */
    private JPanel getMainPanel() {
        // main anel
        if (mainPanel != null)
            return mainPanel;
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


        return mainPanel;
    }


    /**
     * Create if needed a default list model
     *
     * @return SortedListModel
     */
    private SortedListModel getListInModel() {
        if (listInModel != null) return listInModel;

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
                        EntityHeader e1 = (EntityHeader)o1;
                        EntityHeader e2 = (EntityHeader)o2;

                        return e1.getName().compareTo(e2.getName());
                    }
                });

        listInModel.addListDataListener(new ListDataListener() {
            /**
             * @param e  a <code>ListDataEvent</code> encapsulating the
             *    event information
             */
            public void intervalAdded(ListDataEvent e) {
                if (!isLoading) {
                    groupPanel.setModified(true);
                    updateGroupMembers();
                }
            }

            /**
             * @param e  a <code>ListDataEvent</code> encapsulating the
             *    event information
             */
            public void intervalRemoved(ListDataEvent e) {
                if (!isLoading) {
                    groupPanel.setModified(true);
                    updateGroupMembers();
                }
            }

            /**
             * @param e  a <code>ListDataEvent</code> encapsulating the
             *    event information
             */
            public void contentsChanged(ListDataEvent e) {
                if (!isLoading) {
                    groupPanel.setModified(true);
                    updateGroupMembers();
                }
            }

            private void updateGroupMembers() {
                Set memberHeaders = groupPanel.getGroupMembers();
                memberHeaders.clear();
                for (int i = 0; i < listInModel.getSize(); i++) {
                    EntityHeader g = (EntityHeader) listInModel.getElementAt(i);
                    memberHeaders.add(g);
                }
            }

        });

        return listInModel;
    }

    /**
     * THe panel that contains the Group Membership information
     *
     * @return JPanel
     */
    private JPanel getGroupMemberPanel() {
        if (groupMemberPanel != null)
            return groupMemberPanel;

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

        groupMemberPanel.add(getButtonsPanel(),
                new GridBagConstraints(0, 2, 1, 1, 1.0, 1.0,
                        GridBagConstraints.EAST,
                        GridBagConstraints.BOTH,
                        new Insets(8, 0, 0, 0), 0, 0));
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
        // if provider is not read only, enable remove button on user selection
        if (!groupPanel.getIdProvider().isReadOnly())
            getGroupRemove().setEnabled(listInModel.getSize() > 0);
    }

    /**
     * The Add/remove Group button panel
     *
     * @return JPanel
     */
    private JPanel getButtonsPanel() {
        if (buttonsPanel != null)
            return buttonsPanel;

        buttonsPanel = new JPanel();
        buttonsPanel.setLayout(new GridBagLayout());

        Component hStrut = Box.createHorizontalStrut(8);

        // add components
        buttonsPanel.add(hStrut,
                new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0,
                        GridBagConstraints.CENTER,
                        GridBagConstraints.BOTH,
                        new Insets(0, 0, 0, 0), 0, 0));

        buttonsPanel.add(getGroupAdd(),
                new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
                        GridBagConstraints.EAST,
                        GridBagConstraints.NONE,
                        new Insets(0, 0, 0, 0), 0, 0));

        buttonsPanel.add(getGroupRemove(),
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


        return buttonsPanel;
    }

    /**
     * Create if needed the Group add button
     *
     * @return JButton
     */
    private JButton getGroupAdd() {
        if (groupAdd == null) {
            groupAdd = new JButton();
            groupAdd.setText("Add");

            groupAdd.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    JDialog d = (JDialog)SwingUtilities.windowForComponent(GroupUsersPanel.this);

                    JDialog dialog = new NewGroupMemberDialog(d, GroupUsersPanel.this);
                    dialog.setResizable(false);
                    dialog.show();

                    // Perform necessary post-updates
                    setAddRemoveButtons();
                }
            });
            if (groupPanel.getIdProvider().isReadOnly()) groupAdd.setEnabled(false);
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
                    Object[] removals = groupMemberList.getSelectedValues();

                    if (((Group.ADMIN_GROUP_NAME.equals(groupPanel.getGroup().getName())) &&
                            (listInModel.getSize() - removals.length > 0)) ||
                            (!Group.ADMIN_GROUP_NAME.equals(groupPanel.getGroup().getName()))){

                        Set members = groupPanel.getGroupMembers();

                        for (int i = 0; removals != null && i < removals.length; i++) {
                            listInModel.removeElement(removals[i]);
                            members.remove(removals[i]);
                        }
                    }
                    else {
                        // there must be at least one member in this group after the deletion
                        StringBuffer msg = new StringBuffer();
                        msg.append("Cannot delete all members in ");
                        msg.append("Group ").append(groupPanel.getGroup().getName());
                        msg.append(". This group must have at least one member.\n");

                        JOptionPane.showMessageDialog(null,
                                msg.toString(),
                                "Error",
                                JOptionPane.ERROR_MESSAGE);
                    }
                    setAddRemoveButtons();
                }
            });
            if (groupPanel.getIdProvider().isReadOnly()) groupRemove.setEnabled(false);
        }
        return groupRemove;
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
        try {
            isLoading = true;
            Collection members = groupPanel.getGroupMembers();
            if (members != null) {
                listInModel.addAll(members);
            }
        } finally {
            isLoading = false;
        }
    }

    private final ListCellRenderer renderer = new DefaultListCellRenderer() {
        public Component getListCellRendererComponent(JList list, Object value,
                                                      int index, boolean isSelected,
                                                      boolean cellHasFocus) {
            if (isSelected) {
                this.setBackground(list.getSelectionBackground());
                this.setForeground(list.getSelectionForeground());
            } else {
                this.setBackground(list.getBackground());
                this.setForeground(list.getForeground());
            }
            this.setFont(new Font("Dialog", Font.PLAIN, 12));

            // Based on value type, determine cell contents
            setIcon(new ImageIcon(ImageCache.getInstance().getIcon(UserPanel.USER_ICON_RESOURCE)));
            EntityHeader eh = (EntityHeader)value;
            setText(eh.getName());

            return this;
        }
    };
}

