package com.l7tech.console.action;

import com.l7tech.console.logging.ErrorManager;
import com.l7tech.console.tree.*;
import com.l7tech.console.tree.policy.AssertionTreeNode;
import com.l7tech.console.util.Preferences;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.ComponentRegistry;
import com.l7tech.policy.wsp.WspWriter;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * The <code>SavePolicyTemplateAction</code> action saves the policy as
 * as template assertion tree element.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 */
public class SavePolicyTemplateAction extends BaseAction {
    static final Logger log = Logger.getLogger(SavePolicyTemplateAction.class.getName());
    protected AssertionTreeNode node;

    public SavePolicyTemplateAction() {
    }

    public SavePolicyTemplateAction(AssertionTreeNode node) {
        if (node == null) {
            throw new IllegalArgumentException();
        }
        this.node = node;
    }

    /**
     * @return the action name
     */
    public String getName() {
        return "Save policy template";
    }

    /**
     * @return the aciton description
     */
    public String getDescription() {
        return "Save as the policy template";
    }

    /**
     * subclasses override this method specifying the resource name
     */
    protected String iconResource() {
        return "com/l7tech/console/resources/saveTemplate.gif";
    }

    /** Actually perform the action.
     * This is the method which should be called programmatically.

     * note on threading usage: do not access GUI components
     * without explicitly asking for the AWT event thread!
     */
    public void performAction() {
        if (node == null) {
            throw new IllegalStateException("no node specified");
        }

        File templateDir;
        try {
            templateDir = new File(
              Preferences.getPreferences().getHomePath() +
              File.separator + PoliciesFolderNode.TEMPLATES_DIR);
        } catch (IOException e) {
            templateDir = new File(System.getProperty("user.home"));
        }

        JFileChooser chooser = new JFileChooser(templateDir);
        chooser.setDialogTitle("Save as ...");
        // Allow single selection only
        chooser.setMultiSelectionEnabled(false);
        int ret =
          chooser.showSaveDialog(Registry.
          getDefault().
          getWindowManager().getMainWindow());
        if (JFileChooser.APPROVE_OPTION != ret) return;
        String name = chooser.getSelectedFile().getPath();
        System.out.println(name);
        int overwrite = JOptionPane.YES_OPTION;
        File policyFile = new File(name);
        if (policyFile.exists()) {
            overwrite =
              JOptionPane.showConfirmDialog(
                Registry.getDefault().getWindowManager().getMainWindow(),
                "Overwrite " + name + "?",
                "Warning",
                JOptionPane.YES_NO_OPTION);
        }
        if (overwrite != JOptionPane.YES_OPTION) return;
        FileOutputStream fo = null;
        try {
            fo = new FileOutputStream(name, false);
            WspWriter.writePolicy(node.asAssertion(), fo);
            updateAssertionTree(policyFile);
        } catch (FileNotFoundException e) {
            ErrorManager.getDefault().
              notify(Level.WARNING, e,
                "Cannot save policy template " + name);
        } finally {
            if (fo != null) {
                try {
                    fo.close();
                } catch (IOException e) {
                    ErrorManager.getDefault().
                      notify(Level.WARNING, e,
                        "Cannot save policy template " + name);
                }
            }
        }
    }

    private void updateAssertionTree(File policyFile) {
        JTree tree =
          (JTree)ComponentRegistry.getInstance().getComponent(AssertionsTree.NAME);
        if (tree != null) {
            AbstractTreeNode node =
              (AbstractTreeNode)TreeNodeActions.
              nodeByName(PoliciesFolderNode.NAME,
                (DefaultMutableTreeNode)tree.getModel().getRoot());
            if (node != null) {
                TreeNode[] nodes = node.getPath();
                TreePath nPath = new TreePath(nodes);
                if (tree.hasBeenExpanded(nPath)) {
                    DefaultTreeModel model = (DefaultTreeModel)tree.getModel();
                    model.
                      insertNodeInto(new PolicyTemplateNode(policyFile),
                        node, node.getChildCount());
                }
            } else {
                log.log(Level.WARNING, "Unable to reach the palette tree.");
            }
        }


    }
}
