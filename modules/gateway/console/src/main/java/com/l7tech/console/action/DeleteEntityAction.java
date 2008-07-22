package com.l7tech.console.action;

import com.l7tech.gateway.common.security.rbac.AttemptedDeleteSpecific;
import static com.l7tech.gateway.common.security.rbac.EntityType.*;
import com.l7tech.util.Functions;
import com.l7tech.console.event.EntityEvent;
import com.l7tech.console.event.EntityListener;
import com.l7tech.console.event.WeakEventListenerList;
import com.l7tech.console.tree.EntityHeaderNode;
import com.l7tech.console.tree.IdentityProviderNode;
import com.l7tech.console.tree.identity.IdentityProvidersTree;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.TopComponents;
import com.l7tech.identity.AnonymousGroupReference;
import com.l7tech.identity.AnonymousUserReference;
import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.objectmodel.Entity;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;

import javax.swing.*;
import javax.swing.event.EventListenerList;
import javax.swing.tree.DefaultTreeModel;
import java.util.EventListener;
import java.util.logging.Logger;


/**
 * The <code>DeleteEntityAction</code> action deletes the entity
 * such as user, group etc.
 */
public class DeleteEntityAction extends SecureAction {
    static final Logger log = Logger.getLogger(DeleteEntityAction.class.getName());
    private EventListenerList listenerList = new WeakEventListenerList();
    private AttemptedDeleteSpecific attemptedDelete;

    public void setNode(EntityHeaderNode node) {
        this.node = node;
    }

    public void setConfig(IdentityProviderConfig config) {
        this.config = config;
    }

    EntityHeaderNode node;
    protected IdentityProviderConfig config;

    /**
     * create the acciton that deletes
     *
     * @param en the node to deleteEntity
     */
    public DeleteEntityAction(EntityHeaderNode en, IdentityProviderConfig ip) {
        super(null);
        node = en;
        config = ip;
    }

    private AttemptedDeleteSpecific getAttemptedOperation() {
        if (attemptedDelete == null && node != null) {
            EntityHeader header = node.getEntityHeader();
            if (header == null) return null;

            attemptedDelete = getAttemptedDelete(header);
        }
        return attemptedDelete;
    }

    protected AttemptedDeleteSpecific getAttemptedDelete(EntityHeader header) {
        EntityType type = header.getType();
        com.l7tech.gateway.common.security.rbac.EntityType etype;
        Entity deleteMe;
        if (type == EntityType.USER) {
            etype = USER;
            deleteMe = new AnonymousUserReference(header.getStrId(), config.getOid(), header.getName());
        } else if (type == EntityType.GROUP) {
            etype = GROUP;
            deleteMe = new AnonymousGroupReference(header.getStrId(), config.getOid(), header.getName());
        } else if (type == EntityType.ID_PROVIDER_CONFIG) {
            etype = ID_PROVIDER_CONFIG;
            deleteMe = config;
        } else {
            throw new IllegalArgumentException("EntityHeaderNode is a " + type + ", expecting User or Group");
        }
        return new AttemptedDeleteSpecific(etype, deleteMe);
    }


    @Override
    public synchronized boolean isAuthorized() {
        return canAttemptOperation(getAttemptedOperation());
    }

    /**
     * Adds an EntityListener to the action.
     *
     * @param listener to add
     */
    public void addEntityListener(EntityListener listener) {
        listenerList.add(EntityListener.class, listener);
    }

    /**
     * Remove an EntityListener from this .
     *
     * @param listener to remove
     */
    public void removeEntityListener(EntityListener listener) {
        listenerList.remove(EntityListener.class, listener);
    }


    /**
     * @return the action name
     */
    public String getName() {
        return "Delete";
    }

    /**
     * @return the aciton description
     */
    public String getDescription() {
        return "Delete Identity Provider";
    }

    /**
     * subclasses override this method specifying the resource name
     */
    protected String iconResource() {
        return "com/l7tech/console/resources/delete.gif";
    }

    /**
     * Actually perform the action.
     * This is the method which should be called programmatically.
     * note on threading usage: do not access GUI components
     * without explicitly asking for the AWT event thread!
     */
    protected void performAction() {
        Actions.deleteEntity(node, config, new Functions.UnaryVoid<Boolean>() {
            public void call(Boolean deleted) {
                if (deleted) {
                    Registry.getDefault().getSecurityProvider().refreshPermissionCache();
                    JTree tree;
                    if (node instanceof IdentityProviderNode) {
                        tree = (JTree)TopComponents.getInstance().getComponent(IdentityProvidersTree.NAME);
                        DefaultTreeModel model = (DefaultTreeModel)tree.getModel();
                        model.removeNodeFromParent(node);
                    }
                    EventListener[] listeners = listenerList.getListeners(EntityListener.class);
                    for (EventListener listener : listeners) {
                        EntityEvent ev = new EntityEvent(this, node.getEntityHeader());
                        ((EntityListener) listener).entityRemoved(ev);
                    }
                }
            }
        });
    }
}
