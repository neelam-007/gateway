package com.l7tech.console.tree.servicesAndPolicies;

import com.l7tech.console.tree.*;
import com.l7tech.console.util.Registry;
import com.l7tech.console.action.*;
import com.l7tech.gateway.common.service.ServiceAdmin;
import com.l7tech.gateway.common.service.ServiceHeader;
import com.l7tech.gateway.common.security.rbac.EntityType;
import com.l7tech.gateway.common.security.rbac.OperationType;
import com.l7tech.gateway.common.admin.PolicyAdmin;
import com.l7tech.objectmodel.folder.FolderHeader;
import com.l7tech.objectmodel.folder.Folder;
import com.l7tech.objectmodel.folder.HasFolder;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.policy.PolicyHeader;
import com.l7tech.policy.PolicyType;

import javax.swing.tree.TreeNode;
import javax.swing.*;
import java.util.*;
import java.util.logging.Logger;

/**
 * Created by IntelliJ IDEA.
 * User: darmstrong
 * Date: Aug 13, 2008
 * Time: 4:10:28 PM
 * This class IS the root node in the services and policies tree
 * This AbstractTreeNode loads all children shown in this tree
 * It has no subclasses
 */
public final class RootNode extends AbstractTreeNode implements PolicyServiceTreeNodeCreator, FolderNodeBase{
    /**
     * default comparator for the child objects
     */
    public static final Comparator<TreeNode> DEFAULT_COMPARATOR = new Comparator<TreeNode>() {
        public int compare(TreeNode o1, TreeNode o2) {
            if (o1 instanceof AbstractTreeNode && o2 instanceof AbstractTreeNode) {
            //if (o1 instanceof Comparable && o2 instanceof Comparable) {
                if(o1 instanceof FolderNode && !(o2 instanceof FolderNode)) {
                    return -1;
                } else if(!(o1 instanceof FolderNode) && o2 instanceof FolderNode) {
                    return 1;
                } else if(o1 instanceof FolderNode && o2 instanceof FolderNode) {
                    String name1 = ((FolderNode)o1).getName();
                    String name2 = ((FolderNode)o2).getName();
                    return name1.compareToIgnoreCase(name2);
                } else {
                    String name1 = ((EntityHeaderNode)o1).getEntityHeader().getName();
                    String name2 = ((EntityHeaderNode)o2).getEntityHeader().getName();
                    return name1.compareToIgnoreCase(name2);
                }
            }
            return 0; // no order - assume everything equal
        }
    };

    static Logger log = Logger.getLogger(RootNode.class.getName());

    public static final long OID = -5002L;

    private ServiceAdmin serviceManager;
    private PolicyAdmin policyAdmin;
    private String title;

    /**
     * construct the <CODE>ServicesFolderNode</CODE> instance for
     * a given service manager with the name.
     */
    public RootNode(String name) {
        super(null, DEFAULT_COMPARATOR);
        serviceManager = Registry.getDefault().getServiceManager();
        policyAdmin = Registry.getDefault().getPolicyAdmin();
        title = name;
    }

    /**
     * Returns true if the receiver is a leaf.
     *
     * @return true if leaf, false otherwise
     */
    @Override
    public boolean isLeaf() {
        return false;
    }

    /**
     * Returns true if the receiver allows children.
     */
    @Override
    public boolean getAllowsChildren() {
        return true;
    }

    private final Action[] allActions = new Action[]{
        new PublishServiceAction(),
        new CreateServiceWsdlAction(),
        new PublishNonSoapServiceAction(),
        new PublishInternalServiceAction(),
        new CreatePolicyAction(),
        new CreatePolicyFolderAction(OID, this, this, Registry.getDefault().getServiceManager()),
        new RefreshTreeNodeAction(this)
    };
    
    /**
     * The only action you can take on the root node is to paste a new folder
     * @return actions appropriate to the root node
     */
    @Override
    public Action[] getActions() {
        // Filter unlicensed actions
        List<Action> actions = new ArrayList<Action>();
        for (Action action : allActions) {
            if (action.isEnabled())
                actions.add(action);
        }

        Action securePaste = ServicesAndPoliciesTree.getSecuredAction(EntityType.FOLDER,
                                                                OperationType.UPDATE,
                                                                ServicesAndPoliciesTree.ClipboardActionType.PASTE);
        if(securePaste != null) actions.add(securePaste);

        return actions.toArray(new Action[actions.size()]);
    }

    /**
     * load the service and policy folder children
     */
    @Override
    protected void loadChildren() {
        try {
            List<EntityHeader> allFolderEntities = new ArrayList<EntityHeader>();
            List<FolderHeader> allFolderHeaders = new ArrayList<FolderHeader>();

            ServiceHeader[] serviceHeaders = serviceManager.findAllPublishedServices();
            List<ServiceHeader> serviceHeadersList = Arrays.asList(serviceHeaders);
            Collection<PolicyHeader> policyHeaders = policyAdmin.findPolicyHeadersWithTypes(EnumSet.of(PolicyType.INCLUDE_FRAGMENT, PolicyType.INTERNAL));

            allFolderEntities.addAll(serviceHeadersList);
            allFolderEntities.addAll(policyHeaders);

            Collection<FolderHeader> policyFolderHeaders = policyAdmin.findAllPolicyFolders();
            allFolderHeaders.addAll(policyFolderHeaders);

            Collection<FolderHeader> serviceFolderHeaders = serviceManager.findAllPolicyFolders();
            allFolderHeaders.addAll(serviceFolderHeaders);

            children = null;

            FolderHeader root = null;
            for(FolderHeader folder : allFolderHeaders) {
                if(folder.getParentFolderOid() == null) {
                    root = folder;
                }
            }

            for(Iterator<EntityHeader> it = allFolderEntities.iterator();it.hasNext();) {
                EntityHeader header = it.next();
                if(header instanceof HasFolder){
                    HasFolder hasFolder = (HasFolder) header;
                    if(hasFolder.getFolderOid() == root.getOid()) {
                        AbstractTreeNode child = TreeNodeFactory.asTreeNode(header);
                        insert(child, getInsertPosition(child));
                        it.remove();
                    }
                }
            }

            for(FolderHeader folder : policyFolderHeaders) {
                if(folder.getParentFolderOid() != null && root.getOid() == folder.getParentFolderOid()) {
                    FolderNode childNode = getFolderNodeFromHeaders(folder, allFolderEntities, policyFolderHeaders);
                    insert(childNode, getInsertPosition(childNode));
                }
            }
        } catch(FindException e) {
            log.warning("Failed to load folders.");
        }
    }

    /**
     * @return true as this node children can be refreshed
     */
    @Override
    public boolean canRefresh() {
        return true;
    }

    /**
     * Returns the node name.
     * Gui nodes have name to facilitate handling in
     * components such as JTree.
     *
     * @return the name as a String
     */
    public String getName() {
        return title;
    }

    public long getOid() {
        return OID;
    }

    /**
     * subclasses override this method specifying the resource name
     *
     * @param open for nodes that can be opened, can have children
     */
    protected String iconResource(boolean open) {
        return "com/l7tech/console/resources/ServerRegistry.gif";
    }

    private FolderNode getFolderNodeFromHeaders(FolderHeader root,
                                                List<EntityHeader> entityHeaders,
                                                Collection<FolderHeader> foldersHeaders)
    {
        FolderNode node = new FolderNode(root, this);
        for(Iterator<EntityHeader> it = entityHeaders.iterator();it.hasNext();) {
            EntityHeader header = it.next();
            if(header instanceof HasFolder){
                HasFolder hasFolder = (HasFolder) header;
                if(hasFolder.getFolderOid() == root.getOid()) {
                    node.addEntityNode(header);
                    it.remove();
                }
            }
        }

        for(FolderHeader folder : foldersHeaders) {
            if(folder.getParentFolderOid() != null && root.getOid() == folder.getParentFolderOid()) {
                FolderNode childNode = getFolderNodeFromHeaders(folder, entityHeaders, foldersHeaders);
                node.addChild(childNode);
            }
        }

        return node;
    }

    public AbstractTreeNode createFolderNode(Folder folder) {
        FolderHeader header = new FolderHeader(folder);
        return new FolderNode(header, this);
    }
}