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
package com.l7tech.console.xmlviewer.properties;

import com.l7tech.console.xmlviewer.ExchangerDocumentFactory;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;
import org.dom4j.tree.DefaultDocument;
import org.dom4j.tree.DefaultElement;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

/**
 * Handles the eXchaNGeR configuration document.
 *
 * @author Edwin Dankert <edankert@cladonia.com>
 * @version	$Revision$, $Date$
 */
public class ConfigurationProperties {
    private static final boolean DEBUG = false;
    public static final String XNGR_HOME = System.getProperty("user.home") + File.separator + ".xngr" + File.separator;
    private static final String PROPERTIES_FILE = ".xngr.xml";

    private Document document = null;
    private DesktopProperties desktopProperties = null;
    private ExplorerProperties explorerProperties = null;
    private EditorProperties editorProperties = null;
    private ViewerProperties viewerProperties = null;
    private CategoryProperties rootCategoryProperties = null;
    private Element root = null;
    private Vector services = null;
    private URL url = null;

    /**
     * Creates the Configuration Document wrapper.
     * It reads in the root element and sets the list of services.
     *
     * @param the url to the XML document.
     */
    public ConfigurationProperties() {
        if (DEBUG) System.out.println("ConfigurationProperties()");

        File dir = new File(XNGR_HOME);

        if (!dir.exists()) {
            dir.mkdir();
        }

        File file = new File(dir, PROPERTIES_FILE);

        try {
            url = file.toURL(); // MalformedURLException
        } catch (Exception e) {
            // Should never happen, am not sure what to do in this case...
            e.printStackTrace();
        }

        document = readDocument(url);

        // First time, create the document...
        if (document == null) {
            this.document = createDocument();
        }

        root = document.getRootElement();

        desktopProperties = new DesktopProperties(root.element("desktop"));
        explorerProperties = new ExplorerProperties(root.element("explorer"));
        editorProperties = new EditorProperties(root.element("editor"));
        viewerProperties = new ViewerProperties(root.element("viewer"));

        List list = root.elements("service");

        services = new Vector();

        Iterator i = list.iterator();

        while (i.hasNext()) {
            services.addElement(new ServiceProperties((Element)i.next()));
        }

        rootCategoryProperties = new CategoryProperties(root.element("category"));
    }

    /**
     * Returns true when the desktop is shown.
     *
     * @return the desktop visibility.
     */
    public boolean isShowDesktop() {
        if (DEBUG) System.out.println("ConfigurationProperties.isShowDesktop()");
        Element elem = root.element("show-desktop");

        return "true".equals(elem.getText());
    }

    /**
     * Set the desktop (in)visible.
     *
     * @param visible true when the desktop should be visible.
     */
    public void showDesktop(boolean visible) {
        Element elem = root.element("show-desktop");
        elem.setText("" + visible);
    }

    /**
     * Returns the properties for the desktop.
     *
     * @return the desktop properties.
     */
    public DesktopProperties getDesktop() {
        if (DEBUG) System.out.println("ConfigurationProperties.getDesktop()");
        return desktopProperties;
    }

    /**
     * Returns the properties for the editor.
     *
     * @return the editor properties.
     */
    public EditorProperties getEditor() {
        if (DEBUG) System.out.println("ConfigurationProperties.getEditor()");
        return editorProperties;
    }

    /**
     * Returns the properties for the viewer.
     *
     * @return the viewer properties.
     */
    public ViewerProperties getViewer() {
        if (DEBUG) System.out.println("ConfigurationProperties.getViewer()");
        return viewerProperties;
    }

    /**
     * Returns the properties for the explorer.
     *
     * @return the explorer properties.
     */
    public ExplorerProperties getExplorer() {
        if (DEBUG) System.out.println("ConfigurationProperties.getExplorer()");
        return explorerProperties;
    }

    /**
     * Returns a list of Service Properties.
     *
     * @return a list of service properties.
     */
    public Vector getServices() {
        if (DEBUG) System.out.println("ConfigurationProperties.getServices()");
        return services;
    }

    /**
     * Adds a service to the properties.
     *
     * @param the service properties.
     */
    public void addService(ServiceProperties service) {
        if (DEBUG) System.out.println("ConfigurationProperties.addService( " + service + ")");

        root.add(service.getElement());
        services.add(service);
    }

    /**
     * Removes a service from the document and the list of services.
     *
     * @param service the service properties.
     */
    public void removeService(ServiceProperties service) {
        if (DEBUG) System.out.println("ConfigurationProperties.removeService( " + service + ")");

        root.remove(service.getElement());
        services.remove(service);
    }

    /**
     * Returns the root document category.
     *
     * @return the root category.
     */
    public CategoryProperties getRootCategory() {
        return rootCategoryProperties;
    }

    /**
     * Saves the configuration properties to disk.
     */
    public void save() {
        if (DEBUG) System.out.println("ConfigurationProperties.save()");

        // Make sure the properties are uptodate...
        editorProperties.update();
        viewerProperties.update();

        try {
            OutputFormat format = new OutputFormat("  ", true, "UTF-8");
            FileOutputStream out = new FileOutputStream(url.getFile());

            XMLWriter writer = new XMLWriter(out, format);
            writer.write(document);
            writer.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Document createDocument() {
        Document document = null;

        Element rootElement = new DefaultElement("xngr-config");

        rootElement.addElement("show-desktop").setText("" + false);
        rootElement.add(createExplorer());
        rootElement.add(createDesktop());
        rootElement.add(createEditor());
        rootElement.add(createViewer());
        rootElement.add(createRootCategory());

        return new DefaultDocument(rootElement);
    }

    private Element createRootCategory() {
        Element category = new DefaultElement("category");
        category.addElement("name").setText("Categories");

        return category;
    }

    private Element createExplorer() {
        Element explorer = createViewElement("explorer", 0, 0, 600, 400);
        explorer.addElement("divider-location").setText("200");

        return explorer;
    }

    private Element createDesktop() {
        Element desktop = createViewElement("desktop", 100, 100, 300, 250);

        return desktop;
    }

    private Element createEditor() {
        Element editor = null;

        editor = createViewElement("editor", 0, 0, 600, 550);

        editor.addElement("search-match-case").setText("" + false);
        editor.addElement("search-direction-down").setText("" + true);
        editor.addElement("spaces").setText("" + 4);
        editor.addElement("tag-completion").setText("" + true);

        return editor;
    }

    private Element createViewer() {
        Element viewer = null;

        viewer = createViewElement("viewer", 0, 0, 600, 550);

        viewer.addElement("show-namespaces").setText("" + true);
        viewer.addElement("show-attributes").setText("" + true);
        viewer.addElement("show-values").setText("" + true);
        viewer.addElement("show-comments").setText("" + true);

        return viewer;
    }

    private Element createViewElement(String name, int posX, int posY, int w, int h) {
        Element view = new DefaultElement(name);
        Element position = new DefaultElement("position");
        Element x = new DefaultElement("x");
        x.setText("" + posX);
        Element y = new DefaultElement("y");
        y.setText("" + posY);
        position.add(x);
        position.add(y);

        Element dimension = new DefaultElement("dimension");
        Element width = new DefaultElement("width");
        width.setText("" + w);
        Element height = new DefaultElement("height");
        height.setText("" + h);
        dimension.add(width);
        dimension.add(height);

        view.add(position);
        view.add(dimension);

        return view;
    }

    private Document readDocument(URL url) {
        Document document = null;

        try {
            SAXReader reader = new SAXReader(ExchangerDocumentFactory.getInstance(), false);
            reader.setStripWhitespaceText(true);
            reader.setMergeAdjacentText(true);

            document = reader.read(url);
        } catch (Exception e) {
            // The first time starting up the application...
        }

        return document;
    }
} 
