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
 * The Original Code is eXchaNGeR browser code. (org.xngr.browser.*)
 *
 * The Initial Developer of the Original Code is Cladonia Ltd.. Portions created 
 * by the Initial Developer are Copyright (C) 2002 the Initial Developer. 
 * All Rights Reserved. 
 *
 * Contributor(s): Edwin Dankert <edankert@cladonia.com>
 */
package com.l7tech.console.xmlviewer;

import com.l7tech.console.xmlviewer.properties.DocumentProperties;
import com.l7tech.console.xmlviewer.util.DocumentUtilities;
import org.dom4j.Document;
import org.dom4j.DocumentFactory;
import org.dom4j.io.XMLWriter;
import org.xml.sax.SAXParseException;

import javax.swing.*;
import javax.swing.Timer;
import javax.swing.event.EventListenerList;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.MalformedURLException;
import java.util.*;

/**
 * The default implementation of the Exchanger document.
 * This implementation is completely thread safe, the events
 * are always fired on the Swing event thread.
 *
 * @author Edwin Dankert <edankert@cladonia.com>
 * @version	$Revision$, $Date$
 */
public class ExchangerDocument implements XDocument {
    private static final int UPDATE_TIME = 2000;

    private EventListenerList listeners = null;

    private DocumentProperties properties = null;
    private Document document = null;
    private Exception exception = null;
    private boolean readOnly = false;
    private boolean updated = false;
    private static final URL fakeUrl;
    static {
        try {
            fakeUrl = new URL("http://nowhere.example.com/aDocument");
        } catch (MalformedURLException e) {
            throw new RuntimeException(e); // can't happen
        }
    }

    private Timer timer = null;

    /**
     * Constructs an exchanger document from the url and
     * dom4j document supplied.
     * <p/>
     * It creates a new document properties object.
     *
     * @param document the dom4j document.
     */
    public ExchangerDocument(Document document, boolean validate) {
        listeners = new EventListenerList();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        XMLWriter writer = null;
        try {
            writer = new XMLWriter(baos);
            writer.write(document);
            writer.close();
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        this.properties = new DocumentProperties(fakeUrl.toExternalForm(), validate, baos.toString());
        this.document = document;

        // Set this as the document in the root element.
        ((ExchangerElement)document.getRootElement()).document(this);
    }

    /**
     * Gets an element from this document for the specified xpath.
     * Returns Null, if the element cannot be found.
     *
     * @param xpath expression to the element.
     * @return the element.
     */
    public XElement getElement(String xpath) throws Exception {
        if (exception != null) {
            throw exception;
        }

        XElement[] elements = getElements(xpath);

        if (elements.length > 0) {
            return elements[0];
        }

        return null;
    }

    /**
     * Gets a list of elements from this document for the
     * specified xpath. Returns Null, if no elements can
     * be found.
     *
     * @param xpath expression to the elements.
     * @return the elements.
     */
    public XElement[] getElements(String xpath) throws Exception {
        if (exception != null) {
            throw exception;
        }

        List list = document.selectNodes(xpath);
        Vector elements = new Vector();
        Iterator iterator = list.iterator();

        while (iterator.hasNext()) {
            Object object = iterator.next();

            if (object instanceof ExchangerElement) {
                elements.addElement(object);
            }
        }

        XElement[] result = new XElement[elements.size()];

        for (int i = 0; i < elements.size(); i++) {
            result[i] = (XElement)elements.elementAt(i);
        }

        return result;
    }

    /**
     * Sets the Properties for this remote document,
     * Resets the document value and the fires an event...
     * Note: This is only used for remote documents.
     *
     * @param name  the name of this document
     */
    public void setProperties(String name) {
        if (!getName().equals(name)) {
            properties.setName(name);
        }

        // Make sure the event is always fired on the GUI thread!
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                fireDocumentUpdated(null);
            }
        });
    }

    /**
     * Returns the name for this document.
     *
     * @return the name for this document.
     */
    public String getName() {
        return properties.getName();
    }

    public void save() throws IOException {

    }

    public void load() throws IOException, SAXParseException {

    }

    /**
     * Returns the root element for this document.
     *
     * @return the root element.
     */
    public XElement getRoot() throws Exception {
        if (exception != null) {
            throw exception;
        }

        return (XElement)document.getRootElement();
    }

    public URL getURL() {
        return fakeUrl;
    }

    /**
     * Check to see if the document has been loaded in a DOM
     * structure.
     *
     * @return the true when the document has been loaded.
     */
    public boolean isLoaded() {
        return (document != null);
    }

    /**
     * Check to see if the document is a remote document.
     *
     * @return true when the document is not a local file.
     */
    public boolean isRemote() {
        return false;
    }

    /**
     * Check to see if previously loading the document
     * resulted in an error.
     *
     * @return true when loading the document generated an error.
     */
    public boolean isError() {
        return (exception != null);
    }

    /**
     * Fired from the updated element!
     */
    public void fireUpdate() {
        updated = true;
    }

    /**
     * Check to see if the document has been updated.
     *
     * @return true when the document has been updated.
     */
    public boolean isUpdated() {
        return updated;
    }

    /**
     * Check to see if the document is set to read only.
     *
     * @return true when the document cannot be changed.
     */
    public boolean isReadOnly() {
        return isRemote() ? true : readOnly;
    }

    /**
     * Returns the error generated when previously loading
     * the document resulted in an error.
     *
     * @return the error.
     */
    public Exception getError() {
        return exception;
    }

    /**
     * Loads the document from the specified xml string and informs the
     * listeners that the document has been changed.
     * <p/>
     * <b>Note:</b> The update event is always fired on the GUI thread!
     * This makes it easier to run this method in a thread.
     */
    public synchronized void load(String xml) throws IOException, SAXParseException {
        ExchangerElement root = null;
        exception = null;
        document = null;

        try {
            readOnly = true;
            updated = false;
            document = DocumentUtilities.readDocument(xml, properties.validate());
            properties.setSourceXml(xml);

            root = (ExchangerElement)document.getRootElement();

            // Set this as the document in the root element.
            root.document(this);

        } catch (IOException e) {
            exception = e;
            throw e;
        } catch (SAXParseException e) {
            exception = e;
            throw e;
        } finally {
            final ExchangerElement element = root;

            // Make sure the event is always fired on the GUI thread!
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    fireDocumentUpdated(element);
                }
            });
        }
    }


    /**
     * Adds a document listener to the document.
     */
    public void addListener(XDocumentListener listener) {
        listeners.add(XDocumentListener.class, listener);
    }

    /**
     * Removes a document listener from the document.
     */
    public void removeListener(XDocumentListener listener) {
        listeners.remove(XDocumentListener.class, listener);
    }

    /**
     * This is not an XDocument method!
     * This method returns the properties for this document.
     *
     * @return the properties for this document.
     */
    public DocumentProperties getProperties() {
        return properties;
    }

    /**
     * This is not an XDocument method!
     * This method returns the dom4j document.
     *
     * @return the properties for this document.
     */
    public Document getDocument() {
        return document;
    }

    /**
     * This is not an XDocument method!
     * This method returns true if an element of the type supplied
     * exists in the document.
     *
     * @param type of the element.
     * @return true if an element for the element-type exists.
     */
    public boolean hasElement(XElementType type) {
        boolean result = false;

        if (hasElement((ExchangerElement)document.getRootElement(), type)) {
            result = true;
        }

        return result;
    }

    private boolean hasElement(ExchangerElement element, XElementType type) {
        boolean result = false;

        if (element.getType().equals(type)) {
            result = true;
        } else {
            Iterator list = element.elements().iterator();

            while (list.hasNext() && !result) {
                result = hasElement((ExchangerElement)list.next(), type);
            }
        }

        return result;
    }

    /**
     * Notifies the listeners about a change in the document.
     */
    protected void fireDocumentUpdated(XElement element) {
        // Guaranteed to return a non-null array
        Object[] list = listeners.getListenerList();

        // Process the listeners last to first, notifying
        // those that are interested in this event
        for (int i = list.length - 2; i >= 0; i -= 2) {
            if (list[i] == XDocumentListener.class) {
                ((XDocumentListener)list[i + 1]).documentUpdated(new XDocumentEvent(this, element));
            }
        }
    }

    /**
     * Notifies the listeners about the deletion of this document.
     */
    protected void fireDocumentDeleted() {
        // Guaranteed to return a non-null array
        Object[] list = listeners.getListenerList();

        // Process the listeners last to first, notifying
        // those that are interested in this event
        for (int i = list.length - 2; i >= 0; i -= 2) {
            if (list[i] == XDocumentListener.class) {
                try {
                    ((XDocumentListener)list[i + 1]).documentDeleted(new XDocumentEvent(this, getRoot()));
                } catch (Exception e) {
                    // should not happen
                }
            }
        }
    }

    /**
     * Removes all the listeners from this document.
     */
    protected void removeAllListeners() {
        // Guaranteed to return a non-null array
        Object[] list = listeners.getListenerList();

        for (int i = list.length - 2; i >= 0; i -= 2) {
            Object listenerClass = list[i + 1].getClass();
            if(listenerClass instanceof EventListener) {
                listeners.remove(EventListener.class, (EventListener)list[i + 1]);
            }
        }
    }
} 
