package com.l7tech.server.policy.variable;

import com.l7tech.gateway.common.audit.Audit;
import com.l7tech.server.audit.Auditor;
import com.l7tech.util.CollectionUtils;
import org.junit.Test;

import java.util.Map;
import java.util.logging.Logger;

import static org.junit.Assert.assertEquals;

/**
 *
 */
public class ExpandVariablesTemplateTest {
    private static final Logger logger = Logger.getLogger(ExpandVariablesTemplateTest.class.getName());

    private Map<String, ?> vars = CollectionUtils.MapBuilder.<String, Object>builder()
            .put("foo", "qwert")
            .put("bar", "zxcv")
            .unmodifiableMap();

    private Audit audit = new Auditor(this, null, logger);

    @Test
    public void expandStaticString() throws Exception {
        assertEquals("blah blah blah", new ExpandVariablesTemplate("blah blah blah").process(vars, audit));
    }

    @Test
    public void expandSingleVar() throws Exception {
        assertEquals("blah qwert blah", new ExpandVariablesTemplate("blah ${foo} blah").process(vars, audit));
    }

    @Test
    public void expandMultiVar() throws Exception {
        assertEquals("blah qwert zxcv", new ExpandVariablesTemplate("blah ${foo} ${bar}").process(vars, audit));
    }

}
