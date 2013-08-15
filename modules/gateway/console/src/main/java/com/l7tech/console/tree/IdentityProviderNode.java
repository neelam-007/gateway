package com.l7tech.console.tree;

import com.l7tech.console.action.*;
import com.l7tech.console.panels.identity.finder.Options;
import com.l7tech.console.util.EntitySaver;
import com.l7tech.console.util.Registry;
import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.identity.IdentityProviderType;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.objectmodel.UpdateException;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * The class represents an tree node gui node element that
 * corresponds to the Provider entity.
 *
 * @author Emil Marceta
 */
public class IdentityProviderNode extends EntityHeaderNode<EntityHeader> {
    /**
     * construct the <CODE>ProviderNode</CODE> instance for
     * a given entity.
     * The parameter entity must represent a provider, otherwise the
     * runtime IllegalArgumentException exception is thrown.
     *
     * @param e the Entry instance, must be provider
     * @throws IllegalArgumentException thrown if the entity instance is not a provider
     */
    public IdentityProviderNode(EntityHeader e) {
        super(e);
        if (e == null) {
            throw new IllegalArgumentException("entity == null");
        }
    }


    /**
     * Get the set of actions associated with this node.
     * This may be used e.g. in constructing a context menu.
     *
     * @return actions appropriate to the node
     */
    @Override
    public Action[] getActions() {
        java.util.List<Action> list = new ArrayList<Action>();
        list.add(new IdentityProviderPropertiesAction(this));
        Options options = new Options();
        options.setEnableDeleteAction(true);
        Object obj = getUserObject();
        if (obj instanceof EntityHeader) {
            options.setInitialProvider(((EntityHeader)obj).getGoid());
        }
        IdentityProviderConfig config = getProviderConfig();

        if ( config != null ) {
            list.add(new FindIdentityAction(options));
            final Action newUserAction;
            final NewGroupAction newGroupAction;
            final NewVirtualGroupAction newVirtualGroupAction;

            if (config.type() == IdentityProviderType.INTERNAL) {
                newUserAction = new NewInternalUserAction(this);
                newGroupAction = new NewGroupAction(this);
                newVirtualGroupAction = new NewVirtualGroupAction(this);
                newUserAction.setEnabled(true);
                newGroupAction.setEnabled(true);
                newVirtualGroupAction.setEnabled(false);
            } else if (config.type() == IdentityProviderType.FEDERATED) {
                newUserAction = new NewFederatedUserAction(this);
                newGroupAction = new NewGroupAction(this);
                newVirtualGroupAction = new NewVirtualGroupAction(this);
                newUserAction.setEnabled(true);
                newGroupAction.setEnabled(true);
                newVirtualGroupAction.setEnabled(true);
                list.add(new CopyFederatedIdentityProviderAction(this));
            } else {
                // the actions here is dummy as they are always disabled from the beginning
                // this is currently for LDAP user
                newUserAction = new NewInternalUserAction(this);
                newGroupAction = new NewGroupAction(this);
                newVirtualGroupAction = new NewVirtualGroupAction(this);
                newUserAction.setEnabled(false);
                newGroupAction.setEnabled(false);
                newVirtualGroupAction.setEnabled(false);
                if ( config.type() == IdentityProviderType.LDAP ) {
                    list.add(new CopyLdapProviderAction(this));
                } else if ( config.type() == IdentityProviderType.BIND_ONLY_LDAP  ) {
                    list.add(new CopyBindOnlyLdapProviderAction(this));
                }
            }

            list.add(newUserAction);
            list.add(newGroupAction);
            list.add(newVirtualGroupAction);
        }

        list.addAll(Arrays.asList(super.getActions()));
        for ( final Action action : list ) {
            if ( action instanceof DeleteEntityAction ) {
                action.setEnabled( config != null && config.type() != IdentityProviderType.INTERNAL );
            }
        }


        if (config.type() != null && config.type() == IdentityProviderType.INTERNAL) {

            list.add(new ForceAdminPasswordResetAction());
            list.add(new IdentityProviderManagePasswordPolicyAction());
            list.add(new ConfigureSecurityZoneAction<IdentityProviderConfig>(config, new EntitySaver<IdentityProviderConfig>() {
                @Override
                public IdentityProviderConfig saveEntity(@NotNull final IdentityProviderConfig entity) throws SaveException {
                    try {
                        final Goid oid = Registry.getDefault().getIdentityAdmin().saveIdentityProviderConfig(entity);
                        entity.setGoid(oid);
                    } catch (final UpdateException e) {
                        throw new SaveException("Unable to save identity provider: " + e.getMessage(), e);
                    }
                    return entity;
                }
            }));
        }


        return list.toArray(new Action[list.size()]);
    }

    /**
     * Gets the default action for this node.
     *
     * @return <code>null</code> indicating there should be none default action
     */
    @Override
    public Action getPreferredAction() {
        return new IdentityProviderPropertiesAction(this);
    }


    /**
     * Returns true if the receiver is a leaf.
     *
     * @return true if leaf, false otherwise
     */
    @Override
    public boolean isLeaf() {
        return true;
    }

    /**
     * subclasses override this method specifying the resource name
     *
     * @param open for nodes that can be opened, can have children
     */
    @Override
    protected String iconResource(boolean open) {
        return "com/l7tech/console/resources/CreateIdentityProvider16x16.gif";
    }

    /**
     * test whether the node can refresh its children. The provider
     * node can never refresh its children
     *
     * @return always false
     */
    @Override
    public boolean canRefresh() {
        return false;
    }

    /**
     * Test if the node can be deleted. Default is <code>true</code>
     *
     * @return true if the node can be deleted, false otherwise
     */
    @Override
    public boolean canDelete() {
        return true;
    }

}
