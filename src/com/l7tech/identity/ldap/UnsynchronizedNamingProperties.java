/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.identity.ldap;

import javax.naming.Context;
import java.util.*;
import java.util.logging.Logger;
import java.io.*;

/**
 * An unsynchronized version of Properties suitable only for JNDI uses. All mutators must be called
 * before all accessors.
 * 
 * To enforce this, you must call {@link #lock()} after you're done setting properties.
 *
 * Based on org.jnp.interfaces.FastNamingProperties (LGPL)
 *
 * @author alex
 * @version $Revision$
 */
public class UnsynchronizedNamingProperties extends Properties {
    private Map properties = new HashMap();
    private volatile boolean locked = false;

    public UnsynchronizedNamingProperties() {
        properties.put(Context.URL_PKG_PREFIXES,ORG_APACHE_NAMING);
    }

    public void lock() {
        locked = true;
    }

    public String getProperty( String key ) {
        checkAccess();
        return (String)properties.get(key);
    }

    public String getProperty( String key, String defaultValue ) {
        checkAccess();
        String val = getProperty(key);
        if ( val == null ) val = defaultValue;
        return val;
    }

    private void checkAccess() {
        if ( !locked ) throw new IllegalStateException( "Can't call an accessor without lock()ing first" );
    }

    private void checkMutate() {
        if ( locked ) throw new IllegalStateException( "Can't call a mutator after properties lock()ed" );
    }

    public Object setProperty( String key, String value ) {
        checkMutate();
        return properties.put(key,value);
    }

    public Object put( Object key, Object value ) {
        if ( key.equals(Context.URL_PKG_PREFIXES) ) return null;
        if ( locked ) {
            if ( key.equals(Context.PROVIDER_URL) ) {
                // TODO this is worrying.  Does the connection pool legitimately need to vary the URL?
                return null;
            } else {
                logger.warning("put(\"" + key + "\", \"" + value +"\")");
            }
        }
        return properties.put(key,value);
    }

    public Object clone() {
        checkAccess();
        // TODO Is this safe?
        return this;
    }

    public boolean equals( Object o ) {
        checkAccess();
        return properties.equals(o);
    }

    public String toString() {
        checkAccess();
        return properties.toString();
    }

    public Object get( Object key ) {
        checkAccess();
        return properties.get(key);
    }

    /** Throws UnsupportedOperationException unconditionally. */
    public void load( InputStream inStream ) throws IOException {
        throw new UnsupportedOperationException();
    }

    /** Throws UnsupportedOperationException unconditionally. */
    public Enumeration propertyNames() {
        throw new UnsupportedOperationException();
    }

    /** Throws UnsupportedOperationException unconditionally. */
    public void list( PrintStream out ) {
        throw new UnsupportedOperationException();
    }

    /** Throws UnsupportedOperationException unconditionally. */
    public void list( PrintWriter out ) {
        throw new UnsupportedOperationException();
    }

    /** Throws UnsupportedOperationException unconditionally. */
    public int size() {
        throw new UnsupportedOperationException();
    }

    /** Throws UnsupportedOperationException unconditionally. */
    public void save( OutputStream out, String header ) {
        throw new UnsupportedOperationException();
    }

    /** Throws UnsupportedOperationException unconditionally. */
    public void store( OutputStream out, String header ) throws IOException {
        throw new UnsupportedOperationException();
    }

    /** Throws UnsupportedOperationException unconditionally. */
    public int hashCode() {
        throw new UnsupportedOperationException();
    }

    /** Throws UnsupportedOperationException unconditionally. */
    public void clear() {
        throw new UnsupportedOperationException();
    }

    /** Throws UnsupportedOperationException unconditionally. */
    protected void rehash() {
        throw new UnsupportedOperationException();
    }

    /** Throws UnsupportedOperationException unconditionally. */
    public boolean isEmpty() {
        throw new UnsupportedOperationException();
    }

    /** Throws UnsupportedOperationException unconditionally. */
    public boolean contains( Object value ) {
        throw new UnsupportedOperationException();
    }

    /** Throws UnsupportedOperationException unconditionally. */
    public boolean containsKey( Object key ) {
        throw new UnsupportedOperationException();
    }

    /** Throws UnsupportedOperationException unconditionally. */
    public boolean containsValue( Object value ) {
        throw new UnsupportedOperationException();
    }

    /** Throws UnsupportedOperationException unconditionally. */
    public Collection values() {
        throw new UnsupportedOperationException();
    }

    /** Throws UnsupportedOperationException unconditionally. */
    public Enumeration elements() {
        throw new UnsupportedOperationException();
    }

    /** Throws UnsupportedOperationException unconditionally. */
    public Enumeration keys() {
        throw new UnsupportedOperationException();
    }

    /** Throws UnsupportedOperationException unconditionally. */
    public void putAll( Map t ) {
        throw new UnsupportedOperationException();
    }

    /** Throws UnsupportedOperationException unconditionally. */
    public Set entrySet() {
        throw new UnsupportedOperationException();
    }

    /** Throws UnsupportedOperationException unconditionally. */
    public Set keySet() {
        throw new UnsupportedOperationException();
    }


    /** Throws UnsupportedOperationException unconditionally. */
    public Object remove( Object key ) {
        throw new UnsupportedOperationException();
    }

    public static final String ORG_APACHE_NAMING = "org.apache.naming";
    private Logger logger = Logger.getLogger(getClass().getName());
}
