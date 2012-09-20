package com.l7tech.console.tree;

import com.l7tech.console.action.*;
import com.l7tech.console.security.SecurityProvider;
import com.l7tech.console.tree.servicesAndPolicies.*;
import com.l7tech.console.util.Refreshable;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.TopComponents;
import com.l7tech.gateway.common.security.rbac.AttemptedCreate;
import com.l7tech.gateway.common.security.rbac.AttemptedOperation;
import com.l7tech.gateway.common.security.rbac.AttemptedUpdateAny;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.gateway.common.service.ServiceHeader;
import com.l7tech.gui.util.ClipboardActions;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.Utilities;
import com.l7tech.objectmodel.*;
import com.l7tech.policy.Policy;
import com.l7tech.policy.PolicyHeader;
import com.l7tech.util.ArrayUtils;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Functions.Binary;
import com.l7tech.util.Functions.Unary;
import com.l7tech.util.Option;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.FlavorEvent;
import java.awt.datatransfer.FlavorListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.*;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.l7tech.util.Functions.reduce;
import static com.l7tech.util.Option.optional;

/**
 * Class ServiceTree is the specialized <code>JTree</code> that
 * handles services and policies
 *
 * @author Emil Marceta
 */
public class ServicesAndPoliciesTree extends JTree implements Refreshable{
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
        getSelectionModel().setSelectionMode(TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);
        getSelectionModel().addTreeSelectionListener(ClipboardActions.getTreeUpdateListener());
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
        final Clipboard clipboard = ClipboardActions.getClipboard();
        if ( clipboard != null ) {
            clipboard.addFlavorListener(new FlavorListener() {
                @Override
                public void flavorsChanged(FlavorEvent e) {
                    if(e.getSource() instanceof Clipboard){
                        Clipboard clip = (Clipboard)e.getSource();
                        DataFlavor[] flavours;
                        try {
                            flavours = ClipboardActions.getFlavors(clip);
                        } catch (IllegalStateException ise) {
                            // Clipboard busy, give up for now
                            return;
                        }
                        if(flavours == null || !ArrayUtils.contains(flavours, FolderAndNodeTransferable.ALLOWED_DATA_FLAVOR)){
                            setAllChildrenUnCut();
                            setIgnoreCurrentClipboard(true);
                        }
                    }
                }
            });
        }

        setDragEnabled(true);
        setTransferHandler(new ServicesAndPoliciesTreeTransferHandler());

        // disable Edit menu actions
        putClientProperty(ClipboardActions.COPY_HINT, "true");
        putClientProperty(ClipboardActions.CUT_HINT, "true");

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
     * May consider introducing the logic around disabling this while the tree is
     * refreshing if needed
     *
     * @return always true
     */
    @Override
    public boolean canRefresh() {
        return true;
    }

    /**
     * Smartly deletes all selected entities in dependency order.
     *
     * @see #getSmartSelectedNodes
     * @see #deleteMultipleEntities
     */
    public void deleteSelectedEntities() {
        final List<AbstractTreeNode> entities = getSmartSelectedNodes();
        deleteMultipleEntities(entities, false);
    }

    /**
     * Attempts to delete the given nodes in dependency order to maximize the chance of successful deletes.
     *
     * Dependency order:<br />
     * 1. Service aliases<br />
     * 2. Services<br />
     * 3. Policy aliases<br />
     * 4. Policies that contains an include<br />
     * 5. Policies that do not contain an include<br />
     * 6. Folders
     *
     * @param nodes the nodes to delete
     * @param detectConfirmation If true, whether confirmation dialogs be should be displayed will be auto-detected.
     * If false, will not display any confirmation dialogs (other than any UDDI dialogs).
     */
    public void deleteMultipleEntities(final List<AbstractTreeNode> nodes, final boolean detectConfirmation) {
        if (nodes != null && !nodes.isEmpty()) {
            boolean confirmationEnabled = false;
            if(detectConfirmation){
                if(nodes.size() == 1 && nodes.get(0) instanceof FolderNode){
                    // it's a single folder
                    DeleteFolderAction.confirmFolderDeletion(nodes.get(0).getName(), new DialogDisplayer.OptionListener() {
                        @Override
                        public void reportResult(int option) {
                            if(option == JOptionPane.YES_OPTION){
                                sortAndDelete(nodes, false);
                            }
                        }
                    });
                    return;
                }else{
                    confirmationEnabled = nodes.size() == 1 ? true : false;
                }
            }
            sortAndDelete(nodes, confirmationEnabled);
        }
    }

    /**
     * If any services are detected to be published to the UDDI, a dialog box is displayed asking the user to confirm
     * whether these services should be deleted. If not, the services published to the UDDI will be removed from the
     * given list of service nodes.
     */
    private void sortAndDelete(final List<AbstractTreeNode> nodes, boolean confirmationEnabled) {
        final List<ServiceNode> serviceNodes = new ArrayList<ServiceNode>();
        final List<ServiceNodeAlias> serviceAliasNodes = new ArrayList<ServiceNodeAlias>();
        final List<PolicyEntityNode> policyNodes = new ArrayList<PolicyEntityNode>();
        final List<PolicyEntityNode> policyNodesWithInclude = new ArrayList<PolicyEntityNode>();
        final List<PolicyEntityNodeAlias> policyAliasNodes = new ArrayList<PolicyEntityNodeAlias>();
        final List<FolderNode> folderNodes = new ArrayList<FolderNode>();
        sortNodes(nodes, serviceNodes, serviceAliasNodes, policyNodes, policyNodesWithInclude,
                policyAliasNodes, folderNodes);

        final int totalNodes = serviceNodes.size() + serviceAliasNodes.size() + policyNodes.size() +
                policyNodesWithInclude.size() + policyAliasNodes.size() + folderNodes.size();

        final List<ServiceHeader> servicesInUDDI = new ArrayList<ServiceHeader>();
        if (totalNodes > 1 && !serviceNodes.isEmpty()){
            //find out if any have uddi data
            final Set<Long> serviceIds = new HashSet<Long>();
            for(final ServiceNode serviceNode: serviceNodes){
                try {
                    serviceIds.add(serviceNode.getEntity().getOid());
                } catch (FindException e1) {
                    log.log(Level.WARNING, e1.getMessage(), e1);
                    throw new RuntimeException(e1);
                }
            }

            try {
                if(!serviceIds.isEmpty()){
                    servicesInUDDI.addAll(Registry.getDefault().getUDDIRegistryAdmin().getServicesPublishedToUDDI(serviceIds));
                }
            } catch (FindException e) {
                log.log(Level.WARNING, e.getMessage(), e);
                throw new RuntimeException(e);
            }
        } else {
            // only 1 node is selected
            // if it is a service, UDDI check will be performed by DeleteServiceAction
        }

        if (!servicesInUDDI.isEmpty()) {
            final UDDIConfirmationPanel uddiPanel = new UDDIConfirmationPanel(servicesInUDDI);
            DialogDisplayer.showOptionDialog(TopComponents.getInstance().getTopParent(),
                    "Confirm whether all selected services should be deleted or just those which have not published data to UDDI.\n" +
                            "If all services are deleted then data will be orphaned in UDDI.",
                    "Services have published data to UDDI", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE, null,
                    new Object[]{uddiPanel, "Ok", "Cancel"}, null,
                    new UDDIOptionListener(confirmationEnabled, uddiPanel, serviceNodes, servicesInUDDI,
                            serviceAliasNodes, policyNodes, policyNodesWithInclude, policyAliasNodes, folderNodes));
        }else{
            doDependencyOrderDelete(confirmationEnabled, serviceNodes, serviceAliasNodes, policyNodes, policyNodesWithInclude, policyAliasNodes, folderNodes);
        }
    }

    private void doDependencyOrderDelete(final boolean confirmationEnabled,
                                         final List<ServiceNode> serviceNodes,
                                         final List<ServiceNodeAlias> serviceAliasNodes,
                                         final List<PolicyEntityNode> policyNodes,
                                         final List<PolicyEntityNode> policyNodesWithInclude,
                                         final List<PolicyEntityNodeAlias> policyAliasNodes,
                                         final List<FolderNode> folderNodes) {
        for (final ServiceNodeAlias serviceAliasNode : serviceAliasNodes) {
            new DeleteServiceAliasAction(serviceAliasNode, confirmationEnabled).actionPerformed(null);
        }
        for (final ServiceNode serviceNode : serviceNodes) {
            new DeleteServiceAction(serviceNode, confirmationEnabled).actionPerformed(null);
        }
        for (final PolicyEntityNodeAlias policyAliasNode : policyAliasNodes) {
            new DeletePolicyAliasAction(policyAliasNode, confirmationEnabled).actionPerformed(null);
        }
        for (final PolicyEntityNode policyNodeWithInclude : policyNodesWithInclude) {
            new DeletePolicyAction(policyNodeWithInclude, confirmationEnabled).actionPerformed(null);
        }
        for (final PolicyEntityNode policyNode : policyNodes) {
            new DeletePolicyAction(policyNode, confirmationEnabled).actionPerformed(null);
        }
        for (final FolderNode folderNode : folderNodes) {
            deleteFolderNode(folderNode);
        }
    }

    private void deleteFolderNode(final FolderNode folderNode) {
        if(folderNode.getChildCount() == 0){
            try {
                Registry.getDefault().getFolderAdmin().deleteFolder(folderNode.getOid());

                JTree tree = (JTree)TopComponents.getInstance().getComponent(ServicesAndPoliciesTree.NAME);
                if (tree != null) {
                    DefaultTreeModel model = (DefaultTreeModel)tree.getModel();
                    model.removeNodeFromParent(folderNode);
                }
            } catch(ObjectModelException e) {
                JOptionPane.showMessageDialog(TopComponents.getInstance().getTopParent(), "Error deleting folder:\n" + ExceptionUtils.getMessage(e), "Delete Error", JOptionPane.ERROR_MESSAGE );
            }
        }else{
            JOptionPane.showMessageDialog(TopComponents.getInstance().getTopParent(), "Could not delete folder '" +
                    folderNode.getName() + "' because some of its contents are still in use.", "Delete Error", JOptionPane.ERROR_MESSAGE);
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
            JTree tree = (JTree) e.getSource();

            // Sometimes, multiple items (folders, published services, policies, or aliases) are selected.
            final List<AbstractTreeNode> abstractTreeNodes = ((ServicesAndPoliciesTree) tree).getSmartSelectedNodes();
            if (abstractTreeNodes == null || abstractTreeNodes.isEmpty()) return;

            final boolean hasMultipleSelection = tree.getSelectionCount() > 1;

            final boolean rootSelected = abstractTreeNodes.contains((RootNode)getModel().getRoot());
            if (!rootSelected) {
                if (KeyEvent.VK_DELETE == e.getKeyCode()) {
                    if (hasMultipleSelection) {
                        new DeleteTargetsAction().actionPerformed(null);
                    } else {
                        AbstractTreeNode node = abstractTreeNodes.get(0);

                        // if we have only selected a single FolderNode and its deletion is not authorized, do nothing
                        if (node instanceof FolderNode) {
                            DeleteFolderAction a =
                                    new DeleteFolderAction((FolderNode) node,
                                            Registry.getDefault().getFolderAdmin(), false);

                            if(!a.isAuthorized()) {
                                return;
                            }
                        }

                        deleteMultipleEntities(abstractTreeNodes, true);
                    }
                } else if (KeyEvent.VK_ENTER == e.getKeyCode() && !hasMultipleSelection) {
                    AbstractTreeNode node = abstractTreeNodes.get(0);
                    if (node instanceof EntityWithPolicyNode)
                        new EditPolicyAction((EntityWithPolicyNode) node).actionPerformed(null);
                }
            } else {
                //can't delete or edit root node
            }
        }
    }

    /**
     * Option listener for Services published to UDDI deletion confirmation dialog.
     */
    private class UDDIOptionListener implements DialogDisplayer.OptionListener {
        private final boolean confirmationEnabled;
        private final UDDIConfirmationPanel uddiPanel;
        private final List<ServiceNode> serviceNodes;
        private final List<ServiceHeader> servicesInUDDI;
        private final List<ServiceNodeAlias> serviceAliasNodes;
        private final List<PolicyEntityNode> policyNodes;
        private final List<PolicyEntityNode> policyNodesWithInclude;
        private final List<PolicyEntityNodeAlias> policyAliasNodes;
        private final List<FolderNode> folderNodes;
        public UDDIOptionListener(final boolean confirmationEnabled,
                                  final UDDIConfirmationPanel uddiPanel,
                                  final List<ServiceNode> serviceNodes,
                                  final List<ServiceHeader> servicesInUDDI,
                                  final List<ServiceNodeAlias> serviceAliasNodes,
                                  final List<PolicyEntityNode> policyNodes,
                                  final List<PolicyEntityNode> policyNodesWithInclude,
                                  final List<PolicyEntityNodeAlias> policyAliasNodes,
                                  final List<FolderNode> folderNodes){
            this.confirmationEnabled = confirmationEnabled;
            this.uddiPanel = uddiPanel;
            this.serviceNodes = serviceNodes;
            this.servicesInUDDI = servicesInUDDI;
            this.serviceAliasNodes = serviceAliasNodes;
            this.policyNodes = policyNodes;
            this.policyNodesWithInclude = policyNodesWithInclude;
            this.policyAliasNodes = policyAliasNodes;
            this.folderNodes = folderNodes;
        }
        @Override
        public void reportResult(int option) {
            if (option == 1) {//ok
                if(uddiPanel.getDeleteAll().isSelected()){
                    // no need to filter any
                }else{
                    final Set<Long> serviceIdsInUDDI= new HashSet<Long>();
                    for(ServiceHeader header: servicesInUDDI){
                        serviceIdsInUDDI.add(header.getOid());
                    }
                    final List<ServiceNode> toRemove = new ArrayList<ServiceNode>();
                    for(final ServiceNode serviceNode: serviceNodes){
                        try {
                            final PublishedService service = serviceNode.getEntity();
                            if(serviceIdsInUDDI.contains(service.getOid())){
                                toRemove.add(serviceNode);
                            }
                        } catch (FindException e) {
                            log.log(Level.WARNING, e.getMessage(), e);
                            throw new RuntimeException(e);
                        }
                    }
                    serviceNodes.removeAll(toRemove);
                }
                doDependencyOrderDelete(confirmationEnabled, serviceNodes, serviceAliasNodes,
                    policyNodes, policyNodesWithInclude, policyAliasNodes, folderNodes);
            }else{
                // cancelled
            }
        }
    }

    /**
     * Displays a list of services published to UDDI and asks the user to select whether to delete or keep them.
     */
    private class UDDIConfirmationPanel extends JPanel{
        private final JRadioButton deleteAll;
        public UDDIConfirmationPanel(final Collection<ServiceHeader> servicesInUDDI){
            final JTextArea textArea = new JTextArea();
            textArea.setEditable(false);
            textArea.setEnabled(true);
            final StringBuilder builder = new StringBuilder();
            builder.append("The following services have published data to UDDI:\n");

            for(ServiceHeader header: servicesInUDDI){
                builder.append(header.getDisplayName());
                builder.append("\n");
            }
            textArea.setText(builder.toString());
            textArea.setCaretPosition(0);
            final JScrollPane jScrollPane = new JScrollPane();
            jScrollPane.setViewportView(textArea);
            jScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

            final ButtonGroup buttonGroup= new ButtonGroup();
            deleteAll = new JRadioButton("Delete all selected");
            buttonGroup.add(deleteAll);
            final JRadioButton onlyNonUDDI = new JRadioButton("Delete all apart from those published to UDDI");
            buttonGroup.add(onlyNonUDDI);
            onlyNonUDDI.setSelected(true);
            final JPanel radioPanel = new JPanel();
            radioPanel.setLayout(new GridLayout(2, 2));
            radioPanel.add(onlyNonUDDI);
            radioPanel.add(deleteAll);

            setLayout(new BorderLayout());
            add(jScrollPane, BorderLayout.CENTER);
            add(radioPanel, BorderLayout.SOUTH);

            setPreferredSize(new Dimension(600, 200));
        }

        public JRadioButton getDeleteAll(){
            return deleteAll;
        }
    }

    private void sortNodes(final List<AbstractTreeNode> nodes, final List<ServiceNode> serviceNodes,
                                  final List<ServiceNodeAlias> serviceAliasNodes,
                                  final List<PolicyEntityNode> policyNodes,
                                  final List<PolicyEntityNode> policyNodesWithInclude,
                                  final List<PolicyEntityNodeAlias> policyAliasNodes,
                                  final List<FolderNode> folderNodes) {
        for (final AbstractTreeNode node : nodes) {
            if (node.canDelete()) {
                if (node instanceof ServiceNodeAlias) {
                    serviceAliasNodes.add((ServiceNodeAlias) node);
                } else if (node instanceof ServiceNode) {
                    serviceNodes.add((ServiceNode) node);
                } else if (node instanceof PolicyEntityNodeAlias) {
                    policyAliasNodes.add((PolicyEntityNodeAlias) node);
                } else if (node instanceof PolicyEntityNode) {
                    final PolicyEntityNode policyNode = (PolicyEntityNode) node;
                    Policy policy = null;
                    try {
                        policy = policyNode.getPolicy();
                    } catch (final FindException e) {
                        log.warning("Unable to determine if policy contains an Include assertion.");
                    }
                    if (policy != null && policy.getXml().contains("L7p:Include")) {
                        policyNodesWithInclude.add(policyNode);
                    } else {
                        policyNodes.add(policyNode);
                    }
                } else if (node instanceof FolderNode) {
                    final FolderNode folderNode = (FolderNode) node;
                    sortNodes(folderNode.getChildNodes(), serviceNodes, serviceAliasNodes, policyNodes, policyNodesWithInclude,
                            policyAliasNodes, folderNodes);
                    // important to add the folder node to the list AFTER the children have been processed
                    // so that sub folders are ordered before their parent
                    folderNodes.add(folderNode);
                }
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

                    boolean hasMultipleSelection = tree.getSelectionCount() > 1;
                    AbstractTreeNode node = (AbstractTreeNode)tree.getLastSelectedPathComponent();

                    JPopupMenu menu = hasMultipleSelection
                            ? createMultiSelectPopupMenu(node, tree)
                            : node.getPopupMenu(ServicesAndPoliciesTree.this);

                    if (menu != null) {
                        Utilities.removeToolTipsFromMenuItems(menu);
                        menu.setFocusable(false);
                        menu.show(mouseEvent.getComponent(), mouseEvent.getX(), mouseEvent.getY());
                    }
                }
            }
        }

        private JPopupMenu createMultiSelectPopupMenu(AbstractTreeNode node, JTree tree) {
            JPopupMenu pm = new JPopupMenu();

            boolean copyAsAliasAllowed = true;

            for (AbstractTreeNode abstractTreeNode : getSmartSelectedNodes()) {
                if (!(abstractTreeNode instanceof EntityHeaderNode)) {
                    copyAsAliasAllowed = false;
                    break;
                }
            }

            if(copyAsAliasAllowed) {
                MarkEntityToAliasAction markEntityToAliasAction = new MarkEntityToAliasAction((EntityHeaderNode) node);

                if(markEntityToAliasAction.isAuthorized()) {
                    pm.add(markEntityToAliasAction);
                }
            }

            DeleteTargetsAction deleteTargetsAction = new DeleteTargetsAction();

            if(deleteTargetsAction.isAuthorized()) {
                pm.add(deleteTargetsAction);
            }

            RefreshTreeNodeAction refreshTreeNodeAction = new RefreshTreeNodeAction(node);

            if(refreshTreeNodeAction.isAuthorized()) {
                refreshTreeNodeAction.setTree(tree);
                pm.add(refreshTreeNodeAction);
            }

            Action secureCut = getSecuredAction(ClipboardActionType.CUT);

            if(secureCut != null) {
                pm.add(secureCut);
            }

            return pm;
        }
    }

    public static enum ClipboardActionType{
        CUT("Cut"),
        PASTE("Paste"),
        COPY("Copy")
        ;
        private final String actionName;

        public static final EnumSet<ClipboardActionType> ALL_ACTIONS = EnumSet.of(CUT, PASTE, COPY);

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
     * @param clipboardActionType Specify whether you want to 'Copy', 'Cut', or 'Paste'.
     * @return Action if the current user has the correct permissions, otherwise null
     */
    public static Action getSecuredAction( @NotNull final ClipboardActionType clipboardActionType ) {
        return getSecuredAction( clipboardActionType, EntityType.FOLDER );
    }

    /**
     * Get the standard global cut or paste action, but only if the current user has permissions to carry out
     * the supplied operationType on the supplied entityType.
     * Currently only supports FOLDER and UPDATE
     * Use this method when you need a secured Action which is not part of the SecureAction hierarchy
     * If a client uses this method in a once off initialization for cut and paste actions there is the chance that
     * the clipboard is not yet ready, in which case null will be returned.
     *
     * <p>The entity type for a cut or paste must be FOLDER, since these actions only affect folders</p>
     *
     * @param clipboardActionType Specify whether you want to 'Copy', 'Cut', or 'Paste'.
     * @param entityType The applicable entity type.
     * @return Action if the current user has the correct permissions, otherwise null
     */
    public static Action getSecuredAction( @NotNull final ClipboardActionType clipboardActionType,
                                           @NotNull final EntityType entityType ) {
        assert clipboardActionType==ClipboardActionType.COPY || entityType==EntityType.FOLDER : "Entity type must Folder for Cut/Paste";
        if (!ClipboardActions.isSystemClipboardAvailable()) return null;
        if (!Registry.getDefault().isAdminContextPresent()) return null;

        switch(clipboardActionType) {
            case CUT:
                if (!isUserAuthorizedForEntityClipboardUse( EntityType.FOLDER )) return null;
                return ClipboardActions.getGlobalCutAction();
            case PASTE:
                if ( !isUserAuthorizedForEntityClipboardUse( EntityType.FOLDER ) &&
                     !isUserAuthorizedForEntityClipboardUse( EntityType.SERVICE ) &&
                     !isUserAuthorizedForEntityClipboardUse( EntityType.POLICY ) ) return null;
                return ClipboardActions.getGlobalPasteAction();
            case COPY:
                if (!isUserAuthorizedForEntityClipboardUse( entityType )) return null;
                return ClipboardActions.getGlobalCopyAction();
            default:
                throw new IllegalArgumentException();
        }
    }

    private static boolean isUserAuthorizedForEntityClipboardUse( final EntityType entityType ) {
        return isUserAuthorizedForEntityClipboardUse( entityType, false );
    }

    private static boolean isUserAuthorizedForEntityClipboardUse( final EntityType entityType,
                                                                  final boolean isMove ) {
        final Registry registry = Registry.getDefault();
        if ( registry==null || !registry.isAdminContextPresent()) return false;

        final AttemptedOperation operation;
        switch( entityType ) {
            case FOLDER:
                operation = new AttemptedUpdateAny(entityType);
                break;
            default:
                operation = isMove ?
                        new AttemptedUpdateAny(entityType) :
                        new AttemptedCreate(entityType);
                break;
        }

        final SecurityProvider securityProvider = registry.getSecurityProvider();
        return securityProvider.hasPermission( operation );
    }

    public void setIgnoreCurrentClipboard(boolean set){
        ignoreCurrentClipboard = set;
    }

    public boolean getIgnoreCurrentclipboard(){
        return ignoreCurrentClipboard;
    }

    /**
     * Get smart selected nodes if permitted.
     *
     * @see #getSmartSelectedNodes
     */
    public List<AbstractTreeNode> getSmartSelectedNodesForClipboard() {
        final List<AbstractTreeNode> nodes = getSmartSelectedNodes();

        final Unary<Boolean,EntityHeader> cutOrCopyPermission = new Unary<Boolean,EntityHeader>(){
            @Override
            public Boolean call( final EntityHeader entityHeader ) {
                return entityHeader.getType()!=null && isUserAuthorizedForEntityClipboardUse( entityHeader.getType(), true );
            }
        };

        return reduce(
                nodes,
                new ArrayList<AbstractTreeNode>(),
                new Binary<List<AbstractTreeNode>,List<AbstractTreeNode>,AbstractTreeNode>() {
            @Override
            public List<AbstractTreeNode> call( final List<AbstractTreeNode> abstractTreeNodes,
                                                final AbstractTreeNode abstractTreeNode ) {
                if ( abstractTreeNode instanceof EntityHeaderNode ) {
                    final EntityHeaderNode<?> headerNode = (EntityHeaderNode<?>) abstractTreeNode;
                    final Option<EntityHeader> entityHeader = optional( headerNode.getEntityHeader() );
                    if ( !entityHeader.exists( cutOrCopyPermission ) ) {
                        return Collections.emptyList();
                    } else {
                        abstractTreeNodes.add( abstractTreeNode );
                    }
                } else if ( abstractTreeNode instanceof FolderNodeBase ) {
                    if ( !isUserAuthorizedForEntityClipboardUse( EntityType.FOLDER ) ) {
                        return Collections.emptyList();
                    } else {
                        abstractTreeNodes.add( abstractTreeNode );
                    }
                } else if ( abstractTreeNode != null ) {
                    assert false : "Unexpected tree node type " + abstractTreeNode.getClass();
                }

                return abstractTreeNodes;
            }
        } );
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

        Set<Object> nodesToTransfer = new HashSet<Object>(selectedPaths.length);
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

    @NotNull
    public RootNode getRootNode() {
        DefaultTreeModel model = (DefaultTreeModel) getModel();
        return (RootNode) model.getRoot();
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

    private class DeletionCancelledException extends RuntimeException{
        private DeletionCancelledException(final String message) {
            super(message);
        }
    }
}
