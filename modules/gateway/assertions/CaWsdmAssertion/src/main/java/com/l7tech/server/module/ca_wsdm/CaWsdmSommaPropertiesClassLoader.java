package com.l7tech.server.module.ca_wsdm;

import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.IteratorEnumeration;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A ClassLoader that knows how to find a virtual "WsdmSOMMA_Basic.properties" resource by creating it on the fly.
 */
public class CaWsdmSommaPropertiesClassLoader extends ClassLoader {
    protected static final Logger logger = Logger.getLogger(CaWsdmSommaPropertiesClassLoader.class.getName());

    public CaWsdmSommaPropertiesClassLoader() {
        super(null);
    }

    public InputStream getResourceAsStream(String name) {
        byte[] found = getResourceBytes(name);
        return found != null ? new ByteArrayInputStream(found) : null;
    }

    public URL findResource(String name) {
        byte[] found = getResourceBytes(name);
        if (found == null)
            return null;
        return makeResourceUrl(name, found);
    }

    protected Enumeration<URL> findResources(String name) throws IOException {
        List<URL> urls = new ArrayList<URL>();
        Enumeration<URL> resen = super.findResources(name);
        while (resen != null && resen.hasMoreElements()) {
            URL url = resen.nextElement();
            urls.add(url);
        }
        URL url = findResource(name);
        if (url != null)
            urls.add(url);
        return new IteratorEnumeration<URL>(urls.iterator());
    }

    private byte[] getResourceBytes(String name) {
        if (CaWsdmPropertiesAdaptor.SOMMA_FILENAME.equals(name)) {
            try {
                CaWsdmObserver observer = CaWsdmObserver.getInstance();
                if (observer != null) {
                    Properties sommaProps = observer.getPropertiesAdaptor().getSommaBasicProperties();
                    ByteArrayOutputStream bout = new ByteArrayOutputStream();
                    sommaProps.store(bout, CaWsdmPropertiesAdaptor.SOMMA_FILENAME);
                    return bout.toByteArray();
                }
            } catch (IOException e) {
                throw new RuntimeException(e); // can't happen
            }
        }
        return null;
    }

    private URL makeResourceUrl(String name, final byte[] bytes) {
        try {
            return new URL("fake", null, -1, name, new URLStreamHandler() {
                protected URLConnection openConnection(URL url) throws IOException {
                    return new URLConnection(url) {
                        public void connect() throws IOException { }
                        public InputStream getInputStream() throws IOException {
                            return new ByteArrayInputStream(bytes);
                        }
                    };
                }
            });
        } catch (MalformedURLException e) {
            logger.log(Level.SEVERE, "Unable to create resource URL: " + ExceptionUtils.getMessage(e), e);
            return null;
        }
    }
}
