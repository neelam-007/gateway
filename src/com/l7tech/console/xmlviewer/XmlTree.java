/*
 * $Id$
 *
 * The contents of this file are subject to the Mozilla Public License 
 * Version 1.1 (the "License"); you may not use this file except in 
 * compliance with the License. You may obtain a copy of the License at 
 * http://www.mozilla.org/MPL/ 
 *
 * Software distributed under the License is distributed on an "AS IS" basis, 
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License 
 * for the specific language governing rights and limitations under the License.
 *
 * The Original Code is eXchaNGeR Skeleton code. (org.xngr.skeleton.*)
 *
 * The Initial Developer of the Original Code is Cladonia Ltd.. Portions created 
 * by the Initial Developer are Copyright (C) 2002 the Initial Developer. 
 * All Rights Reserved. 
 *
 * Contributor(s): Edwin Dankert <edankert@cladonia.com>
 */
package com.l7tech.console.xmlviewer;

import javax.swing.*;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import java.util.Enumeration;


/**
 * The explorer of documents in the system.
 *
 * @author Edwin Dankert <edankert@cladonia.com>
 * @version	$Revision$, $Date$
 */
public class XmlTree extends JTree {
    DefaultTreeModel model = null;
    XmlElementNode root = null;
    ExchangerElement rootElement = null;
    Viewer viewer = null;

    public XmlTree(Viewer viewer, ExchangerElement element) {
        super();

        rootElement = element;
        this.viewer = viewer;
        setRowHeight(0); // there should be a bigparade # but search does not work on javasoft.com at the moment...
                         // the alternative description is avail at http://developer.apple.com/qa/qa2001/qa1091.html
        root = new XmlElementNode(viewer, element);
        model = new DefaultTreeModel(root);

        setModel(model);
        putClientProperty("JTree.lineStyle", "None");
        setEditable(false);
        setShowsRootHandles(true);
        getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        setCellRenderer(new XmlCellRenderer());
        setRootVisible(true);

        expand(3);
    }

    /**
     * Sets the look and feel to the XML Tree UI look and feel.
     * Override this method if you want to install a different UI.
     */
    public void updateUI() {
        setUI(XmlTreeUI.createUI(this));
    }

    public void expandAll() {
        expandNode(root);
    }

    public void collapseAll() {
        collapseNode((TreeNode)root);
    }

    public void setRoot(Viewer viewer, ExchangerElement element) {
        this.root = new XmlElementNode(viewer, element);

        model.setRoot(root);
    }

    public void update() {
        setRoot(viewer, rootElement);

        model.nodeStructureChanged(root);
        expand(3);
    }

    public void expand(int level) {
        expandNode(root, level);
    }

    private void expandNode(TreeNode node, int level) {
        if (level > 0) {
            expandPath(new TreePath(model.getPathToRoot(node)));

            for (int i = 0; i < node.getChildCount(); i++) {
                expandNode(node.getChildAt(i), level - 1);
            }
        }
    }

    private void expandNode(TreeNode node) {
        expandPath(new TreePath(model.getPathToRoot(node)));

        for (int i = 0; i < node.getChildCount(); i++) {
            expandNode(node.getChildAt(i));
        }
    }

    private void collapseNode(TreeNode node) {
        for (int i = 0; i < node.getChildCount(); i++) {
            collapseNode(node.getChildAt(i));
        }

        collapsePath(new TreePath(model.getPathToRoot(node)));
    }

    /**
     * Selects the node for the given element.
     *
     * @param element the element to select the node for.
     */
    public void setSelectedNode(ExchangerElement element) {
        setSelectedNode(element, false);
    }

    /**
     * Selects the node for the given element.
     *
     * @param element        the element to select the node for.
     * @param selectChildren whether child nodes are selected.
     */
    public void setSelectedNode(ExchangerElement element, boolean selectChildren) {
        XmlElementNode node = getNode(element);
        if (node == null) return;

        TreePath path = new TreePath(model.getPathToRoot(node));
        expandPath(path);
        addSelectionPath(path);
        if (node.getChildCount() == 0) return;
        Enumeration e = node.breadthFirstEnumeration();
        while (e.hasMoreElements()) {
            XmlElementNode child = (XmlElementNode)e.nextElement();
            path = new TreePath(model.getPathToRoot(child));
            expandPath(path);
            addSelectionPath(path);
        }
/*        node = (XmlElementNode)node.getNextSibling();
        path = new TreePath(model.getPathToRoot(node));
        addSelectionPath(path);*/
    }


    /**
     * Returns a node for the ExchangerElement supplied.
     *
     * @param element the element to get the node for.
     * @return the element node.
     */
    public XmlElementNode getNode(ExchangerElement element) {
        return getNode(root, element);
    }

    private XmlElementNode getNode(XmlElementNode node, ExchangerElement element) {

        if (element.equals(node.getElement())) {
            return node;
        } else {
            Enumeration e = node.children();

            while (e.hasMoreElements()) {
                XmlElementNode childNode = getNode((XmlElementNode)e.nextElement(), element);

                if (childNode != null) {
                    return childNode;
                }
            }
        }

        return null;
    }
} 
