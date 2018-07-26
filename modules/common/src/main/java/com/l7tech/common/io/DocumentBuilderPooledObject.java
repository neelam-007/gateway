package com.l7tech.common.io;

import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import java.io.IOException;

public class DocumentBuilderPooledObject extends DocumentBuilder implements AutoCloseable {

    private final DocumentBuilder documentBuilder;
    private final DocumentBuilderPool documentBuilderPool;

    DocumentBuilderPooledObject(DocumentBuilder documentBuilder, DocumentBuilderPool documentBuilderPool) {
        this.documentBuilder = documentBuilder;
        this.documentBuilderPool = documentBuilderPool;
    }

    @Override
    public void close() throws Exception {
        documentBuilderPool.returnObject(this);
    }

    @Override
    public Document parse(InputSource is) throws SAXException, IOException {
        return documentBuilder.parse(is);
    }

    @Override
    public boolean isNamespaceAware() {
        return documentBuilder.isNamespaceAware();
    }

    @Override
    public boolean isValidating() {
        return documentBuilder.isValidating();
    }

    @Override
    public void setEntityResolver(EntityResolver er) {
        documentBuilder.setEntityResolver(er);
    }

    @Override
    public void setErrorHandler(ErrorHandler eh) {
        documentBuilder.setErrorHandler(eh);
    }

    @Override
    public Document newDocument() {
        return documentBuilder.newDocument();
    }

    @Override
    public DOMImplementation getDOMImplementation() {
        return documentBuilder.getDOMImplementation();
    }
}
