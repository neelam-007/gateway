package com.l7tech.console.tree.policy;

import com.l7tech.console.panels.ForEachLoopAssertionPolicyNode;
import com.l7tech.console.policy.PolicyTransferable;
import com.l7tech.console.tree.AbstractTreeNode;
import com.l7tech.console.tree.TreeNodeHidingTransferHandler;
import com.l7tech.policy.assertion.BlankAssertion;
import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.policy.assertion.composite.CompositeAssertion;
import com.l7tech.policy.assertion.composite.ForEachLoopAssertion;
import com.l7tech.policy.assertion.composite.OneOrMoreAssertion;

import javax.swing.*;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreePath;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.util.*;
import java.util.logging.Logger;

public class PolicyTreeTransferHandler extends TreeNodeHidingTransferHandler {
    private static final Logger log = Logger.getLogger(PolicyTreeTransferHandler.class.getName());

    @Override
    protected Transferable createTransferable(JComponent c) {
        if (! (c instanceof JTree)) return null;
        JTree policyTree = (JTree)c;

        return createTransferable(policyTree, getSortedSelectedTreePaths(policyTree));
    }

    @Override
    protected void exportDone(JComponent source, Transferable data, int action) {
        if (!(source instanceof JTree)) return;
        JTree policyTree = (JTree)source;

        if (action == TransferHandler.MOVE) {
            TreePath[] paths = policyTree.getSelectionPaths();
            PolicyTreeModel model = (PolicyTreeModel)policyTree.getModel();
            for (TreePath path: paths) {
                model.removeNodeFromParent((MutableTreeNode)path.getLastPathComponent());
            }
        }
    }

    @Override
    public boolean canImport(JComponent comp, DataFlavor[] transferFlavors) {//used
        for (DataFlavor flav : transferFlavors) {
            if (PolicyTransferable.ASSERTION_DATAFLAVOR.equals(flav) || flav != null && DataFlavor.stringFlavor.equals(flav))
                return true;
        }
        return false;
    }

    @Override
    public int getSourceActions(JComponent c) {
        if (!(c instanceof JTree)) return NONE;
        JTree policyTree = (JTree)c;

        if(policyTree.getModel().getChildCount(policyTree.getModel().getRoot()) > 0) {
            return COPY;
        } else {
            return NONE;
        }
    }

    public Transferable createTransferable(JTree policyTree, TreePath[] paths) {
        if (paths != null && paths.length > 0) {
            return new PolicyTransferable(getTrimmedFromSelection(paths));
        } else {
            // No selection, so copy entire policy
            Object node = policyTree.getModel().getRoot();
            if (node == null) return null;
            if (node instanceof AbstractTreeNode)
                return new PolicyTransferable(new AbstractTreeNode[] {(AbstractTreeNode)node});
            else
                log.fine("Unable to create transferable for non-AbstractTreeNode: " + node.getClass().getName());
        }
        return null;
    }

    /**
     * Sorts the selection.
     * @return The sorted array of selected TreePath's.
     */
    private TreePath[] getSortedSelectedTreePaths(JTree policyTree) {
        int[] selectedRows = policyTree.getSelectionRows();

        //if no selection, then we'll return empty tree path
        if (selectedRows == null) return new TreePath[0];

        Arrays.sort(selectedRows);
        TreePath[] paths = new TreePath[selectedRows.length];
        for(int i = 0;i < selectedRows.length;i++) {
            paths[i] = policyTree.getPathForRow(selectedRows[i]);
        }

        return paths;
    }

    private AbstractTreeNode[] getTrimmedFromSelection(TreePath[] paths) {
        HashMap<AbstractTreeNode, AbstractTreeNode> assertionMap = new HashMap<>();
        HashSet<AbstractTreeNode> assertionsToSkip = new HashSet<>();

        for(TreePath path : paths) {
            if(path.getLastPathComponent() instanceof AbstractTreeNode) {
                AbstractTreeNode node = (AbstractTreeNode)path.getLastPathComponent();
                if (node.asAssertion() instanceof BlankAssertion) {
                    continue;
                }
                // Check for selected ancestors
                CompositeAssertionTreeNode currentAncestor = null;
                CompositeAssertionTreeNode immediateParent = null;
                HashMap<AbstractTreeNode, AbstractTreeNode> ancestorMap = new HashMap<>();
                for(int i = path.getPathCount() - 2;i >= 0;i--) {
                    if(path.getPathComponent(i) instanceof AbstractTreeNode) {
                        AbstractTreeNode ancestor = (AbstractTreeNode)path.getPathComponent(i);

                        CompositeAssertionTreeNode newAncestor;
                        if(ancestor instanceof AllAssertionTreeNode) {
                            newAncestor = new AllAssertionTreeNode(new AllAssertion());
                        } else if(ancestor instanceof OneOrMoreAssertionTreeNode) {
                            newAncestor = new OneOrMoreAssertionTreeNode(new OneOrMoreAssertion());
                        } else if (ancestor instanceof ForEachLoopAssertionPolicyNode) {
                            newAncestor = new ForEachLoopAssertionPolicyNode(new ForEachLoopAssertion());
                        } else {
                            break;
                        }

                        if(currentAncestor != null) {
                            newAncestor.add(currentAncestor);
                            ((CompositeAssertion)newAncestor.asAssertion()).addChild(currentAncestor.asAssertion());
                        }
                        currentAncestor = newAncestor;
                        ancestorMap.put(ancestor, currentAncestor);

                        if(immediateParent == null) {
                            immediateParent = newAncestor;
                        }

                        if(assertionMap.containsKey(ancestor)) {
                            assertionsToSkip.add(node);

                            immediateParent.add((AbstractTreeNode)node.clone());  // add copy, not move node
                            ((CompositeAssertion)immediateParent.asAssertion()).addChild(node.asAssertion());

                            if(assertionMap.get(ancestor) == ancestor) {
                                assertionMap.putAll(ancestorMap);
                            } else {
                                CompositeAssertionTreeNode x = (CompositeAssertionTreeNode)assertionMap.get(ancestor);
                                for(int j = 0;j < currentAncestor.getChildCount();j++) {
                                    AbstractTreeNode child = (AbstractTreeNode)currentAncestor.getChildAt(j);
                                    x.add(child);
                                    ((CompositeAssertion)x.asAssertion()).addChild(child.asAssertion());
                                }
                            }

                            break;
                        }
                    }
                }

                assertionMap.put(node, node);
            }
        }

        List<AbstractTreeNode> assertions = new ArrayList<>();
        for(TreePath path : paths) {
            if(path.getLastPathComponent() instanceof AbstractTreeNode) {
                AbstractTreeNode node = (AbstractTreeNode)path.getLastPathComponent();
                if (node.asAssertion() instanceof BlankAssertion) {
                    continue;
                }
                if(!assertionsToSkip.contains(node)) {
                    assertions.add(assertionMap.get(node));
                }
            }
        }

        return assertions.toArray(new AbstractTreeNode[assertions.size()]);
    }
}
