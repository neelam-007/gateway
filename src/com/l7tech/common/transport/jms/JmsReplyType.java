/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.transport.jms;

import net.sf.hibernate.PersistentEnum;

import java.io.Serializable;
import java.io.ObjectStreamException;

/**
 * @author alex
 * @version $Revision$
 */
public class JmsReplyType implements Serializable, PersistentEnum {
    public static final JmsReplyType AUTOMATIC = new JmsReplyType( 0, "Automatic" );
    public static final JmsReplyType NO_REPLY = new JmsReplyType( 1, "No reply" );
    public static final JmsReplyType REPLY_TO_OTHER = new JmsReplyType( 2, "Reply to other" );
    private static final JmsReplyType[] VALUES = { AUTOMATIC, NO_REPLY, REPLY_TO_OTHER };

    public int getNum() { return _num; }
    public String getName() { return _name; }
    public String toString() {
        return "<JmsReplyType num=\"" + _num + "\" name=\"" + _name + "\"/>";
    }

    private JmsReplyType( int num, String name ) {
        _num = num;
        _name = name;
    }

    private Object readResolve() throws ObjectStreamException {
        return VALUES[_num];
    }

    private final int _num;
    private final String _name;

    public int toInt() {
        return _num;
    }

    public static JmsReplyType fromInt( int i ) {
        return VALUES[i];
    }
}
