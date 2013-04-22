package com.l7tech.gateway.common.service;

import com.l7tech.objectmodel.SecurityZone;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class SampleMessageTest {
    private SampleMessage msg;

    @Before
    public void setup() {
        msg = new SampleMessage();
    }

    @Test
    public void copyFromSetsSecurityZone() {
        final SecurityZone zone = new SecurityZone();
        msg.setSecurityZone(zone);
        final SampleMessage copy = new SampleMessage();
        copy.copyFrom(msg);
        assertEquals(zone, copy.getSecurityZone());
    }

    @Test
    public void copyFromSetsNullSecurityZone() {
        msg.setSecurityZone(null);
        final SampleMessage copy = new SampleMessage();
        copy.setSecurityZone(new SecurityZone());
        copy.copyFrom(msg);
        assertNull(copy.getSecurityZone());
    }
}
