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
import com.l7tech.console.xmlviewer.properties.ConfigurationProperties;
import com.l7tech.console.xmlviewer.util.DocumentUtilities;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.TreeSelectionListener;
import java.awt.*;
import java.io.IOException;
import java.io.File;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URL;

import org.dom4j.DocumentException;
import org.dom4j.Document;
import org.dom4j.io.XMLWriter;
import org.xml.sax.SAXParseException;
import org.apache.xml.serialize.OutputFormat;

/**
 * The viewer for a eXchaNGeR document. This gives a tree view of an
 * XML document.
 *
 * @author Edwin Dankert <edankert@cladonia.com>
 * @version	$Revision$, $Date$
 */
public class Viewer extends JPanel implements XDocumentListener {
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
        Viewer messageViewer = new Viewer(cp.getViewer(), exchangerDocument, scrollPane);
        return messageViewer;
    }

    /**
     * Constructs an viewer view with the ViewerProperties supplied.
     *
     * @param props    the viewer properties.
     * @param document the exchanger document
     */
    public Viewer(ViewerProperties props, ExchangerDocument document) {
        this(props, document, true);

    }

    /**
     * Constructs an viewer view with the ViewerProperties supplied.
     *
     * @param props      the viewer properties.
     * @param document   the exchanger document
     * @param scrollpane whether to use scrollpane
     */
    public Viewer(ViewerProperties props, ExchangerDocument document, boolean scrollpane) {
        this.document = document;
        this.properties = props;

        setLayout(new BorderLayout());

        try {
            if (document !=null) {
                tree = new XmlTree(this, (ExchangerElement)document.getRoot());
                document.addListener(this);
            } else {
                tree = new XmlTree(this, null);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        JComponent c = tree;

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
            XMLWriter writer = new XMLWriter();
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

        ExchangerDocument exchangerDocument = new ExchangerDocument(asTempFileURL(content), false);
        exchangerDocument.load();
        return exchangerDocument;
    }

    protected static URL asTempFileURL(String content)
      throws IOException, DocumentException {
        final File file = File.createTempFile("Temp", ".xml");
        Document doc = DocumentUtilities.createReader(false).read(new StringReader(content));
        DocumentUtilities.writeDocument(doc, file.toURL());
        file.deleteOnExit();
        return file.toURL();
    }

    protected static URL emptyTempFileURL()
      throws IOException {
        final File file = File.createTempFile("Temp", ".xml");
        file.deleteOnExit();
        return file.toURL();
    }
}
