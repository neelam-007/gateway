package com.l7tech.console.tree.policy;

import com.l7tech.common.audit.LogonEvent;
import com.l7tech.common.security.rbac.AttemptedUpdate;
import com.l7tech.common.security.rbac.EntityType;
import com.l7tech.common.policy.Policy;
import com.l7tech.console.action.AddAssertionAction;
import com.l7tech.console.action.AssertionMoveDownAction;
import com.l7tech.console.action.AssertionMoveUpAction;
import com.l7tech.console.action.DeleteAssertionAction;
import com.l7tech.console.security.LogonListener;
import com.l7tech.console.security.SecurityProvider;
import com.l7tech.console.tree.AbstractTreeNode;
import com.l7tech.console.util.Registry;
import com.l7tech.service.PublishedService;

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
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.ArrayList;
import java.util.logging.Logger;

/**
 * The policy toolbar with toolbar actions and listeners.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com>Emil Marceta</a>
 * @version 1.0
 */
public class PolicyToolBar extends JToolBar implements LogonListener {
    static Logger log = Logger.getLogger(PolicyToolBar.class.getName());
    public static final String NAME = "policy.toolbar";
    private AssertionMoveUpAction assertionMoveUpAction;
    private AssertionMoveDownAction assertionMoveDownAction;
    private AddAssertionAction addAssertionAction;
    private DeleteAssertionAction deleteAssertionAction;

    private AbstractTreeNode lastPaletteNode;
    private AssertionTreeNode[] lastAssertionNodes;
    private AssertionTreeNode lastAssertionNode;
    private AssertionTreeNode rootAssertionNode;
    private TreePath[] paths;
    private JTree assertionPalette;
    private JTree assertionTree;

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
        if (assertionPalette != null) {
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
        lastAssertionNode = rootAssertionNode;
        lastAssertionNodes = new AssertionTreeNode[]{rootAssertionNode};
        paths = null;
        assertionTree = tree;
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
        lastAssertionNodes = null;
        paths = null;
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
        setFloatable(false);
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
                nodes = lastAssertionNodes;
                final TreePath[] treePaths = paths;
                super.performAction();
                final JTree tree = assertionTree;
                if (treePaths != null && tree!=null) {
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            tree.setSelectionPaths(treePaths);
                        }
                    });
                }
            }
        };
        return assertionMoveUpAction;
    }

    private AssertionMoveDownAction getAssertionMoveDownAction() {
        if (assertionMoveDownAction != null)
            return assertionMoveDownAction;
        assertionMoveDownAction = new AssertionMoveDownAction(null) {
            /**
             * Invoked when an action occurs.
             */
            protected void performAction() {
                node = lastAssertionNode;
                nodes = lastAssertionNodes;
                final TreePath[] treePaths = paths;
                super.performAction();
                final JTree tree = assertionTree;
                if (treePaths != null && tree!=null) {
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            tree.setSelectionPaths(treePaths);
                        }
                    });
                }
            }
        };
        return assertionMoveDownAction;
    }

    private AddAssertionAction getAddAssertionAction() {
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


    private DeleteAssertionAction getDeleteAssertionAction() {
        if (deleteAssertionAction != null)
            return deleteAssertionAction;
        deleteAssertionAction = new DeleteAssertionAction() {
            /**
             * Invoked when an action occurs.
             */
            protected void performAction() {
                this.node = lastAssertionNode;
                this.nodes = lastAssertionNodes;
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
        SecurityProvider sp = Registry.getDefault().getSecurityProvider();
        if (rootAssertionNode == null) return;
        boolean canUpdate;
        try {
            // Case 1: if the node is associated to a published service
            PublishedService svc = rootAssertionNode.getService();
            canUpdate = sp.hasPermission(new AttemptedUpdate(EntityType.SERVICE, svc));

            // Case 2: if the node is associated to apolicy fragment
            if (svc == null && !canUpdate) {
                Policy policy = rootAssertionNode.getPolicy();
                canUpdate = sp.hasPermission(new AttemptedUpdate(EntityType.POLICY, policy));
            }
        } catch (Exception e) {
            throw new RuntimeException("Couldn't get current service or policy", e);
        }

        if (validPolicyAssertionNode) {
            getDeleteAssertionAction().setEnabled(canUpdate && canDelete(lastAssertionNode, lastAssertionNodes));
            getAssertionMoveDownAction().setEnabled(canUpdate && canMoveDown(lastAssertionNode, lastAssertionNodes));
            getAssertionMoveUpAction().setEnabled(canUpdate && canMoveUp(lastAssertionNode, lastAssertionNodes));
        }
        if (validPNode) {
            validPNode = lastAssertionNode == null || lastAssertionNode.accept(lastPaletteNode);
        }
        getAddAssertionAction().setEnabled(canUpdate && validPNode && validPolicyAssertionNode);
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
                  lastAssertionNodes = new AssertionTreeNode[]{rootAssertionNode};
                  paths = null;
              } else {
                  lastAssertionNode = (AssertionTreeNode)path.getLastPathComponent();
                  paths = ((JTree)e.getSource()).getSelectionPaths();
                  lastAssertionNodes = toAssertionTreeNodeArray(paths);
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
              lastAssertionNodes = null;
              paths = null;
              updateActions();
          }
      };

    private AssertionTreeNode[] toAssertionTreeNodeArray(TreePath[] paths) {
        java.util.List assertionTreeNodes = new ArrayList();

        if (paths != null) {
            for (int p=0; p<paths.length; p++) {
                TreePath path = paths[p];
                assertionTreeNodes.add((AssertionTreeNode)path.getLastPathComponent());
            }
        }

        return (AssertionTreeNode[]) assertionTreeNodes.toArray(new AssertionTreeNode[assertionTreeNodes.size()]);
    }

    /**
     * Invoked on connection event
     *
     * @param e describing the connection event
     */
    public void onLogon(LogonEvent e) {
    }

    /**
     * Invoked on disconnect
     *
     * @param e describing the dosconnect event
     */
    public void onLogoff(LogonEvent e) {

        Runnable r = new Runnable() {
            public void run() {
                log.fine("Policy Toolbar disconnect - disabling actions");
                disableAll();
            }
        };
        SwingUtilities.invokeLater(r);
    }

    private boolean canDelete(AssertionTreeNode node, AssertionTreeNode[] nodes) {
        boolean delete = false;

        if (nodes == null) {
            delete = node.canDelete();
        } else if (nodes != null && nodes.length > 0){
            boolean allDelete = true;
            for (int n=0; n<nodes.length; n++) {
                AssertionTreeNode current = nodes[n];
                if (current == null || !current.canDelete()) {
                    allDelete = false;
                    break;
                }
            }
            delete = allDelete;
        }

        return delete;
    }

    private boolean canMoveUp(AssertionTreeNode node, AssertionTreeNode[] nodes) {
        boolean move = false;

        if (nodes == null) {
            move = node.canMoveUp();
        } else if (nodes != null && nodes.length > 0){
            boolean allMove = true;
            for (int n=0; n<nodes.length; n++) {
                AssertionTreeNode current = nodes[n];
                if (current == null || !current.canMoveUp()) {
                    allMove = false;
                    break;
                }
            }
            move = allMove;
        }

        return move;
    }

    private boolean canMoveDown(AssertionTreeNode node, AssertionTreeNode[] nodes) {
        boolean move = false;

        if (nodes == null) {
            move = node.canMoveDown();
        } else if (nodes != null && nodes.length > 0){
            boolean allMove = true;
            for (int n=0; n<nodes.length; n++) {
                AssertionTreeNode current = nodes[n];
                if (current == null || !current.canMoveDown()) {
                    allMove = false;
                    break;
                }
            }
            move = allMove;
        }

        return move;
    }
}
