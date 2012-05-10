package com.l7tech.server.ems.util;

import org.hibernate.dialect.DerbyDialect;

/**
 * Dialect to fix the following issue:
 *
 * http://opensource.atlassian.com/projects/hibernate/browse/HHH-6205
 */
public class ExtendedDerbyDialect extends DerbyDialect {
    @Override
    public String getQuerySequencesString() {
        String ret = super.getQuerySequencesString();
        if ( ret == null && supportsSequences() )
            return "select sequencename from sys.syssequences";
        return null;
    }
}
