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
 * The Initial Developer of the Original Code is Cladonia Ltd. Portions created 
 * by the Initial Developer are Copyright (C) 2002 the Initial Developer. 
 * All Rights Reserved. 
 *
 * Contributor(s): Edwin Dankert <edankert@cladonia.com>
 */
package com.l7tech.console.xmlviewer;

import com.l7tech.console.xmlviewer.properties.ViewerProperties;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

/**
 * The viewer for a eXchaNGeR document. This gives a tree view of an
 * XML document.
 *
 * @author Edwin Dankert <edankert@cladonia.com>
 * @version	$Revision$, $Date$
 */
public class Viewer extends JPanel implements XDocumentListener {
    private static final boolean DEBUG = false;
    XmlTree tree = null;
    private JScrollPane scrollPane = null;


    private ExchangerDocument document = null;
    private ViewerProperties properties = null;

    /**
     * Constructs an explorer view with the ExplorerProperties supplied.
     *
     * @param props the explorer properties.
     */
    public Viewer(ViewerProperties props, ExchangerDocument document) {
        this.document = document;
        this.properties = props;

        setLayout(new BorderLayout());

        try {
            tree = new XmlTree(this, (ExchangerElement)document.getRoot());
        } catch (Exception e) {
            e.printStackTrace();
            // should not happen
        }
        document.addListener(this);
        scrollPane = new JScrollPane(tree,
          JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
          JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        /**
         * Work around to make sure the scroll pane shows the vertical
         * scrollbar for the first time when resized to a size small enough.
         * JDK 1.3.0-C
         *
         * Got work around from Bug ID: 4243631 (It should be fixed...)
         *
         * ED: Check with JDK1.4
         */
        scrollPane.getViewport().addComponentListener(new ComponentAdapter() {
            public void componentResized(ComponentEvent e) {
                scrollPane.doLayout();
            }
        });

        setBorder(new EmptyBorder(0, 0, 0, 0));
        add(scrollPane, BorderLayout.CENTER);
    }


    /**
     * Check to find out if namespaces should be visible.
     *
     * @return true if namespaces are visible.
     */
    public boolean showNamespaces() {
        return properties.isShowNamespaces();
    }

    /**
     * Check to find out if attributes should be visible.
     *
     * @return true if attributes are visible.
     */
    public boolean showAttributes() {
        return properties.isShowAttributes();
    }

    /**
     * Check to find out if comments should be visible.
     *
     * @return true if comments are visible.
     */
    public boolean showComments() {
        return properties.isShowComments();
    }

    /**
     * Check to find out if values should be visible.
     *
     * @return true if values are visible.
     */
    public boolean showValues() {
        return properties.isShowValues();
    }

    /**
     * Collapses all the nodes in the tree.
     */
    public void collapseAll() {
        tree.collapseAll();
    }

    /**
     * Expands all the nodes in the tree.
     */
    public void expandAll() {
        tree.expandAll();
    }

// Implementation of the XDocumentListener interface...
    public void documentUpdated(XDocumentEvent event) {
        if (!document.isError()) {
            try {
                tree.setRoot(this, (ExchangerElement)document.getRoot());
            } catch (Exception e) {
                // should not happen
            }
            tree.expand(3);
        }
    }

    public void documentDeleted(XDocumentEvent event) {
    }

    public void selectXpath(String xpathString) {
        XElement[] elements = null;
        try {
            elements = (XElement[])document.getElements(xpathString);

            // tree.collapseAll();
            tree.clearSelection();

            for (int i = 0; i < elements.length; i++) {
                final ExchangerElement element = (ExchangerElement)elements[i];
                tree.setSelectedNode(element, true);
            }
            properties.addXPath(xpathString);

        } catch (Throwable t) {
            JOptionPane.showMessageDialog(this,
              t.getMessage(),
              "XPath Error",
              JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Sets whether or not this component controls are enabled.
     *
     * @param enabled true if this component controls should be enabled, false otherwise
     */
    public void setViewerEnabled(boolean enabled) {
        tree.setEnabled(enabled);
    }
}
