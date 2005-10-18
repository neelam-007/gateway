package com.l7tech.server.message;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionActivationListener;
import javax.servlet.http.HttpSessionEvent;

/**
 * PolicyContextCache implementation that is backed by an HttpSession.
 *
 * @author $Author$
 * @version $Revision$
 */
public class HttpSessionPolicyContextCache extends PolicyContextCache {

    //- PUBLIC

    /**
     * Create a cache using the given session as storage. The cacheId is used
     * to allow multiple caches in a given session (e.g. multiple services).
     *
     * @param session the HTTP session object
     * @param cacheId the unique id for the cache
     */
    public HttpSessionPolicyContextCache(HttpSession session, String cacheId) {
        key = PREFIX + cacheId;
        Object dataObject = session.getAttribute(key);
        if(dataObject instanceof Map) {
            if(logger.isLoggable(Level.FINEST)) logger.finest("Extracted cache data object from session");
            data = (Map) dataObject;
        }
        else {
            if(logger.isLoggable(Level.FINEST)) logger.finest("Created new cache data object");
            data = new PurgeOnSerializeHashMap();
            session.setAttribute(key, data);
        }
    }

    //- PROTECTED

    protected void putItem(String name, Item info) {
        if(logger.isLoggable(Level.FINEST)) logger.finest("Adding item to cache: " + name);
        data.put(name, info);
    }

    protected Item getItem(String name) {
        if(logger.isLoggable(Level.FINEST)) logger.finest("Getting item from cache: " + name);
        return (Item) data.get(name);
    }

    protected void removeItem(String name) {
        if(logger.isLoggable(Level.FINEST)) logger.finest("Removing item from cache: " + name);
        data.remove(name);
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger(HttpSessionPolicyContextCache.class.getName());
    private static final String PREFIX = HttpSessionPolicyContextCache.class.getName() + ".";

    private final String key;
    private final Map data;

    /**
     * Map implementation that removes itself from the HttpSession before passivation.
     *
     * This allows us to put non-serializable objects into the session.
     */
    private class PurgeOnSerializeHashMap extends HashMap implements HttpSessionActivationListener {
        public void sessionWillPassivate(HttpSessionEvent httpSessionEvent) {
            httpSessionEvent.getSession().removeAttribute(key);
        }

        public void sessionDidActivate(HttpSessionEvent httpSessionEvent) {
        }
    }
}
