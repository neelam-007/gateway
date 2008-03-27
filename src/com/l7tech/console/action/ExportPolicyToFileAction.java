package com.l7tech.console.action;

import com.l7tech.console.logging.ErrorManager;
import com.l7tech.console.policy.exporter.PolicyExporter;
import com.l7tech.console.tree.*;
import com.l7tech.console.util.TopComponents;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.common.security.rbac.OperationType;
import com.l7tech.common.gui.util.FileChooserUtil;

import org.xml.sax.SAXException;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.AccessControlException;


/**
 * The SSM action type that saves a policy to a file using the policy export mechanism.
 *
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Jul 21, 2004<br/>
 */
public abstract class ExportPolicyToFileAction extends SecureAction {
    private static final Logger log = Logger.getLogger(ExportPolicyToFileAction.class.getName());
    private final String homePath;

    /**
     *
     * @param homePath Path under which the template directory is located (may be null)
     */
    public ExportPolicyToFileAction(String homePath) {
        super(null);
        this.homePath = homePath;
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

    protected OperationType getOperation() {
        return OperationType.UPDATE;
    }

    /** Actually perform the action.
     * This is the method which should be called programmatically.

     * note on threading usage: do not access GUI components
     * without explicitly asking for the AWT event thread!
     */
    protected File exportPolicy(final String title, final Assertion rootAssertion) {
        return (File) AccessController.doPrivileged(new PrivilegedAction<Object>() {
            public Object run() {
                try {
                    return doFileExport(title, rootAssertion);
                } catch (AccessControlException e) {
                    TopComponents.getInstance().showNoPrivilegesErrorMessage();
                }
                return null;
            }
        });
    }

    private File doFileExport(String title, Assertion rootAssertion) {
        JFileChooser chooser;
        File templateDir = null;
        if (homePath != null) {
            try {
                templateDir = new File(homePath +
                                       File.separator + PolicyTemplatesFolderNode.TEMPLATES_DIR);
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
                return null;
            }
            chooser = new JFileChooser(templateDir);
        } else {
            chooser = new JFileChooser(FileChooserUtil.getStartingDirectory());
        }

        chooser.setDialogTitle(title);
        // Allow single selection only
        chooser.setMultiSelectionEnabled(false);
        chooser.setFileFilter(new FileFilter() {
            public boolean accept(File f) {
                return f.getAbsolutePath().endsWith(".xml") || f.getAbsolutePath().endsWith(".XML") || f.isDirectory();
            }
            public String getDescription() {
                return "XML Files";
            }
        });
        int ret = chooser.showSaveDialog(TopComponents.getInstance().getTopParent());
        if (JFileChooser.APPROVE_OPTION != ret) return null;
        String name = chooser.getSelectedFile().getPath();
        // add extension if not present
        if (!name.endsWith(".xml") && !name.endsWith(".XML")) {
            name = name + ".xml";
        }
        int overwrite = JOptionPane.YES_OPTION;
        File policyFile = new File(name);
        final boolean policyFileExists = policyFile.exists();
        if (policyFileExists) {
            overwrite = JOptionPane.showConfirmDialog(TopComponents.getInstance().getTopParent(),
                                                      "Overwrite " + name + "?",
                                                      "Warning",
                                                      JOptionPane.YES_NO_OPTION);
        }
        if (overwrite != JOptionPane.YES_OPTION) return null;
        try {
            serializeToFile(rootAssertion, policyFile);
            // only update template folder if this policy is saved in templates directory
            if (!policyFileExists && templateDir != null && templateDir.equals(chooser.getSelectedFile().getParentFile())) {
                insertIntoAssertionTree(policyFile);
            }
            return policyFile;
        } catch (IOException e) {
            ErrorManager.getDefault().notify(Level.WARNING, e, "Cannot export policy to file " + name);
        } catch (SAXException e) {
            ErrorManager.getDefault().notify(Level.WARNING, e, "Cannot export policy to file " + name);
        }

        return null;
    }

    protected void serializeToFile(Assertion rootAssertion, File policyFile) throws IOException, SAXException {
        PolicyExporter exporter = new PolicyExporter();
        exporter.exportToFile(rootAssertion, policyFile);
    }

    private void insertIntoAssertionTree(File policyFile) {
        JTree tree = (JTree)TopComponents.getInstance().getComponent(AssertionsTree.NAME);
        if (tree != null) {
            AbstractTreeNode node = (AbstractTreeNode)TreeNodeActions.nodeByName(PolicyTemplatesFolderNode.NAME,
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
