package com.l7tech.console.action;


import com.l7tech.console.tree.PoliciesFolderNode;
import com.l7tech.console.tree.PolicyTemplateNode;
import com.l7tech.console.tree.AssertionsTree;
import com.l7tech.console.util.TopComponents;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.Preferences;

import com.l7tech.console.panels.EditPolicyTemplateNameDialog;
import com.l7tech.console.logging.ErrorManager;

import javax.swing.*;
import javax.swing.tree.DefaultTreeModel;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;

/*
 * This class handles the action of editing policy template name.
 *
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

public class EditPolicyTemplateNameAction extends BaseAction {

    static final Logger log = Logger.getLogger(EditServiceNameAction.class.getName());
    protected PolicyTemplateNode node;

    public EditPolicyTemplateNameAction(PolicyTemplateNode node) {
        if (node == null) {
            throw new IllegalArgumentException();
        }
        this.node = node;
    }

    /**
     * @return the action name
     */
    public String getName() {
        return "Rename";
    }

    /**
     * @return the action description
     */
    public String getDescription() {
        return "Edit the policy template name";
    }

    /**
     * specify the resource name for this action
     */
    protected String iconResource() {
        return "com/l7tech/console/resources/Edit16.gif";
    }

    /**
     * Actually perform the action.
     * This is the method which should be called programmatically.
     * <p/>
     * note on threading usage: do not access GUI components
     * without explicitly asking for the AWT event thread!
     */
    public void performAction() {
        SwingUtilities.invokeLater(
          new Runnable() {
              public void run() {
                  TopComponents wm =
                    Registry.getDefault().getComponentRegistry();

                  EditPolicyTemplateNameDialog d =
                    new EditPolicyTemplateNameDialog(wm.getMainWindow(), nameChangeListener);
                  d.show();
              }
          });
    }

    private ActionListener
      nameChangeListener = new ActionListener() {
          /**
           * Fired when an set of children is updated.
           */
          public void actionPerformed(final ActionEvent ev) {
              SwingUtilities.invokeLater(new Runnable() {
                  public void run() {
                      File templateDir = null;
                      templateDir = new File(Preferences.getPreferences().getHomePath() +
                        File.separator + PoliciesFolderNode.TEMPLATES_DIR);

                      final File newName = new File(templateDir.getPath() + File.separator + ev.getActionCommand());
                      boolean success = node.getFile().renameTo(newName);
                      if (!success) {
                          String error = "The system reported problem in accessing " +
                            "the policy template directory " + templateDir.getPath() + "\n" +
                            "The policy template is not renamed.";
                          ErrorManager.getDefault().
                            notify(Level.WARNING, new IOException(error), error);
                          return;
                      }
                      node.setUserObject(newName);
                      JTree tree =
                        (JTree)TopComponents.getInstance().getComponent(AssertionsTree.NAME);
                      if (tree != null) {
                          DefaultTreeModel model = (DefaultTreeModel)tree.getModel();
                          model.nodeChanged(node);
                      }
                  }
              });
          }
      };
}
