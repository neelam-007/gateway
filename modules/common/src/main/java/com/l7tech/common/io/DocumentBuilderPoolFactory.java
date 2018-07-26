package com.l7tech.common.io;


import org.apache.commons.pool.BasePoolableObjectFactory;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Pool factory for DocumentBuilder objects.
 */
class DocumentBuilderPoolFactory extends BasePoolableObjectFactory<DocumentBuilderPooledObject> {

    private static final Logger LOGGER = Logger.getLogger(DocumentBuilderPoolFactory.class.getName());

    private final DocumentBuilderFactory docBuilderFactory;
    private DocumentBuilderPool documentBuilderPool;

    DocumentBuilderPoolFactory(DocumentBuilderFactory docBuilderFactory) {
        this.docBuilderFactory = docBuilderFactory;
    }

    @Override
    public DocumentBuilderPooledObject makeObject() {
        LOGGER.log(Level.FINE, "Creating a new DocumentBuilder object");
        if (documentBuilderPool == null) {
            throw new DocumentBuilderAvailabilityException("Could not create new DocumentBuilder. Must first set documentBuilderPool"); // can't happen
        }
        try {
            return new DocumentBuilderPooledObject(XmlUtil.configureDocumentBuilder(docBuilderFactory.newDocumentBuilder()), documentBuilderPool);
        } catch (ParserConfigurationException e) {
            throw new DocumentBuilderAvailabilityException("Could not create new DocumentBuilder", e); // can't happen
        }
    }

    @Override
    public void destroyObject(DocumentBuilderPooledObject documentBuilder) {
        // do nothing, nothing to destroy
        LOGGER.log(Level.FINE, "Destroying DocumentBuilder object");
    }

    void setDocumentBuilderPool(DocumentBuilderPool documentBuilderPool) {
        this.documentBuilderPool = documentBuilderPool;
    }
}
