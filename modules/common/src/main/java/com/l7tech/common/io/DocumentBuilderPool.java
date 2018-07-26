package com.l7tech.common.io;

import com.l7tech.util.ConfigFactory;
import org.apache.commons.pool.impl.GenericObjectPool;

import javax.xml.parsers.DocumentBuilderFactory;

/**
 * Pool for DocumentBuilder objects.
 *
 * @see <a href="https://commons.apache.org/proper/commons-pool/api-1.6/org/apache/commons/pool/impl/GenericObjectPool.html">GenericObjectPool</a>
 */
class DocumentBuilderPool extends GenericObjectPool<DocumentBuilderPooledObject> {

    private static final String CONFIG_PROP_PREFIX = "com.l7tech.common.documentbuilderpool.";
    static final int DEFAULT_POOL_SIZE = 15;

    static final Config CONFIG;

    static {
        CONFIG = new Config();

        // always create new objects when needed (never fail to borrow)
        CONFIG.whenExhaustedAction = WHEN_EXHAUSTED_GROW;

        // minimum of 15 items in the pool - max pool size is irrelevant as we will always grow if necessary
        CONFIG.maxActive = ConfigFactory.getIntProperty(CONFIG_PROP_PREFIX + "maxActive", DEFAULT_POOL_SIZE);
        CONFIG.minIdle = ConfigFactory.getIntProperty(CONFIG_PROP_PREFIX + "minIdle", DEFAULT_POOL_SIZE);
        CONFIG.maxIdle = ConfigFactory.getIntProperty(CONFIG_PROP_PREFIX + "maxIdle", DEFAULT_POOL_SIZE);

        // treat pool as a stack - unused items over minimum will idle-out and be evicted
        CONFIG.lifo = ConfigFactory.getBooleanProperty(CONFIG_PROP_PREFIX + "lifo", true);

        // evict all items idle over 10 minutes, every 10 minutes, leaving the pool no smaller than the minimum size
        CONFIG.timeBetweenEvictionRunsMillis = ConfigFactory.getLongProperty(CONFIG_PROP_PREFIX + "timeBetweenEvictionRunsMillis", 10 * 60 * 1000L);
        CONFIG.numTestsPerEvictionRun = ConfigFactory.getIntProperty(CONFIG_PROP_PREFIX + "numTestsPerEvictionRun", -1);
        CONFIG.minEvictableIdleTimeMillis = ConfigFactory.getLongProperty(CONFIG_PROP_PREFIX + "minEvictableIdleTimeMillis", -1);
        CONFIG.softMinEvictableIdleTimeMillis = ConfigFactory.getLongProperty(CONFIG_PROP_PREFIX + "softMinEvictableIdleTimeMillis", 10 * 60 * 1000L);
    }

    DocumentBuilderPool(DocumentBuilderFactory docBuilderFactory) {
        this(new DocumentBuilderPoolFactory(docBuilderFactory));
    }

    private DocumentBuilderPool(DocumentBuilderPoolFactory docBuilderFactory) {
        super(docBuilderFactory, CONFIG);
        docBuilderFactory.setDocumentBuilderPool(this);
    }
}
