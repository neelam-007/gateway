package com.l7tech.server.util;

import com.l7tech.objectmodel.Goid;
import org.hibernate.HibernateException;
import org.hibernate.usertype.UserType;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

/**
 * This is used by hibernate to properly convert the varbinary goid representation in the database to a Goid object.
 *
 * @author Victor Kazakov
 */
public class GoidType implements UserType {

    public GoidType() {
    }

    @Override
    public int[] sqlTypes() {
        return new int[]{Types.VARBINARY};
    }

    @Override
    public Class returnedClass() {
        return Goid.class;
    }

    @Override
    public boolean equals(Object x, Object y) throws HibernateException {
        boolean equal = false;

        if (x == y) {
            equal = true;
        } else if (x instanceof Goid &&
                y instanceof Goid) {
            equal = x.equals(y);
        }
        return equal;
    }

    @Override
    public int hashCode(Object x) throws HibernateException {
        return x == null ? 0 : x.hashCode();
    }

    @Override
    public Object nullSafeGet(ResultSet resultSet, String[] names, Object owner) throws HibernateException, SQLException {
        if (names == null || names.length != 1) throw new HibernateException("Expected single column mapping.");
        byte[] goidBytes = resultSet.getBytes(names[0]);
        return goidBytes != null ? new Goid(goidBytes) : null;
    }

    @Override
    public void nullSafeSet(PreparedStatement preparedStatement, Object value, int index) throws HibernateException, SQLException {
        if (value != null) {
            final Goid goid;
            if (value instanceof Goid) {
                goid = (Goid) value;
            } else if (value instanceof String) {
                goid = Goid.parseGoid((String) value);
            } else {
                throw new IllegalArgumentException("Invalid Id type. Must be either Goid or String. Given: " + value.getClass().getName());
            }
            byte[] data = goid.getBytes();
            preparedStatement.setBytes(index, data);
        }
        else {
            preparedStatement.setNull(index, Types.BINARY);
        }
    }

    @Override
    public Object deepCopy(Object value) throws HibernateException {
        return value == null ? null : ((Goid) value).clone();
    }

    @Override
    public boolean isMutable() {
        return false;
    }

    @Override
    public Serializable disassemble(Object value) throws HibernateException {
        return (Goid) value;
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
