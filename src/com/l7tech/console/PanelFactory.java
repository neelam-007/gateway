package com.l7tech.console;

import com.l7tech.console.panels.EditorPanel;
import com.l7tech.console.panels.PanelListener;
import com.l7tech.console.tree.BasicTreeNode;
import com.l7tech.console.tree.DirectoryTreeNode;

/**
 * The Panel Factory class provides panels for entity
 * nodes registered with the directory.
 * The class methods are not not synchronized.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com>Emil Marceta</a>
 */
public class PanelFactory {
    PanelFactory() {
    }


    /**
     * Returns the EditorPanel for the given node and sets its PanelListener to
     * the one provided. The default implementation returns the read write node.
     *
     * @param node   the panel is requestod for the node
     * @param pListener - the PanelListener for the given panel
     * @return the panel
     */
    public static EditorPanel getPanel(DirectoryTreeNode node, PanelListener pListener) {
        return getPanel(node, true, pListener);
    }


    /**
     * Returns the panel for the node with mode (read/write) and panel listener
     * or <B>null</B> in case panel has not been assigned to the object.
     * In case container object is passed the panel is actually
     * the object browser.
     *
     * @param node   the node for which the panel is requested
     * @param rw
     * @param pListener - the PanelListener for the given panel
     * @return the panel
     */
    public static EditorPanel getPanel(DirectoryTreeNode node, boolean rw, PanelListener pListener) {
        Object object = node.getUserObject();
        if (object instanceof BasicTreeNode) {
            return getPanel((BasicTreeNode) object, rw, pListener);
        }
        return null;
    }


    /**
     * Return the panel instance for the given <CODE>Object</CODE> with
     * the specified read/write mode and panel listener.
     *
     * @param dobj   the BasicTreeNode Object
     * @param rw     read-write or read-only (currently unused)
     * @param pListener - the PanelListener for the given panel
     * @return the <CODE>EditorPanel</CODE> for given directory object, null
     *         if no panel assigned
     */
    public static EditorPanel getPanel(Object dobj, boolean rw, PanelListener pListener) {
        EditorPanel panel = null;
        return panel;
    }
}

