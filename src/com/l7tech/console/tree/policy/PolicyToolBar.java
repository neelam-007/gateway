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
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.FocusListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusAdapter;
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

    private AbstractTreeNode lastPaletteNode;
    private AssertionTreeNode lastAssertionNode;
    private AssertionTreeNode rootAssertionNode;
    private JTree assertionPalette;

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
        if (assertionPalette !=null) {
            assertionPalette.removeTreeSelectionListener(assertionPaletteListener);
            assertionPalette.removeFocusListener(assertionPaletteFocusListener);
        }
        tree.addTreeSelectionListener(assertionPaletteListener);
        tree.addFocusListener(assertionPaletteFocusListener);
        assertionPalette = tree;
    }

    /**
     * register the toolbar with the policy editor tree.
     * 
     * @param tree the assertion tree
     */
    public void registerPolicyTree(JTree tree) {
        tree.addTreeSelectionListener(policyTreeListener);
        tree.getModel().addTreeModelListener(policyTreeModelListener);
        rootAssertionNode = (AssertionTreeNode)tree.getModel().getRoot();
        lastAssertionNode  = rootAssertionNode;
        updateActions();
    }

    /**
     * register the toolbar with the policy editor tree.
     * 
     * @param tree the assertion tree
     */
    public void unregisterPolicyTree(JTree tree) {
        if (tree == null) return;
        tree.removeTreeSelectionListener(policyTreeListener);
        final TreeModel model = tree.getModel();
        if (model != null) {
            model.removeTreeModelListener(policyTreeModelListener);
        }
        lastAssertionNode = null;
        disableAll();
    }


    /**
     * disable all the policy toolbar actions
     */
    public void disableAll() {
        getDeleteAssertionAction().setEnabled(false);
        getAssertionMoveDownAction().setEnabled(false);
        getAssertionMoveUpAction().setEnabled(false);
        getAddAssertionAction().setEnabled(false);
    }

    /**
     * initialize the toolbar
     */
    private void initialize() {
        Border border = BorderFactory.createEtchedBorder(EtchedBorder.LOWERED);
        setBorder(new CompoundBorder(border,
          new EmptyBorder(2, 3, 2, 3)));

        Dimension d = new Dimension();
        d.width = 16;
        addSeparator(d);
        setOrientation(JToolBar.VERTICAL);
        putClientProperty("JToolBar.isRollover", Boolean.TRUE);

        JButton b = add(getAddAssertionAction());
        b.setFocusable(false);
        b.setMargin(new Insets(0, 0, 0, 0));
        d = new Dimension();
        d.height = 8;
        addSeparator(d);

        b = add(getAssertionMoveUpAction());
        b.setFocusable(false);
        b.setMargin(new Insets(0, 0, 0, 0));
        addSeparator(d);

        b = add(getAssertionMoveDownAction());
        b.setFocusable(false);
        b.setMargin(new Insets(0, 0, 0, 0));
        addSeparator(d);

        b = add(getDeleteAssertionAction());
        b.setFocusable(false);
        b.setMargin(new Insets(0, 0, 0, 0));
        addSeparator(d);
    }

    private Action getAssertionMoveUpAction() {
        if (assertionMoveUpAction != null)
            return assertionMoveUpAction;
        assertionMoveUpAction = new AssertionMoveUpAction(null) {
            /**
             * Invoked when an action occurs.
             */
            protected void performAction() {
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
            protected void performAction() {
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
            protected void performAction() {
                this.paletteNode = lastPaletteNode;
                this.assertionNode = lastAssertionNode;
                if (this.assertionNode == null) {
                    assertionNode = rootAssertionNode;
                }
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
            protected void performAction() {
                this.node = lastAssertionNode;
                super.performAction();
                this.node = null;
                updateActions();
            }
        };
        return deleteAssertionAction;
    }

    private void updateActions() {
        boolean validPNode = validPaletteNode();
        boolean validPolicyAssertionNode = validPolicyAssertionNode();
        disableAll();
        if (!validPolicyAssertionNode && !validPNode) {
            return;
        }
        if (validPolicyAssertionNode) {
            getDeleteAssertionAction().setEnabled(lastAssertionNode.canDelete());
            getAssertionMoveDownAction().setEnabled(lastAssertionNode.canMoveDown());
            getAssertionMoveUpAction().setEnabled(lastAssertionNode.canMoveUp());
        }
        if (validPNode) {
            validPNode = validPNode && (lastAssertionNode == null || lastAssertionNode.accept(lastPaletteNode));
        }
        getAddAssertionAction().setEnabled(validPNode && validPolicyAssertionNode);
    }

    private boolean validPolicyAssertionNode() {
        return lastAssertionNode != null && !PolicyTree.isIdentityView(lastAssertionNode);
    }

    private boolean validPaletteNode() {
        return lastPaletteNode != null &&
          !lastPaletteNode.getAllowsChildren();
    }


    private TreeSelectionListener
      assertionPaletteListener = new TreeSelectionListener() {
          public void valueChanged(TreeSelectionEvent e) {
              try {
                  TreePath path = e.getNewLeadSelectionPath();
                  if (path == null) {
                      lastPaletteNode = null;
                  } else {
                      lastPaletteNode = (AbstractTreeNode)path.getLastPathComponent();
                  }
                  updateActions();
              } catch (Exception e1) {
                  e1.printStackTrace();
              }
          }
      };

    private FocusListener assertionPaletteFocusListener = new FocusAdapter() {
        public void focusLost(FocusEvent e) {
            lastPaletteNode = null;
            updateActions();
        }
        public void focusGained(FocusEvent e) {
            TreePath path = assertionPalette.getSelectionPath();
            if (path == null) {
                return;
            }
            lastPaletteNode = (AbstractTreeNode)path.getLastPathComponent();
            updateActions();
        }
    };


    private TreeSelectionListener
      policyTreeListener = new TreeSelectionListener() {
          public void valueChanged(TreeSelectionEvent e) {
              TreePath path = e.getNewLeadSelectionPath();
              if (path == null) {
                  lastAssertionNode = rootAssertionNode;
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
  /**
     * Invoked on connection event
     * 
     * @param e describing the connection event
     */
    public void onConnect(ConnectionEvent e) {
    }

    /**
     * Invoked on disconnect
     * 
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
