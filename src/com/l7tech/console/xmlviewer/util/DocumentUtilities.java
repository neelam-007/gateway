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
package com.l7tech.console.xmlviewer.util;

import com.l7tech.console.xmlviewer.ExchangerDocumentFactory;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXParseException;

import javax.swing.*;
import java.io.ByteArrayInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Utilities for reading and writing of XML documents.
 *
 * @author Edwin Dankert <edankert@cladonia.com>
 * @version	$Revision$, $Date$
 */
public class DocumentUtilities {
    private static final boolean DEBUG = false;
    private static final EntityResolver DUMMY_RESOLVER = new DummyEntityResolver();
    private static SAXReader validatingReader = null;
    private static SAXReader nonValidatingReader = null;

    /**
     * Returns the default entity resolver, does not resolve
     * external entities!
     *
     * @return the default Entity resolver.
     */
    public static EntityResolver getEntityResolver() {
        return DUMMY_RESOLVER;
    }

    public static SAXReader getReader(boolean validate) {
        SAXReader result = null;

        if (validate) {
            if (validatingReader == null) {
                validatingReader = createReader(validate);
            }

            result = validatingReader;
        } else {
            if (nonValidatingReader == null) {
                nonValidatingReader = createReader(validate);
            }

            result = nonValidatingReader;
        }

        return result;
    }

    /**
     * Creates a new SAXReader.
     *
     * @param validate when true the reader validates the input.
     * @return the reader.
     */
    public static SAXReader createReader(boolean validate) {
        SAXReader reader = new SAXReader(ExchangerDocumentFactory.getInstance(), validate);

        reader.setStripWhitespaceText(true);
        reader.setMergeAdjacentText(true);

        try {
//			reader.getXMLReader().setFeature( "http://apache.org/xml/features/scanner/notify-char-refs", true);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (!validate) {
            reader.setIncludeExternalDTDDeclarations(false);
            reader.setIncludeInternalDTDDeclarations(false);
            reader.setEntityResolver(DUMMY_RESOLVER);

            try {
//				reader.getXMLReader().setProperty( "http://apache.org/xml/properties/internal/entity-manager", new XNGREntityManager());
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            try {
                reader.getXMLReader().setFeature("http://apache.org/xml/features/validation/schema", true);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return reader;
    }

    /**
     * Reads the document for this URL.
     *
     * @param url the URL of the document.
     * @return the Dom4J document.
     */
    public static synchronized Document readDocument(URL url, boolean validate) throws IOException, SAXParseException {
        if (DEBUG) System.out.println("DocumentUtilities.readDocument( " + url + ")");

        Document document = null;

        try {
            SAXReader reader = getReader(validate);

            document = reader.read(url);
        } catch (DocumentException e) {
            Exception x = (Exception)e.getNestedException();

            if (x instanceof SAXParseException) {
                SAXParseException spe = (SAXParseException)x;
                Exception ex = spe.getException();

                if (ex instanceof IOException) {
                    throw (IOException)ex;
                } else {
                    throw (SAXParseException)x;
                }
            } else if (x instanceof IOException) {
                throw (IOException)x;
            }
        }

        if (DEBUG) System.out.println("DocumentUtilities.readDocument( " + url + ") [" + document + "]");

        return document;
    }

    public static synchronized Document readRemoteDocument(URL url, boolean validate) throws IOException, SAXParseException {
        if (DEBUG) System.out.println("DocumentUtilities.readDocument( " + url + ")");

        Document document = null;

        try {
            SAXReader reader = getReader(validate);

            HttpURLConnection connection = (HttpURLConnection)url.openConnection();
            connection.setDefaultUseCaches(false);
            connection.setUseCaches(false);
            connection.setRequestProperty("User-Agent", "eXchaNGeR/" + System.getProperty("xngr.version") + " (http://xngr.org/)");
            connection.connect();
            InputStream stream = connection.getInputStream();

            document = reader.read(stream);
            stream.close();
            connection.disconnect();
        } catch (DocumentException e) {
            Exception x = (Exception)e.getNestedException();

            if (x instanceof SAXParseException) {
                SAXParseException spe = (SAXParseException)x;
                Exception ex = spe.getException();

                if (ex instanceof IOException) {
                    throw (IOException)ex;
                } else {
                    throw (SAXParseException)x;
                }
            } else if (x instanceof IOException) {
                throw (IOException)x;
            }
        }

        if (DEBUG) System.out.println("DocumentUtilities.readDocument( " + url + ") [" + document + "]");

        return document;
    }

    public static synchronized Document readDocument(InputSource source, boolean validate) throws IOException, SAXParseException {
        if (DEBUG) System.out.println("DocumentUtilities.readDocument( " + source + ")");

        Document document = null;

        try {
            SAXReader reader = getReader(validate);

            document = reader.read(source);
        } catch (DocumentException e) {
            Exception x = (Exception)e.getNestedException();

            if (x instanceof SAXParseException) {
                SAXParseException spe = (SAXParseException)x;
                Exception ex = spe.getException();

                if (ex instanceof IOException) {
                    throw (IOException)ex;
                } else {
                    throw (SAXParseException)x;
                }
            } else if (x instanceof IOException) {
                throw (IOException)x;
            }
        }

        return document;
    }

    /**
     * Writes the document to the location specified by the URL.
     *
     * @param document the dom4j document.
     * @param url      the URL of the document.
     */
    public static synchronized void writeDocument(Document document, URL url) throws IOException {
        if (DEBUG) System.out.println("DocumentUtilities.writeDocument( " + document + ", " + url + ")");

        OutputFormat format = new OutputFormat("  ", true, "UTF-8");
        FileOutputStream out = new FileOutputStream(url.getFile());

        XMLWriter writer = new XMLWriter(out, format);
        writer.write(document);
        writer.flush();
    }

    public static void handleParseException(SAXParseException e, JFrame frame, URL url) {
        String file = url.getFile();
        int index = file.lastIndexOf('/') + 1;

        handleParseException(e, frame, "Error Parsing Document : " + file.substring(index) + "\n");
    }

    public static void handleParseException(SAXParseException e, JFrame frame, String message) {
        int line = e.getLineNumber();
        Exception ex = e.getException();
        message = (message != null) ? message : "";

        if (ex == null) {
            JOptionPane.showMessageDialog(frame,
              message +
              "Error on line " + line + ":\n" +
              e.getMessage(),
              "Parser Error",
              JOptionPane.ERROR_MESSAGE);
        } else {
            e.printStackTrace();
            JOptionPane.showMessageDialog(frame,
              message +
              e.getMessage() + "\n" +
              ex.getMessage(),
              "Parser Error",
              JOptionPane.ERROR_MESSAGE);
        }
    }

    // A dummy entity resolver.
    private static class DummyEntityResolver implements EntityResolver {
        public InputSource resolveEntity(String publicId, String systemId) {
//			System.out.println( "resolveEntity( "+publicId+", "+systemId+")");
            return new InputSource(new ByteArrayInputStream(CommonEntities.asDTD().getBytes()));
        }
    }

}
