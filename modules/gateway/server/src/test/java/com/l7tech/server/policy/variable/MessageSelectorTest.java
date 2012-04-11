package com.l7tech.server.policy.variable;

import com.l7tech.gateway.common.DefaultSyntaxErrorHandler;
import com.l7tech.gateway.common.audit.TestAudit;
import com.l7tech.message.JmsKnobStub;
import com.l7tech.message.Message;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

public class MessageSelectorTest {
    private MessageSelector selector;
    private Message message;
    private JmsKnobStub jmsKnob;
    private Map<String, String> headers;
    private DefaultSyntaxErrorHandler handler;

    @Before
    public void setup() {
        selector = new MessageSelector();
        headers = new HashMap<String, String>();
        headers.put("h1", "h1value");
        headers.put("h2", "h2value");
        headers.put("h3", "h3value");
        jmsKnob = new JmsKnobStub(1234L, false, null);
        jmsKnob.setHeaders(headers);
        message = new Message();
        message.attachJmsKnob(jmsKnob);
        handler = new DefaultSyntaxErrorHandler(new TestAudit());
    }

    @Test
    public void selectJmsHeaderNames() {
        final ExpandVariables.Selector.Selection selection = selector.select(null, message, "jms.headernames", handler, false);

        final String[] selectedValue = (String[]) selection.getSelectedValue();
        final List<String> asList = Arrays.asList(selectedValue);
        assertEquals(3, asList.size());
        assertTrue(asList.contains("h1"));
        assertTrue(asList.contains("h2"));
        assertTrue(asList.contains("h3"));
    }

    @Test
    public void selectJmsHeaderNamesNone() {
        headers.clear();

        final ExpandVariables.Selector.Selection selection = selector.select(null, message, "jms.headernames", handler, false);

        final String[] selectedValue = (String[]) selection.getSelectedValue();
        assertEquals(0, selectedValue.length);
    }

    @Test
    public void selectJmsHeader() {
        final ExpandVariables.Selector.Selection selection = selector.select(null, message, "jms.header.h2", handler, false);

        final String selectedValue = (String) selection.getSelectedValue();
        assertEquals("h2value", selectedValue);
    }

    @Test
    public void selectJmsHeaderNotFound() {
        final ExpandVariables.Selector.Selection selection = selector.select(null, message, "jms.header.h4", handler, false);

        assertNull(selection);
    }

    @Test
    public void selectJmsHeaderValues() {
        final ExpandVariables.Selector.Selection selection = selector.select(null, message, "jms.allheadervalues", handler, false);

        final Object[] selectedValue = (Object[]) selection.getSelectedValue();
        final List<Object> asList = Arrays.asList(selectedValue);
        assertEquals(3, asList.size());
        assertTrue(asList.contains("h1:h1value"));
        assertTrue(asList.contains("h2:h2value"));
        assertTrue(asList.contains("h3:h3value"));
    }

    @Test
    public void selectJmsHeaderValuesNone() {
        headers.clear();

        final ExpandVariables.Selector.Selection selection = selector.select(null, message, "jms.allheadervalues", handler, false);

        final Object[] selectedValue = (Object[]) selection.getSelectedValue();
        assertEquals(0, selectedValue.length);
    }
}
