package com.l7tech.console.action;

import com.l7tech.console.tree.AbstractTreeNode;
import com.l7tech.console.tree.EntityWithPolicyNode;
import com.l7tech.console.tree.ServicesAndPoliciesTree;
import com.l7tech.console.tree.TreeNodeFactory;
import com.l7tech.console.tree.servicesAndPolicies.FolderNode;
import com.l7tech.console.tree.servicesAndPolicies.RootNode;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.SecurityZoneUtil;
import com.l7tech.console.util.TopComponents;
import com.l7tech.gateway.common.security.rbac.AttemptedCreate;
import com.l7tech.gateway.common.service.PublishedServiceAlias;
import com.l7tech.gateway.common.service.ServiceHeader;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.objectmodel.*;
import com.l7tech.objectmodel.folder.Folder;
import com.l7tech.objectmodel.folder.FolderHeader;
import com.l7tech.policy.PolicyAlias;
import com.l7tech.policy.PolicyHeader;
import com.l7tech.util.Functions.NullaryThrows;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.tree.DefaultTreeModel;
import java.awt.*;
import java.util.List;

/**
 * Paste the list of entities stored in RootNode.getEntitiesToAlias() as new aliases.
 *
 * @author darmstrong
 */
public class PasteAsAliasAction extends SecureAction {
    private final FolderNode parentNode;

    public PasteAsAliasAction(FolderNode parentNode) {
        super(new AttemptedCreate(EntityType.SERVICE_ALIAS));
        this.parentNode = parentNode;
    }

    /**
     * @return the action name
     */
    @Override
    public String getName() {
        return "Paste as Alias";
    }

    /**
     * @return the action description
     */
    @Override
    public String getDescription() {
        return "Paste as Alias";
    }

    /**
     * specify the resource name for this action
     */
    @Override
    protected String iconResource() {
        return "com/l7tech/console/resources/folder.gif";
    }

    /**
     */
    @Override
    protected void performAction() {
        final List<AbstractTreeNode> abstractTreeNodes = RootNode.getEntitiesToAlias();
        final JTree tree = (JTree) TopComponents.getInstance().getComponent(ServicesAndPoliciesTree.NAME);
        final Folder parentFolder = parentNode.getFolder();

        //Make sure the folder in which they are being created is not the same the folder they original entities are in
        //this constraint is also enforced in MarkEntityToAliasAction and also in db
        if(abstractTreeNodes.size() > 0){
            final AbstractTreeNode parentNode = (AbstractTreeNode) abstractTreeNodes.get(0).getParent();
            if ( parentNode == null ) {
                DialogDisplayer.showMessageDialog(tree, "Service or policy not found for alias", "Create Error", JOptionPane.ERROR_MESSAGE, null);
                return;
            }

            final FolderHeader fH = (FolderHeader) parentNode.getUserObject();
            final FolderHeader parentHeader = (FolderHeader) this.parentNode.getUserObject();
            if(fH.getOid() == parentHeader.getOid()){
                DialogDisplayer.showMessageDialog(tree, "Cannot create alias in the same folder as original", "Create Error", JOptionPane.ERROR_MESSAGE, null);
                return;
            }
        }

        // Verify that aliases can be created for all entities
        for( final AbstractTreeNode atn: abstractTreeNodes ){
            if(!(atn instanceof EntityWithPolicyNode)) return;
            final EntityHeader eh = ((EntityWithPolicyNode)atn).getEntityHeader();

            //check if an alias already exists for any entity
            if ( eh instanceof OrganizationHeader ) {
                NullaryThrows<Alias<?>,FindException> aliasFinder = null;
                if ( eh.getType() == EntityType.SERVICE ) {
                    aliasFinder = new NullaryThrows<Alias<?>,FindException>(){
                        @Override
                        public Alias<?> call() throws FindException {
                            return Registry.getDefault().getServiceManager().findAliasByEntityAndFolder(eh.getOid(), parentFolder.getOid());
                        }
                    };
                } else if ( eh.getType() == EntityType.POLICY ) {
                    //check if an alias already exists here
                    aliasFinder = new NullaryThrows<Alias<?>,FindException>(){
                        @Override
                        public Alias<?> call() throws FindException {
                            return Registry.getDefault().getPolicyAdmin().findAliasByEntityAndFolder(eh.getOid(), parentFolder.getOid());
                        }
                    };
                }
                if ( aliasFinder==null || aliasExistsInFolder(
                        aliasFinder,
                        tree,
                        "policy " + eh.getName(),
                        parentFolder.getName() ) ) {
                    return;
                }
            } else {
                return;
            }
        }

        RootNode.clearEntitiesToAlias();

        //Create an alias of each node selected
        for( final AbstractTreeNode atn: abstractTreeNodes ){
            final EntityHeader eh = ((EntityWithPolicyNode)atn).getEntityHeader();
            final OrganizationHeader header;
            final Long aliasOid;
            try {
                if ( eh instanceof ServiceHeader ) {
                    header = new ServiceHeader((ServiceHeader)eh);
                    final PublishedServiceAlias psa = new PublishedServiceAlias((ServiceHeader)eh, parentFolder, getSecurityZoneFromHeader((ServiceHeader)eh, EntityType.SERVICE_ALIAS));
                    aliasOid = Registry.getDefault().getServiceManager().saveAlias(psa);
                } else if ( eh instanceof PolicyHeader  ) {
                    header = new PolicyHeader((PolicyHeader)eh);
                    final PolicyAlias pa = new PolicyAlias((PolicyHeader)eh, parentFolder, getSecurityZoneFromHeader((PolicyHeader)eh, EntityType.POLICY_ALIAS));
                    aliasOid = Registry.getDefault().getPolicyAdmin().saveAlias(pa);
                } else {
                    throw new IllegalStateException("Referent was neither a Policy nor a Service");
                }
            } catch (ObjectModelException ome) {
                throw new RuntimeException("Unable to save alias", ome);
            } catch (VersionException ve) {
                throw new RuntimeException("Unable to save alias", ve);
            }

            header.setAliasOid(aliasOid);
            header.setFolderOid(parentFolder.getOid());
            final EntityWithPolicyNode childNode = (EntityWithPolicyNode) TreeNodeFactory.asTreeNode(header, RootNode.getComparator());

            int insertPosition = parentNode.getInsertPosition(childNode, RootNode.getComparator());
            parentNode.insert(childNode, insertPosition);
            final DefaultTreeModel model = (DefaultTreeModel)tree.getModel();
            final RootNode rootNode = (RootNode) model.getRoot();
            model.nodesWereInserted(parentNode, new int[]{insertPosition});
            rootNode.addAlias(header.getOid(), childNode);
        }
    }

    @Nullable
    private SecurityZone getSecurityZoneFromHeader(@NotNull final HasSecurityZoneOid header, @NotNull final EntityType aliasType) {
        SecurityZone zone = null;
        final Long securityZoneOid = header.getSecurityZoneOid();
        if (securityZoneOid != null) {
            final SecurityZone headerZone = SecurityZoneUtil.getSecurityZoneByOid(securityZoneOid);
            if (headerZone != null && headerZone.permitsEntityType(aliasType)) {
                zone = headerZone;
            }
        }
        return zone;
    }

    private boolean aliasExistsInFolder( final NullaryThrows<Alias<?>,FindException> aliasLookup,
                                         final Component parent,
                                         final String description,
                                         final String folderDescription ) {
        try {
            final Alias<?> checkAlias = aliasLookup.call();
            if( checkAlias != null ){
                DialogDisplayer.showMessageDialog(parent,"Alias of " + description + " already exists in folder " + folderDescription, "Create Error", JOptionPane.ERROR_MESSAGE, null);
            }
            return checkAlias != null;
        } catch (FindException e1) {
            throw new RuntimeException("Unable to check for existing alias", e1);
        }
    }

    @Override
    public boolean isAuthorized() {
        return canAttemptOperation(new AttemptedCreate(EntityType.SERVICE_ALIAS)) ||
                canAttemptOperation(new AttemptedCreate(EntityType.POLICY_ALIAS));
    }
}

