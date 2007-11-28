package com.l7tech.console.panels;

import com.l7tech.common.gui.SimpleTableModel;
import com.l7tech.common.gui.util.DialogDisplayer;
import com.l7tech.common.gui.util.TableUtil;
import static com.l7tech.common.gui.util.TableUtil.column;
import com.l7tech.common.gui.util.Utilities;
import com.l7tech.common.policy.PolicyVersion;
import com.l7tech.common.util.BeanUtils;
import com.l7tech.common.util.ExceptionUtils;
import com.l7tech.common.util.Functions;
import com.l7tech.console.tree.policy.PolicyTreeCellRenderer;
import com.l7tech.console.tree.policy.PolicyTreeModel;
import com.l7tech.console.util.Registry;
import com.l7tech.objectmodel.FindException;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.wsp.WspReader;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.text.DateFormat;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Dialog that allows admin to view versions of a policy, to name them, and to
 * designate a new active version.
 */
public class PolicyRevisionsDialog extends JDialog {
    protected static final Logger logger = Logger.getLogger(PolicyRevisionsDialog.class.getName());

    private JPanel mainForm;
    private JTable versionTable;
    private JButton editButton;
    private JButton setActiveButton;
    private JButton closeButton;
    private JTree policyTree;
    private JButton clearActiveButton;
    private JButton setCommentButton;

    private final long policyOid;
    private final DateFormat dateFormat = DateFormat.getInstance();
    private SimpleTableModel<PolicyVersion> tableModel;

    public PolicyRevisionsDialog(Frame owner, long policyOid) {
        super(owner);
        this.policyOid = policyOid;
        initialize();
    }

    public PolicyRevisionsDialog(Dialog owner, long policyOid) {
        super(owner);
        this.policyOid = policyOid;
        initialize();
    }

    private void initialize() {
        setTitle("Policy Revisions");
        setModal(true);
        setContentPane(mainForm);

        tableModel = TableUtil.configureTable(versionTable,
                                              column("A", 12, 24, 24, property("active")),
                                              column("Vers.", 12, 32, 64, property("ordinal")),
                                              column("Administrator", 32, 64, 999999, property("userLogin")),
                                              column("Date and Time", 64, 120, 220, new Functions.Unary<Object,PolicyVersion>() {
                                                  public Object call(PolicyVersion policyVersion) {
                                                      return dateFormat.format(policyVersion.getTime());
                                                  }
                                              }),
                                              column("Comment", 128, 128, 999999, property("name")));

        versionTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                showSelectedPolicy();
            }
        });

        closeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });

        pack();
        Utilities.centerOnScreen(this);

        fetchUpdatedRows();
        
        // TODO select the active revision
        showSelectedPolicy();
    }

    private final TreeModel emptyTreeModel = new DefaultTreeModel(new DefaultMutableTreeNode(""));
    private final TreeCellRenderer defaultRenderer = new DefaultTreeCellRenderer();
    private final PolicyTreeCellRenderer policyTreeCellRenderer = new PolicyTreeCellRenderer();

    private void showSelectedPolicy() {
        int row = versionTable.getSelectedRow();
        if (row < 0) {
            policyTree.setCellRenderer(defaultRenderer);
            policyTree.setModel(emptyTreeModel);
            return;
        }
        PolicyVersion version = tableModel.getRowObject(row);
        try {
            String xml = getVersionXml(version);
            if (xml == null) {
                policyTree.setCellRenderer(defaultRenderer);
                policyTree.setModel(new DefaultTreeModel(new DefaultMutableTreeNode("Empty policy", false)));
                return;
            }
            Assertion assertion = WspReader.getDefault().parsePermissively(xml);
            TreeModel model = new PolicyTreeModel(assertion);
            policyTree.setModel(model);
            policyTree.setCellRenderer(policyTreeCellRenderer);
            Utilities.expandTree(policyTree);
        } catch (IOException e1) {
            policyTree.setCellRenderer(defaultRenderer);
            policyTree.setModel(new DefaultTreeModel(new DefaultMutableTreeNode("Bad policy XML: " + ExceptionUtils.getMessage(e1), false)));
        }
    }

    private String getVersionXml(PolicyVersion version) {
        String xml = version.getXml();
        if (xml == null) {
            // Fault in XML from server as revisions are clicked on for the first time
            try {
                PolicyVersion fullVersion = Registry.getDefault().getPolicyAdmin().findPolicyVersionByPrimaryKey(policyOid, version.getOid());
                BeanUtils.copyProperties(fullVersion, version);
                xml = version.getXml();
            } catch (Exception e) {
                showErrorMessage("Unable to Load Revision", "Unable to load the policy XML for a policy revision: " + ExceptionUtils.getMessage(e), e);
            }
        }
        return xml;
    }

    private static Functions.Unary<Object, PolicyVersion> property(String propName) {
        return Functions.propertyTransform(PolicyVersion.class, propName);
    }

    private void fetchUpdatedRows() {
        try {
            // TODO preserve selection and restore it after the refresh, if selected row still exists
            tableModel.setRows(Registry.getDefault().getPolicyAdmin().findPolicyVersionHeadersByPolicy(policyOid));
        } catch (FindException e) {
            showErrorMessage("Unable to Load Revisions", "There was an error while loading policy revisions.\n\nError message: " + ExceptionUtils.getMessage(e), e);
        }
    }

    private void showErrorMessage(String title, String msg, Throwable e) {
        logger.log(Level.WARNING, msg, e);
        DialogDisplayer.showMessageDialog(this, msg, title, JOptionPane.ERROR_MESSAGE, null);
    }
}
