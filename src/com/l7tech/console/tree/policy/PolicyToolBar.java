package com.l7tech.console.tree.policy;

import com.l7tech.console.action.AddAssertionAction;
import com.l7tech.console.action.AssertionMoveDownAction;
import com.l7tech.console.action.AssertionMoveUpAction;
import com.l7tech.console.action.DeleteAssertionAction;
import com.l7tech.console.event.ConnectionEvent;
import com.l7tech.console.event.ConnectionListener;
import com.l7tech.console.tree.AbstractTreeNode;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.util.logging.Logger;

/**
 * The policy toolbar with toolbar actions and listeners.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com>Emil Marceta</a>
 * @version 1.0
 */
public class PolicyToolBar extends JToolBar implements ConnectionListener {
    static Logger log = Logger.getLogger(PolicyToolBar.class.getName());
    public static final String NAME = "policy.toolbar";
    private AssertionMoveUpAction assertionMoveUpAction;
    private AssertionMoveDownAction assertionMoveDownAction;
    private AddAssertionAction addAssertionAction;
    private DeleteAssertionAction deleteAssertionAction;

    public PolicyToolBar() {
        initialize();
        disableAll();
    }

    /**
     * register the toolbar with the palette assertion tree.
     *
     * @param tree the assertion tree
     */
    public void registerPaletteTree(JTree tree) {
        tree.addTreeSelectionListener(assertionPaletteListener);
    }

    /**
     * register the toolbar with the policy editor tree.
     *
     * @param tree the assertion tree
     */
    public void registerPolicyTree(JTree tree) {
        tree.addTreeSelectionListener(policyTreeListener);
        tree.getModel().addTreeModelListener(policyTreeModelListener);
    }

    /**
     * initialize the toolbar
     */
    private void initialize() {
        Border border = BorderFactory.createEtchedBorder(EtchedBorder.LOWERED);
        setBorder(new CompoundBorder(
          border,
          new EmptyBorder(2, 3, 2, 3))
        );

        addSeparator();
        setOrientation(JToolBar.VERTICAL);
        putClientProperty("JToolBar.isRollover", Boolean.TRUE);
        setFloatable(false);

        JButton b = add(getAddAssertionAction());
        b.setMargin(new Insets(0, 0, 0, 0));
        addSeparator();

        b = add(getAssertionMoveUpAction());
        b.setMargin(new Insets(0, 0, 0, 0));
        addSeparator();

        b = add(getAssertionMoveDownAction());
        b.setMargin(new Insets(0, 0, 0, 0));
        addSeparator();

        b = add(getDeleteAssertionAction());
        b.setMargin(new Insets(0, 0, 0, 0));
        addSeparator();
    }

    private Action getAssertionMoveUpAction() {
        if (assertionMoveUpAction != null)
            return assertionMoveUpAction;
        assertionMoveUpAction = new AssertionMoveUpAction(null) {
            /**
             * Invoked when an action occurs.
             */
            public void performAction() {
                node = lastAssertionNode;
                super.performAction();
            }
        };

        return assertionMoveUpAction;

    }

    private Action getAssertionMoveDownAction() {
        if (assertionMoveDownAction != null)
            return assertionMoveDownAction;
        assertionMoveDownAction = new AssertionMoveDownAction(null) {
            /**
             * Invoked when an action occurs.
             */
            public void performAction() {
                node = lastAssertionNode;
                super.performAction();
            }
        };

        return assertionMoveDownAction;

    }

    private Action getAddAssertionAction() {
        if (addAssertionAction != null)
            return addAssertionAction;
        addAssertionAction = new AddAssertionAction() {
            /**
             * Invoked when an action occurs.
             */
            public void performAction() {
                this.paletteNode = lastPaletteNode;
                this.assertionNode = lastAssertionNode;
                super.performAction();
                updateActions();
            }
        };
        return addAssertionAction;
    }


    private Action getDeleteAssertionAction() {
        if (deleteAssertionAction != null)
            return deleteAssertionAction;
        deleteAssertionAction = new DeleteAssertionAction() {
            /**
             * Invoked when an action occurs.
             */
            public void performAction() {
                this.node = lastAssertionNode;
                super.performAction();
                this.node = null;
                updateActions();
            }
        };
        return deleteAssertionAction;
    }

    private void updateActions() {
        final boolean validPNode = validPaletteNode();
        final boolean validPolicyAssertionNode = validPolicyAssertionNode();
        if (!validPolicyAssertionNode && !validPNode) {
            disableAll();
            return;
        }
        if (validPolicyAssertionNode) {
            if (validPNode) {
                getAddAssertionAction().setEnabled(
                  validPNode &&
                  lastAssertionNode.accept(lastPaletteNode));
            }
            getDeleteAssertionAction().setEnabled(lastAssertionNode.canDelete());
            getAssertionMoveDownAction().setEnabled(lastAssertionNode.canMoveDown());
            getAssertionMoveUpAction().setEnabled(lastAssertionNode.canMoveUp());
        }
    }

    private boolean validPolicyAssertionNode() {
        return lastAssertionNode != null;
    }

    private boolean validPaletteNode() {
        return lastPaletteNode != null &&
          !lastPaletteNode.getAllowsChildren();
    }

    private void disableAll() {
        getDeleteAssertionAction().setEnabled(false);
        getAssertionMoveDownAction().setEnabled(false);
        getAssertionMoveUpAction().setEnabled(false);
        getAddAssertionAction().setEnabled(false);
    }

    private TreeSelectionListener
      assertionPaletteListener = new TreeSelectionListener() {
          public void valueChanged(TreeSelectionEvent e) {
              try {
                  TreePath path = e.getPath();
                  lastPaletteNode = (AbstractTreeNode)path.getLastPathComponent();
                  updateActions();
              } catch (Exception e1) {
                  e1.printStackTrace();
              }
          }
      };

    private TreeSelectionListener
      policyTreeListener = new TreeSelectionListener() {
          public void valueChanged(TreeSelectionEvent e) {
              TreePath path = e.getNewLeadSelectionPath();
              if (path == null) {
                  lastAssertionNode = null;
              } else {
                  lastAssertionNode = (AssertionTreeNode)path.getLastPathComponent();
              }
              updateActions();
          }
      };

    private TreeModelListener
      policyTreeModelListener = new TreeModelListener() {
          public void treeNodesChanged(TreeModelEvent e) {
          }

          public void treeNodesInserted(TreeModelEvent e) {
          }

          public void treeNodesRemoved(TreeModelEvent e) {
              final Object[] children = e.getChildren();
              for (int i = 0; i < children.length; i++) {
                  Object o = children[i];
                  if (o == lastAssertionNode) {
                      lastAssertionNode = null;
                      updateActions();
                      break;
                  }
              }
          }

          public void treeStructureChanged(TreeModelEvent e) {
              lastAssertionNode = null;
              updateActions();
          }
      };


    private AbstractTreeNode lastPaletteNode;
    private AssertionTreeNode lastAssertionNode;

    /**
     * Invoked on connection event
     * @param e describing the connection event
     */
    public void onConnect(ConnectionEvent e) {
    }

    /**
     * Invoked on disconnect
     * @param e describing the dosconnect event
     */
    public void onDisconnect(ConnectionEvent e) {

        Runnable r = new Runnable() {
            public void run() {
                log.fine("Policy Toolbar disconnect - disabling actions");
                assertionMoveUpAction.setEnabled(false);
                assertionMoveDownAction.setEnabled(false);
                addAssertionAction.setEnabled(false);
                deleteAssertionAction.setEnabled(false);
            }
        };
        SwingUtilities.invokeLater(r);
    }
}
