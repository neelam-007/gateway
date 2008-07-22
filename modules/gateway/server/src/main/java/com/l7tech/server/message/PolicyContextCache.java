package com.l7tech.server.message;

/**
 * <p>Cache for context information that has a longer lifetime than a single request.</p>
 *
 * <p>Note that the expiry times are a maximum limit, there is no guarantee that cached
 * data will be retained between requests.</p>
 *
 * @author $Author$
 * @version $Revision$
 */
public abstract class PolicyContextCache {

    /**
     * Put an item into the cache with the given meta-data.
     *
     * @param name the unique name for the object
     * @param value the object to cache
     * @param info the caching guidelines
     */
    public void put(String name, Object value, Info info) {
        if(name==null) throw new IllegalArgumentException("name must not be null");
        if(info==null) throw new IllegalArgumentException("info must not be null");

        // cache if not expired
        if(info.expires<=0 || info.expires > System.currentTimeMillis()) {
            if(value==null) throw new IllegalArgumentException("value must not be null");
            putItem(name, newItem(name, value, info));
        }
        else {
            removeItem(name);
        }
    }

    /**
     * Get an object from the cache.
     *
     * @param name the name of the object
     * @return the object or null if expired / not found
     */
    public Object get(String name) {
        return get(name, null);
    }

    /**
     * Get an object from the cache using the given default.
     *
     * @param name the name of the object
     * @param defaultValue value to return if expired / not found.
     * @return the object or the default value
     */
    public  Object get(String name, Object defaultValue) {
        Object result = null;
        Item item = getItem(name);

        if(item!=null) {
            if(item.info.expires<=0 || item.info.expires > System.currentTimeMillis()) {
                result = item.value;
            }
            else { // expired so remove
                removeItem(name);
            }
        }
        else {
            result = defaultValue;
        }

        return result;
    }

    /**
     * Metadata that describes the policy of the cached data. This is info
     * such as the expiry time and (potentially, if implemented in the future)
     * if the information should be replicated across the cluster.
     */
    public static class Info {
        private final long expires;

        /**
         * Create an Info with the given expiry time.
         *
         * @param expiryTime
         */
        public Info(long expiryTime) {
            this.expires = expiryTime;
        }
    }

    //- PROTECTED

    /**
     * Implement this method for cache storage, no checking is required.
     */
    protected abstract void putItem(String name, Item info);

    /**
     * Implement this method for cache retrieval no checking is required.
     */
    protected abstract Item getItem(String name);

    /**
     * Implement this method for cache removal.
     */
    protected abstract void removeItem(String name);

    /**
     * This factory method may be overridden if a subclass of Item is required.
     *
     * @param name the name of the cached item
     * @param value the cached object
     * @param info the cache descriptor
     * @return the new item
     */
    protected Item newItem(String name, Object value, Info info) {
        return new Item(name, value, info);
    }

    /**
     * "Internal" cached object representation.
     */
    protected class Item {
        private final String name;
        private final Object value;
        private final Info info;

        protected Item(String name, Object value, Info info) {
            this.name = name;
            this.value = value;
            this.info = info;
        }
    }
}
