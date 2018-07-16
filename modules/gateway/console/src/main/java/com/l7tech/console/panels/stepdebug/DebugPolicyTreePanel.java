package com.l7tech.console.panels.stepdebug;

import com.l7tech.console.tree.AssertionLineNumbersTree;
import com.l7tech.console.tree.policy.*;
import com.l7tech.console.util.PopUpMouseListener;
import com.l7tech.gui.util.ImageCache;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.Policy;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.CommentAssertion;
import com.l7tech.policy.wsp.WspReader;
import com.l7tech.util.ExceptionUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * A panel that contains {@link AssertionLineNumbersTree}, {@link AssertionBreakpointsTree}, and policy tree.
 */
public class DebugPolicyTreePanel extends JPanel {
    private static final Icon TOGGLE_BREAKPOINT_ICON = new ImageIcon(ImageCache.getInstance().getIcon("com/l7tech/console/resources/Stop16.gif"));
    private static final Icon REMOVE_ALL_BREAKPOINT_ICON = new ImageIcon(ImageCache.getInstance().getIcon("com/l7tech/console/resources/delete.gif"));
    private static final Icon INVALID_POLICY_ICON = new ImageIcon(ImageCache.getInstance().getIcon("com/l7tech/console/resources/Alert16x16.gif"));

    private final PolicyStepDebugDialog policyStepDebugDialog;
    private AssertionLineNumbersTree lineNumbersTree;
    private AssertionBreakpointsTree breakpointsTree;
    private JTree policyTree;

    private PopUpMouseListener policyTreePopUpMenuListener = new PopUpMouseListener() {
        @Override
        protected void popUpMenuHandler(MouseEvent mouseEvent) {
            if (SwingUtilities.isRightMouseButton(mouseEvent)) {
                int row = policyTree.getClosestRowForLocation(mouseEvent.getX(), mouseEvent.getY());
                policyTree.setSelectionRow(row);
                AssertionTreeNode node = (AssertionTreeNode) policyTree.getLastSelectedPathComponent();
                if (node != null) {
                    toggleBreakpointMenuAction.setEnabled(PolicyStepDebugDialog.isBreakpointAllowed(node.asAssertion()));
                    JPopupMenu menu = new JPopupMenu();
                    menu.add(toggleBreakpointMenuAction);
                    menu.add(removeAllBreakpointsMenuAction);
                    menu.show(mouseEvent.getComponent(), mouseEvent.getX(), mouseEvent.getY());
                }
            }
        }
    };

    private Action toggleBreakpointMenuAction = new AbstractAction("Toggle Breakpoint", TOGGLE_BREAKPOINT_ICON) {
        @Override
        public void actionPerformed(ActionEvent e) {
            AssertionTreeNode node = (AssertionTreeNode) policyTree.getLastSelectedPathComponent();
            if (node != null) {
                policyStepDebugDialog.onToggleBreakpoint(node);
            }
        }
    };

    private Action removeAllBreakpointsMenuAction = new AbstractAction("Remove All Breakpoints", REMOVE_ALL_BREAKPOINT_ICON) {
        @Override
        public void actionPerformed(ActionEvent e) {
            policyStepDebugDialog.onRemoveAllBreakpoints();
        }
    };

    /**
     * Creates <code>DebugPolicyTreePanel</code>.
     *
     * @param policyStepDebugDialog the policy step debug dialog
     */
    DebugPolicyTreePanel(@NotNull PolicyStepDebugDialog policyStepDebugDialog) {
        super();
        this.policyStepDebugDialog = policyStepDebugDialog;
    }

    /**
     * Initializes the policy tree.
     *
     * @param policy the policy
     * @return true if policy tree is valid, false otherwise.
     */
    boolean initialize(@NotNull Policy policy) {
        // Policy tree
        //
        boolean isSuccessful = this.initPolicyTree(policy);

        // Assertion line number tree
        //
        this.initLineNumbersTree();

        // Breakpoint tree
        //
        this.initBreakpointsTree();

        // Add to the panel.
        //
        JPanel panel = new JPanel();
        panel.setBackground(policyTree.getBackground());
        panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
        panel.add(lineNumbersTree);
        panel.add(Box.createHorizontalStrut(5));
        panel.add(new JSeparator(SwingConstants.VERTICAL));
        panel.add(breakpointsTree);
        panel.add(new JSeparator(SwingConstants.VERTICAL));

        JScrollPane scrollPane = new JScrollPane();
        scrollPane.setViewportView(policyTree);
        scrollPane.setRowHeaderView(panel);

        this.setLayout(new BorderLayout());
        this.add(scrollPane, BorderLayout.CENTER);

        return isSuccessful;
    }

    /**
     * Updates the panel.
     *
     * @param scrollToCurrentLine Whether or not to make current node in policy tree visible
     */
    void update(boolean scrollToCurrentLine) {
        if (scrollToCurrentLine) {
            this.setCurrentPolicyTreeNodeVisible();
        }
        policyTree.repaint();
        breakpointsTree.updateTree();
        AssertionTreeNode currentNode = this.findCurrentLineNode();
        if (currentNode instanceof IncludeAssertionPolicyNode &&
            ((IncludeAssertionPolicyNode) currentNode).getPolicy() == null) {
            // If the included fragment policy is not available, disable the Step In button.
            //
            policyStepDebugDialog.disableStepInButton();
        }
    }

    /**
     * Returns the next node when Stepping over.
     *
     * @return the next node. Null if next node is not found.
     */
    @Nullable
    AssertionTreeNode getNextNodeStepOver() {
        AssertionTreeNode currentNode = this.findCurrentLineNode();
        return this.findNextSiblingNode(currentNode);
    }

    /**
     * Returns the next node when Stepping out.
     *
     * @return the next node. Null if next node is not found.
     */
    @Nullable
    AssertionTreeNode getNextNodeStepOut() {
        AssertionTreeNode currentNode = this.findCurrentLineNode();
        AssertionTreeNode parentNode = (AssertionTreeNode) currentNode.getParent();
        return this.findNextSiblingNode(parentNode);
    }

    private boolean initPolicyTree(Policy policy) {
        boolean isSuccessful = false;

        policyTree = new JTree();
        policyTree.setAlignmentX(Component.LEFT_ALIGNMENT);
        policyTree.setAlignmentY(Component.TOP_ALIGNMENT);
        policyTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        // Remove key binding for F2 key in JTree.
        policyTree.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_F2, 0), "none");

        String xml = policy.getXml();
        if (xml == null) {
            this.makeErrorMessagePolicyTree("Empty policy");
        } else {
            try {
                Assertion assertion = WspReader.getDefault().parsePermissively(xml, WspReader.INCLUDE_DISABLED);
                TreeModel model = new PolicyTreeModel(assertion);
                AssertionTreeNode assertionTreeNode = (AssertionTreeNode) model.getRoot();
                Map<Goid,String> identityProviderNameMap = new HashMap<>();
                PolicyTreeUtils.updateAssertions(assertionTreeNode, identityProviderNameMap);

                policyTree.setShowsRootHandles(true);
                policyTree.setRootVisible(false);
                policyTree.setModel(model);
                policyTree.setCellRenderer(new DebugPolicyTreeCellRenderer(policyStepDebugDialog));
                policyTree.addMouseListener(policyTreePopUpMenuListener);

                // Configure "Toggle Breakpoint" and "Remove All Breakpoints" buttons.
                //
                final JButton toggleBreakpointButton = policyStepDebugDialog.getToggleBreakpointButton();
                toggleBreakpointButton.setEnabled(false);

                policyTree.addTreeSelectionListener(new TreeSelectionListener() {
                    @Override
                    public void valueChanged(TreeSelectionEvent e) {
                        AssertionTreeNode node = (AssertionTreeNode) policyTree.getLastSelectedPathComponent();
                        if (node != null) {
                            toggleBreakpointButton.setEnabled(PolicyStepDebugDialog.isBreakpointAllowed(node.asAssertion()));
                        } else {
                            toggleBreakpointButton.setEnabled(false);
                        }
                    }
                });

                toggleBreakpointButton.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        AssertionTreeNode node = (AssertionTreeNode) policyTree.getLastSelectedPathComponent();
                        if (node != null) {
                            policyStepDebugDialog.onToggleBreakpoint(node);
                        }
                    }
                });

                policyStepDebugDialog.getRemoveAllBreakpointsButton().addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        policyStepDebugDialog.onRemoveAllBreakpoints();
                    }
                });

                isSuccessful = true;
            } catch (IOException e) {
                this.makeErrorMessagePolicyTree("Bad policy XML: " + ExceptionUtils.getMessage(e));
            }
        }

        return isSuccessful;
    }

    private void makeErrorMessagePolicyTree(String message) {
        TreeModel model = new DefaultTreeModel(makeErrorMessageNode(message));
        DefaultTreeCellRenderer renderer = new DefaultTreeCellRenderer();
        renderer.setLeafIcon(INVALID_POLICY_ICON);
        policyTree.setCellRenderer(renderer);
        policyTree.setShowsRootHandles(false);
        policyTree.setRootVisible(true);
        policyTree.setModel(model);
    }

    private void initLineNumbersTree() {
        lineNumbersTree = new AssertionLineNumbersTree(policyTree);
        lineNumbersTree.setVisible(true);
        lineNumbersTree.updateOrdinalsDisplaying();
    }

    private void initBreakpointsTree() {
        breakpointsTree = new AssertionBreakpointsTree(policyStepDebugDialog, policyTree);
        breakpointsTree.updateTree();
    }

    private void setCurrentPolicyTreeNodeVisible() {
        AssertionTreeNode currentNode = this.findCurrentLineNode();
        if (currentNode != null) {
            TreePath path = new TreePath(currentNode.getPath());
            policyTree.scrollPathToVisible(path);
        }
    }

    /**
     * Find the next sibling node. If next sibling node does not exit, attempt to
     * find parent's next sibling recursively.
     */
    private AssertionTreeNode findNextSiblingNode(AssertionTreeNode node) {
        AssertionTreeNode nextSiblingNode = (AssertionTreeNode) node.getNextSibling();
        if (nextSiblingNode != null) {
            Assertion assertion = nextSiblingNode.asAssertion();
            if (PolicyStepDebugDialog.isBreakpointAllowed(assertion)) {
                return nextSiblingNode;
            } else {
                return this.findNextSiblingNode(nextSiblingNode);
            }
        } else {
            if (!node.isRoot()) {
                AssertionTreeNode parentNode = (AssertionTreeNode) node.getParent();
                return this.findNextSiblingNode(parentNode);
            }
        }
        return null;
    }

    /**
     * Find the current node.
     */
    private AssertionTreeNode findCurrentLineNode() {
        AssertionTreeNode root = (AssertionTreeNode) policyTree.getModel().getRoot();
        Enumeration e = root.preorderEnumeration();
        while (e.hasMoreElements()) {
            AssertionTreeNode node = (AssertionTreeNode) e.nextElement();
            if (policyStepDebugDialog.isCurrentLine(node)) {
                return node;
            }
        }
        return null;
    }

    private static TreeNode makeErrorMessageNode(final String message) {
        return new LeafAssertionTreeNode<CommentAssertion>(new CommentAssertion(message)) {

            @Override
            public String getName(final boolean decorate, final boolean withComments) {
                return message;
            }

            @Override
            protected String iconResource(boolean open) {
                return null;
            }

            @Override
            public String toString() {
                return message;
            }
        };
    }
}