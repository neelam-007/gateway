/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 * Copyright (C) 2005 http://www.hibernate.org/272.html
 */
package com.l7tech.server.util;

import org.hibernate.HibernateException;
import org.hibernate.usertype.ParameterizedType;
import org.hibernate.usertype.UserType;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;
import java.util.EnumSet;
import java.util.Arrays;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Hibernate user mapping for EnumSet to a comma-delimited string.
 */
public class GenericEnumSetUserType implements UserType, ParameterizedType {

    private static final Logger logger = Logger.getLogger(GenericEnumSetUserType.class.getName());

    private static final int[] SQL_TYPES = new int[] {java.sql.Types.VARCHAR};
    private Class<? extends Enum> enumClass;
    private Method valueOfMethod;
    private boolean ignoreInvalidEnumArguments = false;

    @Override
    public void setParameterValues(Properties parameters) {
        String enumClassName = parameters.getProperty("enumClass");
        try {
            enumClass = Class.forName(enumClassName).asSubclass(Enum.class);
        } catch (ClassNotFoundException cfne) {
            throw new HibernateException("Enum class not found", cfne);
        }

        try {
            valueOfMethod = enumClass.getMethod("valueOf", String.class);
        } catch (Exception e) {
            throw new HibernateException("Failed to obtain valueOf method", e);
        }

        // defaults to false if param is not present
        ignoreInvalidEnumArguments = Boolean.valueOf(parameters.getProperty("ignoreInvalidEnumArguments"));
    }

    @Override
    public Class returnedClass() {
        return EnumSet.class;
    }

    @Override
    public Object nullSafeGet(ResultSet rs, String[] names, Object owner) throws HibernateException, SQLException {
        if (names.length != 1)
            throw new HibernateException("Exactly one column name expected; got: " + Arrays.toString(names));

        EnumSet result = EnumSet.noneOf(enumClass);

        String sqlValue = rs.getString(names[0]);
        if (sqlValue == null || sqlValue.isEmpty()) return result;

        String[] stringValues = sqlValue.split(",");
        for (String stringValue : stringValues) {
            try {
                result.add(valueOfMethod.invoke(null, stringValue));
            } catch (IllegalArgumentException e) {
                String errMsg = "Invalid enum value: " + stringValue;
                if (ignoreInvalidEnumArguments)
                    logger.log(Level.WARNING, errMsg);
                else
                    throw new HibernateException(errMsg, e);
            } catch (IllegalAccessException e1) {
                throw new HibernateException(e1);
            } catch (InvocationTargetException e1) {
                throw new HibernateException(e1);
            }
        }

        return result;
    }

    @Override
    public void nullSafeSet(PreparedStatement st, Object value, int index) throws HibernateException, SQLException {
        if (value == null) {
            st.setNull(index, java.sql.Types.VARCHAR);
        } else {
            // handle any Set<Enum>, e.g. Collections.unmodifiableSet(Enum);
            Set<? extends Enum> enumSet = (Set<? extends Enum>) value;
            StringBuffer sb = new StringBuffer();
            for (Object o : enumSet) {
                sb.append(((Enum)o).name().toUpperCase()).append(",");
            }
            if (sb.length() > 0) sb.deleteCharAt(sb.length()-1);

            st.setString(index, sb.toString());
        }
    }

    @Override
    public int[] sqlTypes() {
        return SQL_TYPES;
    }

    @Override
    public Object assemble(Serializable cached, Object owner) throws HibernateException {
        return cached;
    }

    @Override
    public Object deepCopy(Object value) throws HibernateException {
        return value;
    }

    @Override
    public Serializable disassemble(Object value) throws HibernateException {
        return (Serializable) value;
    }

    @Override
    public boolean equals(Object x, Object y) throws HibernateException {
        return x == y;
    }

    @Override
    public int hashCode(Object x) throws HibernateException {
        return x.hashCode();
    }

    @Override
    public boolean isMutable() {
        return false;
    }

    @Override
    public Object replace(Object original, Object target, Object owner) throws HibernateException {
        return original;
    }
}