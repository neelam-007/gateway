package com.l7tech.gateway.common.custom;

import com.l7tech.policy.assertion.TargetMessageType;
import com.l7tech.policy.assertion.ext.targetable.CustomMessageTargetableSupport;

import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.*;

/**
 * Test CustomToMessageTargetableConverter
 */
public class CustomToMessageTargetableConverterTest {
    
    @Before
    public void setUp() throws Exception {
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNullSupport() {
        //noinspection ConstantConditions
        new CustomToMessageTargetableConverter(null);
        fail("This message should not have been displayed");
    }

    @Test
    public void testGetTarget() throws Exception {
        CustomMessageTargetableSupport customMessageTargetableSupport = new CustomMessageTargetableSupport();
        CustomToMessageTargetableConverter converter = new CustomToMessageTargetableConverter(customMessageTargetableSupport);

        assertEquals(converter.getCustomMessageTargetable(), customMessageTargetableSupport);
        assertEquals(converter.isTargetModifiedByGateway(), customMessageTargetableSupport.isTargetModifiedByGateway());
        //assertTrue(Arrays.equals(converter.getVariablesSet(), customMessageTargetableSupport.getVariablesSet()));
        assertTrue(Arrays.equals(converter.getVariablesUsed(), customMessageTargetableSupport.getVariablesUsed()));
        assertEquals(converter.getTarget(), TargetMessageType.REQUEST);
        assertNull(converter.getOtherTargetMessageVariable());
        assertEquals(converter.getTargetName(), "Request");
        assertEquals(converter.isTargetModifiedByGateway(), false);
        assertTrue(converter.getVariablesSet().length == 0);
        assertTrue(converter.getVariablesUsed().length == 0);

        customMessageTargetableSupport.setTargetMessageVariable("ReSponSE");
        assertEquals(converter.getCustomMessageTargetable(), customMessageTargetableSupport);
        assertEquals(converter.isTargetModifiedByGateway(), customMessageTargetableSupport.isTargetModifiedByGateway());
        //assertTrue(Arrays.equals(converter.getVariablesSet(), customMessageTargetableSupport.getVariablesSet()));
        assertTrue(Arrays.equals(converter.getVariablesUsed(), customMessageTargetableSupport.getVariablesUsed()));
        assertEquals(converter.getTarget(), TargetMessageType.RESPONSE);
        assertNull(converter.getOtherTargetMessageVariable());
        assertEquals(converter.getTargetName(), "Response");
        assertEquals(converter.isTargetModifiedByGateway(), false);
        assertTrue(converter.getVariablesSet().length == 0);
        assertTrue(converter.getVariablesUsed().length == 0);

        customMessageTargetableSupport.setTargetMessageVariable("tesVar");
        customMessageTargetableSupport.setTargetModifiedByGateway(true);
        customMessageTargetableSupport.setSourceUsedByGateway(true);
        assertEquals(converter.getCustomMessageTargetable(), customMessageTargetableSupport);
        assertEquals(converter.isTargetModifiedByGateway(), customMessageTargetableSupport.isTargetModifiedByGateway());
        //assertTrue(Arrays.equals(converter.getVariablesSet(), customMessageTargetableSupport.getVariablesSet()));
        assertTrue(Arrays.equals(converter.getVariablesUsed(), customMessageTargetableSupport.getVariablesUsed()));
        assertEquals(converter.getTarget(), TargetMessageType.OTHER);
        assertEquals(converter.getOtherTargetMessageVariable(), "tesVar");
        assertEquals(converter.getTargetName(), "${tesVar}");
        assertEquals(converter.isTargetModifiedByGateway(), true);
        assertTrue(converter.getVariablesSet().length == 1);
        assertTrue(converter.getVariablesUsed().length == 1);
    }

    @Test
    public void testSetTarget() throws Exception {
        CustomMessageTargetableSupport customMessageTargetableSupport = new CustomMessageTargetableSupport();
        CustomToMessageTargetableConverter converter = new CustomToMessageTargetableConverter(customMessageTargetableSupport);

        converter.setTarget(TargetMessageType.REQUEST);
        assertEquals(customMessageTargetableSupport.getTargetMessageVariable(), CustomMessageTargetableSupport.TARGET_REQUEST);
        assertEquals(converter.getTarget(), TargetMessageType.REQUEST);
        assertEquals(converter.getTargetName(), "Request");
        assertEquals(converter.getTargetName(), customMessageTargetableSupport.getTargetName());

        converter.setTarget(TargetMessageType.OTHER);
        assertNull(customMessageTargetableSupport.getTargetMessageVariable());
        assertEquals(converter.getTarget(), TargetMessageType.OTHER);
        assertNull(converter.getTargetName());
        assertEquals(converter.getTargetName(), customMessageTargetableSupport.getTargetName());

        converter.setTarget(TargetMessageType.RESPONSE);
        assertEquals(customMessageTargetableSupport.getTargetMessageVariable(), CustomMessageTargetableSupport.TARGET_RESPONSE);
        assertEquals(converter.getTarget(), TargetMessageType.RESPONSE);
        assertEquals(converter.getTargetName(), "Response");
        assertEquals(converter.getTargetName(), customMessageTargetableSupport.getTargetName());

        converter.setTarget(TargetMessageType.OTHER);
        converter.setOtherTargetMessageVariable("testVal");
        assertEquals(customMessageTargetableSupport.getTargetMessageVariable(), "testVal");
        assertEquals(converter.getOtherTargetMessageVariable(), "testVal");
        assertEquals(converter.getTarget(), TargetMessageType.OTHER);
        assertEquals(converter.getTargetName(), "${testVal}");
        assertEquals(converter.getTargetName(), customMessageTargetableSupport.getTargetName());
    }
}
