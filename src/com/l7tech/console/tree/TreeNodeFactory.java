package com.l7tech.console.tree;

import com.l7tech.objectmodel.EntityHeader;

import java.util.Enumeration;


/**
 * The class <code>TreeNodeFactory</code> is a factory
 * class that creates <code>TreeNode</code> instances that
 * are placed in <code>DirTreeModel.</code>.
 *
 *
 * @author <a href="mailto:emarceta@layer7-tech.com>Emil Marceta</a>
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
    public static BasicTreeNode getTreeNode(EntityHeader entry) {
        if (entry == null) {
            throw new NullPointerException("entry");
        }
        return null;
    }

    /**
     * returns an TreeNodeEnumeration for a given Enumeration
     *
     * @param en     the enumeration that the TreeNodeEnumeration encapsulates
     * @return the TreeNodeEnumeration instance for a given Enumeration
     */
    public static Enumeration getTreeNodeEnumeration(Enumeration en) {
        return new TreeNodeEnumeration(en);
    }

    /**
     * The class <CODE>TreeNodeEnumeraiton</CODE> is a transparent
     * closure (Decorator pattern) over an Enumeration.
     * It is used to create BasicTreeNode instances from enumerations
     * of Entry instances.
     * For example the classes that are containers (ProvidersFolderNode,
     * CompanyFolderTreeNode etc) may use this class to create special
     * instances of BasicTreeNode for their respective children. This
     * allows the feasibility to create a (gui) hierarchy that consist
     * of folders and actual entries while preserving the same interface.
     */
    private static final class
            TreeNodeEnumeration implements Enumeration {
        private Enumeration nodes;

        /**
         * construct thhe TreeNodeEnumeraion instance with a given
         * <CODE>Enumeration</CODE>
         *
         * @param en     the enumeraiton used as a source
         */
        TreeNodeEnumeration(Enumeration en) {
            nodes = en;
        }

        /**
         * Tests if this enumeration contains more elements.
         *
         * @return  <code>true</code> if and only if this enumeration object
         *           contains at least one more element to provide;
         *          <code>false</code> otherwise.
         */
        public boolean hasMoreElements() {
            return nodes.hasMoreElements();
        }

        /**
         * Returns the next element of this enumeration if this enumeration
         * object has at least one more element to provide.
         *
         * @return     the next element of this enumeration.
         */
        public Object nextElement() {
            return getEnumerationElement(nodes.nextElement());
        }

        /**
         * Returns an enumeration element. It runs the element through the
         * getTreeNode method if it is an Entry.
         *
         * @param element object the is the next requested enumeration element
         * @return the  BasicTreeNode instance for a given Entry if the object
         *         is an Entry, or the same element otherwise.
         */
        private Object getEnumerationElement(Object element) {
            return element;
        }
    }
}
