package com.l7tech.console.action;

import com.l7tech.console.event.EntityEvent;
import com.l7tech.console.event.EntityListener;
import com.l7tech.console.event.WeakEventListenerList;
import com.l7tech.console.tree.EntityHeaderNode;
import com.l7tech.console.tree.ProviderNode;
import com.l7tech.console.tree.identity.IdentityProvidersTree;
import com.l7tech.console.util.TopComponents;
import com.l7tech.identity.IdentityProviderConfig;

import javax.swing.*;
import javax.swing.event.EventListenerList;
import javax.swing.tree.DefaultTreeModel;
import java.util.EventListener;
import java.util.logging.Logger;


/**
 * The <code>DeleteEntityAction</code> action deletes the entity
 * such as user, group etc.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 */
public class DeleteEntityAction extends SecureAction {
    static final Logger log = Logger.getLogger(DeleteEntityAction.class.getName());
    private EventListenerList listenerList = new WeakEventListenerList();

    public void setNode(EntityHeaderNode node) {
        this.node = node;
    }

    public void setConfig(IdentityProviderConfig config) {
        this.config = config;
    }

    EntityHeaderNode node;
    private IdentityProviderConfig config;

    /**
     * create the acciton that deletes
     *
     * @param en the node to deleteEntity
     */
    public DeleteEntityAction(EntityHeaderNode en, IdentityProviderConfig ip) {
        node = en;
        config = ip;
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
        boolean deleted = false;
        deleted = Actions.deleteEntity(node, config);
        if (deleted) {
            JTree tree = null;
            if (node instanceof ProviderNode) {
                tree = (JTree)TopComponents.getInstance().getComponent(IdentityProvidersTree.NAME);
                DefaultTreeModel model = (DefaultTreeModel)tree.getModel();
                model.removeNodeFromParent(node);
            }
            EventListener[] listeners = listenerList.getListeners(EntityListener.class);
            for (int i = 0; i < listeners.length; i++) {
                EventListener listener = listeners[i];
                EntityEvent ev = new EntityEvent(this, node.getEntityHeader());
                ((EntityListener)listener).entityRemoved(ev);
            }
        }
    }
}
