/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 * @author darmstrong
 */
package com.l7tech.gateway.common.audit;

import com.l7tech.util.Functions;
import com.l7tech.util.Pair;
import org.junit.Assert;
import org.junit.Test;

import java.util.logging.Level;

public class MessagesUtilTest {

    private static Functions.Binary<Level, Integer, Level> filterFunction;
    private static MessagesUtil.AuditDetailLevelFilter filter = new MessagesUtil.AuditDetailLevelFilter() {
        @Override
        public Level filterLevelForAuditDetailMessage(int id, Level defaultLevel) {
            return filterFunction.call(id, defaultLevel);
        }
    };

    static{
        MessagesUtil.registerMessageLevelFilter(filter);
    }

    @Test
    public void testMessageDoesNotExist(){
        //filter not needed in this test
        final Pair<Boolean,AuditDetailMessage> pair = MessagesUtil.getAuditDetailMessageByIdWithFilter(Integer.MIN_VALUE);
        Assert.assertFalse("Message does not exist", pair.left);
    }

    @Test
    public void testMessageSetToNever(){
        filterFunction = new Functions.Binary<Level, Integer, Level>() {
            @Override
            public Level call(Integer integer, Level level) {
                return null;
            }
        };

        final Pair<Boolean, AuditDetailMessage> pair = MessagesUtil.getAuditDetailMessageByIdWithFilter(-1);
        Assert.assertTrue("Property exists", pair.left);
        
        Assert.assertNull("Audit should not be audited", pair.right);
    }

    @Test
    public void testMessageLevelChanged(){
        filterFunction = new Functions.Binary<Level, Integer, Level>() {
            @Override
            public Level call(Integer integer, Level level) {
                return Level.FINEST;
            }
        };

        final Pair<Boolean, AuditDetailMessage> pair = MessagesUtil.getAuditDetailMessageByIdWithFilter(-1);
        Assert.assertTrue("Property exists", pair.left);

        Assert.assertEquals("Audit level should be changed", Level.FINEST, pair.right.getLevel());
    }
}
