/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.policy;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.composite.CompositeAssertion;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

/**
 * @author alex
 * @version $Revision$
 */
public abstract class PolicyFactory {
    protected abstract String getPackageName();
    protected abstract String getPrefix();

    public static final String PACKAGE_PREFIX = "com.l7tech.policy.assertion";

    private Constructor getConstructor( Class genericAssertionClass ) {
        Constructor ctor = (Constructor)_genericClassToConstructorMap.get( genericAssertionClass );
        if ( ctor != null ) return ctor;

        String genericAssertionClassname = genericAssertionClass.getName();
        int ppos = genericAssertionClassname.lastIndexOf(".");
        if ( ppos <= 0 ) throw new RuntimeException( "Invalid classname " + genericAssertionClassname );
        String genericPackage = genericAssertionClassname.substring(0,ppos);
        String genericName = genericAssertionClassname.substring(ppos+1);

        StringBuffer specificName = new StringBuffer( getPackageName() );

        if ( genericPackage.equals( PACKAGE_PREFIX ) ) {
            specificName.append( "." );
            specificName.append( getPrefix() );
            specificName.append( genericName );
        } else if ( genericPackage.startsWith( PACKAGE_PREFIX ) ) {
            specificName.append( genericPackage.substring( PACKAGE_PREFIX.length() ) );
            specificName.append( "." );
            specificName.append( getPrefix() );
            specificName.append( genericName );
        } else
            throw new RuntimeException( "Couldn't handle " + genericAssertionClassname );

        Class serverClass;
        try {
            serverClass = Class.forName( specificName.toString() );
            ctor = serverClass.getConstructor( new Class[] { genericAssertionClass } );
            return ctor;
        } catch ( ClassNotFoundException cnfe ) {
            throw new RuntimeException( cnfe );
        } catch ( NoSuchMethodException nsme ) {
            throw new RuntimeException( nsme );
        }
    }

    protected Object makeSpecificPolicy( Assertion rootAssertion ) {
        try {
            Class assClass = rootAssertion.getClass();

            return getConstructor( assClass ).newInstance( new Object[] { rootAssertion } );
        } catch ( InstantiationException ie ) {
            throw new RuntimeException( ie );
        } catch ( IllegalAccessException iae ) {
            throw new RuntimeException( iae );
        } catch ( InvocationTargetException ite ) {
            throw new RuntimeException( ite );
        }
    }

    public List makeCompositePolicy( CompositeAssertion compositeAssertion ) {
        Assertion ass;
        List result = new ArrayList();
        for (Iterator i = compositeAssertion.children(); i.hasNext();) {
            ass = (Assertion)i.next();
            result.add( makeSpecificPolicy( ass ) );
        }
        return result;
    }

    protected Map _genericClassToConstructorMap = new HashMap(23);
}
