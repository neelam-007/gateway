package com.l7tech.misc;

import java.util.*;
import java.io.*;

/**
 * @author alex
 */
public class Locator {
    public static final String PROPERTIES_RESOURCE_PATH_PROPERTY = "com.l7tech.misc.locator.propertiespath";
    public static final String DEFAULT_PROPERTIES_RESOURCE_PATH = "com/l7tech/misc/Locator.properties";

    public Object locate( Class interfaceClass ) {
        String interfaceClassName = interfaceClass.getName();
        String implClassName = (String)_properties.get( interfaceClassName );
        Object impl = _classnameToInstanceMap.get( implClassName );

        if ( impl == null ) {
            Class implClass;
            try {
                implClass = Class.forName( implClassName );
            } catch ( ClassNotFoundException cnfe ) {
                throw new RuntimeException( "Couldn't load implementation class " + implClassName + " for interface " + interfaceClassName );
            }

            if ( !interfaceClass.isAssignableFrom( implClass ) )
                throw new RuntimeException( "Implementation class " + implClassName + " is not compatible with the specified interface class " + interfaceClassName + "!");

            try {
                impl = implClass.newInstance();
            } catch (InstantiationException e) {
                e.printStackTrace();
                throw new RuntimeException( "Couldn't instantiate a " + implClassName + " for interface " + interfaceClassName + "! (" + e.toString() + ")" );
            } catch (IllegalAccessException e) {
                e.printStackTrace();
                throw new RuntimeException( "Couldn't instantiate a " + implClassName + " for interface " + interfaceClassName + "! (" + e.toString() + ")" );
            }

            _classnameToInstanceMap.put( interfaceClassName, impl );
        }
        return impl;
    }

    public static Locator getInstance() {
        if ( _instance == null ) _instance = new Locator();
        return _instance;
    }

    protected static Locator _instance;

    protected Locator() {
        String propertiesFileResourcePath = System.getProperty( PROPERTIES_RESOURCE_PATH_PROPERTY );

        if ( propertiesFileResourcePath == null || propertiesFileResourcePath.length() == 0 )
            propertiesFileResourcePath = DEFAULT_PROPERTIES_RESOURCE_PATH;

        InputStream is = getClass().getClassLoader().getResourceAsStream( propertiesFileResourcePath );
        if ( is == null )
            throw new RuntimeException( "Couldn't find Locator.properties file!  Check your classpath!" );
        else {
            // TODO: Defaults?
            _properties = new Properties();
            try {
                _properties.load( is );
            } catch ( IOException ioe ) {
                throw new RuntimeException( "An IOException occurred while trying to load Locator.properties file: " + ioe.toString() );
            }
        }
        _classnameToInstanceMap = new WeakHashMap();
    }

    protected Properties _properties;
    protected Map _classnameToInstanceMap;
}
