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
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.metal.MetalTreeUI;
import javax.swing.tree.TreePath;
import java.awt.*;

/**
 * The View part for the Xml Tree component.
 *
 * @author Edwin Dankert <edankert@cladonia.com>
 * @version	$Revision$, $Date$
 */
public class XmlTreeUI extends MetalTreeUI {

    protected static XmlTreeUI treeUI = new XmlTreeUI();

    public static ComponentUI createUI(JComponent c) {
        return new XmlTreeUI();
    }

    /**
     * Paints the expand (toggle) part of a row. The reciever should
     * NOT modify <code>clipBounds</code>, or <code>insets</code>.
     */
    protected void paintExpandControl(Graphics g,
                                      Rectangle clipBounds, Insets insets,
                                      Rectangle bounds, TreePath path,
                                      int row, boolean isExpanded,
                                      boolean hasBeenExpanded,
                                      boolean isLeaf) {

        Object value = path.getLastPathComponent();

        // Draw icons if not a leaf and either hasn't been loaded,
        // or the model child count is > 0.
        if (!isLeaf && (!hasBeenExpanded || treeModel.getChildCount(value) > 0)) {
            int x = bounds.x - (getRightChildIndent() - 1);
            int y = bounds.y;

            Icon icon = null;

            if (isExpanded) {
                icon = getExpandedIcon();
            } else {
                icon = getCollapsedIcon();
            }

            // Draws the icon horizontally centered at (x,y)
            if (icon != null) {
                icon.paintIcon(tree, g, x - icon.getIconWidth() / 2, y);
            }
        }
    }

    protected void paintRow(Graphics g, Rectangle clipBounds,
                            Insets insets, Rectangle bounds, TreePath path,
                            int row, boolean isExpanded,
                            boolean hasBeenExpanded, boolean isLeaf) {
        // Don't paint the renderer if editing this row.
        if (editingComponent != null && editingRow == row) {
            return;
        }

        Object object = path.getLastPathComponent();

        Component component = currentCellRenderer.getTreeCellRendererComponent(tree,
          object, tree.isRowSelected(row),
          isExpanded, isLeaf, row, false); // hasfocus???

        // don't indent the end-tag as far...
        if (object instanceof XmlElementNode && ((XmlElementNode)object).isEndTag()) {
            int indent = getLeftChildIndent() + getRightChildIndent();
            rendererPane.paintComponent(g, component, tree, bounds.x - indent, bounds.y, bounds.width, bounds.height, true);
        } else {
            rendererPane.paintComponent(g, component, tree, bounds.x, bounds.y, bounds.width, bounds.height, true);
        }
    }

    protected void installDefaults() {
        super.installDefaults();

        setExpandedIcon(new ImageIcon(getClass().getResource("/com/l7tech/console/resources/ExpandedIcon.gif")));
        setCollapsedIcon(new ImageIcon(getClass().getResource("/com/l7tech/console/resources/CollapsedIcon.gif")));

        setLeftChildIndent(8);
        setRightChildIndent(8);
    }
}
