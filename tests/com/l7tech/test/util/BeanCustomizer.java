/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.test.util;

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.IntrospectionException;

/**
 * Customize a bean for a serialization test by configuring each of its properties away from the default.
 * For Bug #1896.
 */
public class BeanCustomizer {

    /**
     * Customize the specified object, to the extent possible.
     *
     * @param object  the object to customize.  Must not be null.
     * @return the customized object.  May or may not be a new instance.  Never null.
     * @throws IntrospectionException if the object can't be introspected
     * @throws NullPointerException if object is null
     */
    public Object customize(Object object) throws IntrospectionException, IllegalAccessException, InstantiationException {
        return customize(object, object.getClass());
    }

    /**
     * Customize the specified object, to the extent possible.
     *
     * @param object  the object to customize.  May be null; in this case, a new instance will be created
     *                and returned.
     * @param type    the reference type of this object.  Must NOT be null.
     * @return the customized object.  Never null.
     */
    public Object customize(Object object, Class type) throws IntrospectionException, IllegalAccessException, InstantiationException {
        if (object == null) object = type.newInstance();

        if (object instanceof Integer) {
            return ((Integer)object) + 1;
        } else if (object instanceof Long) {
            return ((Long)object) + 1;
        } else if (object instanceof Byte) {
            return ((Byte)object) + 1;
        } else if (object instanceof Character) {
            return ((Character)object) + 1;
        } else if (object instanceof String) {
            return object + "1";
        } else if (object instanceof Float) {
            return ((Float)object) * 2 + 1;
        } else if (object instanceof Double) {
            return ((Double)object) * 2 + 1;
        } else if (object instanceof Boolean) {
            return !((Boolean)object);
        } else if (object instanceof Object[]) {
            // It's an array
            Object[] array = ((Object[])object);
            Class componentType = array.getClass().getComponentType();

            // TODO instantiate the array itself as ProperType[] rather than just Object[] ?
            Object[] newarray = new Object[array.length];

            for (int i = 0; i < newarray.length; i++) {
                newarray[i] = customize(array[i], componentType);
            }
            return newarray;
        }

        // TODO handle collections

        BeanInfo info = Introspector.getBeanInfo(type);

        // TODO handle beans

        // TODO do the actual customization
        return object;
    }
}
