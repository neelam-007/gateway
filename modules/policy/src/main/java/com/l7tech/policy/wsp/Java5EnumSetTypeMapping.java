/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.policy.wsp;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import java.util.Iterator;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Maps an {@link EnumSet} to a String with comma-delimited enum names.
 * @author alex
 */
public class Java5EnumSetTypeMapping<ET extends Enum<ET>> extends BasicTypeMapping {
    private static final Logger logger = Logger.getLogger(Java5EnumSetTypeMapping.class.getName());

    private final Class<ET> enumClass;
    private final Method valueOfMethod;

    public Java5EnumSetTypeMapping(Class<EnumSet<ET>> clazz, Class<ET> enumClass, String externalName) {
        super(clazz, externalName);
        this.enumClass = enumClass;

        try {
            this.valueOfMethod = enumClass.getMethod("valueOf", Class.class, String.class);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e); // Can't happen
        }
    }

    private static final Pattern parser = Pattern.compile(",\\s*");

    @Override
    protected Object stringToObject(String value) throws InvalidPolicyStreamException {
        String[] strings = parser.split(value);
        if (strings.length == 0 || (strings.length == 1 && "".equals(strings[0]))) return null;
        Set<ET> set = new HashSet<ET>();
        for (String string : strings) {
            try {
                ET eval = (ET) valueOfMethod.invoke(clazz, enumClass, string);
                if (eval != null) {
                    set.add(eval);
                } else {
                    logger.warning("EnumSet string for " + enumClass.getName() + " contained unknown value " + string + "; skipping");
                }
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e); // Can't happen
            } catch (InvocationTargetException e) {
                throw new RuntimeException(e); // Can't happen
            }
        }
        return EnumSet.copyOf(set);
    }

    @Override
    protected String objectToString(Object target) {
        EnumSet<ET> set = (EnumSet<ET>) target;
        if (set.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (Iterator<ET> iterator = set.iterator(); iterator.hasNext();) {
            ET et = iterator.next();
            sb.append(et.name());
            if (iterator.hasNext()) sb.append(",");
        }
        return sb.toString();
    }
}