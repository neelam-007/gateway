/**
 * Copyright (C) 2008 Layer 7 Technologies Inc.
 */
package com.l7tech.wsdl;

import com.l7tech.util.Functions;
import com.l7tech.util.ResourceUtils;
import org.xml.sax.InputSource;

import javax.wsdl.xml.WSDLLocator;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * A minimally useful WSDLLocator that lets you fill in your own URL retrieval strategy.
 *
 * This class does no caching of anything but the root WSDL URL; the provided URLGetter can feel free to do its own
 * caching, set up a custom socket factory, etc.
 *
 * @author alex
 */
public class PrettyGoodWSDLLocator implements WSDLLocator {
    private final URL rootWsdlUrl;
    private final Functions.UnaryThrows<InputSource, URL, IOException> urlGetter;
    private final List<InputSource> inputSources = new ArrayList<InputSource>();

    private String lastImportLocation;

    private volatile boolean closed = false; // Volatile because it's checked outside sync

    /**
     * @param rootWsdlUrl the URL of the root WSDL
     * @param urlGetter a function that retrieves an InputSource from a URL
     */
    public PrettyGoodWSDLLocator(URL rootWsdlUrl, Functions.UnaryThrows<InputSource, URL, IOException> urlGetter) {
        this.rootWsdlUrl = rootWsdlUrl;
        this.urlGetter = urlGetter;
    }

    /**
     * @throws RuntimeException to propagate the URLGetter's IOExceptions
     */
    @Override
    public InputSource getBaseInputSource() {
        checkClosed();
        try {
            final InputSource baseSource = urlGetter.call(rootWsdlUrl);
            synchronized (this) {
                inputSources.add(baseSource);
            }
            return baseSource;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @throws RuntimeException to propagate MalformedURLExceptions as well as the URLGetter's IOExceptions
     */
    @Override
    public InputSource getImportInputSource(String parentLocation, String importLocation) {
        checkClosed();
        try {
            URL parentUrl = new URL(parentLocation);
            URL importUrl = new URL(parentUrl, importLocation);

            final InputSource inputSource = urlGetter.call(importUrl);

            final String surly = importUrl.toString();
            inputSource.setSystemId(surly);

            synchronized (this) {
                inputSources.add(inputSource);
                this.lastImportLocation = surly;
            }
            return inputSource;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getBaseURI() {
        checkClosed();
        return rootWsdlUrl.toString();
    }

    @Override
    public String getLatestImportURI() {
        checkClosed();
        synchronized (this) {
            return lastImportLocation;
        }
    }

    @Override
    public synchronized void close() {
        for (InputSource is : inputSources) {
            ResourceUtils.closeQuietly(is.getByteStream());
            ResourceUtils.closeQuietly(is.getCharacterStream());
        }
        inputSources.clear();
        closed = true;
    }

    private void checkClosed() {
        if (closed) throw new IllegalStateException("WSDLLocator has already been closed");
    }
}
