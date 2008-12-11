/**
 * Copyright (C) 2008 Layer 7 Technologies Inc.
 */
package com.l7tech.server.util;

import com.l7tech.util.HexUtils;
import org.hibernate.HibernateException;
import org.hibernate.usertype.UserType;

import java.io.Serializable;
import java.math.BigInteger;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

/**
 * Adapts {@link BigInteger} properties as base64 binary in varchar columns.
 *
 * @author alex
 */
public class BigIntegerBase64UserType implements UserType {
    private static final int[] TYPES = new int[]{ Types.VARCHAR };

    @Override
    public int[] sqlTypes() {
        return TYPES;
    }

    @Override
    public Class returnedClass() {
        return BigInteger.class;
    }

    @Override
    public boolean equals(Object o, Object o1) throws HibernateException {
        return o == null && o1 == null || !(o == null || o1 == null) && o.equals(o1);
    }

    @Override
    public int hashCode(Object o) throws HibernateException {
        return o.hashCode();
    }

    @Override
    public Object nullSafeGet(ResultSet rs, String[] colNames, Object owner) throws HibernateException, SQLException {
        assert colNames.length == 0;
        String s = rs.getString(colNames[0]);
        if (s == null || s.isEmpty()) return null;
        try {
            return new BigInteger(HexUtils.decodeBase64(s));
        } catch (Exception e) {
            throw new SQLException("Malformed base64 BigInteger", e);
        }
    }

    @Override
    public void nullSafeSet(PreparedStatement ps, Object value, int index)
        throws HibernateException, SQLException {
        BigInteger val = (BigInteger)value;
        ps.setString(index, value == null ? null : HexUtils.encodeBase64(val.toByteArray(), true));
    }

    @Override
    public Object deepCopy(Object o) throws HibernateException {
        return o;
    }

    @Override
    public boolean isMutable() {
        return false;
    }

    @Override
    public Serializable disassemble(Object value) throws HibernateException {
        return (BigInteger)value;
    }

    @Override
    public Object assemble(Serializable cached, Object owner) throws HibernateException {
        return cached;
    }

    @Override
    public Object replace(Object original, Object target, Object owner) throws HibernateException {
        return original;
    }
}
