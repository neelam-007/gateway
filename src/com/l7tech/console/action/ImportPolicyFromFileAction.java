package com.l7tech.console.action;

import com.l7tech.console.logging.ErrorManager;
import com.l7tech.console.policy.exporter.PolicyImporter;
import com.l7tech.console.tree.PoliciesFolderNode;
import com.l7tech.console.util.TopComponents;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.wsp.WspWriter;
import com.l7tech.service.PublishedService;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.security.*;


/**
 * The SSM action type that imports a policy from a file.
 *
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Jul 21, 2004<br/>
 */
public abstract class ImportPolicyFromFileAction extends ServiceNodeAction {
    private static final Logger log = Logger.getLogger(ImportPolicyFromFileAction.class.getName());
    private final String homePath;

    /**
     *
     */
    public ImportPolicyFromFileAction(final String path) {
        super(null);
        this.homePath = path;
    }

    /**
     * @return the action name
     */
    public String getName() {
        return "Import Policy";
    }

    /**
     * @return the aciton description
     */
    public String getDescription() {
        return "Import a policy from a file along with external references.";
    }

    /**
     * subclasses override this method specifying the resource name
     */
    protected String iconResource() {
        return "com/l7tech/console/resources/saveTemplate.gif";
    }

    /**
     * Actually perform the action.
     * This is the method which should be called programmatically.
     */
    protected boolean importPolicy(PublishedService service) {
        try {
            return doFileImport(service);
        } catch (AccessControlException e) {
            TopComponents.getInstance().showNoPrivilegesErrorMessage();
        }
        return false;
    }

    private boolean doFileImport(PublishedService service) {
        // get file from user
        JFileChooser chooser;
        File templateDir = null;
        if (homePath != null) {
            try {
                templateDir = new File(homePath +
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
                return false;
            }
            chooser = new JFileChooser(templateDir);
        } else {
            chooser = new JFileChooser();
        }

        chooser.setDialogTitle("Import Policy");
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
        int ret = chooser.showOpenDialog(TopComponents.getInstance().getTopParent());
        if (JFileChooser.APPROVE_OPTION != ret) return false;

        try {
            Assertion newRoot = PolicyImporter.importPolicy(chooser.getSelectedFile());
            // for some reason, the PublishedService class does not allow to set a policy
            // directly, it must be set through the XML
            if (newRoot != null) {
                String newPolicyXml = WspWriter.getPolicyXml(newRoot);
                service.setPolicyXml(newPolicyXml);
                return true;
            }
        } catch (IOException e) {
            log.log(Level.WARNING, "could not localize or read policy from " + chooser.getSelectedFile().getPath(), e);
            JOptionPane.showMessageDialog(TopComponents.getInstance().getTopParent(),
                                          "Could not find policy export in the selected file",
                                          "Policy Not Found",
                                          JOptionPane.WARNING_MESSAGE);
        }

        return false;
    }
}
