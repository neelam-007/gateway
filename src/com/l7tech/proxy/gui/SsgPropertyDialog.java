package com.l7tech.proxy.gui;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.FalseAssertion;
import com.l7tech.policy.assertion.TrueAssertion;
import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.policy.assertion.composite.OneOrMoreAssertion;
import com.l7tech.proxy.datamodel.Ssg;
import com.l7tech.console.tree.policy.PolicyTreeModel;
import org.apache.log4j.Category;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreeModel;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

/**
 * Panel for editing properties of an SSG object.
 * User: mike
 * Date: May 26, 2003
 * Time: 11:14:36 AM
 * To change this template use Options | File Templates.
 */
public class SsgPropertyDialog extends PropertyDialog {
    private final Category log = Category.getInstance(SsgPropertyDialog.class);

    private Ssg ssg; // The real Ssg instnace, to which changes may be committed.
    private int gridY = 0; // Used for layout

    private JComponent generalPane;
    private JTextField fieldName;
    private JTextField fieldLocalEndpoint;
    private JTextField fieldServerUrl;
    private JTextField fieldKeyStorePath;

    private JComponent policiesPane;
    private JTree policyTree;
    private JTable policyTable;
    private ArrayList displayPolicies;
    private DisplayPolicyTableModel displayPolicyTableModel;


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

    private class DisplayPolicy {
        String headerType;  // URI or SOAPAction
        String headerValue; // ie, http://www.example.com/services/StockQuote
        Assertion policy;   // root of the policy tree
        DisplayPolicy(String headerType, String headerValue, Assertion policy) {
            this.headerType = headerType;
            this.headerValue = headerValue;
            this.policy = policy;
        }
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
                    return ((DisplayPolicy)displayPolicies.get(rowIndex)).headerType;
                case 1:
                    return ((DisplayPolicy)displayPolicies.get(rowIndex)).headerValue;
            }
            log.error("SsgPropertyDialog: policyTable: invalid columnIndex: " + columnIndex);
            return null;
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
                     new GridBagConstraints(0, y++, 1, 1, 1.0, 1.0,
                                            GridBagConstraints.CENTER,
                                            GridBagConstraints.BOTH,
                                            new Insets(6, 6, 6, 6), 3, 3));

            displayPolicies = new ArrayList();
            displayPolicyTableModel = new DisplayPolicyTableModel();
            policyTable = new JTable(displayPolicyTableModel);
            policyTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            policyTable.setCellSelectionEnabled(false);
            policyTable.setRowSelectionAllowed(true);
            policyTable.setBorder(BorderFactory.createLineBorder(Color.BLACK));
            policyTable.setPreferredScrollableViewportSize(new Dimension(300, 100));
            policyTable.setPreferredSize(new Dimension(300, 100));
            policyTable.getColumnModel().getColumn(0).setHeaderValue("Attachment key");
            policyTable.getColumnModel().getColumn(1).setHeaderValue("Value");
            JScrollPane policyTableSp = new JScrollPane(policyTable);
            pane.add(policyTableSp,
                     new GridBagConstraints(0, y++, 1, 1, 1.0, 1.0,
                                            GridBagConstraints.CENTER,
                                            GridBagConstraints.BOTH,
                                            new Insets(6, 6, 6, 6), 3, 3));

            pane.add(new JLabel("Associated policy:"),
                     new GridBagConstraints(0, y++, 1, 1, 1.0, 1.0,
                                            GridBagConstraints.CENTER,
                                            GridBagConstraints.BOTH,
                                            new Insets(4, 6, 0, 6), 3, 3));

            policyTree = new JTree((TreeModel)null);
            policyTree.setBorder(BorderFactory.createLineBorder(Color.BLACK));
            policyTree.setPreferredSize(new Dimension(300, 200));
            JScrollPane policyTreeSp = new JScrollPane(policyTree);
            pane.add(policyTreeSp,
                     new GridBagConstraints(0, y++, 1, 1, 1.0, 1.0,
                                            GridBagConstraints.CENTER,
                                            GridBagConstraints.BOTH,
                                            new Insets(2, 6, 6, 6), 3, 3));

            policyTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
                public void valueChanged(ListSelectionEvent e) {
                    // do this?    if (e.getValueIsAdjusting()) return;
                    Assertion policy = null;
                    int row = policyTable.getSelectedRow();
                    if (row >= 0 && row < displayPolicies.size())
                        policy = ((DisplayPolicy)displayPolicies.get(row)).policy;
                    policyTree.setModel(policy == null ? null : new PolicyTreeModel(policy));
                }
            });
        }
        return policiesPane;
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

            fieldLocalEndpoint = new JTextField();
            fieldLocalEndpoint.setPreferredSize(new Dimension(200, 20));
            pane.add(new JLabel("Endpoint:"), gbcLabel());
            pane.add(fieldLocalEndpoint, gbc());

            fieldServerUrl = new JTextField();
            fieldServerUrl.setPreferredSize(new Dimension(250, 20));
            pane.add(new JLabel("SSG URL:"), gbcLabel());
            pane.add(fieldServerUrl, gbc());

            fieldKeyStorePath = new JTextField();
            fieldKeyStorePath.setPreferredSize(new Dimension(200, 20));
            pane.add(new JLabel("Key store:"), gbcLabel());
            pane.add(fieldKeyStorePath, gbc());

            // Have a spacer eat any leftover space
            pane.add(new JPanel(),
                     new GridBagConstraints(0, gridY++, 1, 1, 1.0, 1.0,
                                            GridBagConstraints.CENTER,
                                            GridBagConstraints.BOTH,
                                            new Insets(0, 0, 0, 0), 0, 0));
        }

        return generalPane;
    }

    /** Set the Ssg object being edited by this panel. */
    public void setSsg(final Ssg ssg) {
        this.ssg = ssg;

        fieldName.setText(ssg.getName());
        fieldLocalEndpoint.setText(ssg.getLocalEndpoint());
        fieldServerUrl.setText(ssg.getServerUrl());
        fieldKeyStorePath.setText(ssg.getKeyStorePath());

        displayPolicies.clear();
        Map saMap = ssg.getPoliciesBySoapAction();
        if (saMap != null) {
            Collection ssgsBySoapAction = saMap.keySet();
            for (Iterator i = ssgsBySoapAction.iterator(); i.hasNext();) {
                String soapAction = (String) i.next();
                displayPolicies.add(new DisplayPolicy("SOAPAction header", soapAction,
                                                      ssg.getPolicyBySoapAction(soapAction)));
                log.info("Noted policy for SOAPAction=" + soapAction);
            }
        }
        Map uriMap = ssg.getPoliciesByUri();
        if (uriMap != null) {
            Collection ssgsByUri = uriMap.keySet();
            for (Iterator i = ssgsByUri.iterator(); i.hasNext();) {
                String uri = (String) i.next();
                displayPolicies.add(new DisplayPolicy("Body namespace URI", uri,
                                                      ssg.getPolicyByUri(uri)));
                log.info("Noted policy for URI=" + uri);
            }
        }
        displayPolicyTableModel.fireTableDataChanged();
    }

    /**
     * Called when the Ok button is pressed.
     * Should copy any updated properties into the target object and return normally.
     * Caller is responsible for hiding and disposing of the property dialog.
     */
    protected void commitChanges() {
        ssg.setName(fieldName.getText());
        ssg.setLocalEndpoint(fieldLocalEndpoint.getText());
        ssg.setServerUrl(fieldServerUrl.getText());
        ssg.setKeyStorePath(fieldKeyStorePath.getText());
        setSsg(ssg);
    }

    public static void main(String[] argv) {
        Ssg ssg = new Ssg(1, "Test SSG", "ssg", "http://blah.bloof.com");
        ssg.attachPolicy("http://example.com/Quoter", null, new TrueAssertion());
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
