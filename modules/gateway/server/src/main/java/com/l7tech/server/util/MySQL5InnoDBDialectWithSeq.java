package com.l7tech.server.util;

import org.hibernate.MappingException;
import org.hibernate.dialect.MySQL5InnoDBDialect;

/**
 * Extension of MySQL 5.x InnoDB dialect that adds "sequence" support for ID generation.
 */
public class MySQL5InnoDBDialectWithSeq extends MySQL5InnoDBDialect {

    /**
     * Ignores the name and returns the next value from a global sequence
     */
    @Override
    public String getSequenceNextValString( final String sequenceName ) throws MappingException {
        return "select next_hi()";
    }

    /**
     * http://opensource.atlassian.com/projects/hibernate/browse/HHH-3940
     */
    @Override
    public boolean hasSelfReferentialForeignKeyBug() {
        return false;
    }
}