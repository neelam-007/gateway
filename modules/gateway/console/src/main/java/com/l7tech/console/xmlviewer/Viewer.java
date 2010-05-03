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

import com.l7tech.console.xmlviewer.properties.ConfigurationProperties;
import com.l7tech.console.xmlviewer.properties.ViewerProperties;
import com.l7tech.console.xmlviewer.util.DocumentUtilities;
import com.l7tech.gui.util.ClipboardActions;
import org.dom4j.DocumentException;
import org.dom4j.io.XMLWriter;
import org.xml.sax.SAXParseException;

import javax.swing.*;
import javax.swing.tree.TreeModel;
import javax.swing.border.EmptyBorder;
import javax.swing.event.TreeSelectionListener;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.io.StringWriter;

/**
 * The viewer for a eXchaNGeR document. This gives a tree view of an
 * XML document.
 *
 * @author Edwin Dankert <edankert@cladonia.com>
 */
public class Viewer extends JPanel implements XDocumentListener {
    private static final String DEFAULT_COPY_LABEL = "Copy Document";

    XmlTree tree = null;
    private JScrollPane scrollPane = null;


    private ExchangerDocument document = null;
    private ViewerProperties properties = null;

    /**
     * Routes to the {@link Viewer#createMessageViewer(String, boolean)}
     * with the scroll pane set to true.
     *
     * @see Viewer#createMessageViewer(String, boolean)
     */
    public static Viewer createMessageViewer(String content)
      throws DocumentException, IOException, SAXParseException {
        return createMessageViewer(content, true);
    }

    /**
     * Create the essage viewer for the cml content string.
     *
     * @param content    the xml content string. If null or emtpy the emtpy
     *                   viewer is created.
     * @param scrollPane boolean whether to use the scrollpane
     * @return the viewer widget
     * @throws DocumentException thrown on dom4j processing error
     * @throws IOException       on io error
     * @throws SAXParseException on xml parsing error
     */
    public static Viewer createMessageViewer(String content, boolean scrollPane)
      throws DocumentException, IOException, SAXParseException {
        ConfigurationProperties cp = new ConfigurationProperties();
        ExchangerDocument exchangerDocument = null;
        if (!(content == null || "".equals(content))) {
            exchangerDocument = asExchangerDocument(content);
        }
        return new Viewer(cp.getViewer(), exchangerDocument, scrollPane, DEFAULT_COPY_LABEL);
    }

    /**
     * Constructs an viewer view with the ViewerProperties supplied.
     *
     * @param props    the viewer properties.
     * @param document the exchanger document
     */
    public Viewer(ViewerProperties props, ExchangerDocument document) {
        this(props, document, true, DEFAULT_COPY_LABEL);

    }

    /**
     * Constructs an viewer view with the ViewerProperties supplied.
     *
     * @param props      the viewer properties.
     * @param document   the exchanger document
     * @param scrollpane whether to use scrollpane
     * @param copyLabel  text to use for copy context menu option
     */
    public Viewer( final ViewerProperties props,
                   final ExchangerDocument document,
                   final boolean scrollpane,
                   final String copyLabel ) {
        this.document = document;
        this.properties = props;

        setLayout(new BorderLayout());

        ExchangerElement rootElement;
        try {
            if (document !=null) {
                rootElement = (ExchangerElement) document.getRoot();
                tree = new XmlTree(this, rootElement);
                document.addListener(this);
            } else {
                rootElement = null;
                tree = new XmlTree(this, null);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        JComponent c = tree;

        tree.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) { pop(e); }

            @Override
            public void mouseReleased(MouseEvent e) { pop(e); }

            private void pop(MouseEvent ev) {
                if (!ev.isPopupTrigger())
                    return;
                JPopupMenu menu = makeMenu();
                menu.show((Component)ev.getSource(), ev.getX(), ev.getY());
            }

            private JPopupMenu makeMenu() {
                JPopupMenu contextMenu = new JPopupMenu();
                JMenuItem item = new JMenuItem(new AbstractAction(copyLabel) {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        ExchangerElement rootElement = getCurrentTreeRootElement();
                        if (rootElement == null)
                            return;
                        final Clipboard clip = ClipboardActions.getClipboard();
                        if (clip == null)
                            return;
                        final String toCopy = rootElement.asXML();
                        clip.setContents(new Transferable() {
                            @Override
                            public DataFlavor[] getTransferDataFlavors() {
                                return new DataFlavor[] { DataFlavor.stringFlavor };
                            }

                            @Override
                            public boolean isDataFlavorSupported(DataFlavor flavor) {
                                return DataFlavor.stringFlavor.equals(flavor);
                            }

                            @Override
                            public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
                                return toCopy;
                            }
                        }, null);
                    }
                });
                ExchangerElement rootElement = getCurrentTreeRootElement();
                final boolean haveRoot = rootElement != null;
                final boolean haveClip = ClipboardActions.isSystemClipboardAvailable();
                item.setEnabled(haveClip && haveRoot);
                contextMenu.add(item);
                return contextMenu;
            }
        });

        if (scrollpane) {
            scrollPane = new JScrollPane(tree,
              JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
              JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
            c = scrollPane;
            JScrollBar scrollBar = scrollPane.getVerticalScrollBar();
            scrollBar.setUnitIncrement(scrollBar.getUnitIncrement() * 5);
        }

        setBorder(new EmptyBorder(0, 0, 0, 0));
        add(c, BorderLayout.CENTER);
    }

    private ExchangerElement getCurrentTreeRootElement() {
        final TreeModel model = tree.getModel();
        if (model == null)
            return null;
        XmlElementNode rootElementNode = (XmlElementNode) model.getRoot();
        return rootElementNode == null ? null : rootElementNode.getElement();
    }

    /**
     * Set the content for this message viewer
     *
     * @param content the new xml content
     * @throws DocumentException on dom4j exception
     * @throws IOException       on i/o error
     * @throws SAXParseException on prse error
     */
    public void setContent(String content) throws DocumentException, IOException, SAXParseException {
        ExchangerDocument exchangerDocument = asExchangerDocument(content);
        if (document != null) {
            document.removeAllListeners();
        }
        document = exchangerDocument;
        document.addListener(this);
        document.load();
        documentUpdated(null);
    }

    public String getContent() {
        if (document == null) {
            return null;
        }
        try {
            XElement rootElement = document.getRoot();
            StringWriter sw = new StringWriter();
            XMLWriter writer = new XMLWriter(sw);
            writer.write(rootElement);
            return sw.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
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
                throw new RuntimeException(e);
            }
            tree.expand(3);
        }
    }

    public void documentDeleted(XDocumentEvent event) {
    }

    public boolean selectXpath(String xpathString) {
        XElement[] elements;
        try {
            elements = document.getElements(xpathString);

            // tree.collapseAll();
            tree.clearSelection();

            for (XElement element1 : elements) {
                final ExchangerElement element = (ExchangerElement) element1;
                tree.setSelectedNode(element, true);
            }
            properties.addXPath(xpathString);
            return true;
        } catch (Throwable t) {
            JOptionPane.showMessageDialog(this,
              t.getMessage(),
              "XPath Error",
              JOptionPane.ERROR_MESSAGE);
        }
        return false;
    }

    /**
     * Sets whether or not this component controls are enabled.
     *
     * @param enabled true if this component controls should be enabled, false otherwise
     */
    public void setViewerEnabled(boolean enabled) {
        tree.setEnabled(enabled);
    }

    /**
     * Adds a listener for document selection (TreeSelection) events.
     *
     * @param listener the new listener
     */
    public void addDocumentTreeSelectionListener(TreeSelectionListener listener) {
        tree.addTreeSelectionListener(listener);
    }

    /**
     * Removes a Document element selection (TreeSelection) event listener.
     *
     * @param listener the document listener
     */
    public void removeDocumentTreeSelectionListener(TreeSelectionListener listener) {
        tree.removeTreeSelectionListener(listener);
    }

    /**
     * Get the scrollpane component. Note that it can be null in case the
     * scrollpane was not requested in constructor.
     *
     * @return the scrollpane hosting the xml tree or null
     */
    public JScrollPane getScrollPane() {
        return scrollPane;
    }

    /**
     * Create the exchanger document instance for the content string.
     *
     * @param content the content string
     * @return the exchanger document for the content string
     * @throws IOException
     * @throws DocumentException
     * @throws SAXParseException
     */
    protected static ExchangerDocument asExchangerDocument(String content)
      throws IOException, DocumentException, SAXParseException {

        ExchangerDocument exchangerDocument = new ExchangerDocument(DocumentUtilities.readDocument(content, false), false);
        exchangerDocument.load();
        return exchangerDocument;
    }
}
