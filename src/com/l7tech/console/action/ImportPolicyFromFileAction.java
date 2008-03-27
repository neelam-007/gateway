/*
 * Copyright (C) 2004-2007 Layer 7 Technologies Inc.
 */
package com.l7tech.console.action;

import com.l7tech.common.gui.util.DialogDisplayer;
import com.l7tech.common.gui.util.FileChooserUtil;
import com.l7tech.common.policy.Policy;
import com.l7tech.console.logging.ErrorManager;
import com.l7tech.console.policy.exporter.PolicyImporter;
import com.l7tech.console.tree.PolicyTemplatesFolderNode;
import com.l7tech.console.util.TopComponents;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.Include;
import com.l7tech.policy.assertion.composite.CompositeAssertion;
import com.l7tech.policy.wsp.WspWriter;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.io.File;
import java.io.IOException;
import java.security.AccessControlException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.HashMap;
import java.util.Iterator;

/**
 * The SSM action type that imports a policy from a file.
 */
public abstract class ImportPolicyFromFileAction extends PolicyNodeAction {
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
    protected boolean importPolicy(Policy policy) {
        try {
            return doFileImport(policy);
        } catch (AccessControlException e) {
            TopComponents.getInstance().showNoPrivilegesErrorMessage();
        }
        return false;
    }

    private boolean doFileImport(Policy policy) {
        // get file from user
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
                return false;
            }
            chooser = new JFileChooser(templateDir);
        } else {
            chooser = new JFileChooser(FileChooserUtil.getStartingDirectory());
        }
        FileChooserUtil.addListenerToFileChooser(chooser);

        chooser.setDialogTitle("Import Policy");
        chooser.setMultiSelectionEnabled(false);
        chooser.setFileFilter(new FileFilter() {
            public boolean accept(File f) {
                return f.getAbsolutePath().endsWith(".xml") || f.getAbsolutePath().endsWith(".XML") || f.isDirectory();
            }
            public String getDescription() {
                return "XML Files";
            }
        });
        int ret = chooser.showOpenDialog(TopComponents.getInstance().getTopParent());
        if (JFileChooser.APPROVE_OPTION != ret) return false;

        try {
            PolicyImporter.PolicyImporterResult result = PolicyImporter.importPolicy(chooser.getSelectedFile());
            Assertion newRoot = (result != null) ? result.assertion : null;
            // for some reason, the PublishedService class does not allow to set a policy
            // directly, it must be set through the XML
            if (newRoot != null) {
                String newPolicyXml = WspWriter.getPolicyXml(newRoot);
                policy.setXml(newPolicyXml);
                addPoliciesToIncludeAssertions(policy.getAssertion(), result.policyFragments);
                return true;
            }
        } catch (IOException e) {
            log.log(Level.WARNING, "could not localize or read policy from " + chooser.getSelectedFile().getPath(), e);
            DialogDisplayer.showMessageDialog(TopComponents.getInstance().getTopParent(),
                                          "Could not find policy export in the selected file",
                                          "Policy Not Found",
                                          JOptionPane.WARNING_MESSAGE, null);
        }

        return false;
    }

    private void addPoliciesToIncludeAssertions(Assertion rootAssertion, HashMap<String, Policy> fragments) throws IOException {
        if(rootAssertion instanceof CompositeAssertion) {
            CompositeAssertion compAssertion = (CompositeAssertion)rootAssertion;
            for(Iterator it = compAssertion.children();it.hasNext();) {
                Assertion child = (Assertion)it.next();
                addPoliciesToIncludeAssertions(child, fragments);
            }
        } else if(rootAssertion instanceof Include) {
            Include includeAssertion = (Include)rootAssertion;
            if(fragments.containsKey(includeAssertion.getPolicyName())) {
                includeAssertion.replaceFragmentPolicy(fragments.get(includeAssertion.getPolicyName()));
                addPoliciesToIncludeAssertions(includeAssertion.retrieveFragmentPolicy().getAssertion(), fragments);
            }
        }
    }
}
