/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.transport.jms;

import org.hibernate.usertype.UserType;
import org.hibernate.HibernateException;

import java.io.ObjectStreamException;
import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * @author alex
 * @version $Revision$
 */
public class JmsReplyType implements Serializable {

    //- PUBLIC

    public static final JmsReplyType AUTOMATIC = new JmsReplyType( 0, "Automatic" );
    public static final JmsReplyType NO_REPLY = new JmsReplyType( 1, "No reply" );
    public static final JmsReplyType REPLY_TO_OTHER = new JmsReplyType( 2, "Reply to other" );

    public int getNum() { return _num; }
    public String getName() { return _name; }
    public String toString() {
        return "<JmsReplyType num=\"" + _num + "\" name=\"" + _name + "\"/>";
    }

    /**
     * Class that supports hibernate mapping of JmsReplyTypes
     */
    public static class Mapper implements UserType {

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
                equal = jrt1._num==jrt2._num;
            }

            return equal;
        }

        public int hashCode(Object x) throws HibernateException {
            return x.hashCode();
        }

        public Object nullSafeGet(ResultSet resultSet, String[] strings, Object object) throws HibernateException, SQLException {
            int value = resultSet.getInt(strings[0]);
            if(value<0 || value>=VALUES.length) value=0;
            return VALUES[value];
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

    //- PRIVATE

    private static final JmsReplyType[] VALUES = { AUTOMATIC, NO_REPLY, REPLY_TO_OTHER };

    private final int _num;
    private final String _name;

    private JmsReplyType( int num, String name ) {
        _num = num;
        _name = name;
    }

    private Object readResolve() throws ObjectStreamException {
        return VALUES[_num];
    }
}
