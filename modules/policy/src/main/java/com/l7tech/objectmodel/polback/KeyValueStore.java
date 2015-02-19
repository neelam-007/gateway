package com.l7tech.objectmodel.polback;

/**
 * A policy-backed service interface that implements a key-value store.
 */
@PolicyBacked
public interface KeyValueStore {

    /**
     * Look up a value from the key-value store.
     *
     * @param key name of the value to retrieve.  Required.
     * @return the value, or null.
     */
    @PolicyBackedMethod( singleResult = @PolicyParam( "value" ))
    String get( @PolicyParam( "key" ) String key );

    /**
     * Store a value into the key-value store, overwriting any previous value.
     *
     * @param key the name of the value to store.  Required.
     * @param value the value.  Required.
     */
    void put( @PolicyParam( "key" ) String key,
              @PolicyParam( "value" ) String value);
}
