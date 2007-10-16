/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.policy.wsp;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * @author alex
 */
public class Java5EnumTypeMapping extends BasicTypeMapping {
    public Java5EnumTypeMapping(Class<? extends Enum> clazz, String externalName) {
        super(clazz, externalName);
    }

    @Override
    protected Object stringToObject(String value) throws InvalidPolicyStreamException {
        try {
            Method valueOfMethod = clazz.getMethod("valueOf", Class.class, String.class);
            return valueOfMethod.invoke(clazz, clazz, value);
        } catch (IllegalArgumentException e) {
            throw new InvalidPolicyStreamException("Couldn't find Enum value for " + value + " in " + clazz.getSimpleName(), e);
        } catch (NoSuchMethodException e) {
            throw new InvalidPolicyStreamException("Couldn't find " + clazz.getSimpleName() +".valueOf() method", e);
        } catch (InvocationTargetException e) {
            throw new InvalidPolicyStreamException("Couldn't invoke " + clazz.getSimpleName() +".valueOf() method", e);
        } catch (IllegalAccessException e) {
            throw new InvalidPolicyStreamException("Couldn't invoke " + clazz.getSimpleName() +".valueOf() method", e);
        }
    }

    @Override
    protected String objectToString(Object target) {
        return ((Enum) target).name();
    }
}
