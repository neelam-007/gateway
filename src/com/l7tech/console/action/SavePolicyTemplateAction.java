package com.l7tech.console.action;

import com.l7tech.console.logging.ErrorManager;
import com.l7tech.console.tree.*;
import com.l7tech.console.tree.policy.AssertionTreeNode;
import com.l7tech.console.util.Preferences;
import com.l7tech.console.util.TopComponents;
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
public class SavePolicyTemplateAction extends SecureAction {
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
        return "Save as Template";
    }

    /**
     * @return the aciton description
     */
    public String getDescription() {
        return "Save the policy assertions as a template";
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
    protected void performAction() {
        if (node == null) {
            throw new IllegalStateException("no node specified");
        }

        File templateDir = null;
        try {
            templateDir = new File(
              Preferences.getPreferences().getHomePath() +
              File.separator + PoliciesFolderNode.TEMPLATES_DIR);
            if (!templateDir.exists()) {
                if (!templateDir.mkdir())
                    throw new IOException("Cannot create " + templateDir.getPath());
            }
        } catch (IOException e) {
            ErrorManager.getDefault().
              notify(Level.WARNING, e,
                "The system reported problem in accessing or creating" +
                "the policy template directory " + templateDir.getPath() + "\n" +
                "The policy template is not saved.");
            return;
        }

        JFileChooser chooser = new JFileChooser(templateDir);
        chooser.setDialogTitle("Save as ...");
        // Allow single selection only
        chooser.setMultiSelectionEnabled(false);
        int ret =
          chooser.showSaveDialog(TopComponents.getInstance().getMainWindow());
        if (JFileChooser.APPROVE_OPTION != ret) return;
        String name = chooser.getSelectedFile().getPath();
        System.out.println(name);
        int overwrite = JOptionPane.YES_OPTION;
        File policyFile = new File(name);
        final boolean policyFileExists = policyFile.exists();
        if (policyFileExists) {
            overwrite =
              JOptionPane.showConfirmDialog(
                TopComponents.getInstance().getMainWindow(),
                "Overwrite " + name + "?",
                "Warning",
                JOptionPane.YES_NO_OPTION);
        }
        if (overwrite != JOptionPane.YES_OPTION) return;
        FileOutputStream fo = null;
        try {
            fo = new FileOutputStream(name, false);
            WspWriter.writePolicy(node.asAssertion(), fo);
            if (!policyFileExists) {
                insertIntoAssertionTree(policyFile);
            }
        } catch (FileNotFoundException e) {
            ErrorManager.getDefault().
              notify(Level.WARNING, e,
                "Cannot save policy template " + name);
        } catch (IOException e) {
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

    private void insertIntoAssertionTree(File policyFile) {
        JTree tree =
          (JTree)TopComponents.getInstance().getComponent(AssertionsTree.NAME);
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
                    PolicyTemplateNode ptn = new PolicyTemplateNode(policyFile);
                    model.insertNodeInto(ptn, node, node.getInsertPosition(ptn));
                }
            } else {
                log.log(Level.WARNING, "Unable to reach the palette tree.");
            }
        }


    }
}
