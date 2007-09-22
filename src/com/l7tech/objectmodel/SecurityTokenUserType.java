package com.l7tech.objectmodel;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.PreparedStatement;
import java.sql.Types;

import org.hibernate.usertype.UserType;
import org.hibernate.HibernateException;

import com.l7tech.common.security.token.SecurityTokenType;

/**
 * A Hibernate UserType that stores SecurityTokenTypes as ints.
 *
 * @author Steve Jones
 */
public class SecurityTokenUserType implements UserType {

    //- PUBLIC

    public SecurityTokenUserType() {
    }

    public Object assemble(Serializable cached, Object owner) throws HibernateException {
        return cached;
    }

    public Object deepCopy(Object value) throws HibernateException {
        return value;
    }

    public Serializable disassemble(Object value) throws HibernateException {
        return (Serializable) value;
    }

    public boolean equals(Object value1, Object value2) throws HibernateException {
        boolean equal = false;

        if (value1 == value2) {
            equal = true;
        }

        return equal;
    }

    public int hashCode(Object value) throws HibernateException {
        return value==null ? 0 : value.hashCode();
    }

    public boolean isMutable() {
        return false;
    }

    public Object nullSafeGet(ResultSet resultSet, String[] names, Object owner) throws HibernateException, SQLException {
        if (names==null || names.length!=1) throw new HibernateException("Expected single column mapping.");
        SecurityTokenType securityTokenType = null;

        int num = resultSet.getInt(names[0]);
        boolean wasNull = resultSet.wasNull();
        if (!wasNull) {
            securityTokenType = SecurityTokenType.getByNum(num);    
        }

        return securityTokenType;
    }

    public void nullSafeSet(PreparedStatement preparedStatement, Object value, int index) throws HibernateException, SQLException {
        if (value != null) {
            SecurityTokenType securityTokenType = (SecurityTokenType) value;
            preparedStatement.setInt(index, securityTokenType.getNum());
        }
        else {
            preparedStatement.setNull(index, Types.INTEGER);
        }
    }

    public Object replace(Object original, Object target, Object owner) throws HibernateException {
        return original;
    }

    public Class returnedClass() {
        return String.class;
    }

    public int[] sqlTypes() {
        return new int[]{ Types.INTEGER };
    }

}
