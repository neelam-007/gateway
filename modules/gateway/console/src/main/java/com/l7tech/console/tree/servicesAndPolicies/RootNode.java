package com.l7tech.console.tree.servicesAndPolicies;

import com.l7tech.console.action.*;
import com.l7tech.console.tree.*;
import com.l7tech.console.util.EntitySaver;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.SecurityZoneUtil;
import com.l7tech.gateway.common.admin.FolderAdmin;
import com.l7tech.gateway.common.admin.PolicyAdmin;
import com.l7tech.gateway.common.service.ServiceAdmin;
import com.l7tech.gateway.common.service.ServiceHeader;
import com.l7tech.objectmodel.*;
import com.l7tech.objectmodel.folder.Folder;
import com.l7tech.objectmodel.folder.FolderHeader;
import com.l7tech.policy.PolicyHeader;
import com.l7tech.policy.PolicyType;
import com.l7tech.util.Functions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.tree.TreeNode;
import java.io.Serializable;
import java.util.*;
import java.util.logging.Logger;

import static com.l7tech.policy.PolicyType.*;

/**
 * This class IS the root node in the services and policies tree; it loads all children shown in this tree and has no
 * subclasses
 *
 * @author darmstrong
 */
public final class RootNode extends FolderNode {
    private final static ServicesAndPoliciesNodeComparator comparator = new ServicesAndPoliciesNodeComparator();
    private SortComponents sortComponents;

    /**
     * All operations around oidToAliases are
     * convenience so that we don't need to search the tree to manage the state of displayable aliases to
     * original entities when either of them changes. This saves us a lot of tree searching so it's worth it
     * When ever you add or remove from the tree the corresponding method to udpate oidToAliases must be called
     */
    private final Map<Goid, Set<AbstractTreeNode>> goidToAliases = new HashMap<Goid, Set<AbstractTreeNode>>();
    private final Map<Goid, AbstractTreeNode> goidToEntity = new HashMap<Goid, AbstractTreeNode>();
    /**
     * Used to track the current entities which the user has selected to alias
     * We record AbstractTreeNode's, the entities are represented by the EntityHeader which is stored in
     * the AbstractTreeNode's user object
     */
    private static List<AbstractTreeNode> entitiesToAlias = new ArrayList<AbstractTreeNode>();

    public static RootNode.ServicesAndPoliciesNodeComparator getComparator(){
        return comparator;
    }

    public static class ServicesAndPoliciesNodeComparator implements Comparator<TreeNode>, Serializable {
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
         * As a result this method is called *descending* as this is the default
         * If you want ascending you got to to supply false here
         * @param descending
         */
        public void setTypeDescending(boolean descending){
            typeDescending = descending;
        }

        public boolean isNameAscending() {
            return nameAscending;
        }

        public boolean isTypeDescending() {
            return typeDescending;
        }

        @Override
        public int compare(TreeNode o1, TreeNode o2) {
            if (o1 instanceof AbstractTreeNode && o2 instanceof AbstractTreeNode) {
                if(o1 instanceof FolderNode && !(o2 instanceof FolderNode)) {
                    return -1;
                } else if(!(o1 instanceof FolderNode) && o2 instanceof FolderNode) {
                    return 1;
                } else if(o1 instanceof FolderNode) {
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
                    if (o1 instanceof ServiceNode) {    //use full name for service display name + URI
                        name1 = ((ServiceNode)o1).getName();
                    }

                    String name2 = eH2.getName();
                    if (o2 instanceof ServiceNode) {
                        name2 = ((ServiceNode)o2).getName();
                    }

                    String eT1 = eH1.getType().toString();
                    String eT2 = eH2.getType().toString();

                    int compVal;
                    if(eT1.equals(eT2)){
                        //entites are the same, just use the name
                        compVal = name1.compareToIgnoreCase(name2);
                        if(!nameAscending){
                            compVal = compVal * -1;
                            return compVal;
                        } else {
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

    private final ServiceAdmin serviceManager;
    private final PolicyAdmin policyAdmin;
    private final String title;

    /**
     * construct the <CODE>ServicesFolderNode</CODE> instance for
     * a given service manager with the name.
     */
    public RootNode(String name) {
        this(name, new FolderHeader(Folder.ROOT_FOLDER_ID, name, null, 0, "/", SecurityZoneUtil.getCurrentUnavailableZone().getGoid()));
    }

    public RootNode(String name, @NotNull final FolderHeader rootFolderHeader) {
        super(rootFolderHeader, null);
        this.serviceManager = Registry.getDefault().getServiceManager();
        this.policyAdmin = Registry.getDefault().getPolicyAdmin();
        this.title = name;
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

    public void setSortComponents(SortComponents sortComponents) {
        this.sortComponents = sortComponents;
    }

    @Override
    protected JMenu getSortMenu(){
        JMenu sortMenu = new JMenu("Sort By");
        return sortComponents.addSortMenu(sortMenu);
    }

    @Override
    protected JMenu getFilterMenu(){
        JMenu filterMenu = new JMenu("Filter");
        return sortComponents.addFilterMenu(filterMenu);
    }

    private final Action[] allActions = new Action[]{
        new PublishServiceAction(),
        new CreateServiceWsdlAction(),
        new PublishNonSoapServiceAction(),
        new PublishRestServiceAction(),
        new PublishInternalServiceAction(),
        new PublishReverseWebProxyAction(),
        new CreatePolicyAction(),
        new CreateFolderAction(getFolder(), this, Registry.getDefault().getFolderAdmin()),
        new ConfigureSecurityZoneAction<Folder>(getFolder(), new EntitySaver<Folder>() {
                @Override
                public Folder saveEntity(@NotNull final Folder entity) throws SaveException {
                    // only want to save the security zone change
                    try {
                        final SecurityZone selectedZone = entity.getSecurityZone();
                        Registry.getDefault().getRbacAdmin().setSecurityZoneForEntities(selectedZone == null ? null : selectedZone.getGoid(),
                                EntityType.FOLDER, Collections.<Serializable>singleton(entity.getId()));
                    } catch (final UpdateException e) {
                        throw new SaveException("Could not save root folder: " + e.getMessage(), e);
                    }
                    return entity;
                }
            }),
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

        Action securePaste = ServicesAndPoliciesTree.getSecuredAction(ServicesAndPoliciesTree.ClipboardActionType.PASTE);
        if(securePaste != null) actions.add(securePaste);

        return actions.toArray(new Action[actions.size()]);
    }

    /**
     * Called from loadChildren but also used by {@link PasteAsAliasAction}
     * @param entityGoid
     * @param aliasNode
     */
    public void addAlias(Goid entityGoid, AbstractTreeNode aliasNode){
        if(!goidToAliases.containsKey(entityGoid)){
            Set<AbstractTreeNode> aliases = new HashSet<AbstractTreeNode>();
            goidToAliases.put(entityGoid, aliases);
        }
        goidToAliases.get(entityGoid).add(aliasNode);
    }

    public void addEntity(Goid entityGoid, AbstractTreeNode origEntity){
        goidToEntity.put(entityGoid, origEntity);
    }
    /**
     * Remove an aliases from the set we are tracking for an entity.
     * @param entityGoid
     * @param aliasNode
     * @throws RuntimeException if aliasNode is not found for entityOid and if it's not sucessfully removed
     */
    public void removeAlias(Goid entityGoid, AbstractTreeNode aliasNode) throws RuntimeException{
        Set<AbstractTreeNode> aliases = goidToAliases.get(entityGoid);
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

    public void removeEntity(Goid entityGoid) throws RuntimeException{
        if(!goidToAliases.containsKey(entityGoid)){
            throw new RuntimeException("Aliases not found. Cannot remove");
        }

        goidToAliases.remove(entityGoid);
    }

    public Set<AbstractTreeNode> getAliasesForEntity(Goid entityGoid){
        Set<AbstractTreeNode> aliases = goidToAliases.get(entityGoid);
        if(aliases == null){
            return Collections.emptySet();
        }
        return aliases;
    }

    public AbstractTreeNode getNodeForEntity(Goid entityGoid){
        AbstractTreeNode atn = goidToEntity.get(entityGoid);
        if(atn == null){
            throw new RuntimeException("Cannot find entity");
        }
        return atn;
    }

    @Nullable
    public SecurityZone getSecurityZone() {
        return getFolder().getSecurityZone();
    }

    public void setSecurityZone(@Nullable final SecurityZone zone) {
        getFolder().setSecurityZone(zone);
    }

    /**
     * load the service and policy folder children
     */
    @Override
    protected void doLoadChildren() {
        try {

            //download all servics and policies the user can view
            List<OrganizationHeader> allFolderEntities = new ArrayList<OrganizationHeader>();
            ServiceHeader[] serviceHeaders = serviceManager.findAllPublishedServices(true);
            List<ServiceHeader> serviceHeadersList = Arrays.asList(serviceHeaders);
            EnumSet<PolicyType> policyTypes = EnumSet.of( INCLUDE_FRAGMENT, INTERNAL, GLOBAL_FRAGMENT, IDENTITY_PROVIDER_POLICY, POLICY_BACKED_OPERATION );
            Collection<PolicyHeader> policyHeaders = policyAdmin.findPolicyHeadersWithTypes(policyTypes, true);

            allFolderEntities.addAll(serviceHeadersList);
            allFolderEntities.addAll(policyHeaders);

            //download all folders the user can view
            FolderAdmin folderAdmin = Registry.getDefault().getFolderAdmin();
            Collection<FolderHeader> allFolders = folderAdmin.findAllFolders();
            Set<FolderHeader> allFolderHeaders = new HashSet<FolderHeader>(allFolders);

            //process the entities into folders and create the tree
            children = null;
            goidToAliases.clear();
            goidToEntity.clear();

            FolderHeader root = null;
            for(FolderHeader folder : allFolderHeaders) {
                if(folder.getParentFolderGoid() == null) {
                    root = folder;
                }
            }

            //if this user has no permission to view any folders they will see an empty tree
            if (root == null) return;

            for (Iterator<OrganizationHeader> it = allFolderEntities.iterator();it.hasNext();) {
                OrganizationHeader header = it.next();
                final Goid fGoid = header.getFolderId();
                if(fGoid != null && Goid.equals(fGoid, root.getGoid())) {
                    AbstractTreeNode child = TreeNodeFactory.asTreeNode(header, RootNode.getComparator());
                    insert(child, getInsertPosition(child, RootNode.getComparator()));
                    it.remove();
                    if(header.isAlias()){
                        //remember the EntityHeader is created by the findAll - the oid of an alias is the
                        //oid of the original
                        addAlias(header.getGoid(), child);
                    }else{
                        addEntity(header.getGoid(), child);
                    }
                }
            }

            for(FolderHeader folder : allFolderHeaders) {
                if(folder.getParentFolderGoid() != null && Goid.equals(folder.getParentFolderGoid(), root.getGoid())) {
                    FolderNode childNode = getFolderNodeFromHeaders(folder, allFolderEntities, allFolderHeaders, new ArrayList<Folder>());
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
    @Override
    public String getName() {
        return title;
    }

    @Override
    public Goid getGoid() {
        return Folder.ROOT_FOLDER_ID;
    }

    /**
     * subclasses override this method specifying the resource name
     *
     * @param open for nodes that can be opened, can have children
     */
    @Override
    protected String iconResource(boolean open) {
        return "com/l7tech/console/resources/ServerRegistry.gif";
    }

    private FolderNode getFolderNodeFromHeaders(final FolderHeader root,
                                                List<OrganizationHeader> organizationHeaders,
                                                Collection<FolderHeader> foldersHeaders,
                                                Collection<Folder> folders)
    {
        Folder parentNode = Functions.grepFirst(folders,new Functions.Unary<Boolean, Folder>() {
            @Override
            public Boolean call(Folder folder) {
                if(folder.getGoid().equals(root.getParentFolderGoid()))
                    return true;
                return false;
            }
        });
        FolderNode node = new FolderNode(root, parentNode);
        folders.add(node.getFolder());
        for(Iterator<OrganizationHeader> it = organizationHeaders.iterator();it.hasNext();) {
            OrganizationHeader header = it.next();
            if(header.getFolderId() != null && Goid.equals(header.getFolderId(), root.getGoid())) {
                AbstractTreeNode child = node.addEntityNode(header);
                it.remove();
                if(header.isAlias()){
                    //remember the EntityHeader is created by the findAll - the goid of an alias is the
                    //goid of the original
                    addAlias(header.getGoid(), child);
                }else{
                    addEntity(header.getGoid(), child);
                }
            }
        }

        for(FolderHeader folder : foldersHeaders) {
            if(folder.getParentFolderGoid() != null && Goid.equals(folder.getParentFolderGoid(), root.getGoid()) ) {
                FolderNode childNode = getFolderNodeFromHeaders(folder, organizationHeaders, foldersHeaders,folders);
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
       return !entitiesToAlias.isEmpty();
    }

    @Override
    public boolean canDelete() {
        return false;
    }
}
