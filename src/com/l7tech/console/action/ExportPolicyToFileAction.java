package com.l7tech.console.action;

import com.l7tech.console.logging.ErrorManager;
import com.l7tech.console.tree.*;
import com.l7tech.console.tree.policy.AssertionTreeNode;
import com.l7tech.console.util.Preferences;
import com.l7tech.console.util.TopComponents;
import com.l7tech.policy.exporter.PolicyExporter;
import org.xml.sax.SAXException;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.filechooser.FileFilter;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * The SSM action type that saves a policy to a file using the policy export mechanism.
 *
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Jul 21, 2004<br/>
 */
public class ExportPolicyToFileAction extends SecureAction {
    static final Logger log = Logger.getLogger(ExportPolicyToFileAction.class.getName());
    protected AssertionTreeNode node;

    public ExportPolicyToFileAction() {
    }

    public ExportPolicyToFileAction(AssertionTreeNode node) {
        if (node == null) {
            throw new IllegalArgumentException();
        }
        this.node = node;
    }

    /**
     * @return the action name
     */
    public String getName() {
        return "Export Policy";
    }

    /**
     * @return the aciton description
     */
    public String getDescription() {
        return "Exports the policy to a file along with external references.";
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
            templateDir = new File(Preferences.getPreferences().getHomePath() +
                                   File.separator + PoliciesFolderNode.TEMPLATES_DIR);
            if (!templateDir.exists()) {
                if (!templateDir.mkdir()) {
                    throw new IOException("Cannot create " + templateDir.getPath());
                }
            }
        } catch (IOException e) {
            ErrorManager.getDefault().notify(Level.WARNING,
                                             e,
                                             "The system reported problem in accessing or creating" +
                                             "the policy template directory " + templateDir.getPath() + "\n" +
                                             "The policy template is not saved.");
            return;
        }

        JFileChooser chooser = new JFileChooser(templateDir);
        chooser.setDialogTitle("Export to ...");
        // Allow single selection only
        chooser.setMultiSelectionEnabled(false);
        chooser.setFileFilter(new FileFilter() {
            public boolean accept(File f) {
                if (f.getAbsolutePath().endsWith(".xml") || f.getAbsolutePath().endsWith(".XML")) {
                    return true;
                }
                if (f.isDirectory()) return true;
                return false;
            }
            public String getDescription() {
                return "XML Files";
            }
        });
        int ret = chooser.showSaveDialog(TopComponents.getInstance().getMainWindow());
        if (JFileChooser.APPROVE_OPTION != ret) return;
        String name = chooser.getSelectedFile().getPath();
        // add extension if not present
        if (!name.endsWith(".xml") && !name.endsWith(".XML")) {
            name = name + ".xml";
        }
        int overwrite = JOptionPane.YES_OPTION;
        File policyFile = new File(name);
        final boolean policyFileExists = policyFile.exists();
        if (policyFileExists) {
            overwrite = JOptionPane.showConfirmDialog(TopComponents.getInstance().getMainWindow(),
                                                      "Overwrite " + name + "?",
                                                      "Warning",
                                                      JOptionPane.YES_NO_OPTION);
        }
        if (overwrite != JOptionPane.YES_OPTION) return;
        PolicyExporter exporter = new PolicyExporter();
        try {
            exporter.exportToFile(node.asAssertion(), policyFile);
            // only update template folder if this policy is saved in templates directory
            if (!policyFileExists && templateDir.equals(chooser.getSelectedFile().getParentFile())) {
                insertIntoAssertionTree(policyFile);
            }
        } catch (IOException e) {
            ErrorManager.getDefault().notify(Level.WARNING, e, "Cannot export policy to file " + name);
        } catch (SAXException e) {
            ErrorManager.getDefault().notify(Level.WARNING, e, "Cannot export policy to file " + name);
        }
    }

    private void insertIntoAssertionTree(File policyFile) {
        JTree tree = (JTree)TopComponents.getInstance().getComponent(AssertionsTree.NAME);
        if (tree != null) {
            AbstractTreeNode node = (AbstractTreeNode)TreeNodeActions.nodeByName(PoliciesFolderNode.NAME,
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
