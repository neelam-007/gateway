package com.l7tech.message;

import com.l7tech.common.io.XmlUtil;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class MessageTest {
    private Message message;

    @Before
    public void setup() {
        message = new Message();
    }

    @Test
    public void getHeadersKnobBeforeInitialize() {
        assertNull(message.getHeadersKnob());
        assertNull(message.getKnob(HeadersKnob.class));
    }

    @Test
    public void getHeadersKnobAfterInitialize() {
        message.initialize(XmlUtil.createEmptyDocument());
        assertNotNull(message.getHeadersKnob());
        assertTrue(message.getHeadersKnob().getHeaders().isEmpty());
        assertEquals(message.getHeadersKnob(), message.getKnob(HeadersKnob.class));
    }
}
