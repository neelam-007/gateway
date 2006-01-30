/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.policy.variable;

import java.io.ObjectStreamException;
import java.io.InvalidObjectException;

/**
 * @author alex
 */
public final class VariableScope {
    private static int i = 0;
    public static final VariableScope REQUEST = new VariableScope(i++, "Request");
    public static final VariableScope SERVER = new VariableScope(i++, "Server");
    public static final VariableScope CLUSTER = new VariableScope(i++, "Cluster");
    private static VariableScope[] VALUES = { REQUEST, SERVER, CLUSTER };

    private final int num;
    private final String name;

    private VariableScope(int num, String name) {
        this.num = num;
        this.name = name;
    }

    public int getNum() {
        return num;
    }

    public String getName() {
        return name;
    }

    protected Object readResolve() throws ObjectStreamException {
        if (num >= 0 && num < VALUES.length)
            return VALUES[num];
        else throw new InvalidObjectException("Index '" + num + "' out of range!");
    }
}
