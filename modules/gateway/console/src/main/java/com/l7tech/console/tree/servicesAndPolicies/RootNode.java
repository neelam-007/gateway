package com.l7tech.console.tree.servicesAndPolicies;

import com.l7tech.console.tree.*;
import com.l7tech.console.util.Registry;
import com.l7tech.console.action.*;
import com.l7tech.gateway.common.service.ServiceAdmin;
import com.l7tech.gateway.common.service.ServiceHeader;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.gateway.common.security.rbac.OperationType;
import com.l7tech.gateway.common.admin.PolicyAdmin;
import com.l7tech.gateway.common.admin.FolderAdmin;
import com.l7tech.objectmodel.folder.FolderHeader;
import com.l7tech.objectmodel.*;
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
public final class RootNode extends FolderNode{

    private final static ServicesAndPoliciesNodeComparator comparator = new ServicesAndPoliciesNodeComparator();
    private final AlterDefaultSortAction nameSort;
    private final AlterDefaultSortAction typeSort;
    /**
     * All operations around oidToAliases are
     * convenience so that we don't need to search the tree to manage the state of displayable aliases to
     * original entities when either of them changes. This saves us a lot of tree searching so it's worth it
     * When ever you add or remove from the tree the corresponding method to udpate oidToAliases must be called
     */
    private final Map<Long, Set<AbstractTreeNode>> oidToAliases = new HashMap<Long, Set<AbstractTreeNode>>();
    private final Map<Long, AbstractTreeNode> oidToEntity = new HashMap<Long, AbstractTreeNode>();
    /**
     * Used to track the current entities which the user has selected to alias
     * We record AbstractTreeNode's, the entities are represented by the EntityHeader which is stored in
     * the AbstractTreeNode's user object
     */
    private static List<AbstractTreeNode> entitiesToAlias = new ArrayList<AbstractTreeNode>();

    public static RootNode.ServicesAndPoliciesNodeComparator getComparator(){
        return comparator;
    }

    public static class ServicesAndPoliciesNodeComparator implements Comparator<TreeNode>{
        //defaults
        private boolean nameAscending = true;
        private boolean typeDescending = true;

        public void setNameAscending(boolean ascending){
            nameAscending = ascending;
        }

        private ServicesAndPoliciesNodeComparator(){
            
        }
        /**
         * Services are shown before policies in a folder by default
         * As a result this method is called *descenidng* as this is the default
         * If you want ascending you got to to supply false here 
         * @param descending
         */
        public void setTypeDescending(boolean descending){
            typeDescending = descending;            
        }

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
                    int compVal = name1.compareToIgnoreCase(name2);
                    if(!nameAscending){
                        compVal = compVal * -1; //reverse the sort val
                    }
                    return compVal;
                } else {
                    EntityHeader eH1 = ((EntityHeaderNode)o1).getEntityHeader();
                    EntityHeader eH2 = ((EntityHeaderNode)o2).getEntityHeader();

                    String name1 = eH1.getName();
                    String name2 = eH2.getName();

                    String eT1 = eH1.getType().toString();
                    String eT2 = eH2.getType().toString();

                    int compVal = 0;
                    if(eT1.equals(eT2)){
                        //entites are the same, just use the name
                        compVal = name1.compareToIgnoreCase(name2);
                        if(!nameAscending){
                            compVal = compVal * -1;
                            return compVal;
                        }
                    }

                    compVal = eT1.compareTo(eT2);
                    if(typeDescending){
                        compVal = compVal * -1;
                    }
                    
                    return compVal;
                }
            }
            return 0; // no order - assume everything equal            
        }
    }

    static Logger log = Logger.getLogger(RootNode.class.getName());

    public static final long OID = -5002L;

    private final ServiceAdmin serviceManager;
    private final PolicyAdmin policyAdmin;
    private final String title;
    private final JLabel filterLabel;

    /**
     * construct the <CODE>ServicesFolderNode</CODE> instance for
     * a given service manager with the name.
     */
    public RootNode(String name, JLabel filterLabel) {
        super(new FolderHeader(OID, name, null));
        this.serviceManager = Registry.getDefault().getServiceManager();
        this.policyAdmin = Registry.getDefault().getPolicyAdmin();
        this.title = name;
        this.filterLabel = filterLabel;
        this.nameSort = AlterDefaultSortAction.getSortAction(AlterDefaultSortAction.SortType.NAME);
        this.typeSort = AlterDefaultSortAction.getSortAction(AlterDefaultSortAction.SortType.TYPE);
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

    protected JMenu getSortMenu(){
        JMenu returnMenu = new JMenu("Change sort");
        returnMenu.add(nameSort);
        returnMenu.add(typeSort);
        return returnMenu;                
    }

    protected JMenu getFilterMenu(){
        JMenu returnMenu = new JMenu("Filter");
        returnMenu.add(new AlterFilterAction(AlterFilterAction.FilterType.ALL, filterLabel));
        returnMenu.add(new AlterFilterAction(AlterFilterAction.FilterType.SERVICES, filterLabel));
        returnMenu.add(new AlterFilterAction(AlterFilterAction.FilterType.POLICY_FRAGMENT, filterLabel));
        return returnMenu;                
    }

    private final Action[] allActions = new Action[]{
        new PublishServiceAction(),
        new CreateServiceWsdlAction(),
        new PublishNonSoapServiceAction(),
        new PublishInternalServiceAction(),
        new CreatePolicyAction(),
        new CreateFolderAction(OID, this, Registry.getDefault().getFolderAdmin()),
        new PasteAsAliasAction(this),
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
            if(action instanceof PasteAsAliasAction){
                if(!RootNode.isAliasSet()) continue;
            }
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
     * Called from loadChildren but also used by {@link PasteAsAliasAction}
     * @param entityOid
     * @param aliasNode
     */
    public void addAlias(Long entityOid, AbstractTreeNode aliasNode){
        if(!oidToAliases.containsKey(entityOid)){
            Set<AbstractTreeNode> aliases = new HashSet<AbstractTreeNode>();
            oidToAliases.put(entityOid, aliases);
        }
        oidToAliases.get(entityOid).add(aliasNode);
    }

    public void addEntity(Long entityOid, AbstractTreeNode origEntity){
        oidToEntity.put(entityOid, origEntity);
    }
    /**
     * Remove an aliases from the set we are tracking for an entity.
     * @param entityOid
     * @param aliasNode
     * @throws RuntimeException if aliasNode is not found for entityOid and if it's not sucessfully removed
     */
    public void removeAlias(Long entityOid, AbstractTreeNode aliasNode) throws RuntimeException{
        Set<AbstractTreeNode> aliases = oidToAliases.get(entityOid);
        if(aliases == null){
            throw new RuntimeException("Aliases not found. Cannot remove");
        }
        if(!aliases.contains(aliasNode)){
            throw new RuntimeException("Aliases not found. Cannot remove");
        }

        if(!aliases.remove(aliasNode)){
            throw new RuntimeException("Aliases not found. Cannot remove");            
        }
    }

    public void removeEntity(Long entityOid) throws RuntimeException{
        if(!oidToAliases.containsKey(entityOid)){
            throw new RuntimeException("Aliases not found. Cannot remove");
        }

        oidToAliases.remove(entityOid);
    }

    public Set<AbstractTreeNode> getAliasesForEntity(Long entityOid){
        Set<AbstractTreeNode> aliases = oidToAliases.get(entityOid);
        if(aliases == null){
            return Collections.emptySet();
        }
        return aliases;
    }

    public AbstractTreeNode getNodeForEntity(Long entityOid){
        AbstractTreeNode atn = oidToEntity.get(entityOid);
        if(atn == null){
            throw new RuntimeException("Cannot find entity");
        }
        return atn;
    }

    /**
     * load the service and policy folder children
     */
    @Override
    protected void loadChildren() {
        try {

            //download all servics and policies the user can view
            List<OrganizationHeader> allFolderEntities = new ArrayList<OrganizationHeader>();
            ServiceHeader[] serviceHeaders = serviceManager.findAllPublishedServices(true);
            List<ServiceHeader> serviceHeadersList = Arrays.asList(serviceHeaders);
            Collection<PolicyHeader> policyHeaders = policyAdmin.findPolicyHeadersWithTypes(EnumSet.of(PolicyType.INCLUDE_FRAGMENT, PolicyType.INTERNAL), true);

            allFolderEntities.addAll(serviceHeadersList);
            allFolderEntities.addAll(policyHeaders);

            //download all folders the user can view
            FolderAdmin folderAdmin = Registry.getDefault().getFolderAdmin();
            Collection<FolderHeader> allFolders = folderAdmin.findAllFolders();
            Set<FolderHeader> allFolderHeaders = new HashSet<FolderHeader>(allFolders);

            //process the entities into folders and create the tree
            children = null;
            oidToAliases.clear();
            oidToEntity.clear();

            FolderHeader root = null;
            for(FolderHeader folder : allFolderHeaders) {
                if(folder.getParentFolderOid() == null) {
                    root = folder;
                }
            }

            //if this user has no permission to view any folders they will see an empty tree
            if(root == null) return;

            for(Iterator<OrganizationHeader> it = allFolderEntities.iterator();it.hasNext();) {
                OrganizationHeader header = it.next();
                if(header.getFolderOid() == root.getOid()) {
                    AbstractTreeNode child = TreeNodeFactory.asTreeNode(header, RootNode.getComparator());
                    insert(child, getInsertPosition(child, RootNode.getComparator()));
                    it.remove();
                    if(header.isAlias()){
                        //remember the EntityHeader is created by the findAll - the oid of an alias is the
                        //oid of the original
                        addAlias(header.getOid(), child);
                    }else{
                        addEntity(header.getOid(), child);
                    }
                }
            }

            for(FolderHeader folder : allFolderHeaders) {
                if(folder.getParentFolderOid() != null && root.getOid() == folder.getParentFolderOid()) {
                    FolderNode childNode = getFolderNodeFromHeaders(folder, allFolderEntities, allFolderHeaders);
                    insert(childNode, getInsertPosition(childNode, RootNode.getComparator()));
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
                                                List<OrganizationHeader> organizationHeaders,
                                                Collection<FolderHeader> foldersHeaders)
    {
        FolderNode node = new FolderNode(root);
        for(Iterator<OrganizationHeader> it = organizationHeaders.iterator();it.hasNext();) {
            OrganizationHeader header = it.next();
            if(header.getFolderOid() == root.getOid()) {
                AbstractTreeNode child = node.addEntityNode(header);
                it.remove();
                if(header.isAlias()){
                    //remember the EntityHeader is created by the findAll - the oid of an alias is the
                    //oid of the original
                    addAlias(header.getOid(), child);
                }else{
                    addEntity(header.getOid(), child);
                }
            }
        }

        for(FolderHeader folder : foldersHeaders) {
            if(folder.getParentFolderOid() != null && root.getOid() == folder.getParentFolderOid()) {
                FolderNode childNode = getFolderNodeFromHeaders(folder, organizationHeaders, foldersHeaders);
                node.addChild(childNode);
            }
        }

        return node;
    }

    /**
     * Set the nodes the user wants to alias. This is recorded until clearEntitiesToAlias is called
     * We record AbstractTreeNode's, the entities are represented by the EntityHeader which is stored in
     * the AbstractTreeNode's user object
     * @param nodes Any collection of nodes you want to alias
     */
    public static void setEntitiesToAlias(List<AbstractTreeNode> nodes){
        entitiesToAlias = nodes;
    }

    /**
     * Get the list of entities which the user wants to alias. Extract the user object from the AbstractTreeNode
     * and get it's user object to get the entity's header
     * @return
     */
    public static List<AbstractTreeNode> getEntitiesToAlias(){
        return entitiesToAlias;
    }

    /**
     * Clear the fact that any entities have been recorded to be aliased
     */
    public static void clearEntitiesToAlias(){
        entitiesToAlias = Collections.emptyList();
    }

    /**
     * Used where you want to decide if the paste alias menu should be shown. False when entitiesToAlias has
     * no elements
     * @return
     */
    public static boolean isAliasSet(){
       return (!entitiesToAlias.isEmpty())? true: false;
    }
}