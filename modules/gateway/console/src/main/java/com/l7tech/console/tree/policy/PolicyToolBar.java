package com.l7tech.console.tree.policy;

import com.l7tech.console.action.*;
import com.l7tech.console.security.LogonListener;
import com.l7tech.console.security.SecurityProvider;
import com.l7tech.console.tree.AbstractTreeNode;
import com.l7tech.console.tree.PolicyTemplateNode;
import com.l7tech.console.util.Registry;
import com.l7tech.gateway.common.audit.LogonEvent;
import com.l7tech.gateway.common.security.rbac.AttemptedUpdate;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.policy.Policy;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.event.*;
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
    private JButton enableOrDisableButton;
    private JButton expandOrCollapseButton;

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
        tree.addTreeExpansionListener(policyTreeExpansionListener);
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
        tree.removeTreeExpansionListener(policyTreeExpansionListener);
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
        enableOrDisableButton.setEnabled(false);
        expandOrCollapseButton.setEnabled(false);
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

        expandOrCollapseButton = add(getExpandOrCollapseAssertionAction());
        expandOrCollapseButton.setText(null);
        expandOrCollapseButton.setFocusable(false);
        expandOrCollapseButton.setMargin(new Insets(0, 0, 0, 0));
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

        enableOrDisableButton = add(getDisableOrEnableAssertionAction());
        enableOrDisableButton.setText(null);
        enableOrDisableButton.setFocusable(false);
        enableOrDisableButton.setMargin(new Insets(0, 0, 0, 0));
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
            @Override
            protected void performAction() {
                node = lastAssertionNode;
                nodes = lastAssertionNodes;
                final TreePath[] treePaths = paths;
                super.performAction();
                final JTree tree = assertionTree;
                if (treePaths != null && tree!=null) {
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
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
            @Override
            protected void performAction() {
                node = lastAssertionNode;
                nodes = lastAssertionNodes;
                final TreePath[] treePaths = paths;
                super.performAction();
                final JTree tree = assertionTree;
                if (treePaths != null && tree!=null) {
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
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
            @Override
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
            @Override
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

    /**
     * The returned action is dependent on whether the selected assertion is disabled or enabled.
     * @return an action to disable/enable assertions
     */
    private DisableOrEnableAssertionAction getDisableOrEnableAssertionAction() {
        // Check if the action is available or not.
        if ( !canEnableDisable() ) {
            DisableAssertionAction disableAssertionAction = new DisableAssertionAction(null);
            disableAssertionAction.setEnabled( false );
            return disableAssertionAction;
        }

        // Create a new action every time, since the disable/enable status may be always changed.
        //noinspection UnnecessaryLocalVariable
        DisableOrEnableAssertionAction enableOrDisableAction = (lastAssertionNode.isAssertionEnabled())?
            new DisableAssertionAction(lastAssertionNode) {
                @Override
                public void performAction() {
                    super.performAction();
                    updateActions();
                }
            }
            :
            new EnableAssertionAction(lastAssertionNode) {
                @Override
                public void performAction() {
                    super.performAction();
                    updateActions();
                }
            };

        return enableOrDisableAction;
    }

    /**
     * The returned action depends on the expanding status of the selected assertion nodes.  If an expanded assertion is
     * selected, then the action will an ExpandAssertionAction.  Vice verse.
     * @return an action to expand/collapse the selected assertions.
     */
    private NodeAction getExpandOrCollapseAssertionAction() {
        if (lastAssertionNode == null) {// lastAssertionNode is null only when the policy tool bar is being initialized.
            return new ExpandAssertionAction();  // This is only for displaying purpose.
        }

        // Create a new action every time, since the expand/collapse status may be always changed.
        AssertionTreeNode targetNode = getExpandedOrCollapsedNode();
        return targetNode.isExpanded()?
            new CollapseAssertionAction() {
                @Override
                public void performAction() {
                    super.performAction();
                    updateActions();
                }
            }
            :
            new ExpandAssertionAction() {
                @Override
                public void performAction() {
                    super.performAction();
                    updateActions();
                }
            };
    }

    /**
     *  Get the tree node to be expanded or collapsed.
     * @return a tree node to be processed, either root assertion node or last assertion node.
     */
    private AssertionTreeNode getExpandedOrCollapsedNode() {
        return(assertionTree != null && assertionTree.getSelectionCount() == 0)?
            rootAssertionNode : lastAssertionNode;
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

        enableOrDisableButton.setAction(getDisableOrEnableAssertionAction());
        enableOrDisableButton.setText(null);

        expandOrCollapseButton.setAction(getExpandOrCollapseAssertionAction());
        expandOrCollapseButton.setText(null);

        if (validPolicyAssertionNode) {
            getDeleteAssertionAction().setEnabled(canUpdate && canDelete(lastAssertionNode, lastAssertionNodes));
            getAssertionMoveDownAction().setEnabled(canUpdate && canMoveDown(lastAssertionNode, lastAssertionNodes));
            getAssertionMoveUpAction().setEnabled(canUpdate && canMoveUp(lastAssertionNode, lastAssertionNodes));
            enableOrDisableButton.setEnabled(canUpdate && canEnableDisable() );
            expandOrCollapseButton.setEnabled(getExpandedOrCollapsedNode().getChildCount() > 0);
        }
        if (validPNode) {
            // Allow an include policy node to receive a new assertion at the position below the include policy node.
            if (lastAssertionNode instanceof IncludeAssertionPolicyNode) {
                // At this case, an include policy node should act like a leaf assertion node.
                validPNode = (lastPaletteNode instanceof PolicyTemplateNode || lastAssertionNode.getParent() != null)
                    && new SavePolicyAction(true).isAuthorized();
            } else {
                validPNode = lastAssertionNode == null || lastAssertionNode.accept(lastPaletteNode);
            }
        }
        getAddAssertionAction().setEnabled(canUpdate && validPNode && validPolicyAssertionNode);
    }

    private boolean validPolicyAssertionNode() {
        return lastAssertionNode != null;
    }

    private boolean validPaletteNode() {
        return lastPaletteNode != null &&
          !lastPaletteNode.getAllowsChildren();
    }

    private TreeSelectionListener
      assertionPaletteListener = new TreeSelectionListener() {
          @Override
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
        @Override
        public void focusLost(FocusEvent e) {
            lastPaletteNode = null;
            updateActions();
        }

        @Override
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
          @Override
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
          @Override
          public void treeNodesChanged(TreeModelEvent e) {
              updateActions();
          }

          @Override
          public void treeNodesInserted(TreeModelEvent e) {
              updateActions();
          }

          @Override
          public void treeNodesRemoved(TreeModelEvent e) {
              final Object[] children = e.getChildren();
              for (Object o : children) {
                  if (o == lastAssertionNode) {
                      lastAssertionNode = null;
                      updateActions();
                      break;
                  }
              }
          }

          @Override
          public void treeStructureChanged(TreeModelEvent e) {
              lastAssertionNode = null;
              lastAssertionNodes = null;
              paths = null;
              updateActions();
          }
      };

    private TreeExpansionListener policyTreeExpansionListener = new TreeExpansionListener() {
        @Override
        public void treeExpanded(TreeExpansionEvent event) {
            TreePath path = event.getPath();
            AssertionTreeNode node = (AssertionTreeNode) path.getLastPathComponent();
            node.setExpanded(true);
            updateActions();
        }

        @Override
        public void treeCollapsed(TreeExpansionEvent event) {
            TreePath path = event.getPath();
            AssertionTreeNode node = (AssertionTreeNode) path.getLastPathComponent();
            node.setExpanded(false);
            updateActions();
        }
    };

    private AssertionTreeNode[] toAssertionTreeNodeArray(TreePath[] paths) {
        java.util.List<AssertionTreeNode> assertionTreeNodes = new ArrayList<AssertionTreeNode>();

        if (paths != null) {
            for (TreePath path : paths) {
                assertionTreeNodes.add((AssertionTreeNode)path.getLastPathComponent());
            }
        }

        return assertionTreeNodes.toArray(new AssertionTreeNode[assertionTreeNodes.size()]);
    }

    /**
     * Invoked on connection event
     *
     * @param e describing the connection event
     */
    @Override
    public void onLogon(LogonEvent e) {
    }

    /**
     * Invoked on disconnect
     *
     * @param e describing the dosconnect event
     */
    @Override
    public void onLogoff(LogonEvent e) {

        Runnable r = new Runnable() {
            @Override
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
        } else if (nodes.length > 0){
            boolean allDelete = true;
            for (AssertionTreeNode current : nodes) {
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
        } else if (nodes.length > 0){
            boolean allMove = true;
            for (AssertionTreeNode current : nodes) {
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
        } else if (nodes.length > 0){
            boolean allMove = true;
            for (AssertionTreeNode current : nodes) {
                if (current == null || !current.canMoveDown()) {
                    allMove = false;
                    break;
                }
            }
            move = allMove;
        }

        return move;
    }

    private boolean canEnableDisable() {
        return
            lastAssertionNode != null &&
            !lastAssertionNode.isDescendantOfInclude(false) &&
            assertionTree != null &&
            assertionTree.getSelectionCount() > 0;
    }
}
