package com.l7tech.console.tree;

import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;

import java.util.Enumeration;


/**
 * The class <code>TreeNodeFactory</code> is a factory
 * class that creates <code>TreeNode</code> instances that
 * are placed in <code>TreeModel.</code>.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.1
 */
public class TreeNodeFactory {

    /**
     * private constructor, this class cannot be instantiated
     */
    private TreeNodeFactory() {
    }

    /**
     * Returns the corresponding TreeNode instance for
     * an directory <code>Entry</code>
     *
     * @return the TreeNode for a given Entry
     */
    public static AbstractTreeNode asTreeNode(EntityHeader entity) {
        if (entity == null) {
            throw new NullPointerException("entity");
        }
        if (EntityType.ID_PROVIDER_CONFIG.equals(entity.getType())) {
            return new IdentityProviderNode(entity);
        } else if (EntityType.GROUP.equals(entity.getType())) {
            return new GroupNode(entity);
        } else if (EntityType.USER.equals(entity.getType())) {
            return new UserNode(entity);
        } else if (EntityType.SERVICE.equals(entity.getType())) {
            return new ServiceNode(entity);
        } else if (EntityType.ALERT_TRIGGER.equals(entity.getType())) {
            return new AlertEventNode(entity);
        } else if (EntityType.ALERT_ACTION.equals(entity.getType())) {
            return new AlertNotificationNode(entity);
        }

        throw new IllegalArgumentException("Unknown entity type " + entity.getType());
    }

    /**
     * returns an TreeNodeEnumeration for a given Enumeration
     *
     * @param en the enumeration that the TreeNodeEnumeration encapsulates
     * @return the TreeNodeEnumeration instance for a given Enumeration
     */
    public static Enumeration getTreeNodeEnumeration(Enumeration en) {
        return new TreeNodeEnumeration(en);
    }

    /**
     * The class <CODE>TreeNodeEnumeration</CODE> is a transparent
     * closure (Decorator pattern) over an Enumeration.
     * It is used to create AbstractTreeNode instances from enumerations
     * of Entry instances.
     * For example the nodes that have children use this class to create special
     * instances of AbstractTreeNode for their respective children.
     */
    private static final class
      TreeNodeEnumeration implements Enumeration {
        private Enumeration nodes;

        /**
         * construct thhe TreeNodeEnumeraion instance with a given
         * <CODE>Enumeration</CODE>
         *
         * @param en the enumeraiton used as a source
         */
        TreeNodeEnumeration(Enumeration en) {
            nodes = en;
        }

        /**
         * Tests if this enumeration contains more elements.
         *
         * @return <code>true</code> if and only if this enumeration object
         *         contains at least one more element to provide;
         *         <code>false</code> otherwise.
         */
        public boolean hasMoreElements() {
            return nodes.hasMoreElements();
        }

        /**
         * Returns the next element of this enumeration if this enumeration
         * object has at least one more element to provide.
         *
         * @return the next element of this enumeration.
         */
        public Object nextElement() {
            return getEnumerationElement(nodes.nextElement());
        }

        /**
         * Returns an enumeration element. It runs the element through the
         * asTreeNode method if it is an Entry.
         *
         * @param element object the is the next requested enumeration element
         * @return the  AbstractTreeNode instance for a given Entry if the object
         *         is an Entry, or the same element otherwise.
         */
        private Object getEnumerationElement(Object element) {
            if (element instanceof EntityHeader) {
                return TreeNodeFactory.asTreeNode((EntityHeader)element);
            }
            return element;
        }
    }
}
