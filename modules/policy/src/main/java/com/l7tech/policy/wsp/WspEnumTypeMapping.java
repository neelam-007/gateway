/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.policy.wsp;

import com.l7tech.util.EnumTranslator;

import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;

/**
 * Type mapping for enum-style classes.  Will automatically generate mappings for any enum-style class that
 * has a static method getEnumTranslator that returns an instance of {@link EnumTranslator}.
 */
public class WspEnumTypeMapping extends BasicTypeMapping {
    private final EnumTranslator enumTranslator;

    /**
     * Create a type mapping for the specified enum-style class,
     * which must contain a static method getEnumTranslator() that returns an instance of EnumTranslator.
     * @throws IllegalArgumentException if the specified class has no static method getEnumTranslator().
     */
    public WspEnumTypeMapping(Class clazz, String externalName) throws IllegalArgumentException {
        super(clazz, externalName);
        try {
            Method xlatGetter = clazz.getMethod("getEnumTranslator", new Class[0]);
            Object ret = xlatGetter.invoke(null, new Object[0]);
            if (!(ret instanceof EnumTranslator))
                throw new IllegalArgumentException("getEnumTranslator static method of class " +
                        clazz.getName() + " returned unexpected value: " + ret);
            enumTranslator = (EnumTranslator)ret;
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException(e);
        } catch (IllegalAccessException e) {
            throw new IllegalArgumentException(e);
        } catch (InvocationTargetException e) {
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * Create a type mapping for the specified enum-style class,
     * using the specified EnumTranslator.
     *
     * @param clazz
     * @param externalName
     * @param translator  the translator to use, instead of trying to locate it using reflection.
     */
    public WspEnumTypeMapping(Class clazz, String externalName, EnumTranslator translator) {
        super(clazz, externalName);
        this.enumTranslator = translator;
    }

    protected String objectToString(Object in) {
        try {
            return enumTranslator.objectToString(in);
        } catch (ClassCastException e) {
            throw new IllegalArgumentException(e);
        }
    }

    protected Object stringToObject(String in) throws InvalidPolicyStreamException {
        try {
            return enumTranslator.stringToObject(in);
        } catch (IllegalArgumentException e) {
            throw new InvalidPolicyStreamException(e);
        }
    }
}
