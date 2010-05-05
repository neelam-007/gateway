/*
 * Copyright (C) 2004-2007 Layer 7 Technologies Inc.
 */
package com.l7tech.console.action;

import com.l7tech.console.policy.exporter.PolicyExportUtils;
import com.l7tech.gui.util.FileChooserUtil;
import com.l7tech.policy.Policy;
import com.l7tech.console.logging.ErrorManager;
import com.l7tech.console.tree.PolicyTemplatesFolderNode;
import com.l7tech.console.tree.PolicyEntityNode;
import com.l7tech.console.util.TopComponents;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.io.File;
import java.io.IOException;
import java.security.AccessControlException;
import java.util.logging.Level;

/**
 * The SSM action type that imports a policy from a file.
 */
public abstract class ImportPolicyFromFileAction extends EntityWithPolicyNodeAction<PolicyEntityNode> {
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
    @Override
    public String getName() {
        return "Import Policy";
    }

    /**
     * @return the action description
     */
    @Override
    public String getDescription() {
        return "Import a policy from a file along with external references.";
    }

    /**
     * subclasses override this method specifying the resource name
     */
    @Override
    protected String iconResource() {
        return "com/l7tech/console/resources/saveTemplate.gif";
    }

    /**
     * Actually perform the action.
     */
    protected boolean importPolicy( final Policy policy ) {
        try {
            return doFileImport(policy);
        } catch (AccessControlException e) {
            TopComponents.getInstance().showNoPrivilegesErrorMessage();
        }
        return false;
    }

    private boolean doFileImport( final Policy policy ) {
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
            @Override
            public boolean accept(File f) {
                return f.getAbsolutePath().endsWith(".xml") || f.getAbsolutePath().endsWith(".XML") || f.isDirectory();
            }
            @Override
            public String getDescription() {
                return "XML Files";
            }
        });
        int ret = chooser.showOpenDialog(TopComponents.getInstance().getTopParent());

        return JFileChooser.APPROVE_OPTION == ret &&
               PolicyExportUtils.importPolicyFromFile( policy, chooser.getSelectedFile() );
    }
}
