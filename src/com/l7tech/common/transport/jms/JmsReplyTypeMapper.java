package com.l7tech.common.transport.jms;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.PreparedStatement;
import java.io.Serializable;

import org.hibernate.usertype.UserType;
import org.hibernate.HibernateException;

/**
 * Class that supports hibernate mapping of JmsReplyTypes
 */
public class JmsReplyTypeMapper implements UserType {

    public int[] sqlTypes() {
        return new int[]{java.sql.Types.SMALLINT};
    }

    public Class returnedClass() {
        return JmsReplyType.class;
    }

    public boolean equals(Object object, Object object1) throws HibernateException {
        boolean equal = false;

        if(object==object1) {
            equal = true;
        }
        else if(object instanceof JmsReplyType && object1 instanceof JmsReplyType) {
            JmsReplyType jrt1 = (JmsReplyType) object;
            JmsReplyType jrt2 = (JmsReplyType) object1;
            equal = jrt1.equals(jrt2);
        }

        return equal;
    }

    public int hashCode(Object x) throws HibernateException {
        return x.hashCode();
    }

    public Object nullSafeGet(ResultSet resultSet, String[] strings, Object object) throws HibernateException, SQLException {
        int value = resultSet.getInt(strings[0]);
        if(value<0 || value>=JmsReplyType.VALUES.length) value=0;
        return JmsReplyType.VALUES[value];
    }

    public void nullSafeSet(PreparedStatement preparedStatement, Object object, int i) throws HibernateException, SQLException {
        int value = 0;
        if(object!=null) value = ((JmsReplyType) object).getNum();
        preparedStatement.setInt(i, value);
    }

    public Object deepCopy(Object object) throws HibernateException {
        return object;
    }

    public boolean isMutable() {
        return false;
    }

    public Serializable disassemble(Object value) throws HibernateException {
        return (Serializable)value;
    }

    public Object assemble(Serializable cached, Object owner) throws HibernateException {
        return cached;
    }

    public Object replace(Object original, Object target, Object owner) throws HibernateException {
        return original;
    }
}
