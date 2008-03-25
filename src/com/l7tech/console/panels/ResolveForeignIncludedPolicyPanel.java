package com.l7tech.console.panels;

import com.l7tech.console.policy.exporter.ExternalSchemaReference;
import com.l7tech.console.policy.exporter.IncludedPolicyReference;
import com.l7tech.common.gui.util.Utilities;
import com.l7tech.common.gui.util.DialogDisplayer;
import com.l7tech.common.policy.Policy;
import com.l7tech.console.util.Registry;
import com.l7tech.console.tree.policy.PolicyTreeModel;
import com.l7tech.console.tree.policy.PolicyTreeCellRenderer;
import com.l7tech.objectmodel.FindException;

import javax.swing.*;
import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.io.IOException;

/**
 * This wizard panel allows the administrator to take action on a conflicting policy fragment
 * refered to in the imported policy.
 *
 * <p/>
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: njordan<br/>
 * Date: Mar 25, 2008<br/>
 */
public class ResolveForeignIncludedPolicyPanel extends WizardStepPanel {
    public static class NoLongerApplicableException extends Exception {
    }
    
    private static final Logger logger = Logger.getLogger(ResolveExternalSchemaReferencePanel.class.getName());
    private JPanel mainPanel;
    private IncludedPolicyReference foreignIncludedPolicyReference;
    private Policy importedPolicy;
    private Policy existingPolicy;
    private JRadioButton useExistingRadio;
    private JRadioButton overwriteRadio;
    private JRadioButton renameRadio;
    private JTree importedPolicyTree;
    private JTree existingPolicyTree;
    private JTextField newName;

    public ResolveForeignIncludedPolicyPanel(WizardStepPanel next, IncludedPolicyReference policyRef)
    throws NoLongerApplicableException, IOException
    {
        super(next);
        foreignIncludedPolicyReference = policyRef;
        importedPolicy = new Policy(foreignIncludedPolicyReference.getType(),
                foreignIncludedPolicyReference.getName(),
                foreignIncludedPolicyReference.getXml(),
                foreignIncludedPolicyReference.isSoap());

        try {
            existingPolicy = Registry.getDefault().getPolicyAdmin().findPolicyByUniqueName(foreignIncludedPolicyReference.getName());
        } catch(FindException e) {
            throw new NoLongerApplicableException();
        }

        initialize();
    }

    private void initialize() throws IOException {
        setLayout(new BorderLayout());
        add(mainPanel);

        createPolicyTree(importedPolicyTree, importedPolicy);
        createPolicyTree(existingPolicyTree, existingPolicy);

        ButtonGroup actionRadios = new ButtonGroup();
        actionRadios.add(useExistingRadio);
        actionRadios.add(overwriteRadio);
        actionRadios.add(renameRadio);
        renameRadio.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                JRadioButton button = (JRadioButton)e.getSource();
                newName.setEnabled(button.isSelected());
            }
        });

        newName.setEnabled(false);
        useExistingRadio.setSelected(true);
    }

    private void createPolicyTree(JTree tree, Policy policy) throws IOException {
        tree.setRootVisible(false);
        tree.setShowsRootHandles(true);
        tree.setRowHeight((int)(tree.getRowHeight() * 1.3));
        tree.setCellRenderer(new PolicyTreeCellRenderer());
        PolicyTreeModel treeModel = PolicyTreeModel.make(policy.getAssertion());
        tree.setModel(treeModel);
        TreeNode root = (TreeNode)treeModel.getRoot();
        TreePath rootPath = new TreePath(((DefaultMutableTreeNode)root).getPath());
        tree.expandPath(rootPath);
    }

    public boolean onNextButton() {
        if(useExistingRadio.isSelected()) {
            foreignIncludedPolicyReference.setUseType(IncludedPolicyReference.UseType.USE_EXISTING);
        } else if(overwriteRadio.isSelected()) {
            foreignIncludedPolicyReference.setUseType(IncludedPolicyReference.UseType.UPDATE);
        } else if(renameRadio.isSelected()) {
            if(newName.getText().trim().length() == 0) {
                JOptionPane.showMessageDialog(this, "No new name was entered.", "Error", JOptionPane.ERROR_MESSAGE);
                return false;
            } else if(newName.getText().equals(foreignIncludedPolicyReference.getName())) {
                JOptionPane.showMessageDialog(this, "New name is the same as the old name.", "Error", JOptionPane.ERROR_MESSAGE);
                return false;
            } else {
                try {
                    Policy p = Registry.getDefault().getPolicyAdmin().findPolicyByUniqueName(newName.getText());
                    if(p != null) {
                        JOptionPane.showMessageDialog(this, "There is already a policy with the name \"" + newName.getText() + "\".", "Error", JOptionPane.ERROR_MESSAGE);
                        return false;
                    }
                } catch(FindException e) {
                    // Ignore, the new name should be unique
                }
            }

            foreignIncludedPolicyReference.setUseType(IncludedPolicyReference.UseType.RENAME);
            foreignIncludedPolicyReference.setOldName(foreignIncludedPolicyReference.getName());
            foreignIncludedPolicyReference.setName(newName.getText());
        }

        return true;
    }

    public String getDescription() {
        return getStepLabel();
    }

    public String getStepLabel() {
        return "Conflicting policy fragment " + foreignIncludedPolicyReference.getName();
    }

    public boolean canFinish() {
        return !hasNextPanel();
    }

}