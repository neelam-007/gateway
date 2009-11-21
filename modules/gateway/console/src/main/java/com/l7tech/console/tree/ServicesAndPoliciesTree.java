package com.l7tech.console.tree;

import com.l7tech.console.action.*;
import com.l7tech.console.security.SecurityProvider;
import com.l7tech.console.tree.servicesAndPolicies.RootNode;
import com.l7tech.console.tree.servicesAndPolicies.ServicesAndPoliciesTreeTransferHandler;
import com.l7tech.console.tree.servicesAndPolicies.SortComponents;
import com.l7tech.console.tree.servicesAndPolicies.FolderNode;
import com.l7tech.console.util.Refreshable;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.TopComponents;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.gateway.common.security.rbac.AttemptedUpdateAny;
import com.l7tech.gateway.common.service.ServiceHeader;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.gui.util.ClipboardActions;
import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.OrganizationHeader;
import com.l7tech.policy.PolicyHeader;

import javax.swing.*;
import javax.swing.tree.*;
import java.awt.event.*;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Class ServiceTree is the specialized <code>JTree</code> that
 * handles services and policies
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 */
public class ServicesAndPoliciesTree extends JTree implements Refreshable, FocusListener{
    static Logger log = Logger.getLogger(ServicesAndPoliciesTree.class.getName());
    private boolean ignoreCurrentClipboard = false;
    private SortComponents sortComponents;

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
        setTransferHandler(new ServicesAndPoliciesTreeTransferHandler());

        // disable Edit menu actions
        putClientProperty(ClipboardActions.COPY_HINT, "false");
        putClientProperty(ClipboardActions.CUT_HINT, "true");
        putClientProperty(ClipboardActions.PASTE_HINT, "true");

        ClipboardActions.replaceClipboardActionMap(this);
    }

    public void initializeSortComponents(JLabel filterLabel) {
        if (sortComponents == null ) {
            sortComponents = new SortComponents(filterLabel);
        }
    }

    public SortComponents getSortComponents() {
        return sortComponents;
    }

    @Override
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
    @Override
    public boolean canRefresh() {
        return true;
    }

    @Override
    public void focusGained(FocusEvent e) {
    }

    @Override
    public void focusLost(FocusEvent e) {
        //let user see that all cut nodes have been undone
        setAllChildrenUnCut();
        setIgnoreCurrentClipboard(true);
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
            JTree tree = (JTree) e.getSource();
            // Sometimes, multiple items (folders, published services, policies, or aliases) are selected.
            final List<AbstractTreeNode> abstractTreeNodes = ((ServicesAndPoliciesTree) tree).getSmartSelectedNodes();
            if (abstractTreeNodes == null || abstractTreeNodes.isEmpty()) return;

            final boolean hasMultipleSelection = abstractTreeNodes.size() > 1;
            int keyCode = e.getKeyCode();
            if (keyCode == KeyEvent.VK_DELETE) {
                if (hasMultipleSelection) {
                    DialogDisplayer.showConfirmDialog(TopComponents.getInstance().getTopParent(),
                            "Are you sure you want to delete multiple selected targets?",
                            "Multi-deletion Confirmation", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE,
                            new DialogDisplayer.OptionListener() {
                                @Override
                                public void reportResult(int option) {
                                    if (option == JOptionPane.YES_OPTION) {
                                        handleMultipleEntityDelete(abstractTreeNodes);
                                    }
                                }
                            });
                }

            } else if (keyCode == KeyEvent.VK_ENTER && !hasMultipleSelection) {
                AbstractTreeNode node = abstractTreeNodes.get(0);
                if (node instanceof EntityWithPolicyNode)
                    new EditPolicyAction((EntityWithPolicyNode) node).actionPerformed(null);
            }
        }
    }

    private void handleMultipleEntityDelete(final List<AbstractTreeNode> abstractTreeNodes){
        final Set<Long> allServicesInUddi = new HashSet<Long>();
        //find out if any have uddi data
        for(AbstractTreeNode abstractTreeNode: abstractTreeNodes){
            if(abstractTreeNode instanceof ServiceNode){
                final ServiceNode serviceNode = (ServiceNode) abstractTreeNode;
                try {
                    allServicesInUddi.add(serviceNode.getEntity().getOid());
                } catch (FindException e1) {
                    log.log(Level.WARNING, e1.getMessage(), e1);
                    throw new RuntimeException(e1);
                }
            }
        }

        final Collection<ServiceHeader> servicesInUDDI;
        try {
            if(!allServicesInUddi.isEmpty()){
                servicesInUDDI = Registry.getDefault().getUDDIRegistryAdmin().getServicesPublishedToUDDI(allServicesInUddi);
            }else{
                servicesInUDDI = Collections.emptySet();
            }

        } catch (FindException e) {
            log.log(Level.WARNING, e.getMessage(), e);
            throw new RuntimeException(e);
        }

        if (!servicesInUDDI.isEmpty()) {
            final JTextArea textArea = new JTextArea();
            textArea.setEditable(false);
            textArea.setEnabled(true);
            final StringBuilder builder = new StringBuilder();
            builder.append("The following services have published data to UDDI:\n");

            final Set<Long> allOids = new HashSet<Long>();
            for(ServiceHeader header: servicesInUDDI){
                builder.append(header.getDisplayName());
                builder.append("\n");
                allOids.add(header.getOid());
            }
            textArea.setText(builder.toString());
            final JScrollPane jScrollPane = new JScrollPane();
            jScrollPane.setViewportView(textArea);
            jScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

            final ButtonGroup buttonGroup= new ButtonGroup();
            final JRadioButton deleteAll = new JRadioButton("Delete all selected");
            buttonGroup.add(deleteAll);
            final JRadioButton onlyNonUDDI = new JRadioButton("Delete all apart from those published to UDDI");
            buttonGroup.add(onlyNonUDDI);
            onlyNonUDDI.setSelected(true);
            final JPanel radioPanel = new JPanel();
            radioPanel.setLayout(new GridLayout(2, 2));
            radioPanel.add(onlyNonUDDI);
            radioPanel.add(deleteAll);

            final JPanel container = new JPanel();
            container.setLayout(new BorderLayout());
            container.add(jScrollPane, BorderLayout.CENTER);
            container.add(radioPanel, BorderLayout.SOUTH);

            container.setPreferredSize(new Dimension(600, 200));
            DialogDisplayer.showOptionDialog(TopComponents.getInstance().getTopParent(),
                    "Confirm whether all selected services should be deleted or just those which have not published data to UDDI.\n" +
                            "If all services are deleted then data will be orphaned in UDDI.",
                    "Services have published data to UDDI", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE, null,
                    new Object[]{container, "Ok", "Cancel"}, null, new DialogDisplayer.OptionListener() {
                        @Override
                        public void reportResult(int option) {
                            if (option == 1) {//ok
                                if(deleteAll.isSelected()){
                                    deleteMultipleEntities(abstractTreeNodes);
                                }else{
                                    final List<AbstractTreeNode> subset = new ArrayList<AbstractTreeNode>();
                                    for(AbstractTreeNode abstractTreeNode: abstractTreeNodes){
                                        if(abstractTreeNode instanceof ServiceNode){
                                            ServiceNode serviceNode = (ServiceNode) abstractTreeNode;
                                            try {
                                                final PublishedService service = serviceNode.getEntity();
                                                if(allOids.contains(service.getOid())) continue;
                                            } catch (FindException e) {
                                                log.log(Level.WARNING, e.getMessage(), e);
                                                throw new RuntimeException(e);
                                            }
                                        }
                                        subset.add(abstractTreeNode);
                                    }
                                    deleteMultipleEntities(subset);
                                }
                            }
                            //else nothing to do
                        }
                    });
        } else {
            deleteMultipleEntities(abstractTreeNodes);
        }
    }

    private void deleteMultipleEntities(final List<AbstractTreeNode> abstractTreeNodes){
        final boolean confirmationEnabled = abstractTreeNodes.size() <= 1;

        for (AbstractTreeNode node: abstractTreeNodes) {
            if (!node.canDelete()) return;
            if (node instanceof ServiceNodeAlias) {
                new DeleteServiceAliasAction((ServiceNodeAlias)node, confirmationEnabled).actionPerformed(null);
            } else if (node instanceof ServiceNode) {
                new DeleteServiceAction((ServiceNode)node, confirmationEnabled).actionPerformed(null);
            } else if (node instanceof PolicyEntityNodeAlias) {
                new DeletePolicyAliasAction((PolicyEntityNodeAlias)node, confirmationEnabled).actionPerformed(null);
            } else if (node instanceof EntityWithPolicyNode) {
                new DeletePolicyAction((PolicyEntityNode)node, confirmationEnabled).actionPerformed(null);
            } else if (node instanceof FolderNode) {
                FolderNode folderNode = (FolderNode) node;
                new DeleteFolderAction(folderNode.getOid(), node, Registry.getDefault().getFolderAdmin(), confirmationEnabled).actionPerformed(null);
            }
        }
    }

    class TreeMouseListener extends MouseAdapter {
        /**
         * Invoked when the mouse has been clicked on a component.
         */
        @Override
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

        @Override
        public void mousePressed(MouseEvent e) {
            popUpMenuHandler(e);
        }

        @Override
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
            //bug 6263: sets the focus when a mouse is clicked (ie right mouse click) on the service and policy tree
            //calling requestFocus() will set this tree to be focused and the KeyboardFocusManager (in ClipBoardActions.java)
            // will get notified for the property "permanentFocusOwner" change. Then, KeyboardFocusManager will reset if
            // clipboard global actions (such as Copy, Copy All, and Paste) are enabled or disabled.
            requestFocus();

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

                    boolean hasMultipleSelection = ServicesAndPoliciesTree.this.getSmartSelectedNodes().size() > 1;
                    if (hasMultipleSelection) {
                        for (Component component: menu.getComponents()) {
                            if (component instanceof JMenuItem) {
                                Action action = ((JMenuItem)component).getAction();
                                String actionName = (String)action.getValue(Action.NAME);
                                //Precondition: multiple items are selected.
                                if (
                                    // Case 1: if "node" is an EntityWithPolicyNode (published service node, policy node, or alias),
                                    // all assoicated actions except "Copy as Alias", "Refresh", and "Cut" are disabled.
                                    (node instanceof EntityWithPolicyNode && !(action instanceof MarkEntityToAliasAction) && !(action instanceof RefreshTreeNodeAction) && !"Cut".equals(actionName))
                                        ||
                                    // Case 2: if "node" is a folder node, all associated actions except "Cut" are disabled.
                                    (node instanceof FolderNode && !"Cut".equals(actionName))
                                   ) {
                                    action.setEnabled(false);
                                }
                            }
                        }
                    }

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

        @Override
        public String toString() {
            return actionName;
        }
    }

    /**
     * Get the standard global cut or paste action, but only if the current user has permissions to carry out
     * the supplied operationType on the supplied entityType.
     * Currently only supports FOLDER and UPDATE
     * Use this method when you need a secured Action which is not part of the SecureAction hierarchy
     * If a client uses this method in a once off initialization for cut and paste actions there is the chance that
     * the clipboard is not yet ready, in which case null will be returned.
     *
     * @param clipboardActionType Specify whether you want to 'Cut' or 'Paste'. Currently all that is supported
     * @return Action if the current user has the correct permissions, otherwise null
     */
    public static Action getSecuredAction( final ClipboardActionType clipboardActionType ) {
        if (!ClipboardActions.isSystemClipboardAvailable()) return null;
        if (!Registry.getDefault().isAdminContextPresent()) return null;

        if ( !isUserAuthorizedToUpdateFolders() ) return null;

        switch(clipboardActionType) {
            case CUT:
                return ClipboardActions.getGlobalCutAction();
            case PASTE:
                return ClipboardActions.getGlobalPasteAction();
            default:
                throw new IllegalArgumentException();
        }
    }

    /**
     * The user can update folders if they have the permission to update any entity of type folder
     * 
     * @return true if authorized, false otherwise
     */
    public static boolean isUserAuthorizedToUpdateFolders(){
        if (!Registry.getDefault().isAdminContextPresent()) return false;
        
        SecurityProvider securityProvider = Registry.getDefault().getSecurityProvider();
        AttemptedUpdateAny attemptedUpdate = new AttemptedUpdateAny(EntityType.FOLDER);
        return securityProvider.hasPermission(attemptedUpdate);
    }

    public void setIgnoreCurrentClipboard(boolean set){
        ignoreCurrentClipboard = set;
    }

    public boolean getIgnoreCurrentclipboard(){
        return ignoreCurrentClipboard;
    }

    /**
     * Smart method to return only the highest level selected nodes.
     * E.g. if A-B and A-B-C is selected, then only the node A-B will be 
     * in the returned List as A-B-C is a sub element to A-B
     * @return List<AbstractTreeNode> only the nodes which are needed to provide operations on all selected nodes
     */
    public List<AbstractTreeNode> getSmartSelectedNodes(){
        TreePath[] selectedPaths = this.getSelectionPaths();
        List<AbstractTreeNode> selectedNodes;
        if (selectedPaths == null) return new ArrayList<AbstractTreeNode>(0);
        else selectedNodes = new ArrayList<AbstractTreeNode>(selectedPaths.length);

        HashSet<Object> nodesToTransfer = new HashSet<Object>(selectedPaths.length);
        for(TreePath path : selectedPaths) {
            nodesToTransfer.add(path.getLastPathComponent());
        }

        for(TreePath path : selectedPaths) {
            // Skip a node if an ancestor of its is selected
            boolean skip = false;
            for(int i = 0;i < path.getPathCount() - 1;i++) {
                if(nodesToTransfer.contains(path.getPathComponent(i))) {
                    skip = true;
                    break;
                }
            }
            if(skip) {
                continue;
            }

            Object selectedNode = path.getLastPathComponent();
            if(selectedNode instanceof AbstractTreeNode) {
                AbstractTreeNode item = (AbstractTreeNode)path.getLastPathComponent();
                selectedNodes.add(item);
            }else{
                throw new RuntimeException("Node not a AbstractTreeNode: " + selectedNode);
            }
        }

        return selectedNodes;
    }

    public void updateAllAliases(Long entityOid){
        DefaultTreeModel model = (DefaultTreeModel) getModel();
        RootNode rootNode = (RootNode) model.getRoot();
        Set<AbstractTreeNode> aliases = rootNode.getAliasesForEntity(entityOid);
        AbstractTreeNode origEntity = rootNode.getNodeForEntity(entityOid);
        Object origUserObject = origEntity.getUserObject();

        for(AbstractTreeNode atn: aliases){
            Object userObj = atn.getUserObject();
            if(!(userObj instanceof OrganizationHeader)) return;

            OrganizationHeader oH = (OrganizationHeader) userObj;
            long folderOid = oH.getFolderOid();

            OrganizationHeader newHeader;
            if(atn instanceof ServiceNode){
                ServiceHeader origServiceHeader = (ServiceHeader) origUserObject;
                newHeader = new ServiceHeader(origServiceHeader);
            }else if(atn instanceof PolicyEntityNode){
                PolicyHeader origPolicyHeader = (PolicyHeader) origUserObject;
                newHeader = new PolicyHeader(origPolicyHeader);
            }else{
                String msg = "Tree node was not of correct type";
                log.log(Level.INFO, msg);
                throw new RuntimeException(msg);
            }
            newHeader.setFolderOid(folderOid);
            newHeader.setAliasOid(oH.getAliasOid());

            atn.setUserObject(newHeader);
            EntityWithPolicyNode ewpn = (EntityWithPolicyNode) atn;
            try {
                ewpn.updateUserObject();
            } catch (FindException e) {
                log.log(Level.INFO, e.getMessage());
            }
            model.nodeStructureChanged(atn);
        }
    }

    /**
     * Sets the services and policies tree back to the default filtering view, which would be no filter is applied.
     */
    public void filterTreeToDefault() {
        sortComponents.selectDefaultFilter();
    }
}
