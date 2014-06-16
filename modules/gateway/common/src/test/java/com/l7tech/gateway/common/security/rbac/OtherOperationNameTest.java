package com.l7tech.gateway.common.security.rbac;

import org.junit.Test;

import static org.junit.Assert.*;

public class OtherOperationNameTest {
    @Test
    public void getByName() {
        for (final OtherOperationName op : OtherOperationName.values()) {
            assertEquals(op, OtherOperationName.getByName(op.getOperationName()));
        }
    }

    @Test
    public void getByNameNotFound() {
        assertNull(OtherOperationName.getByName("doesnotexist"));
    }

    @Test
    public void getByNameCaseSensitive() {
        assertNull(OtherOperationName.getByName(OtherOperationName.DEBUGGER.getOperationName().toUpperCase()));
    }
}
