package com.l7tech.console.tree.policy;

import com.l7tech.console.MainWindow;
import com.l7tech.console.action.*;
import com.l7tech.console.logging.ErrorManager;
import com.l7tech.console.panels.ForEachLoopAssertionPolicyNode;
import com.l7tech.console.panels.InformationDialog;
import com.l7tech.console.poleditor.PolicyEditorPanel;
import com.l7tech.console.policy.PolicyTransferable;
import com.l7tech.console.tree.AbstractTreeNode;
import com.l7tech.console.tree.TransferableTreePath;
import com.l7tech.console.tree.TransferableTreePaths;
import com.l7tech.console.util.PopUpMouseListener;
import com.l7tech.console.util.Refreshable;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.TopComponents;
import com.l7tech.gateway.common.security.rbac.AttemptedUpdate;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.gui.util.ClipboardActions;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.Utilities;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.policy.Policy;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.Include;
import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.policy.assertion.composite.CompositeAssertion;
import com.l7tech.policy.assertion.composite.ForEachLoopAssertion;
import com.l7tech.policy.assertion.composite.OneOrMoreAssertion;
import com.l7tech.policy.wsp.WspReader;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Pair;
import org.apache.commons.lang.ArrayUtils;
import org.springframework.context.ApplicationContext;

import javax.swing.*;
import javax.swing.Timer;
import javax.swing.border.Border;
import javax.swing.event.*;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.dnd.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Class PolicyTree is the extended <code>JTree</code> with additional
 */
public class PolicyTree extends JTree implements DragSourceListener,
  DragGestureListener, Autoscroll, TreeModelListener, Refreshable {
    static final Logger log = Logger.getLogger(PolicyTree.class.getName());
    /**
     * component name
     */
    public final static String NAME = "policy.tree";
    private PolicyEditorPanel policyEditorPanel;

    // d&d
    private TreePath[] pathSource;                // The path being dragged
    private BufferedImage imgGhost;                    // The 'drag image'
    private Point ptOffset = new Point();    // Where, in the drag image, the mouse was clicked
    private Border topBorder;
    private boolean writeAccess;
    private final WspReader wspReader;

    public PolicyEditorPanel getPolicyEditorPanel() {
        return policyEditorPanel;
    }

    /**
     * Create empty policy tree
     */
    public PolicyTree(ApplicationContext applicationContext) {
        super((PolicyTreeModel)null);
        wspReader = applicationContext.getBean("wspReader", WspReader.class);
        initialize();
        setSelectionModel(getTreeSelectionModel());
    }

    @Override
    public void setModel(TreeModel newModel) {
        if (newModel == null) return;

        TreeModel oldModel = getModel();
        if (oldModel != null) {
            oldModel.removeTreeModelListener(this);
        }
        super.setModel(newModel);
        newModel.addTreeModelListener(this);
    }

    /**
     * initialize
     */
    private void initialize() {
        topBorder = BorderFactory.createEmptyBorder(10, 0, 0, 0);
        setBorder(topBorder);
        // Make this JTree a drag source
        DragSource dragSource = DragSource.getDefaultDragSource();
        dragSource.createDefaultDragGestureRecognizer(this, DnDConstants.ACTION_COPY_OR_MOVE, this);

        // Also, make this JTree a drag target
        DropTarget dropTarget = new DropTarget(this, new PolicyDropTargetListener());
        dropTarget.setDefaultActions(DnDConstants.ACTION_COPY_OR_MOVE);

        addKeyListener(new TreeKeyListener());
        addMouseListener(new TreeMouseListener());
        addTreeExpansionListener(new ExpansionListener());
        setCellRenderer(new PolicyTreeCellRenderer());

        ToolTipManager.sharedInstance().registerComponent(this);


        TreeSelectionListener tsl = new TreeSelectionListener() {
            ClipboardOwner owner = new ClipboardOwner() {
                @Override
                public void lostOwnership(Clipboard clipboard, Transferable contents) {
                    // No action required
                }
            };

            @Override
            public void valueChanged(TreeSelectionEvent e) {
                Clipboard sel = Utilities.getDefaultSystemSelection();
                if (sel == null) return;
                sel.setContents(policyTreeTransferHandler.createTransferable(PolicyTree.this, e.getPaths()), owner);
            }
        };

        if (Utilities.getDefaultSystemSelection() != null)
            getSelectionModel().addTreeSelectionListener(tsl);

        // Disable cut (and ctrl-X)
        putClientProperty(ClipboardActions.CUT_HINT, Boolean.FALSE);

        ClipboardActions.replaceClipboardActionMap(this);

        // To support "Copy All", need to register a "copyAll" action that does equivalent of Select All followed by Copy.
        getActionMap().put("copyAll", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                getSelectionModel().clearSelection();
                ClipboardActions.getCopyAction().actionPerformed(e);
            }
        });

        setTransferHandler(policyTreeTransferHandler);
    }

    /**
     * Callers should ensure they are on the swing thread
     *
     * @param stringOrdinal display the assertion tree node whose assertion has this ordinal. May be either
     * a single integer e.e. '2' or may use a sub index syntax e.g. 2.3.2, where each integer before the last, must
     * resolve to an include assertion, otherwise an error message will be shown the currently selected assertion will
     * not change
     *
     * @throws NumberFormatException if any number cannot be parsed from the stringOrdinal
     */
    public void goToAssertionTreeNode(final String stringOrdinal) throws NumberFormatException {

        StringTokenizer tokenizer = new StringTokenizer(stringOrdinal, ".,-");
        final List<Integer> indexPath = new ArrayList<>();
        while (tokenizer.hasMoreTokens()) {
            indexPath.add(Integer.parseInt(tokenizer.nextToken()));
        }

        final Object rootObj = this.getModel().getRoot();
        if (!(rootObj instanceof AssertionTreeNode))
            throw new IllegalStateException("Invalid root found");//coding error

        AssertionTreeNode root = (AssertionTreeNode) rootObj;

        AssertionTreeNode foundNode;
        try {
            foundNode = findTreeNode(root, indexPath);
        } catch (OrdinalIndexOutOfRangeException e) {
            InformationDialog iDialog = new InformationDialog(e.getMessage());
            MainWindow.showInformationDialog(iDialog, null);
            foundNode = e.getNodeToGoto();
            if(foundNode == root){
                foundNode = (AssertionTreeNode) root.getFirstChild();
            }
        }

        if (foundNode == null) {
            //this should only happen with an empty policy
            InformationDialog iDialog = new InformationDialog("Assertion #" + stringOrdinal + " not found.");
            MainWindow.showInformationDialog(iDialog, null);
            return;
        }

        final TreeNode[] pathToNode = foundNode.getPath();
        final TreePath path = new TreePath(pathToNode);
        this.setSelectionPath(path);
        this.scrollPathToVisible(path);
        this.requestFocusInWindow();
    }

    private AssertionTreeNode findTreeNode(final AssertionTreeNode treeNode, final List<Integer> assertionOrdinal) throws OrdinalIndexOutOfRangeException{

        AssertionTreeNode foundAssertion = treeNode;
        for (int i = 0, assertionOrdinalSize = assertionOrdinal.size(); i < assertionOrdinalSize; i++) {
            Integer index = assertionOrdinal.get(i);
            final Pair<AssertionTreeNode, AssertionTreeNode> nodePair = findTreeNode(foundAssertion, index);
            foundAssertion = nodePair.left;

            if(foundAssertion == null) {
                //assertion index was too large
                throw new OrdinalIndexOutOfRangeException(
                        "Assertion number " + index + " not found. Going to assertion #" +
                                AssertionTreeNode.getVirtualOrdinalString(nodePair.right), nodePair.right);
            }

            if (assertionOrdinal.size() > 1 && i != assertionOrdinal.size() - 1) { //any assertion before the last must be an include
                if (!(foundAssertion.asAssertion() instanceof Include)) {
                    //when indexing e.g. 1.2.3 and were not on the last assertion and we don't find an include, then the index is invalid
                    throw new OrdinalIndexOutOfRangeException(
                            "Invalid assertion number sub index. Going to assertion #" +
                            AssertionTreeNode.getVirtualOrdinalString(foundAssertion), foundAssertion);

                }
            }
        }

        return foundAssertion;
    }

    private Pair<AssertionTreeNode, AssertionTreeNode> findTreeNode(final AssertionTreeNode treeNode, final int assertionOrdinal) throws OrdinalIndexOutOfRangeException{

        if(assertionOrdinal < Assertion.MIN_DISPLAYED_ORDINAL){
            final String assertionNumberToGoTo;
            if(treeNode.getParent() == null){//this is the root node
                assertionNumberToGoTo = String.valueOf(Assertion.MIN_DISPLAYED_ORDINAL);
            }else{
                assertionNumberToGoTo = AssertionTreeNode.getVirtualOrdinalString(treeNode);
            }

            throw new OrdinalIndexOutOfRangeException(
                    "Invalid assertion number." +
                            " Each number must be >= " + Assertion.MIN_DISPLAYED_ORDINAL+". Going to assertion #" +
                    assertionNumberToGoTo, treeNode);
        }

        AssertionTreeNode lastFoundNode = treeNode;
        for(int i = 0; i < treeNode.getChildCount(); i++){
            final TreeNode childTreeNode = treeNode.getChildAt(i);
            if(!( childTreeNode instanceof AssertionTreeNode)) continue;

            final AssertionTreeNode childAssertionTreeNode = (AssertionTreeNode) childTreeNode;
            final Assertion childAssertion = childAssertionTreeNode.asAssertion();

            if(childAssertion.getOrdinal() == assertionOrdinal)
                return new Pair<>(childAssertionTreeNode, lastFoundNode);

            if (childAssertion instanceof Include)
                continue;//includes are only accessed if indexed correctly i.e. don't look at the includes children

            final Pair<AssertionTreeNode, AssertionTreeNode> foundNode = findTreeNode(childAssertionTreeNode, assertionOrdinal);
            if(foundNode.left != null) return foundNode;
            lastFoundNode = foundNode.right;
        }

        return new Pair<>(null, lastFoundNode);
    }

    private static class OrdinalIndexOutOfRangeException extends Exception{
        private final AssertionTreeNode nodeToGoto;

        private OrdinalIndexOutOfRangeException(String message, AssertionTreeNode nodeToGoto) {
            super(message);
            if(nodeToGoto == null) throw new NullPointerException("nodeToGoto cannot be null");
            this.nodeToGoto = nodeToGoto;
        }

        public AssertionTreeNode getNodeToGoto() {
            return nodeToGoto;
        }
    }

    /**
     * Import the specified assertion subtree into this policy.  Attempt to insert it at the current selection,
     * if possible; otherwise, insert it at the end.
     * @param ass The assertion to import
     * @return true if the assertion subtree was imported successfully; false if nothing was done.
     */
    private boolean importAssertion(Assertion ass) {
        TreePath path = getSelectionPath();
        if (path == null) {
            AbstractTreeNode atn = (AbstractTreeNode)getModel().getRoot();
            path = new TreePath(atn.getPath());
        }

        if (!(path.getLastPathComponent() instanceof AssertionTreeNode)) {
            // todo is this possible?
            log.warning("Rejecting paste -- paste target is not an AssertionTreeNode");
            return false;
        }
        AssertionTreeNode assertionTreeNodeCopy = AssertionTreeNodeFactory.asTreeNode(ass);
        if ( policyEditorPanel != null ) policyEditorPanel.updateAssertions( assertionTreeNodeCopy );

        Pair<AssertionTreeNode,Integer> target = getTargetForPath( path, assertionTreeNodeCopy, false );
        if ( target.left != null ) {
            PolicyTreeModel model = (PolicyTreeModel)getModel();
            model.rawInsertNodeInto(assertionTreeNodeCopy, target.left, target.right);
            if ( target.right==0 && !target.left.isExpanded() ) {
                target.left.setExpanded( true ); // Expand so other pasted nodes end up in the folder
            }
            return true;
        }

        return false;
    }

    private Pair<AssertionTreeNode,Integer> getTargetForPath( final TreePath path,
                                                              final AbstractTreeNode toDrop,
                                                              final boolean dropAsFirstContainerChild ) {
        return getTargetForPath(path, new AbstractTreeNode[]{toDrop}, dropAsFirstContainerChild, true);
    }

    private Pair<AssertionTreeNode,Integer> getTargetForPath( final TreePath path,
                                                              final AbstractTreeNode[] toDrop,
                                                              final boolean dropAsFirstContainerChild ) {
           return getTargetForPath( path,  toDrop, dropAsFirstContainerChild, true );
    }

    private Pair<AssertionTreeNode,Integer> getTargetForPath( final TreePath path,
                                                              final AbstractTreeNode toDrop,
                                                              final boolean dropAsFirstContainerChild,
                                                              final boolean allowAppendToEndOfChildList ) {
        return getTargetForPath( path,  new AbstractTreeNode[]{toDrop}, dropAsFirstContainerChild, allowAppendToEndOfChildList );
    }

    private Pair<AssertionTreeNode,Integer> getTargetForPath( final TreePath path,
                                                              final AbstractTreeNode[] toDrop,
                                                              final boolean dropAsFirstContainerChild,
                                                              final boolean allowAppendToEndOfChildList ) {
        AssertionTreeNode targetTreeNode =
                ((AssertionTreeNode) path.getLastPathComponent());

        TreePath pathToTarget = path;
        AssertionTreeNode insertAfter = null;
        int targetIndex = 0;
        if (path.getPathCount()==1 || (targetTreeNode.getAllowsChildren() && allowAppendToEndOfChildList)) {
            targetIndex = targetTreeNode.getChildCount();
        }
        while ( targetTreeNode != null ) {
            if (targetTreeNode.getAllowsChildren()
                    && (pathToTarget.getParentPath()==null || isModelExpanded(pathToTarget) || (isModelExpanded(pathToTarget.getParentPath()) && targetTreeNode.getChildCount()==0))
                    && acceptsAll(targetTreeNode,toDrop)) {
                if ( insertAfter != null ) {
                    targetIndex = targetTreeNode.getIndex( insertAfter ) + 1;
                }
                break;
            } else {
                insertAfter = targetTreeNode;
                targetTreeNode = (AssertionTreeNode) targetTreeNode.getParent();
                pathToTarget = pathToTarget.getParentPath();
            }
        }

        if ( dropAsFirstContainerChild ) {
            targetIndex = 0;
        }

        return new Pair<>(targetTreeNode,targetIndex);
    }

    /**
     * As JTree.isExpanded but checks both the model and the UI to see if either thinks the node is
     * expanded.
     */
    private boolean isModelExpanded( final TreePath path ) {
        boolean expanded = true;

        AssertionTreeNode node = (AssertionTreeNode) path.getLastPathComponent();
        TreePath nodePath = path;
        while ( node != null && nodePath != null ) {
            if ( !(node.isExpanded() || isExpanded(nodePath)) ) {
                expanded = false;
                break;
            }
            node = (AssertionTreeNode) node.getParent();
            nodePath = nodePath.getParentPath();
        }

        return expanded;
    }

    private boolean acceptsAll( final AssertionTreeNode target,
                                final AbstractTreeNode[] nodes ) {
        boolean accepts = true;

        for ( final AbstractTreeNode node : nodes ) {
            if ( !target.accept( node ) ) {
                accepts = false;
                break;
            }
        }

        return accepts;
    }

    public void setPolicyEditor(PolicyEditorPanel pe) {
        policyEditorPanel = pe;
    }

    class ExpansionListener implements TreeExpansionListener{
        @Override
        public void treeExpanded(final TreeExpansionEvent event) {
            setSelectedNode(event);
        }

        @Override
        public void treeCollapsed(final TreeExpansionEvent event) {
            setSelectedNode(event);
        }

        private void setSelectedNode(final TreeExpansionEvent event) {
            // do on event dispatch thread
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    final Object source = event.getSource();
                    if (source instanceof PolicyTree) {
                        final PolicyTree policyTree = (PolicyTree) source;
                        final Object selected = policyTree.getLastSelectedPathComponent();
                        if (selected == null) {
                            // select the node that was expanded/collapsed
                            policyTree.setSelectionPath(event.getPath());
                        }
                    }
                }
            });
        }
    }

    /**
     * KeyAdapter for the policy trees
     */
    class TreeKeyListener extends KeyAdapter {
        /**
         * Invoked when a key has been pressed.
         */
        @Override
        public void keyPressed(KeyEvent e) {
            JTree tree = (JTree)e.getSource();
            TreePath path = tree.getSelectionPath();
            if (path == null) return;
            AssertionTreeNode node =
              (AssertionTreeNode)path.getLastPathComponent();
            if (node == null) return;
            int keyCode = e.getKeyCode();
            if (keyCode == KeyEvent.VK_DELETE) {
                AssertionTreeNode[] nodes = toAssertionTreeNodeArray(tree.getSelectionPaths());
                if (nodes.length < 2) nodes = null;
                if (canDelete(node, nodes)){
                    new DeleteAssertionAction(node, nodes).actionPerformed(null);
                }
            } else if (keyCode == KeyEvent.VK_ENTER) {
                final TreePath[] paths = PolicyTree.this.getSelectionPaths();
                if ( paths != null && paths.length == 1 && paths[0] != null && paths[0].getLastPathComponent() instanceof AbstractTreeNode) {
                    final Action action = ((AbstractTreeNode) paths[0].getLastPathComponent()).getPreferredAction();
                    if ( action != null ) action.actionPerformed( new ActionEvent(tree, ActionEvent.ACTION_PERFORMED, "enter-key") );
                }
            }
        }

        private AssertionTreeNode[] toAssertionTreeNodeArray(TreePath[] paths) {
            java.util.List<AssertionTreeNode> assertionTreeNodes = new ArrayList<>();

            if (paths != null) {
                for (TreePath path : paths) {
                    assertionTreeNodes.add((AssertionTreeNode) path.getLastPathComponent());
                }
            }

            return assertionTreeNodes.toArray(new AssertionTreeNode[assertionTreeNodes.size()]);
        }

        private boolean canDelete(AssertionTreeNode node, AssertionTreeNode[] nodes) {
            if (!Registry.getDefault().isAdminContextPresent()) return false;

            try {
                // Case 1: if the node is associated to a published service
                PublishedService svc = node.getService();
                boolean hasPermission = Registry.getDefault().getSecurityProvider().hasPermission(new AttemptedUpdate(EntityType.SERVICE, svc));

                // Case 2: if the node is associated to a policy fragment
                if (svc == null && !hasPermission) {
                    Policy policy = node.getPolicy();
                    hasPermission = Registry.getDefault().getSecurityProvider().hasPermission(new AttemptedUpdate(EntityType.POLICY, policy));
                }

                if (!hasPermission) {
                    return false;
                }
            } catch (Exception e) {
                throw new RuntimeException("Couldn't get current service or policy", e);
            }

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
    }

    class TreeMouseListener extends PopUpMouseListener {
        int initialToolTipDelay = -1;
        int dismissToolTipDelay = -1;

        /**
         * Handle the mouse click popup when the Tree item is right clicked. The context sensitive
         * menu is displayed if the right click was over an item.
         *
         * @param mouseEvent The event
         */
        @Override
        protected void popUpMenuHandler(final MouseEvent mouseEvent) {
            // When the mouse right button is clicked, set the policy tree as focused.
            PolicyTree.this.requestFocus();
            // After the policy tree is set to be focused on, KeyboardFocusManager (in ClipBoardActions.java) will
            // get notified for the property "permanentFocusOwner" change. Then, KeyboardFocusManager will reset if
            // clipboard global actions (such as Copy, Copy All, and Paste) are enabled or disabled.

            // After the setting of clipboard global actions are ready, then the context menu will pop up with/without Copy, Copy All, and Paste.
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    JTree tree = (JTree)mouseEvent.getSource();

                    AssertionTreeNode node;
                    if (mouseEvent.isPopupTrigger()) {
                        int closestRow = tree.getClosestRowForLocation(mouseEvent.getX(), mouseEvent.getY());
                        if (closestRow == -1) {
                            node = (AssertionTreeNode)tree.getModel().getRoot();
                            if (node == null) {
                                return;
                            }
                        } else {
                            int[] rows = tree.getSelectionRows();
                            boolean found = false;

                            for (int i = 0; rows != null && i < rows.length; i++) {
                                if (rows[i] == closestRow) {
                                    found = true;
                                    break;
                                }
                            }

                            if (!found) {
                                tree.setSelectionRow(closestRow);
                            }
                            node = (AssertionTreeNode)tree.getLastSelectedPathComponent();
                        }

                        if (node != null) {
                            Action[] actions;
                            if (policyEditorPanel != null) {
                                actions = policyEditorPanel.getActions();
                            }else{
                                actions = node.getActions();
                            }
                            // If the node is nested in a policy include, all editor actions (such as AddAllAssertionAction,
                            // AddOneOrMoreAssertionAction, DeleteAssertionAction, AssertionMoveDownAction, AssertionMoveUpAction,
                            // Disable Assertion, and Enable Assertion) will not be displayed in the context menu.
                            if (node.isDescendantOfInclude(false)) {
                                actions = verifyActionsInPolicyInclude(actions);
                            }


                            JPopupMenu menu = getPopupMenu(actions);
                            if (menu != null) {
                                menu.show(mouseEvent.getComponent(), mouseEvent.getX(), mouseEvent.getY());
                            }

                        }
                    }
                }
            });
        }

        /**
         * A helper method checks if each actions is an editing-assertion actions.  If so, remove it from the action list.
         *
         * @param actions the actions need verifying.
         */
        private Action[] verifyActionsInPolicyInclude(Action[] actions) {
            List<Action> actionList = new ArrayList<>(Arrays.asList(actions));

            // Check if each actions is an editing-assertion actions.
            for (Action action: actions) {
                if (action instanceof SecureAction) {
                    SecureAction sa = (SecureAction)action;
                    if (sa instanceof AddAllAssertionAction ||
                        sa instanceof AddOneOrMoreAssertionAction ||
                        sa instanceof DeleteAssertionAction ||
                        sa instanceof AssertionMoveDownAction ||
                        sa instanceof AssertionMoveUpAction ||
                        sa instanceof DisableAssertionAction ||
                        sa instanceof EnableAssertionAction ||
                        sa instanceof EnableAllAssertionsAction ||
                        sa instanceof AddIdentityAssertionAction) {
                        actionList.remove(sa);
                    }
                }
            }
            // return the updated action array
            return actionList.toArray(new Action[actionList.size()]);
        }

        /**
         * Invoked when the mouse has been clicked on a component.
         */
        @Override
        public void mouseClicked(MouseEvent e) {
            if (e.getClickCount() != 2) return;
            JTree tree = (JTree)e.getSource();
            TreePath path = tree.getSelectionPath();
            if (path == null) return;
            AssertionTreeNode node = (AssertionTreeNode)path.getLastPathComponent();
            if (node == null) return;

            AbstractTreeNode closestNode;
            int closestRow = tree.getRowForLocation(e.getX(), e.getY());
            if (closestRow == -1) {
                closestNode = (AbstractTreeNode)tree.getModel().getRoot();
                if (closestNode != node) {
                    return;
                }
            }

            Action a = node.getPreferredAction();
            if (a != null) {
                if (a instanceof SecureAction) {
                    SecureAction sa = (SecureAction)a;
                    if (!sa.isAuthorized()) return;
                }

                ActionManager.getInstance().invokeAction(a);
            }

        }

        /**
         * The main purpose of overriding this method is to check if the location of a left-click is within the bounds
         * of the JTree.  If it is not within the bounds, then set no selection in the JTree.  This overridden is for
         * fixing the bug 7427 - "Selection is not cleared in policy editor when focus moves outside assertion area."
         *
         * @param e: the mouse event
         */
        @Override
        public void mousePressed(MouseEvent e) {
            JTree tree = (JTree)e.getSource();
            int closestRow = tree.getRowForLocation(e.getX(), e.getY());
            if (closestRow == -1) { // -1 means not within the bounds of the JTree
                PolicyTree.this.clearSelection();
            }
            
            super.mousePressed(e);
        }

        /**
         * Invoked when the mouse enters a component.
         */
        @Override
        public void mouseEntered(MouseEvent e) {
            initialToolTipDelay = ToolTipManager.sharedInstance().getInitialDelay();
            ToolTipManager.sharedInstance().setInitialDelay(100);
            dismissToolTipDelay = ToolTipManager.sharedInstance().getDismissDelay();
            ToolTipManager.sharedInstance().setDismissDelay(60 * 1000);
        }

        /**
         * Invoked when the mouse exits a component.
         */
        @Override
        public void mouseExited(MouseEvent e) {
            if (initialToolTipDelay != -1) {
                ToolTipManager.sharedInstance().setInitialDelay(initialToolTipDelay);
            }
            if (dismissToolTipDelay != -1) {
                ToolTipManager.sharedInstance().setDismissDelay(dismissToolTipDelay);
            }
        }
    }

    /**
     * Make a popup menu from actions.
     * The menu is constructed from the set of actions and returned. If the actions arrays
     * is empty, or there are no actions that could be added to the menu (unauthorized user)
     * the <b>null</b>  <code>JPopupMenu</code> is returned
     *
     * @return the popup menu
     */
    private JPopupMenu getPopupMenu(Action[] actions) {
        JPopupMenu pm = new JPopupMenu();
        boolean empty = true;
        for (final Action action : actions) {
            if (action instanceof SecureAction) {
                if (((SecureAction) action).isAuthorized()) {
                    pm.add(action);
                    empty = false;
                }
            } else {
                pm.add(action);
                empty = false;
            }
        }

        // If system clipboard is unavailable, the context menu would go to the "fake" clipboard,
        // but keyboard shortcuts might still work properly with the "real" system clipboard.
        // To prevent this confusing behavior, we'll just suppress the context menu Copy/Paste if
        // the system clipboard isn't accessible to our code.
        if (ClipboardActions.isSystemClipboardAvailable() && (ClipboardActions.getGlobalCopyAction().isEnabled() ||
                ClipboardActions.getGlobalPasteAction().isEnabled()))
        {
            if (!empty)
                pm.add(new JPopupMenu.Separator());
            pm.add(ClipboardActions.getGlobalCopyAction());
            empty = false;
            if (ClipboardActions.getGlobalCopyAllAction().isEnabled())
                pm.add(ClipboardActions.getGlobalCopyAllAction());
            // To prevent obvious UI tragedy, we never add Paste as first item unless Copy is safely above it
            if (ClipboardActions.getGlobalPasteAction().isEnabled())
                pm.add(ClipboardActions.getGlobalPasteAction());
        }

        if (empty) { // no items have been added
            return null;
        }

        Utilities.removeToolTipsFromMenuItems(pm);
        return pm;
    }


    @Override
    public void dragEnter(DragSourceDragEvent dsde) {
        log.finest("entering dragEnter()");
    }

    @Override
    public void dragOver(DragSourceDragEvent dsde) {
        //log.finest("entering dragOver()");  // very spammy trace
    }

    @Override
    public void dropActionChanged(DragSourceDragEvent dsde) {
        log.finest("entering dropActionChanged()");
    }

    @Override
    public void dragDropEnd(DragSourceDropEvent dsde) {
        log.fine("entering dragDropEnd()");
        if (dsde.getDropSuccess()) {
            int nAction = dsde.getDropAction();
            if (nAction == DnDConstants.ACTION_MOVE) {
                // The dragged item (pathSource) has been inserted at the target selected by the user.
                // Now it is time to delete it from its original location.
                logPaths("REMOVING", pathSource);
                DefaultTreeModel model = (DefaultTreeModel)getModel();

                for(TreePath path : pathSource) {
                    model.removeNodeFromParent((MutableTreeNode)path.getLastPathComponent());
                }
                pathSource = null;
            }
        }
        //repaint();
    }

    @Override
    public void dragExit(DragSourceEvent dse) {
    }

    private TreePath[] trimTreePaths(TreePath[] paths) {
        if(paths == null) {
            return null;
        }

        HashSet<TreePath> pathSet = new HashSet<>(paths.length);
        pathSet.addAll(Arrays.asList(paths));

        List<TreePath> trimmedList = new ArrayList<>(paths.length);
        for(TreePath path : paths) {
            TreePath parentPath = path.getParentPath();
            boolean add = true;
            while(parentPath != null) {
                if(pathSet.contains(parentPath)) {
                    add = false;
                    break;
                }
                parentPath = parentPath.getParentPath();
            }

            if(add) {
                trimmedList.add(path);
            }
        }

        TreePath[] retVal = new TreePath[trimmedList.size()];
        int index = 0;
        for(TreePath path : trimmedList) {
            retVal[index++] = path;
        }

        return retVal;
    }

    @Override
    public void dragGestureRecognized(DragGestureEvent dge) {
        Point ptDragOrigin = dge.getDragOrigin();
        TreePath[] paths = trimTreePaths(getSelectionPaths());
        if (!canStartDrag(paths)) {
            return;
        }
        //Make sure that the drag origin is a valid tree path. We do not want to allow dragging from the expand collapse +-
        //This fixes [SSM-4150]
        TreePath path = getPathForLocation(ptDragOrigin.x, ptDragOrigin.y);
        if (path == null) {
            return;
        }

        // Work out the offset of the drag point from the TreePath bounding rectangle origin
        Rectangle raPath = getPathBounds(paths[0]);
        ptOffset.setLocation(ptDragOrigin.x - raPath.x, ptDragOrigin.y - raPath.y);

        // Get the cell renderer (which is a JLabel) for the path being dragged
        JLabel lbl = (JLabel)getCellRenderer().getTreeCellRendererComponent
          (this, // tree
           paths[0].getLastPathComponent(), // value
           false, // isSelected	(dont want a colored background)
           isExpanded(paths[0]), // isExpanded
           getModel().isLeaf(paths[0].getLastPathComponent()), // isLeaf
           0, // row			(not important for rendering)
           false                                            // hasFocus		(dont want a focus rectangle)
          );
        lbl.setSize((int)raPath.getWidth(), (int)raPath.getHeight()); // <-- The layout manager would normally do this

        // Get a buffered image of the selection for dragging a ghost image
        imgGhost = new BufferedImage((int)raPath.getWidth(), (int)raPath.getHeight(), BufferedImage.TYPE_INT_ARGB_PRE);
        Graphics2D g2 = imgGhost.createGraphics();

        // Ask the cell renderer to paint itself into the BufferedImage
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC, 0.5f));        // Make the image ghostlike
        lbl.paint(g2);

        // Now paint a gradient UNDER the ghosted JLabel text (but not under the icon if any)
        Icon icon = lbl.getIcon();
        int nStartOfText = (icon == null) ? 0 : icon.getIconWidth() + lbl.getIconTextGap();
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.DST_OVER, 0.5f));    // Make the gradient ghostlike
        g2.setPaint(new GradientPaint(nStartOfText, 0, SystemColor.controlShadow,
                                      getWidth(), 0, new Color(255, 255, 255, 0)));
        g2.fillRect(nStartOfText, 0, getWidth(), imgGhost.getHeight());

        g2.dispose();


        setSelectionPaths(paths);    // Select this path in the tree

        logPaths("DRAGGING", paths);

        // Wrap the path being transferred into a Transferable object
        Transferable transferable = new TransferableTreePaths(paths);

        // Remember the path being dragged (because if it is being moved, we will have to delete it later)
        pathSource = paths;

        // We pass our drag image just in case it IS supported by the platform
        dge.startDrag(null, imgGhost, new Point(5, 5), transferable, this);
    }

    private void logPaths( final String prefix, final TreePath[] pathSource ) {
        if ( log.isLoggable( Level.FINE ) ) {
            StringBuilder logStatement = new StringBuilder();
            logStatement.append(prefix);
            logStatement.append(": ");
            boolean isFirst = true;
            for(TreePath p : pathSource) {
                if(isFirst) {
                    isFirst = false;
                } else {
                    logStatement.append(", ");
                }
                logStatement.append(p.getLastPathComponent());
            }
            log.fine(logStatement.toString());
        }
    }

    @SuppressWarnings({"SimplifiableIfStatement"})
    private boolean canStartDrag(TreePath[] paths) {
        if (paths == null || paths.length == 0) {
            return false;
        } else {
            for(TreePath path : paths) {
                if (isRootPath(path)) // Ignore everything if user trying to drag the root node
                    return false;
            }
        }


        if ( hasWriteAccess() ) {
            // prohibit dragging from assertions that do not allow it (e.g included)
            for(TreePath path : paths) {
                Object treeObject = path.getLastPathComponent();
                if ( treeObject instanceof AssertionTreeNode ) {
                    AssertionTreeNode aTreeNode = (AssertionTreeNode) treeObject;
                    if(!aTreeNode.canDrag()) {
                        return false;
                    }
                }
            }

            return true;
        }

        return false;
    }

    private boolean hasWriteAccess() {
        /*Subject s = Subject.getSubject(AccessController.getContext());
        AssertionTreeNode an = (AssertionTreeNode)getModel().getRoot();
        final ServiceNode serviceNodeCookie = an.getServiceNodeCookie();
        if (serviceNodeCookie == null) {
            throw new IllegalStateException();
        }
        try {
            ObjectPermission op = new ObjectPermission(serviceNodeCookie.getEntity(), ObjectPermission.WRITE);
            return Registry.getDefault().getSecurityProvider().hasPermission(s, op);
        } catch (Exception e) {
            log.log(Level.WARNING, "Error performing permisison check", e);
        }*/

        return writeAccess;
    }

    public void setWriteAccess(boolean writeAccess) {
        this.writeAccess = writeAccess;
    }


    // helpers...
      private TreePath getChildPath(TreePath pathParent, int nChildIndex) {
        TreeModel model = getModel();
        return pathParent.pathByAddingChild(model.getChild(pathParent.getLastPathComponent(), nChildIndex));
    }


    private boolean isRootPath(TreePath path) {
        TreePath rp = new TreePath(getModel().getRoot());
        return rp.equals(path);
        // return isRootVisible() && getRowForPath(path) == 0;
    }

// PolicyDropTargetListener interface object...

    class PolicyDropTargetListener implements DropTargetListener {
        // Fields...
        private TreePath pathLast = null;
        private Rectangle2D raCueLine = new Rectangle2D.Float();
        private Rectangle2D raGhost = new Rectangle2D.Float();
        private Color colorCueLine;
        private Point ptLast = new Point();
        private Timer timerHover;

        // Constructor...
          PolicyDropTargetListener() {
            colorCueLine = new Color(SystemColor.controlShadow.getRed(),
                                     SystemColor.controlShadow.getGreen(),
                                     SystemColor.controlShadow.getBlue(),
                                     64);

            // Set up a hover timer, so that a node will be automatically expanded or collapsed
            // if the user lingers on it for more than a short time
            timerHover = new Timer(1000, new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (isRootPath(pathLast))
                        return;    // Do nothing if we are hovering over the root node
                    if ( isExpanded(pathLast) ) {
                        collapsePath(pathLast);
                    } else {
                        expandPath( pathLast );
                    }
                }
            });
            timerHover.setRepeats(false);    // Set timer to one-shot mode
        }

        // PolicyDropTargetListener interface
          @Override
          public void dragEnter(DropTargetDragEvent e) {
            if (!hasWriteAccess()) {
                log.fine("dragEnter: DropTargetListener does not have write access.");
                e.rejectDrag();
                return;
            }
            if (!isDragAcceptable( e )) {
                log.fine("dragEnter: drag is not acceptable.");
                e.rejectDrag();
            } else {
                log.fine("dragEnter: accepting drag.");
                e.acceptDrag( e.getDropAction() );
            }
        }

        @Override
        public void dragExit(DropTargetEvent e) {
            if (!DragSource.isDragImageSupported()) {
                repaint( raGhost.getBounds() );
            }
        }

        @Override
        public void dragOver(DropTargetDragEvent e) {
            if (!hasWriteAccess()) {
                e.rejectDrag();
                return;
            }

            DataFlavor[] flavors = e.getCurrentDataFlavors();
            for (DataFlavor flavor : flavors) {
                if (TransferableTreePath.TREEPATH_FLAVOR.equals(flavor)) {
                    treePathDragOver( e );
                    break;
                } else if (PolicyTransferable.ASSERTION_DATAFLAVOR.equals(flavor)) {
                    assertionDragOver(e);
                }
            }
        }

        /**
         * The policy tree drag over handler
         * This is where the ghost image is drawn
         */
        private void treePathDragOver( final DropTargetDragEvent e ) {
            Point pt = e.getLocation();
            if (pt.equals(ptLast))
                return;

            // Try to determine whether the user is flicking the cursor right or left
            ptLast = pt;

            TreePath path = getClosestPathForLocation(pt.x, pt.y);
            if ( path == null ) {
                path = new TreePath(getModel().getRoot());
            }

            if (!(path == pathLast)) {
                pathLast = path;
                timerHover.restart();
            }

            // Do this if you want to prohibit dropping onto the drag source
            boolean rejected = false;
            Pair<AssertionTreeNode,Integer> target = null;
            if(pathSource != null) {
                for(TreePath p : pathSource) {
                    if (path.equals(p)) {
                        e.rejectDrag();
                        rejected = true;
                        break;
                    } else if (p.isDescendant(path) && ((TreeNode)p.getLastPathComponent()).getAllowsChildren()) {
                        e.rejectDrag();
                        rejected = true;
                        break;
                    } else if ( target == null ) {
                        final boolean dropAsFirstContainerChild = dropAsFirstContainerChild( pt );
                        final AssertionTreeNode an = (AssertionTreeNode) p.getLastPathComponent();
                        target = getTargetForPath( path, an, dropAsFirstContainerChild, false );
                    }
                }
            } else {
                rejected = true;
            }

            // In any case draw (over the ghost image if necessary) a cue line indicating where a drop will occur
            int renderOffsetMultiplier;
            TreePath targetPath;
            if ( target!=null && target.left != null && target.right > -1 ) {
                if ( target.right == 0 ) {
                    renderOffsetMultiplier = 0;
                    if ( target.left.getChildCount() == 0 ) {
                        renderOffsetMultiplier = 1;
                        targetPath = asTreePath(target.left);
                    } else {
                        targetPath = asTreePath(target.left.getChildAt( 0 ));
                    }
                    if ( targetPath.getParentPath()!=null && !isExpanded( targetPath.getParentPath() ) ) {
                        renderOffsetMultiplier = 1;
                        targetPath = getLastExpandedChildPath( targetPath.getParentPath() );
                    }
                } else {
                    renderOffsetMultiplier = 1;
                    targetPath = getLastExpandedChildPath( asTreePath( target.left.getChildAt( target.right-1 ) ) );
                }
            } else {
                renderOffsetMultiplier = 1;
                targetPath = path;
            }
            renderCueLine( pt, renderOffsetMultiplier, targetPath );

            if(!rejected) {
                boolean accept = true;

                // prohibit dropping onto assertions that do not allow it
                Object treeObject = path.getLastPathComponent();
                try {
                    TreePath[] pathSource = (TreePath[]) e.getTransferable().getTransferData(TransferableTreePath.TREEPATH_FLAVOR);
                    if ( pathSource != null ) {
                        for(TreePath p : pathSource) {
                            Object transferData = p.getLastPathComponent();
                            if ( treeObject instanceof AssertionTreeNode &&
                                 transferData instanceof AbstractTreeNode ) {
                                AssertionTreeNode aTreeNode = (AssertionTreeNode) treeObject;
                                AbstractTreeNode node = (AbstractTreeNode) transferData;

                                accept = acceptNode( aTreeNode, node );
                                if (!accept) break;
                            }
                        }
                    }
                } catch (IOException | UnsupportedFlavorException ex) {
                    log.log(Level.WARNING, "Drag and drop error, '"+ExceptionUtils.getMessage( ex )+"'", ExceptionUtils.getDebugException( ex ));
                }

                if ( accept ) {
                    e.acceptDrag(e.getDropAction());
                } else {
                    e.rejectDrag();                    
                }
            }
        }

        private TreePath getLastExpandedChildPath( final TreePath treePath ) {
            TreePath path = treePath;

            // Move towards child
            while ( path.getLastPathComponent() instanceof TreeNode ) {
                final TreeNode lastPathNode = (TreeNode)path.getLastPathComponent();
                if ( lastPathNode.getChildCount() > 0 && isExpanded( path ) ) {
                    path = path.pathByAddingChild( lastPathNode.getChildAt( lastPathNode.getChildCount()-1 ) );
                } else {
                    break;
                }
            }

            // Move towards root
            while ( path.getLastPathComponent() instanceof TreeNode ) {
                if ( path.getParentPath() != null && !isExpanded( path.getParentPath() ) ) {
                    path = path.getParentPath();
                } else {
                    break;
                }
            }

            return path;
        }

        private void renderCueLine( final Point pt, final int renderOffsetMultiplier, final TreePath targetPath ) {
            Rectangle raPath = getPathBounds(targetPath);
            if ( raPath != null ) { // caller is responsible for ensuring path is visible (expanded)
                Graphics2D g2 = (Graphics2D)getGraphics();

                // If a drag image is not supported by the platform, then draw our own drag image
                if (!DragSource.isDragImageSupported()) {
                    paintImmediately(raGhost.getBounds());    // Rub out the last ghost image and cue line
                    // And remember where we are about to draw the new ghost image
                    raGhost.setRect(pt.x - ptOffset.x, pt.y - ptOffset.y, imgGhost.getWidth(), imgGhost.getHeight());
                    g2.drawImage(imgGhost, AffineTransform.getTranslateInstance( raGhost.getX(), raGhost.getY() ), null);
                } else    // Just rub out the last cue line
                    paintImmediately(raCueLine.getBounds());

                raCueLine.setRect(0, raPath.y + ((int)raPath.getHeight() * renderOffsetMultiplier), getWidth(), 2);

                g2.setColor(colorCueLine);
                g2.fill(raCueLine);

                // And include the cue line in the area to be rubbed out next time
                raGhost = raGhost.createUnion(raCueLine);
            }
        }

        private TreePath asTreePath( final TreeNode node ) {
            final List<TreeNode> nodes = new ArrayList<>();
            TreeNode pathStep = node;
            while( pathStep != null ) {
                nodes.add( pathStep );
                pathStep = pathStep.getParent();
            }
            Collections.reverse( nodes );
            return new TreePath( nodes.toArray() );
        }

        private boolean acceptNode( AssertionTreeNode aTreeNode,
                                    final AbstractTreeNode node ) {
            boolean accept = false;

            while ( aTreeNode != null ) {
                if ( aTreeNode.accept(node ) ) {
                    accept = true;
                    break;
                }
                aTreeNode = (AssertionTreeNode) aTreeNode.getParent();
            }

            return accept;
        }

        /**
         * @param e the drop target event
         */
        private void assertionDragOver(DropTargetDragEvent e) {
            boolean accept = true;
            boolean isLastRow = false;

            Point pt = e.getLocation();
            TreePath path = getClosestPathForLocation(pt.x, pt.y);
            if (path != null) {
                setSelectionPath(path);

                if ( path != pathLast ) {
                    pathLast = path;
                    timerHover.restart();
                }

                // prohibit dropping onto assertions that do not allow it
                Object treeObject = path.getLastPathComponent();
                try {
                    Object transferData = e.getTransferable().getTransferData( PolicyTransferable.ASSERTION_DATAFLAVOR );
                    if ( treeObject instanceof AssertionTreeNode &&
                         transferData instanceof AbstractTreeNode ) {
                        AssertionTreeNode aTreeNode = (AssertionTreeNode) treeObject;
                        AbstractTreeNode node = (AbstractTreeNode) transferData;
                        accept = acceptNode(aTreeNode, node);
                    }
                } catch (IOException | UnsupportedFlavorException ex) {
                    log.log(Level.WARNING, "Drag and drop error, '"+ExceptionUtils.getMessage( ex )+"'", ExceptionUtils.getDebugException( ex ));
                }

                int rowForPath = getRowForPath(path);
                isLastRow = rowForPath==getRowCount()-1;
            }

            if ( accept || isLastRow ) {
                e.acceptDrag( e.getDropAction() );
            } else {
                e.rejectDrag();
            }
        }

        @Override
        public void dropActionChanged(DropTargetDragEvent e) {
            log.fine("dropActionChanged " + Arrays.toString(e.getCurrentDataFlavors()));
            if (!isDragAcceptable(e))
                e.rejectDrag();
            else
                e.acceptDrag(e.getDropAction());
        }

        @Override
        public void drop(DropTargetDropEvent e) {
            pathLast = null;
            try {
                DataFlavor[] flavors = e.getCurrentDataFlavors();
                for (DataFlavor flavor : flavors) {
                    if (TransferableTreePath.TREEPATH_FLAVOR.equals(flavor)) {
                        dropTreePath(e);
                        break;
                    } else if (PolicyTransferable.ASSERTION_DATAFLAVOR.equals(flavor)) {
                        dropAssertion(e);
                    } else {
                        log.fine("drop ignoring flavour: " + flavor);
                    }
                }
            }
            catch(Throwable throwable) {
                ErrorManager.getDefault().notify(Level.WARNING, throwable, ErrorManager.DEFAULT_ERROR_MESSAGE);     
            }
        }

        private void dropAssertion(DropTargetDropEvent e) {
            try {
                final Object transferData = e.getTransferable().getTransferData(PolicyTransferable.ASSERTION_DATAFLAVOR);
                log.fine("DROPPING assertion: " + transferData);
                AbstractTreeNode[] nodes = (AbstractTreeNode[])transferData;

                // Check for JDK bug 6945178  (Bug #8874)
                if (nodes == null) {
                    DialogDisplayer.showMessageDialog(TopComponents.getInstance().getTopParent(),
                            "<html>Unable to accept drag and drop using installed version of the Java plug-in when applet is running in untrusted mode." +
                                    "<p>Please try one of the following: " +
                                    "<ul><li>Upgrade to latest version of Java plug-in; or," +
                                    "<li>Run the applet in trusted mode; or," +
                                    "<li>Use the toolbar buttons instead.</ul>",
                            "Drag and Drop Failed",
                            JOptionPane.WARNING_MESSAGE,
                            null);
                    return;
                }

                Point pt = e.getLocation();
                boolean dropAsFirstContainerChild = dropAsFirstContainerChild( pt );
                TreePath path = getClosestPathForLocation(pt.x, pt.y);
                if ( path == null || dropAsFirstContainerChild ) {
                    path = new TreePath(getModel().getRoot());
                }

                final Pair<AssertionTreeNode,Integer> target = getTargetForPath( path, nodes, dropAsFirstContainerChild );
                if ( target.left != null ) {
                    e.acceptDrop(e.getDropAction());
                    if ( target.left instanceof CompositeAssertionTreeNode ) {
                        CompositeAssertionTreeNode compositeAssertionTreeNode = (CompositeAssertionTreeNode)target.left;
                        for( AbstractTreeNode node : nodes ) {
                            compositeAssertionTreeNode.receive(node, target.right);
                        }
                    } else {
                        for(AbstractTreeNode node : nodes) {
                            target.left.receive(node);
                        }
                    }
                    e.dropComplete(true);
                } else {
                    log.warning(path.getLastPathComponent().getClass().getSimpleName() + " rejected drop");
                    e.rejectDrop();
                }
                return;
            } catch (UnsupportedFlavorException | IOException ex) {
                log.log(Level.WARNING, "error in drop ", ex);
            }
            e.rejectDrop();
        }

        private void dropTreePath(DropTargetDropEvent e) {
            timerHover.stop();    // Prevent hover timer from doing an unwanted expandPath or collapsePath

            if (!isTreePathDropAcceptable(e)) {
                log.fine("rejecting drop tree path");
                e.rejectDrop();
                return;
            }

            e.acceptDrop(e.getDropAction());

            Transferable transferable = e.getTransferable();
            DataFlavor[] flavors = transferable.getTransferDataFlavors();
            boolean dropComplete = true;
            for (DataFlavor flavor : flavors) {
                if (TransferableTreePath.TREEPATH_FLAVOR.equals(flavor)) {
                    try {
                        Point pt = e.getLocation();

                        boolean dropAsFirstContainerChild = dropAsFirstContainerChild( pt );
                        TreePath pathTarget = getClosestPathForLocation(pt.x, pt.y);
                        if ( pathTarget == null || dropAsFirstContainerChild ) {
                            pathTarget = new TreePath(getModel().getRoot());
                        }

                        TreePath[] pathSource = (TreePath[]) transferable.getTransferData(flavor);
                        logPaths( "DROPPING tree path", pathSource );
                        PolicyTreeModel model = (PolicyTreeModel) getModel();

                        // each node is inserted immediately after the drop point
                        // reversing the node array preserves the original order
                        ArrayUtils.reverse(pathSource);

                        for( final TreePath p : pathSource ) {
                            final AssertionTreeNode an = (AssertionTreeNode) p.getLastPathComponent();
                            Assertion a = (Assertion) an.asAssertion().clone();
                            final AssertionTreeNode assertionTreeNodeCopy = AssertionTreeNodeFactory.asTreeNode(a);

                            final Pair<AssertionTreeNode,Integer> target = getTargetForPath(
                                    pathTarget, assertionTreeNodeCopy, dropAsFirstContainerChild, false );

                            if ( target.left != null && target.right > -1 ) {
                                if (e.getDropAction() == DnDConstants.ACTION_MOVE) {
                                    model.moveNodeInto(assertionTreeNodeCopy, target.left, target.right);
                                } else {
                                    model.insertNodeInto(assertionTreeNodeCopy, target.left, target.right);
                                }
                            }
                        }

                        //setSelectionPath(new TreePath(assertionTreeNodeCopy.getPath()));	// Mark this as the selected path in the tree
                        break; // No need to check remaining flavors
                    } catch (UnsupportedFlavorException ufe) {
                        log.log(Level.WARNING, "Internal error", ufe);
                        dropComplete = false;
                    } catch (IOException ioe) {
                        log.log(Level.WARNING, "Internal error", ioe);
                        dropComplete = false;
                    }
                } else {
                    log.fine("drop ignoring flavour: " + flavor);
                }
            }
            repaint(raGhost.getBounds());
            e.dropComplete(dropComplete);
        }

        private boolean dropAsFirstContainerChild( final Point pt ) {
            boolean dropAsFirstContainerChild = false;

            Insets insets = topBorder.getBorderInsets(PolicyTree.this);
            if (insets.top >= pt.y) {
                dropAsFirstContainerChild = true;
            }
            return dropAsFirstContainerChild;
        }

        // Helpers...
          private boolean isDragAcceptable(DropTargetDragEvent e) {
            if (!hasWriteAccess()) return false;

            // Only accept COPY or MOVE gestures (ie LINK is not supported)
            if ((e.getDropAction() & DnDConstants.ACTION_COPY_OR_MOVE) == 0)
                return false;

            Point pt = e.getLocation();
            TreePath path = getClosestPathForLocation(pt.x, pt.y);
            if ( path == null ) {
                path = new TreePath(getModel().getRoot());
            }

            // Only accept this particular flavor
            if (e.isDataFlavorSupported(TransferableTreePath.TREEPATH_FLAVOR)) {
                // prohibit dropping onto the drag source...
                for(TreePath p : pathSource) {
                    if (path.equals(p)) {
                        log.fine("REJECTING DRAG: " + p.getLastPathComponent());
                        return false;
                    }
                }

                return true;
            } else if (e.isDataFlavorSupported(PolicyTransferable.ASSERTION_DATAFLAVOR)) {
                // prohibit dropping onto assertions that do not allow it
                Object treeObject = path.getLastPathComponent();
                try {
                    Object transferData = e.getTransferable().getTransferData( PolicyTransferable.ASSERTION_DATAFLAVOR );
                    if ( treeObject instanceof AssertionTreeNode &&
                         transferData instanceof AbstractTreeNode ) {
                        AssertionTreeNode aTreeNode = (AssertionTreeNode) treeObject;
                        AbstractTreeNode node = (AbstractTreeNode) transferData;
                        return acceptNode( aTreeNode, node );
                    }
                } catch (IOException | UnsupportedFlavorException ex) {
                    log.log(Level.WARNING, "Drag and drop error, '"+ExceptionUtils.getMessage( ex )+"'", ExceptionUtils.getDebugException( ex ));
                }

                return true;
            }
            log.log(Level.INFO, "not supported dataflavor " + Arrays.toString(e.getCurrentDataFlavors()));
            return false;
        }

        private boolean isTreePathDropAcceptable(DropTargetDropEvent e) {
            // Only accept COPY or MOVE gestures (ie LINK is not supported)
            if ((e.getDropAction() & DnDConstants.ACTION_COPY_OR_MOVE) == 0)
                return false;

            // Only accept this particular flavor
            if (!e.isDataFlavorSupported(TransferableTreePath.TREEPATH_FLAVOR)) {
                log.log(Level.INFO, "not supported dataflavor " + Arrays.toString(e.getCurrentDataFlavors()));
                return false;
            }

            // Do this if you want to prohibit dropping onto the drag source...
//            Point pt = e.getLocation();
//            TreePath path = getClosestPathForLocation(pt.x, pt.y);
//            if (path == null) {
//                path = new TreePath(getModel().getRoot());
//            }
//            if (path.equals(pathSource))
//                return false;

            return true;
        }
    }


// Autoscroll Interface...
// The following code was borrowed from the book:
//		Java Swing
//		By Robert Eckstein, Marc Loy & Dave Wood
//		Paperback - 1221 pages 1 Ed edition (September 1998)
//		O'Reilly & Associates; ISBN: 156592455X
//
// The relevant chapter of which can be found at:
//		http://www.oreilly.com/catalog/jswing/chapter/dnd.beta.pdf

    private static final int AUTOSCROLL_MARGIN = 12;

// Ok, we've been told to scroll because the mouse cursor is in our
// scroll zone.

    @Override
    public void autoscroll(Point pt) {
        // Figure out which row we're on.
        int nRow = -1;
        for ( int x=0; x<getWidth(); x+=10 ) {
            // we don't care about the x position, so search the x-axis for the row
            nRow = getRowForLocation( x, pt.y );
            if ( nRow >= 0 ) break;
        }

        // If we are not on a row then ignore this autoscroll request
        if (nRow < 0)
            return;

        Rectangle raOuter = getBounds();
// Now decide if the row is at the top of the screen or at the
// bottom. We do this to make the previous row (or the next
// row) visible as appropriate. If we're at the absolute top or
// bottom, just return the first or last row respectively.

        nRow = (pt.y + raOuter.y <= AUTOSCROLL_MARGIN)            // Is row at top of screen?
               ?
               (nRow <= 0 ? 0 : nRow - 1)                        // Yes, scroll up one row
               :
               (nRow < getRowCount() - 1 ? nRow + 1 : nRow);    // No, scroll down one row

        scrollRowToVisible(nRow);
    }

// Calculate the insets for the *JTREE*, not the viewport
// the tree is in. This makes it a bit messy.

    @Override
    public Insets getAutoscrollInsets() {
        Rectangle raOuter = getBounds();
        Rectangle raInner = getParent().getBounds();
        return new Insets(raInner.y - raOuter.y + AUTOSCROLL_MARGIN, raInner.x - raOuter.x + AUTOSCROLL_MARGIN,
                          raOuter.height - raInner.height - raInner.y + raOuter.y + AUTOSCROLL_MARGIN,
                          raOuter.width - raInner.width - raInner.x + raOuter.x + AUTOSCROLL_MARGIN);
    }

// TreeModelListener interface implemntations

    @Override
    public void treeNodesChanged(TreeModelEvent e) {
        //   log.fine("treeNodesChanged");
        //   sayWhat(e);
// We dont need to reset the selection path, since it has not moved
    }

    /**
     * After an AssertionTreeNode has been inserted the following is performed:
     * <ul>
     * <li>The new node is selected in policy editor panel.</li>
     * <li>The nodes parent has it's assertion's (it's user object) children set to include it's existing assertion
     * children and the assertion from the newly inserted AssertionTreeNode in the correct location, which then
     * causes the children to be reparented correctly. If this is not done then any changes made to the inserted
     * node's assertion will be lost as the assertion in the new tree node will not be referenced from the
     * composite assertion in the new nodes parent. </li>
     * </ul>
     * *
     *
     * @param e tree model event
     */
    @Override
    public void treeNodesInserted(TreeModelEvent e) {
        //sayWhat(e);
        int nChildIndex = e.getChildIndices()[0];
        TreePath pathParent = e.getTreePath();
        final TreePath childPath = getChildPath(pathParent, nChildIndex);
        Runnable doSelect = new Runnable() {
            @Override
            public void run() {
                setSelectionPath(childPath);
                PolicyTree.this.requestFocusInWindow();
            }
        };

        AssertionTreeNode parent =
          (AssertionTreeNode)pathParent.getLastPathComponent();

        final Assertion pass = parent.asAssertion();
        if (!(pass instanceof CompositeAssertion)) return;

        CompositeAssertion ca = (CompositeAssertion) pass;

        java.util.List<Assertion> newChildren = new ArrayList<>();
        Enumeration en = parent.children();
        while (en.hasMoreElements()) {
            newChildren.add(((AssertionTreeNode)en.nextElement()).asAssertion());
        }
        SwingUtilities.invokeLater(doSelect);

        log.finer("set children " + newChildren);
        ca.setChildren(newChildren);
        log.finer("children assertions = " + ca.getChildren().size());
        log.finer("nodes          tree = " + parent.getChildCount());
    }

    @Override
    public void treeNodesRemoved(TreeModelEvent e) {
        final AssertionTreeNode parent = (AssertionTreeNode)e.getTreePath().getLastPathComponent();
        final Assertion pass = parent.asAssertion();
        if (pass instanceof Include) return;

        java.util.List<Assertion> removed = new ArrayList<>();
        Object[] objects = e.getChildren();
        for( Object object : objects ) {
            AssertionTreeNode an = (AssertionTreeNode) object;
            removed.add( an.asAssertion() );
        }
        CompositeAssertion ca = (CompositeAssertion) pass;
        //noinspection unchecked
        java.util.List<Assertion> children = ca.getChildren();
        java.util.List<Assertion> remove = new ArrayList<>();
        for (Assertion a : children) {
            // fla bugfix 2531, this catches all assertions using equals instead of the one assertion targeted
            // if (removed.contains(a)) {
            //   remove.add(a);
            // }
            for (Assertion toRemove : removed) {
                if (toRemove == a) remove.add(a);
            }
        }
        log.finer("removing " + remove);
        // fla bugfix 2531, if you pass one element to removeAll and children has that element more
        // than once, all instances will be removed
        // children.removeAll(remove);

        for (Assertion toRemove : remove) {
            for (int i = 0 ; i < children.size(); i++) {
                Object o = children.get(i);
                if (o == toRemove) {
                    children.remove(i);
                    break;
                }
            }
        }
        ca.treeChanged();
        log.finer("children assertions = " + ca.getChildren().size());
        log.finer("nodes          tree = " + parent.getChildCount());
    }

    @Override
    public void treeStructureChanged(TreeModelEvent e) {
        //log.fine("treeStructureChanged ");
        //sayWhat(e);
    }

    @Override
    public void refresh() {
        if (canRefresh()) {
            new EditPolicyAction(policyEditorPanel.getPolicyNode(), true).invoke();
        }
    }

    @Override
    public boolean canRefresh() {
        return policyEditorPanel != null && policyEditorPanel.getPolicyNode() != null;
    }

    private TreeSelectionModel getTreeSelectionModel() {
        return new DefaultTreeSelectionModel();
    }

    private PolicyTreeTransferHandler policyTreeTransferHandler = new PolicyTreeTransferHandler() {
        @Override
        public boolean importData(JComponent comp, Transferable t) {
            PolicyTree policyTree = comp instanceof PolicyTree ? (PolicyTree)comp : PolicyTree.this;

            String maybePolicyXml = null;
            boolean ignoreRoot = false;
            if (t.isDataFlavorSupported(PolicyTransferable.ASSERTION_DATAFLAVOR)) {
                // Try to get some tree nodes
                try {
                    AbstractTreeNode[] nodes = (AbstractTreeNode[])t.getTransferData(PolicyTransferable.ASSERTION_DATAFLAVOR);
                    final List<AbstractTreeNode> nodeList = Arrays.asList( nodes );

                    // if there's a user selection which is not a folder, each node is inserted immediately after the selection
                    // reversing the node list preserves the original order
                    // insert after a folder selection if its not empty, not expanded or is a policy fragment
                    final TreePath path = getSelectionPath();
                    if (path != null && path.getPathCount()!=1 && path.getLastPathComponent() != null &&
                            ((!((TreeNode) path.getLastPathComponent()).getAllowsChildren()) ||
                                    (!isModelExpanded(path) && !(((TreeNode) path.getLastPathComponent()).getChildCount()==0 ) ||  !((AssertionTreeNode) path.getLastPathComponent()).accept(null) )))
                    {
                        Collections.reverse( nodeList );
                    }

                    for( final AbstractTreeNode node : nodeList ) {
                        Assertion clone = (Assertion)node.asAssertion().clone();
                        if(!policyTree.importAssertion(clone)) {
                            return false;
                        }
                    }
                    return true;
                } catch(Exception e) {
                    log.log(Level.FINE, "Paste rejected: " + ExceptionUtils.getMessage(e), e);
                }
            } else {
                // Try to get an XML String
                try {
                    Object got = null;
                    if (t.isDataFlavorSupported(PolicyTransferable.HEADLESS_GROUP_DATAFLAVOR)) {
                        got = t.getTransferData(PolicyTransferable.HEADLESS_GROUP_DATAFLAVOR);
                        ignoreRoot = true;
                    } else if (t.isDataFlavorSupported(DataFlavor.stringFlavor))
                        got = t.getTransferData(DataFlavor.stringFlavor);

                    if (got instanceof String) {
                        maybePolicyXml = (String)got;
                    }
                } catch (UnsupportedFlavorException e) {
                    log.log(Level.FINE, "Paste rejected: " + ExceptionUtils.getMessage(e), e);
                    return false;
                } catch (IOException e) {
                    log.log(Level.FINE, "Paste rejected: " + ExceptionUtils.getMessage(e), e);
                    return false;
                }
            }

            if (maybePolicyXml == null) {
                log.fine("Paste of unrecognized transferable: " + t.getClass().getName());
                return false;
            }

            try {

                Assertion ass = wspReader.parsePermissively(maybePolicyXml, WspReader.INCLUDE_DISABLED);
                if (ass == null) {
                    log.fine("Paste of null policy; ignoring");
                    return false;
                }

                // Now we have an assertion tree to import into this location in the policy tree.
                if(ignoreRoot) {
                    //noinspection unchecked
                    List<Assertion> list = new ArrayList(((CompositeAssertion)ass).getChildren());

                    // if there's a user selection which is not a folder, each node is inserted immediately after the selection
                    // reversing the node list preserves the original order
                    // insert after a folder selection if its not empty, not expanded or is a policy fragment
                    final TreePath path = getSelectionPath();
                   if (path != null && path.getPathCount()!=1 && path.getLastPathComponent() != null &&
                           ((!((TreeNode) path.getLastPathComponent()).getAllowsChildren()) ||
                                   (!isModelExpanded(path) && !(((TreeNode) path.getLastPathComponent()).getChildCount()==0 ) ||  !((AssertionTreeNode) path.getLastPathComponent()).accept(null) )))

                    {
                        Collections.reverse( list );
                    }

                    for( Assertion assertion : list) {
                        // Clone assertions
                        Assertion child = (Assertion) assertion.clone();
                        if(!policyTree.importAssertion(child)) {
                            return false;
                        }
                    }
                    return true;
                } else {
                    return policyTree.importAssertion((Assertion)ass.clone());
                }

            } catch (IOException e) {
                log.log(Level.FINE, "Paste rejected: " + ExceptionUtils.getMessage(e), e);
                return false;
            } 
        }
    };
}
