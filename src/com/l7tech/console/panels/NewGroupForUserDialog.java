package com.l7tech.console.panels;

import com.l7tech.console.util.FilterListModel;
import com.l7tech.console.util.IconManager;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.SortedListModel;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.*;

/**
 * This dialog adds the group to the user.
 */
public class NewGroupForUserDialog extends JDialog {
    private FilterListModel listOutModel;
    private JList nonGroupMemberList;
    private final Comparator entityNameComparator = new Comparator() {
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
                    };

    /**
     * Create a new NewGroupMemberDialog
     *
     * @param parent the GroupPanel parent.
     */
    public NewGroupForUserDialog(JDialog owner, UserGroupsPanel parent) {
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
        resources = ResourceBundle.getBundle("com.l7tech.console.resources.NewGroupMemberDialog", locale);
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

        addWindowListener(new WindowAdapter() {
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
        panelTitle.add(getGroupsNonMemberLabel());
        panelTitle.add(Box.createHorizontalGlue());
        panel.add(panelTitle);
        panel.add(getGroupsOutListJScrollPane());
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
        panel.add(Box.createRigidArea(new Dimension(5, 0)));

        // cancel button
        JButton cancelButton = new JButton();
        cancelButton.setText(resources.getString("cancelButton.label"));
        cancelButton.setActionCommand(CMD_CANCEL);
        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                windowAction(event.getActionCommand());
            }
        });
        panel.add(cancelButton);
        // space
        panel.add(Box.createRigidArea(new Dimension(5, 0)));

        // equalize buttons
        Utilities.equalizeButtonSizes(new JButton[]{getAddButton(), cancelButton});

        return panel;
    } // createButtonPanel()

    private JLabel getGroupsNonMemberLabel() {
        if (groupsNonMemberLabel == null) {
            groupsNonMemberLabel = new JLabel(NON_MEMBER_LABEL);
            groupsNonMemberLabel.setHorizontalAlignment(SwingConstants.CENTER);
        }
        return groupsNonMemberLabel;
    }

    /**
     * Create if needed the Users out scroll pane
     *
     * @return JScrollPane
     */
    private JScrollPane getGroupsOutListJScrollPane() {
        if (groupsOutListJScrollPane == null) {
            groupsOutListJScrollPane = new JScrollPane(getNonGroupMemberList());
            groupsOutListJScrollPane.setViewportBorder(BorderFactory.createEtchedBorder());
            groupsOutListJScrollPane.setBorder(BorderFactory.createLoweredBevelBorder());
            groupsOutListJScrollPane.setMinimumSize(new Dimension(140, 140));
            groupsOutListJScrollPane.setPreferredSize(new Dimension(140, 140));
            groupsOutListJScrollPane.getViewport().
                    setBackground(getNonGroupMemberList().getBackground());
        }
        return groupsOutListJScrollPane;
    }


    /**
     * Create if needed the Group members list
     *
     * @return JList
     */
    private JList getNonGroupMemberList() {
        if (nonGroupMemberList == null) {
            nonGroupMemberList = new JList(getListOutModel());
            nonGroupMemberList.setCellRenderer(renderer);
            nonGroupMemberList.addListSelectionListener(new ListSelectionListener() {
                /** Called whenever the value of the selection changes.*/
                public void valueChanged(ListSelectionEvent e) {
                    updateAddButton();
                }
            });
        }
        return nonGroupMemberList;
    }

    /**
     * Create the list of non memberst list model
     *
     * @return ListModel
     */
    private ListModel getListOutModel() {
        if (listOutModel != null) return listOutModel;

        final SortedListModel sl =
                new SortedListModel(entityNameComparator);

        listOutModel = new FilterListModel(sl);

        SwingUtilities.invokeLater(new Runnable() {
            /** */
            public void run() {
                try {
                    Set currentGroups = new TreeSet(entityNameComparator);
                    Set set = parent.getCurrentGroups();
                    if (set !=null) currentGroups.addAll(set);

                    Collection nonMemberOf = Registry.getDefault().getInternalGroupManager().findAllHeaders();

                    for(Iterator i = nonMemberOf.iterator();i.hasNext();) {
                        EntityHeader eh = (EntityHeader)i.next();
                        if (!currentGroups.contains(eh)) sl.add(eh);
                    }

                } catch (FindException e) {
                    e.printStackTrace();
                    throw new RuntimeException(e);
                }
            }
        });
        return listOutModel;
    }


    /** Updates Add button */
    private void updateAddButton() {
        getAddButton().setEnabled(0 != getNonGroupMemberList().getSelectedIndices().length);
    }


    private JButton getAddButton() {
        if (null == okButton) {
            okButton = new JButton();
            okButton.setText(resources.getString("okButton.label"));
            okButton.setToolTipText(resources.getString("okButton.tooltip"));
            okButton.setActionCommand(CMD_OK);
            okButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    windowAction(event.getActionCommand());
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
    private void windowAction(String actionCommand) {

        if (actionCommand == null) {
            // do nothing
        } else if (actionCommand.equals(CMD_CANCEL)) {
            this.dispose();
        } else if (actionCommand.equals(CMD_OK)) {
            addSelectedGroups();
        }
    }

    /** Adds selected users into the group */
    private void addSelectedGroups() {
        // Get selected users
        int[] groupsToAdd = getNonGroupMemberList().getSelectedIndices();
        int size = groupsToAdd.length;
        if (0 == size) {
            return;
        }

        Set groupHeaders = new HashSet();

        for (int i=size-1; i >=0;i--){
            groupHeaders.add(listOutModel.getElementAt(groupsToAdd[i]));
        }
        parent.addGroups(groupHeaders);
        dispose();
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
            setIcon(new ImageIcon(Utilities.loadImage(GroupPanel.GROUP_ICON_RESOURCE)));
            EntityHeader eh = (EntityHeader)value;
            setText(eh.getName());

            return this;
        }
    };


    /** UI Elements */
    private JButton okButton;

    /** Parent GroupPanel where users are to be added to */
    private UserGroupsPanel parent;
    private JScrollPane groupsOutListJScrollPane;
    private JLabel groupsNonMemberLabel;

    private ResourceBundle resources;
    private final String CMD_CANCEL = "cmd.cancel";
    private final String CMD_OK = "cmd.ok";
    private static final String NON_MEMBER_LABEL = "Not member of";

    private final ClassLoader cl = getClass().getClassLoader();
}

