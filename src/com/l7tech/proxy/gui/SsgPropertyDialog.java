package com.l7tech.proxy.gui;

import com.l7tech.console.tree.EntityTreeCellRenderer;
import com.l7tech.console.tree.policy.PolicyTreeModel;
import com.l7tech.console.panels.Utilities;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.FalseAssertion;
import com.l7tech.policy.assertion.TrueAssertion;
import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.policy.assertion.composite.OneOrMoreAssertion;
import com.l7tech.policy.assertion.credential.http.HttpBasic;
import com.l7tech.policy.assertion.identity.SpecificUser;
import com.l7tech.proxy.datamodel.PolicyAttachmentKey;
import com.l7tech.proxy.datamodel.Ssg;
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

    private Ssg ssg; // The real Ssg instnace, to which changes may be committed.
    private int gridY = 0; // Used for layout

    private JComponent generalPane;
    private JTextField fieldName;
    private JLabel fieldLocalEndpoint;
    private JTextField fieldServerUrl;
    private JTextField fieldKeyStorePath;
    private JTextField fieldUsername;
    private JButton buttonClearPassword;
    private JButton buttonSetPassword;
    private JCheckBox cbPromptForPassword;
    private JLabel fieldPassword;
    private char[] editPassword;

    private JComponent policiesPane;
    private JTree policyTree;
    private JTable policyTable;
    private ArrayList displayPolicies;
    private DisplayPolicyTableModel displayPolicyTableModel;
    private JButton buttonFlushPolicies;
    private boolean policyFlushRequested = false;


    /** Create an SsgPropertyDialog ready to edit an Ssg instance. */
    private SsgPropertyDialog(final Ssg ssg) {
        super("SSG Properties");
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
    public static PropertyDialog getPropertyDialogForObject(final Ssg ssg) {
        return new SsgPropertyDialog(ssg);
    }

    /** Make a GridBagConstraints for a control, and move to next row. */
    private GridBagConstraints gbc() {
        return new GridBagConstraints(1, gridY++, 1, 1, 1000.0, 0.0,
                                      GridBagConstraints.WEST,
                                      GridBagConstraints.HORIZONTAL,
                                      new Insets(5, 5, 0, 5), 0, 0);
    }

    /** Make a GridBagConstraints for a label. */
    private GridBagConstraints gbcLabel() {
        return new GridBagConstraints(0, gridY, 1, 1, 0.0, 0.0,
                                      GridBagConstraints.EAST,
                                      GridBagConstraints.NONE,
                                      new Insets(5, 5, 0, 0), 0, 0);
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

            pane.add(new JLabel("SSG policies being cached by this client"),
                     new GridBagConstraints(0, y++, 1, 1, 1.0, 1.0,
                                            GridBagConstraints.CENTER,
                                            GridBagConstraints.BOTH,
                                            new Insets(6, 6, 6, 6), 3, 3));

            pane.add(new JLabel("Attachments:"),
                     new GridBagConstraints(0, y++, 2, 1, 1.0, 1.0,
                                            GridBagConstraints.CENTER,
                                            GridBagConstraints.BOTH,
                                            new Insets(6, 6, 6, 6), 3, 3));

            buttonFlushPolicies = new JButton("Clear Policy Cache");
            buttonFlushPolicies.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    policyFlushRequested = true;
                    updatePolicyPanel();
                }
            });
            pane.add(buttonFlushPolicies,
                     new GridBagConstraints(1, 0, 1, 1, 1.0, 1.0,
                                            GridBagConstraints.EAST,
                                            GridBagConstraints.NONE,
                                            new Insets(0, 0, 0, 0), 0, 0));

            displayPolicies = new ArrayList();
            displayPolicyTableModel = new DisplayPolicyTableModel();
            policyTable = new JTable(displayPolicyTableModel);
            policyTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            policyTable.setCellSelectionEnabled(false);
            policyTable.setRowSelectionAllowed(true);
            policyTable.setBorder(BorderFactory.createLineBorder(Color.BLACK));
            policyTable.setPreferredScrollableViewportSize(new Dimension(300, 100));
            policyTable.setPreferredSize(new Dimension(300, 100));
            policyTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
            policyTable.setAutoCreateColumnsFromModel(true);
            policyTable.getColumnModel().getColumn(0).setHeaderValue("Body URI");
            policyTable.getColumnModel().getColumn(1).setHeaderValue("SOAPAction");
            JScrollPane policyTableSp = new JScrollPane(policyTable);
            pane.add(policyTableSp,
                     new GridBagConstraints(0, y++, 2, 1, 1.0, 1.0,
                                            GridBagConstraints.CENTER,
                                            GridBagConstraints.BOTH,
                                            new Insets(6, 6, 6, 6), 3, 3));

            pane.add(new JLabel("Associated policy:"),
                     new GridBagConstraints(0, y++, 2, 1, 1.0, 1.0,
                                            GridBagConstraints.CENTER,
                                            GridBagConstraints.BOTH,
                                            new Insets(4, 6, 0, 6), 3, 3));

            policyTree = new JTree((TreeModel)null);
            policyTree.setBorder(BorderFactory.createLineBorder(Color.BLACK));
            policyTree.setPreferredSize(new Dimension(300, 200));
            policyTree.setCellRenderer(new EntityTreeCellRenderer());
            JScrollPane policyTreeSp = new JScrollPane(policyTree);
            pane.add(policyTreeSp,
                     new GridBagConstraints(0, y++, 2, 1, 1.0, 1.0,
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
            fieldName.setPreferredSize(new Dimension(200, 20));
            pane.add(new JLabel("Name:"), gbcLabel());
            pane.add(fieldName, gbc());

            fieldServerUrl = new JTextField();
            fieldServerUrl.setPreferredSize(new Dimension(250, 20));
            pane.add(new JLabel("SSG URL:"), gbcLabel());
            pane.add(fieldServerUrl, gbc());

            fieldLocalEndpoint = new JLabel("");
            fieldLocalEndpoint.setPreferredSize(new Dimension(200, 20));
            pane.add(new JLabel("Endpoint:"), gbcLabel());
            pane.add(fieldLocalEndpoint, gbc());

            fieldKeyStorePath = new JTextField();
            fieldKeyStorePath.setPreferredSize(new Dimension(200, 20));
            pane.add(new JLabel("Key store:"), gbcLabel());
            pane.add(fieldKeyStorePath, gbc());

            fieldUsername = new JTextField();
            fieldUsername.setPreferredSize(new Dimension(200, 20));
            pane.add(new JLabel("Username:"), gbcLabel());
            pane.add(fieldUsername, gbc());

            JPanel passwordStuff = new JPanel();
            passwordStuff.setLayout(new GridBagLayout());

            fieldPassword = new JLabel();
            pane.add(new JLabel("Password:"), gbcLabel());
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
            pane.add(passwordStuff, gbc());

            cbPromptForPassword = new JCheckBox("Prompt for username and password when needed");
            cbPromptForPassword.setPreferredSize(new Dimension(200, 20));
            pane.add(cbPromptForPassword, gbc());

            // Have a spacer eat any leftover space
            pane.add(new JPanel(),
                     new GridBagConstraints(0, gridY++, 1, 1, 1.0, 1.0,
                                            GridBagConstraints.CENTER,
                                            GridBagConstraints.BOTH,
                                            new Insets(0, 0, 0, 0), 0, 0));
        }

        return generalPane;
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
        fieldLocalEndpoint.setText("/" + ssg.getLocalEndpoint());
        fieldServerUrl.setText(ssg.getServerUrl());
        fieldKeyStorePath.setText(ssg.getKeyStorePath());
        fieldUsername.setText(ssg.getUsername());
        editPassword = ssg.getPassword();
        fieldPassword.setText(passwordToString(editPassword));
        cbPromptForPassword.setSelected(ssg.isPromptForUsernameAndPassword());
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
            ssg.setLocalEndpoint(fieldLocalEndpoint.getText());
            ssg.setServerUrl(fieldServerUrl.getText());
            ssg.setKeyStorePath(fieldKeyStorePath.getText());
            ssg.setUsername(fieldUsername.getText());
            ssg.setPassword(editPassword);
            ssg.setPromptForUsernameAndPassword(cbPromptForPassword.isSelected());
            if (policyFlushRequested)
                ssg.clearPolicies();
        }
        setSsg(ssg);
    }

    public static void main(String[] argv) {
        Ssg ssg = new Ssg(1, "Test SSG", "http://blah.bloof.com");
        log.info("SSG prompt bit: " + ssg.isPromptForUsernameAndPassword());
        ssg.attachPolicy("http://example.com/Quoter", null, new TrueAssertion());
        ssg.attachPolicy("http://blah", null, new AllAssertion(Arrays.asList(new Assertion[] {
            new HttpBasic(),
            new SpecificUser(444, "blahuser"),
        })));
        ssg.attachPolicy("http://example.com/Other", "http://example.com/soapaction/other",
                         new AllAssertion(Arrays.asList(new Assertion[] {
                             new TrueAssertion(),
                             new OneOrMoreAssertion(Arrays.asList(new Assertion[] {
                                 new TrueAssertion(),
                                 new FalseAssertion()
                             })),
                         })));
        SsgPropertyDialog.getPropertyDialogForObject(ssg).show();
        System.exit(0);
    }
}
