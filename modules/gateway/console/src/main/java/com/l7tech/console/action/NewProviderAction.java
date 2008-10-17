package com.l7tech.console.action;

import com.l7tech.console.event.EntityEvent;
import com.l7tech.console.event.EntityListener;
import com.l7tech.console.tree.AbstractTreeNode;
import com.l7tech.console.util.Registry;
import com.l7tech.gateway.common.admin.IdentityAdmin;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.gateway.common.security.rbac.AttemptedCreate;
import com.l7tech.objectmodel.EntityType;

import javax.swing.event.EventListenerList;
import java.util.EventListener;

/**
 * <p> Copyright (C) 2004 Layer 7 Technologies Inc.</p>
 * <p> @author fpang </p>
 * $Id$
 */
abstract public class NewProviderAction extends NodeAction {

    protected EventListenerList listenerList = new EventListenerList();

    public NewProviderAction(AbstractTreeNode node) {
        super(node, LIC_AUTH_ASSERTIONS, new AttemptedCreate(EntityType.ID_PROVIDER_CONFIG));
    }

    /**
     * notfy the listeners that the entity has been added
     *
     * @param header
     */
    protected void fireEventProviderAdded(EntityHeader header) {
        EntityEvent event = new EntityEvent(this, header);
        EventListener[] listeners = listenerList.getListeners(EntityListener.class);
        for (EventListener listener : listeners) {
            ((EntityListener) listener).entityAdded(event);
        }
    }

    /**
     * add the EntityListener
     *
     * @param listener the EntityListener
     */
    public void addEntityListener(EntityListener listener) {
        listenerList.add(EntityListener.class, listener);
    }

    /**
     * remove the the EntityListener
     *
     * @param listener the EntityListener
     */
    protected void removeEntityListener(EntityListener listener) {
        listenerList.remove(EntityListener.class, listener);
    }

    protected IdentityAdmin getIdentityAdmin()
            throws RuntimeException {
        IdentityAdmin admin = Registry.getDefault().getIdentityAdmin();
        if (admin == null) {
            throw new RuntimeException("Could not find registered " + IdentityAdmin.class);
        }

        return admin;
    }
}
