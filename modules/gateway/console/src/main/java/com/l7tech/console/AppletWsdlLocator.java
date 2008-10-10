package com.l7tech.console;

import org.xml.sax.InputSource;

import javax.wsdl.xml.WSDLLocator;
import java.net.URL;
import java.net.URI;
import java.net.URISyntaxException;
import java.io.InputStream;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A WSDL locator that the applet should use. This allows socket connections to be made while
 * parsing a WSDL.
 */
public class AppletWsdlLocator implements WSDLLocator {
    private static final Logger logger = Logger.getLogger(AppletWsdlLocator.class.getName());
    
    private final String uri;
    private String lastUri = null;

    /**
     * Creates a new AppletWsdlLocator object for the WSDL at the provided URI.
     * @param uri The URI of the WSDL
     */
    public AppletWsdlLocator(String uri) {
        this.uri = uri;
    }

    public String getBaseURI() {
        lastUri = uri;
        return uri;
    }

    public InputSource getBaseInputSource() {
        try {
            InputSource inputSource = new InputSource();
            inputSource.setSystemId(uri);
            URL url = new URL(uri);
            InputStream is = url.openStream();
            inputSource.setByteStream(is);
            return inputSource;
        } catch(Exception e) {
            return null;
        }
    }

    public String getLatestImportURI() {
        return lastUri;
    }

    public InputSource getImportInputSource(String parentLocation, String importLocation) {
        InputSource is = null;
        try {
            URI resolvedUri;
            lastUri = importLocation; // ensure set even if not valid

            if (parentLocation != null) {
                URI base = new URI(parentLocation);
                URI relative = new URI(importLocation);
                resolvedUri = base.resolve(relative);
            }
            else {
                resolvedUri = new URI(importLocation);
            }

            lastUri = resolvedUri.toString();
            URL url = resolvedUri.toURL();
            InputStream iStream = url.openStream();

            logger.log(Level.FINE, "Resolving WSDL uri '"+resolvedUri.toString()+"', document found '"+(is != null)+"'.");

            if (is != null) {
                is = new InputSource();
                is.setSystemId(lastUri);
                is.setByteStream(iStream);
            }
        }
        catch (URISyntaxException use) {
            // of interest?
        }
        catch (IOException ioe) {
            // of interest?
        }
        return is;
    }

    public void close() {
        //?
    }
}
