package com.l7tech.proxy.gui;

import com.l7tech.console.panels.Utilities;
import com.l7tech.console.tree.EntityTreeCellRenderer;
import com.l7tech.console.tree.policy.PolicyTreeModel;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.proxy.datamodel.PolicyAttachmentKey;
import com.l7tech.proxy.datamodel.Ssg;
import com.l7tech.proxy.ClientProxy;
import org.apache.log4j.Category;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.tree.TreeModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Panel for editing properties of an SSG object.
 * User: mike
 * Date: May 26, 2003
 * Time: 11:14:36 AM
 * To change this template use Options | File Templates.
 */
public class SsgPropertyDialog extends PropertyDialog {
    private static final Category log = Category.getInstance(SsgPropertyDialog.class);

    // Model
    private Ssg ssg; // The real Ssg instance, to which changes may be committed.

    // View
    private int gridY = 0; // Used for layout

    //   View for General pane
    private JComponent generalPane;
    private JTextField fieldName;
    private JLabel fieldLocalEndpoint;
    private JTextField fieldServerAddress;
    private JCheckBox cbDefault;
    private JTextField fieldUsername;
    private JButton buttonClearPassword;
    private JButton buttonSetPassword;
    private JCheckBox cbPromptForPassword;
    private JLabel fieldPassword;
    private char[] editPassword;

    //   View for Policy pane
    private JComponent policiesPane;
    private JTree policyTree;
    private JTable policyTable;
    private ArrayList displayPolicies;
    private DisplayPolicyTableModel displayPolicyTableModel;
    private JButton buttonFlushPolicies;
    private boolean policyFlushRequested = false;
    private ClientProxy clientProxy;


    /** Create an SsgPropertyDialog ready to edit an Ssg instance. */
    private SsgPropertyDialog(ClientProxy clientProxy, final Ssg ssg) {
        super("SSG Properties");
        this.clientProxy = clientProxy;
        tabbedPane.add("General", getGeneralPane());
        tabbedPane.add("Policies", getPoliciesPane());
        setSsg(ssg);
        pack();
    }

    /**
     * Attempt to build an "edit properties" dialog box for the given Ssg.
     * @param ssg The ssg whose properties we intend to edit
     * @return The property dialog that will edit said properties.  Call show() on it to run it.
     */
    public static SsgPropertyDialog makeSsgPropertyDialog(ClientProxy clientProxy, final Ssg ssg) {
        return new SsgPropertyDialog(clientProxy, ssg);
    }

    private class DisplayPolicyTableModel extends AbstractTableModel {
        public int getRowCount() {
            return displayPolicies.size();
        }

        public int getColumnCount() {
            return 2;
        }

        public Object getValueAt(int rowIndex, int columnIndex) {
            switch (columnIndex) {
                case 0:
                    return ((PolicyAttachmentKey)displayPolicies.get(rowIndex)).getUri();
                case 1:
                    return ((PolicyAttachmentKey)displayPolicies.get(rowIndex)).getSoapAction();
            }
            log.error("SsgPropertyDialog: policyTable: invalid columnIndex: " + columnIndex);
            return null;
        }
    }

    private String passwordToString(char[] password) {
        if (password == null) {
            return "<Not set>";
        } else if ("".equals(new String(password))) {
            return "<Empty password>";
        } else {
            char[] stars = new char[password.length];
            Arrays.fill(stars, '*');
            return new String(stars);
        }
    }

    private JComponent getPoliciesPane() {
        if (policiesPane == null) {
            int y = 0;
            JPanel pane = new JPanel(new GridBagLayout());
            policiesPane = new JScrollPane(pane);
            policiesPane.setBorder(BorderFactory.createEmptyBorder());

            pane.add(new JLabel("SSG policies being cached by this client"),
                     new GridBagConstraints(0, y++, 1, 1, 0.0, 0.0,
                                            GridBagConstraints.CENTER,
                                            GridBagConstraints.BOTH,
                                            new Insets(6, 6, 6, 6), 3, 3));

            pane.add(new JLabel("Web services with cached policies:"),
                     new GridBagConstraints(0, y++, 2, 1, 0.0, 0.0,
                                            GridBagConstraints.CENTER,
                                            GridBagConstraints.BOTH,
                                            new Insets(6, 6, 0, 6), 3, 3));

            buttonFlushPolicies = new JButton("Clear Policy Cache");
            buttonFlushPolicies.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    policyFlushRequested = true;
                    updatePolicyPanel();
                }
            });
            pane.add(buttonFlushPolicies,
                     new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
                                            GridBagConstraints.EAST,
                                            GridBagConstraints.NONE,
                                            new Insets(6, 6, 0, 6), 0, 0));

            displayPolicies = new ArrayList();
            displayPolicyTableModel = new DisplayPolicyTableModel();
            policyTable = new JTable(displayPolicyTableModel);
            policyTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            policyTable.setCellSelectionEnabled(false);
            policyTable.setRowSelectionAllowed(true);
            policyTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
            policyTable.setAutoCreateColumnsFromModel(true);
            policyTable.getColumnModel().getColumn(0).setHeaderValue("Body Namespace");
            policyTable.getColumnModel().getColumn(1).setHeaderValue("SOAPAction");
            JScrollPane policyTableSp = new JScrollPane(policyTable);
            policyTableSp.setPreferredSize(new Dimension(120, 120));
            pane.add(policyTableSp,
                     new GridBagConstraints(0, y++, 2, 1, 1.0, 1.0,
                                            GridBagConstraints.CENTER,
                                            GridBagConstraints.BOTH,
                                            new Insets(0, 6, 3, 6), 0, 0));

            pane.add(new JLabel("Associated policy:"),
                     new GridBagConstraints(0, y++, 2, 1, 0.0, 0.0,
                                            GridBagConstraints.CENTER,
                                            GridBagConstraints.BOTH,
                                            new Insets(4, 6, 0, 6), 0, 0));

            policyTree = new JTree((TreeModel)null);
            policyTree.setCellRenderer(new EntityTreeCellRenderer());
            JScrollPane policyTreeSp = new JScrollPane(policyTree);
            policyTreeSp.setPreferredSize(new Dimension(120, 120));
            pane.add(policyTreeSp,
                     new GridBagConstraints(0, y++, 2, 1, 100.0, 100.0,
                                            GridBagConstraints.CENTER,
                                            GridBagConstraints.BOTH,
                                            new Insets(2, 6, 6, 6), 3, 3));

            policyTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
                public void valueChanged(ListSelectionEvent e) {
                    displaySelectedPolicy();
                }
            });
        }
        return policiesPane;
    }

    private void displaySelectedPolicy() {
        // do this?    if (e.getValueIsAdjusting()) return;
        Assertion policy = null;
        int row = policyTable.getSelectedRow();
        if (row >= 0 && row < displayPolicies.size())
            policy = ssg.lookupPolicy((PolicyAttachmentKey)displayPolicies.get(row));
        policyTree.setModel(policy == null ? null : new PolicyTreeModel(policy));
    }

    /** Create panel controls.  Should be called only from a constructor. */
    private JComponent getGeneralPane() {
        if (generalPane == null) {
            gridY = 0;
            JPanel pane = new JPanel(new GridBagLayout());
            generalPane = new JScrollPane(pane);
            generalPane.setBorder(BorderFactory.createEmptyBorder());

            fieldName = new JTextField();
            pane.add(new JLabel("Name:"),
                     new GridBagConstraints(0, gridY, 1, 1, 0.0, 0.0,
                                            GridBagConstraints.EAST,
                                            GridBagConstraints.NONE,
                                            new Insets(15, 5, 0, 0), 0, 0));
            pane.add(fieldName,
                     new GridBagConstraints(1, gridY++, 1, 1, 1000.0, 0.0,
                                            GridBagConstraints.WEST,
                                            GridBagConstraints.HORIZONTAL,
                                            new Insets(15, 5, 0, 5), 0, 0));

            getFieldServerAddress();
            pane.add(new JLabel("SSG Hostname:"),
                     new GridBagConstraints(0, gridY, 1, 1, 0.0, 0.0,
                                            GridBagConstraints.EAST,
                                            GridBagConstraints.NONE,
                                            new Insets(5, 5, 0, 0), 0, 0));
            pane.add(fieldServerAddress,
                     new GridBagConstraints(1, gridY++, 1, 1, 1000.0, 0.0,
                                            GridBagConstraints.WEST,
                                            GridBagConstraints.HORIZONTAL,
                                            new Insets(5, 5, 0, 5), 0, 0));

            // Authentication panel

            JPanel authp = new JPanel(new GridBagLayout());
            authp.setBorder(BorderFactory.createTitledBorder("Username and password"));
            pane.add(authp,
                     new GridBagConstraints(0, gridY++, 2, 1, 1000.0, 0.0,
                                            GridBagConstraints.WEST,
                                            GridBagConstraints.HORIZONTAL,
                                            new Insets(15, 5, 5, 5), 0, 0));

            int oy = gridY;
            gridY = 0;

            fieldUsername = new JTextField();
            fieldUsername.setPreferredSize(new Dimension(200, 20));
            authp.add(new JLabel("Username:"),
                      new GridBagConstraints(0, gridY, 1, 1, 0.0, 0.0,
                                             GridBagConstraints.EAST,
                                             GridBagConstraints.NONE,
                                             new Insets(5, 5, 0, 0), 0, 0));
            authp.add(fieldUsername,
                      new GridBagConstraints(1, gridY++, 1, 1, 1000.0, 0.0,
                                             GridBagConstraints.WEST,
                                             GridBagConstraints.HORIZONTAL,
                                             new Insets(5, 5, 0, 5), 0, 0));

            JPanel passwordStuff = new JPanel();
            passwordStuff.setLayout(new GridBagLayout());

            fieldPassword = new JLabel();
            authp.add(new JLabel("Password:"),
                      new GridBagConstraints(0, gridY, 1, 1, 0.0, 0.0,
                                             GridBagConstraints.EAST,
                                             GridBagConstraints.NONE,
                                             new Insets(5, 5, 0, 0), 0, 0));
            passwordStuff.add(fieldPassword,
                              new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0,
                                                     GridBagConstraints.WEST,
                                                     GridBagConstraints.BOTH,
                                                     new Insets(0, 0, 0, 0), 0, 0));

            buttonClearPassword = new JButton("Clear");
            buttonClearPassword.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    editPassword = null;
                    fieldPassword.setText(passwordToString(editPassword));
                }
            });
            passwordStuff.add(buttonClearPassword,
                              new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
                                                     GridBagConstraints.EAST,
                                                     GridBagConstraints.NONE,
                                                     new Insets(0, 0, 0, 0), 0, 0));

            buttonSetPassword = new JButton("Change");
            buttonSetPassword.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    char[] word = PasswordDialog.getPassword(Gui.getInstance().getFrame(), "Set Password");
                    if (word != null) {
                        editPassword = word;
                        fieldPassword.setText(passwordToString(editPassword));
                    }
                }
            });
            passwordStuff.add(buttonSetPassword,
                                new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0,
                                                       GridBagConstraints.EAST,
                                                       GridBagConstraints.NONE,
                                                       new Insets(0, 0, 0, 0), 0, 0));

            Utilities.equalizeButtonSizes(new AbstractButton[] { buttonClearPassword, buttonSetPassword });
            authp.add(passwordStuff, new GridBagConstraints(1, gridY++, 1, 1, 1000.0, 0.0,
                                                              GridBagConstraints.WEST,
                                                              GridBagConstraints.HORIZONTAL,
                                                              new Insets(5, 5, 0, 5), 0, 0));

            cbPromptForPassword = new JCheckBox("Prompt for username and password as needed");
            authp.add(cbPromptForPassword, new GridBagConstraints(1, gridY++, 1, 1, 1000.0, 0.0,
                                                              GridBagConstraints.WEST,
                                                              GridBagConstraints.HORIZONTAL,
                                                              new Insets(5, 5, 0, 5), 0, 0));
            gridY = oy;

            // Endpoint panel

            JPanel epp = new JPanel(new GridBagLayout());
            epp.setBorder(BorderFactory.createTitledBorder("Local endpoint binding"));
            pane.add(epp, new GridBagConstraints(0, gridY++, 2, 1, 1000.0, 0.0,
                                                              GridBagConstraints.WEST,
                                                              GridBagConstraints.HORIZONTAL,
                                                              new Insets(14, 5, 5, 5), 0, 0));

            oy = gridY;
            gridY = 0;
            fieldLocalEndpoint = new JLabel("");
            fieldLocalEndpoint.setPreferredSize(new Dimension(120, 20));
            epp.add(new JLabel("Endpoint:"), new GridBagConstraints(0, gridY, 1, 1, 0.0, 0.0,
                                                              GridBagConstraints.EAST,
                                                              GridBagConstraints.NONE,
                                                              new Insets(5, 5, 0, 0), 0, 0));
            epp.add(fieldLocalEndpoint, new GridBagConstraints(1, gridY++, 1, 1, 1000.0, 0.0,
                                                              GridBagConstraints.WEST,
                                                              GridBagConstraints.HORIZONTAL,
                                                              new Insets(5, 5, 0, 5), 0, 0));

            cbDefault = new JCheckBox("Route unknown endpoints to this SSG");
            cbDefault.setPreferredSize(new Dimension(120, 20));
            epp.add(cbDefault, new GridBagConstraints(1, gridY++, 1, 1, 1000.0, 0.0,
                                                              GridBagConstraints.WEST,
                                                              GridBagConstraints.HORIZONTAL,
                                                              new Insets(5, 5, 0, 5), 0, 0));
            gridY = oy;

            // Have a spacer eat any leftover space
            pane.add(new JPanel(),
                     new GridBagConstraints(0, gridY++, 1, 1, 1.0, 1.0,
                                            GridBagConstraints.CENTER,
                                            GridBagConstraints.BOTH,
                                            new Insets(0, 0, 0, 0), 0, 0));
        }

        return generalPane;
    }

    /** Get the Server URL text field. */
    private JTextField getFieldServerAddress() {
        if (fieldServerAddress == null) {
            fieldServerAddress = new JTextField();
            fieldServerAddress.setPreferredSize(new Dimension(250, 20));
        }
        return fieldServerAddress;
    }

    /** Update the policy display panel with information from the Ssg bean. */
    private void updatePolicyPanel() {
        displayPolicies.clear();
        if (!policyFlushRequested)
            displayPolicies = new ArrayList(ssg.getPolicyAttachmentKeys());
        displayPolicyTableModel.fireTableDataChanged();
        displaySelectedPolicy();
    }

    /** Set the Ssg object being edited by this panel. */
    public void setSsg(final Ssg ssg) {
        this.ssg = ssg;

        fieldName.setText(ssg.getName());
        fieldLocalEndpoint.setText("http://localhost:" + clientProxy.getBindPort() + "/" +
                                       ssg.getLocalEndpoint());
        fieldServerAddress.setText(ssg.getSsgAddress());
        fieldUsername.setText(ssg.getUsername());
        editPassword = ssg.password();
        fieldPassword.setText(passwordToString(editPassword));
        cbPromptForPassword.setSelected(ssg.promptForUsernameAndPassword());
        cbDefault.setSelected(ssg.isDefaultSsg());
        policyFlushRequested = false;

        updatePolicyPanel();
    }

    /**
     * Called when the Ok button is pressed.
     * Should copy any updated properties into the target object and return normally.
     * Caller is responsible for hiding and disposing of the property dialog.
     */
    protected void commitChanges() {
        synchronized (ssg) {
            ssg.setName(fieldName.getText());
            ssg.setSsgAddress(fieldServerAddress.getText().trim().toLowerCase());
            ssg.setUsername(fieldUsername.getText().trim());
            ssg.password(editPassword);
            ssg.promptForUsernameAndPassword(cbPromptForPassword.isSelected());
            ssg.setDefaultSsg(cbDefault.isSelected());
            if (policyFlushRequested)
                ssg.clearPolicies();
        }
        setSsg(ssg);
    }
}
