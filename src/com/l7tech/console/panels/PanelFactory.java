package com.l7tech.console.panels;

import com.l7tech.console.panels.EntityEditorPanel;
import com.l7tech.console.panels.PanelListener;
import com.l7tech.console.panels.GroupPanel;
import com.l7tech.console.tree.BasicTreeNode;
import com.l7tech.console.tree.EntityTreeNode;
import com.l7tech.console.tree.EntityHeaderNode;
import com.l7tech.identity.Group;
import com.l7tech.identity.User;
import com.l7tech.objectmodel.EntityType;

import javax.swing.*;

/**
 * The Panel Factory class provides panels for entities.
 *
 * todo: extract interface, and move to factory package.
 * @author <a href="mailto:emarceta@layer7-tech.com>Emil Marceta</a>
 */
public class PanelFactory {
    PanelFactory() {
    }

    /**
     * Returns the panel for the node with mode (read/write) and panel listener
     * or <B>null</B> in case panel has not been assigned to the object.
     * In case container object is passed the panel is actually
     * the object browser.
     *
     * @param node   the node for which the panel is requested

     * @param pListener - the PanelListener for the given panel
     * @return the panel
     */
    public static EntityEditorPanel getPanel(EntityTreeNode node, PanelListener pListener) {
        Object object = node.getUserObject();
        if (object == null) throw new IllegalArgumentException("node.getUserObject() returns null");
        if (object instanceof EntityHeaderNode) {
            EntityEditorPanel panel =
                    getPanel(((EntityHeaderNode)object).getEntityHeader().getType(), pListener);
            return panel;
        }
        JOptionPane.showMessageDialog(null,
                        "Could not retrieve panel for given class ("+object.getClass()+")",
                        "Warning",
                        JOptionPane.WARNING_MESSAGE);

        return null;
    }


    /**
     * Return the panel instance for the given <CODE>Object</CODE> with
     * the specified read/write mode and panel listener.
     *
     * @param cls     the class that the editor is looked for
     * @param l    - the PanelListener for the given panel
     * @return the <CODE>EntityEditorPanel</CODE> for given directory object, null
     *         if no panel assigned
     */
    public static EntityEditorPanel getPanel(EntityType type, PanelListener l) {
        EntityEditorPanel panel = null;
        if (type.equals(EntityType.GROUP)) {
            panel = new GroupPanel();
            panel.setPanelListener(l);
        } else if(type.equals(EntityType.USER)) {
            panel = new UserPanel();
            panel.setPanelListener(l);
        }
        return panel;
    }
}

