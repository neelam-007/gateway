package com.l7tech.console.tree;

import com.l7tech.objectmodel.EntityHeader;

import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

/**
 * The class represents an tree node gui node element that
 * corresponds to the Provider entity.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com>Emil Marceta</a>
 * @version 1.2
 */
public class ProviderNode extends EntityHeaderNode {
    /**
     * construct the <CODE>ProviderNode</CODE> instance for
     * a given entity.
     * The parameter entity must represent a provider, otherwise the
     * runtime IllegalArgumentException exception is thrown.
     *
     * @param e  the Entry instance, must be Realm
     * @exception IllegalArgumentException
     *                   thrown if the entity instance is not a provider
     */
    public ProviderNode(EntityHeader e) {
        super(e);
        if (e == null) {
            throw new IllegalArgumentException("entity == null");
        }
    }

    /**
     * Returns the children of the reciever as an Enumeration.
     *
     * @return the Enumeration of the child nodes.
     */
    public Enumeration children() {
        List list =
                Arrays.asList(new BasicTreeNode[]{});
        return Collections.enumeration(list);
    }

    public boolean getAllowsChildren() {
        return false;
    }

    /**
     * subclasses override this method specifying the resource name
     *
     * @param open for nodes that can be opened, can have children
     */
    protected String iconResource(boolean open) {
        return "com/l7tech/console/resources/providers16.gif";
    }
}
