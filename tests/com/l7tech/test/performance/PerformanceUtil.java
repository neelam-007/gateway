/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.test.performance;

import com.sun.japex.report.TestSuiteReport;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;

/**
 * Miscellaneous utility functions for performance test framework.
 *
 * @author rmak
 */
public class PerformanceUtil {

    /**
     * @return true if given file is a Japex test report
     */
    public static boolean isJapexReport(File f) throws ParserConfigurationException, IOException {
        try {
            final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);
            final Document doc = dbf.newDocumentBuilder().parse(f);
            final String nodeName = doc.getDocumentElement().getNodeName();
            final String namespaceUri = doc.getDocumentElement().getNamespaceURI();
            return nodeName.equals("testSuiteReport") && namespaceUri.equals("http://www.sun.com/japex/testSuiteReport");
        } catch (SAXException e) {
            return false;
        }
    }

    /**
     * @return the value of a parameter in a Japex test report; an empty string if not found
     */
    public static String getParameter(final TestSuiteReport report, final String paramName) {
        if (report == null || paramName == null) return "";

        String tagName = paramName;
        if (tagName.startsWith("japex.")) {
            tagName = tagName.substring(6);
        }

        String value = report.getParameters().get(tagName);
        if (value == null) value = "";
        return value;
    }

    /**
     * Insert a string into a text at the first marker found in the text.
     * The marker remains after the inserted string.
     *
     * @param text      text to modify
     * @param marker    marker inside the text
     * @param str       string to insert
     */
    public static void insertAtMarker(final StringBuilder text, final String marker, final String str) {
        final int index = text.indexOf(marker);
        if (index != -1) {
            text.replace(index, index, str);
        }
    }

    /**
     * Replaces all occurrences of the given marker by a given string.
     *
     * @param text      text to modify
     * @param marker    marker to be replaced
     * @param str       replacement string
     */
    public static void replaceMarkerAll(final StringBuilder text, final String marker, final String str) {
        int next = 0;       // position to start next search for marker
        int found = -1;
        while ((found = text.indexOf(marker, next)) != -1) {
            text.replace(found, found + marker.length(), str);
            next = found + str.length();
        }
    }

    /**
     * Reads a resource into a string.
     *
     * @param clazz         the class loader of this class will be used to load
     *                      the resource (see {@link Class#getResourceAsStream(String)})
     * @param resourceName  name of resource
     * @return the resource as string
     * @throws IOException if I/O error occurs
     */
    public static StringBuilder readResource(final Class clazz, final String resourceName) throws IOException {
        Reader ir = null;
        try {
            final InputStream is = clazz.getResourceAsStream(resourceName);
            if (is == null) {
                throw new IOException("Resource not found: " + resourceName);
            }
            ir = new BufferedReader(new InputStreamReader(is));
            final StringBuilder sb = new StringBuilder();
            int c;
            while ((c = ir.read()) != -1) {
                sb.append((char)c);
            }
            return sb;
        } finally {
            if (ir != null) {
                try {
                    ir.close();
                } catch (IOException e) {
                    // ignore
                }
            }
        }
    }

    /**
     * Copies a resource into a file.
     *
     * @param clazz         the class loader of this class will be used to load
     *                      the resource (see {@link Class#getResourceAsStream(String)})
     * @param resourceName  name of resource
     * @param filePath      path of destination file
     * @throws IOException if I/O error occurs
     */
    public static void copyResource(final Class clazz, final String resourceName, final String filePath) throws IOException {
        InputStream is = null;
        OutputStream os = null;
        try {
            final InputStream in = clazz.getResourceAsStream(resourceName);
            if (in == null) {
                throw new IOException("Resource not found: " + resourceName);
            }
            is = new BufferedInputStream(in);
            os = new BufferedOutputStream(new FileOutputStream(filePath));
            int c;
            while ((c = is.read()) != -1) {
                os.write(c);
            }
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    // ignore
                }
            }
            if (os != null) {
                try {
                    os.close();
                } catch (IOException e) {
                    // ignore
                }
            }
        }
    }
}
