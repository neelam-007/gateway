package com.l7tech.console.tree;

import com.l7tech.console.action.*;
import com.l7tech.console.util.Refreshable;
import com.l7tech.console.util.Registry;
import com.l7tech.console.security.SecurityProvider;
import com.l7tech.console.tree.servicesAndPolicies.ServicesAndPoliciesTreeTransferHandler;
import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.util.ClipboardActions;
import com.l7tech.gateway.common.security.rbac.EntityType;
import com.l7tech.gateway.common.security.rbac.OperationType;
import com.l7tech.gateway.common.security.rbac.AttemptedUpdate;
import com.l7tech.objectmodel.folder.Folder;

import javax.swing.*;
import javax.swing.tree.*;
import java.awt.event.*;
import java.util.logging.Logger;
import java.util.EnumSet;

/**
 * Class ServiceTree is the specialized <code>JTree</code> that
 * handles services and policies
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 */
public class ServicesAndPoliciesTree extends JTree implements Refreshable, FocusListener{
    static Logger log = Logger.getLogger(ServicesAndPoliciesTree.class.getName());
    private boolean ignoreCurrentClipboard = false;

    /**
     * component name
     */
    public final static String NAME = "servicesAndPolicies.tree";

    /**
     * Create the new policy tree with the policy model.
     *
     * @param newModel
     */
    public ServicesAndPoliciesTree(DefaultTreeModel newModel) {
        super(newModel);
        initialize();
        addFocusListener(this);
        getSelectionModel().setSelectionMode(TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);
    }

    /**
     * Create empty policy tree
     */
    public ServicesAndPoliciesTree() {
        this(null);
    }

    public void setAllChildrenUnCut(){
        DefaultTreeModel model = (DefaultTreeModel)this.getModel();
        //When the user is logged out, we will get this event but the tree model may be gone
        if(model == null) return;
        Object rootObj = model.getRoot();
        if(rootObj instanceof DefaultMutableTreeNode){
            DefaultMutableTreeNode rootNode = (DefaultMutableTreeNode) rootObj;
            //rootNode has two children
            for(int i = 0; i < rootNode.getChildCount(); i++){
                TreeNode childNode = rootNode.getChildAt(i);
                if(childNode instanceof AbstractTreeNode){
                    AbstractTreeNode aTreeNode = (AbstractTreeNode) childNode;
                    aTreeNode.setCut(false);
                    aTreeNode.setChildrenCut(false);
                }
            }
            model.nodeChanged(rootNode);
        }
    }
    /**
     * initialize
     */
    private void initialize() {
        addKeyListener(new TreeKeyListener());
        addMouseListener(new TreeMouseListener());
        setCellRenderer(new EntityTreeCellRenderer());

        setDragEnabled(true);
        setDropMode(DropMode.ON);
        setTransferHandler(new ServicesAndPoliciesTreeTransferHandler());

        // disable Edit menu actions
        putClientProperty(ClipboardActions.COPY_HINT, "false");
        putClientProperty(ClipboardActions.CUT_HINT, "true");
        putClientProperty(ClipboardActions.PASTE_HINT, "true");

        ClipboardActions.replaceClipboardActionMap(this);
    }

    public void refresh() {
        //refresh(null);
        refresh((AbstractTreeNode)this.getModel().getRoot());
    }

    public void refresh(AbstractTreeNode n) {
        TreePath path = getSelectionPath();
        if (n == null && path != null) {
            n = (AbstractTreeNode)path.getLastPathComponent();
        }
        if (n != null) {
            final Action[] actions = n.getActions(RefreshAction.class);
            if (actions.length == 0) {
                log.finer("No refresh action found");
            } else {
                ((NodeAction)actions[0]).setTree(this);
                ActionManager.getInstance().invokeAction(actions[0]);
            }
            if (path != null) setSelectionPath(path);
        }
    }

    /**
     * This tree can always be refreshed
     * <p/>
     * May consider introducing the logic arround disabling this while the tree is
     * refreshing if needed
     *
     * @return always true
     */
    public boolean canRefresh() {
        return true;
    }

    public void focusGained(FocusEvent e) {
    }

    public void focusLost(FocusEvent e) {
        //let user see that all cut nodes have been undone
        setAllChildrenUnCut();
        //Set no nodes as being selected
        this.clearSelection();
        setIgnoreCurrentClipboard(true);
    }

    /**
     * KeyAdapter for the policy trees
     */
    class TreeKeyListener extends KeyAdapter {

        /**
         * Invoked when a key has been pressed.
         */
        public void keyPressed(KeyEvent e) {
            JTree tree = (JTree)e.getSource();
            TreePath path = tree.getSelectionPath();
            if (path == null) return;
            AbstractTreeNode node =
              (AbstractTreeNode)path.getLastPathComponent();
            if (node == null) return;
            int keyCode = e.getKeyCode();
            if (keyCode == KeyEvent.VK_DELETE) {
                if (!node.canDelete()) return;
                if (node instanceof ServiceNode) {
                    new DeleteServiceAction((ServiceNode)node).actionPerformed(null);
                } else if (node instanceof EntityWithPolicyNode) {
                    new DeletePolicyAction((PolicyEntityNode)node).actionPerformed(null);
                }
            } else if (keyCode == KeyEvent.VK_ENTER) {
                if (node instanceof EntityWithPolicyNode)
                    new EditPolicyAction((EntityWithPolicyNode)node).actionPerformed(null);
            }
        }
    }

    class TreeMouseListener extends MouseAdapter {
        /**
         * Invoked when the mouse has been clicked on a component.
         */
        public void mouseClicked(MouseEvent e) {
            if (e.getClickCount() != 2) return;
            JTree tree = (JTree)e.getSource();
            TreePath path = tree.getSelectionPath();
            if (path == null) return;
            AbstractTreeNode node =
              (AbstractTreeNode)path.getLastPathComponent();
            if (node == null) return;
            if (node instanceof EntityWithPolicyNode)
                new EditPolicyAction((EntityWithPolicyNode)node).actionPerformed(null);
        }

        public void mousePressed(MouseEvent e) {
            popUpMenuHandler(e);
        }

        public void mouseReleased(MouseEvent e) {
            popUpMenuHandler(e);
        }

        /**
         * Handle the mouse click popup when the Tree item is right clicked. The context sensitive
         * menu is displayed if the right click was over an item.
         *
         * @param mouseEvent
         */
        private void popUpMenuHandler(MouseEvent mouseEvent) {
            JTree tree = (JTree)mouseEvent.getSource();

            if (mouseEvent.isPopupTrigger()) {
                int closestRow = tree.getClosestRowForLocation(mouseEvent.getX(), mouseEvent.getY());

                if (closestRow != -1
                  && tree.getRowBounds(closestRow).contains(mouseEvent.getX(), mouseEvent.getY())) {
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
                    AbstractTreeNode node = (AbstractTreeNode)tree.getLastSelectedPathComponent();

                    JPopupMenu menu = node.getPopupMenu(ServicesAndPoliciesTree.this);
                    if (menu != null) {
                        Utilities.removeToolTipsFromMenuItems(menu);
                        menu.setFocusable(false);
                        menu.show(mouseEvent.getComponent(), mouseEvent.getX(), mouseEvent.getY());
                    }
                }
            }
        }
    }

    public static enum ClipboardActionType{
        CUT("Cut"),
        PASTE("Paste"),
        ;
        private final String actionName;

        public static final EnumSet<ClipboardActionType> ALL_ACTIONS = EnumSet.of(CUT, PASTE);

        private ClipboardActionType(String name) {
            this.actionName = name;
        }

        public String getName() {
            return actionName;
        }

        public String toString() {
            return actionName;
        }
    }

    /**
     * Get the standard global cut action, but only if the current user has permissions to carry out
     * the supplied operationType on the supplied entityType.
     * Currently only supports POLICY_FOLDER and UPDATE
     * Use this method when you need a secured Action which is not part of the SecureAction hierarchy
     * If a client uses this method in a once off initialization for cut and paste actions there is the chance that
     * the clipboard is not yet ready, in which case null will be returned.
     *
     * @param entityType The EntityType the user must have a permission for
     * @param operationType The OperationType the user must have on the supplied EntityType
     * @param clipboardActionType Specify whether you want to 'Cut' or 'Paste'. Currently all that is supported
     * @return Action if the current user has the correct permissions, otherwise null
     */
    public static Action getSecuredAction(EntityType entityType,
                                          OperationType operationType,
                                          ClipboardActionType clipboardActionType
    ) {

        if(entityType.equals(EntityType.FOLDER) && operationType.equals(OperationType.UPDATE)){
            if(!ClipboardActionType.ALL_ACTIONS.contains(clipboardActionType)) return null;

            if(ClipboardActions.isSystemClipboardAvailable()) {
                //use an AttemptedUpdate, which represents an Update attempty on a Policy_Folder to determine
                //whether cut should be available for this node
                final Folder folder = new Folder("TestFolder", null);
                AttemptedUpdate attemptedUpdate = new AttemptedUpdate(EntityType.FOLDER, folder);
                if (Registry.getDefault().isAdminContextPresent()){
                    SecurityProvider securityProvider = Registry.getDefault().getSecurityProvider();
                    if(securityProvider.hasPermission(attemptedUpdate)){
                        if(clipboardActionType.equals(ClipboardActionType.CUT)){
                            return ClipboardActions.getGlobalCutAction();
                        }else if(clipboardActionType.equals(ClipboardActionType.PASTE)){
                            return ClipboardActions.getGlobalPasteAction();
                        }
                    }
                }
            }
        }
        return null;
    }

    public void setIgnoreCurrentClipboard(boolean set){
        ignoreCurrentClipboard = set;
    }

    public boolean getIgnoreCurrentclipboard(){
        return ignoreCurrentClipboard;
    }
}
