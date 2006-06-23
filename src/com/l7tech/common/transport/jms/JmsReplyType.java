/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.transport.jms;

import java.io.ObjectStreamException;
import java.io.Serializable;

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

    //- PRIVATE

    static final JmsReplyType[] VALUES = { AUTOMATIC, NO_REPLY, REPLY_TO_OTHER };

    private final int _num;
    private final String _name;

    private JmsReplyType( int num, String name ) {
        _num = num;
        _name = name;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final JmsReplyType that = (JmsReplyType) o;

        if (_num != that._num) return false;

        return true;
    }

    public int hashCode() {
        return _num;
    }

    private Object readResolve() throws ObjectStreamException {
        return VALUES[_num];
    }
}
