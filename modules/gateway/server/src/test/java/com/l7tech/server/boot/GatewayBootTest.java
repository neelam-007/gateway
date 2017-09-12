package com.l7tech.server.boot;

import com.l7tech.util.SyspropUtil;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

public class GatewayBootTest {

    @Test
    public void testGatewayModes() {
        // The default mode, should be TRADITIONAL
        GatewayBoot.Mode mode = GatewayBootUtil.getMode();
        Assert.assertEquals(GatewayBoot.Mode.TRADITIONAL, mode);

        // Test setting a specific mode
        SyspropUtil.setProperty(GatewayBootUtil.PROP_SERVER_MODE, GatewayBoot.Mode.RUNTIME.toString());
        mode = GatewayBootUtil.getMode();
        Assert.assertEquals(GatewayBoot.Mode.RUNTIME, mode);

        // Test setting a bad mode
        SyspropUtil.setProperty(GatewayBootUtil.PROP_SERVER_MODE, "BadMode!!!");
        boolean caught = false;
        try {
            GatewayBootUtil.getMode();
        } catch (IllegalArgumentException e) {
            // Validate that the error message contains some useful info
            // The error message contains the bad mode
            Assert.assertTrue(e.getMessage().contains("BadMode!!!"));
            // The error message contains at least one valid alternative.
            Assert.assertTrue(e.getMessage().contains(GatewayBoot.Mode.TRADITIONAL.toString()));
            caught = true;
        }
        Assert.assertTrue(caught);
    }

    @Test
    public void testListComponentsContexts() {
        List<String> contexts = GatewayBootUtil.getComponentsContexts(GatewayBoot.Mode.TRADITIONAL);
        Assert.assertEquals(GatewayBootUtil.DEFAULT_COMPONENTS.size() * 2, contexts.size());
        Assert.assertTrue(contexts.contains("com/l7tech/server/resources/uddiRuntimeContext.xml"));
        Assert.assertTrue(contexts.contains("com/l7tech/server/resources/uddiAdminContext.xml"));

        contexts = GatewayBootUtil.getComponentsContexts(GatewayBoot.Mode.RUNTIME);
        Assert.assertEquals(GatewayBootUtil.DEFAULT_COMPONENTS.size(), contexts.size());
        Assert.assertTrue(contexts.contains("com/l7tech/server/resources/uddiRuntimeContext.xml"));
        Assert.assertFalse(contexts.contains("com/l7tech/server/resources/uddiAdminContext.xml"));

        SyspropUtil.setProperty(GatewayBootUtil.PROP_COMPONENTS, "bloop,blah");
        contexts = GatewayBootUtil.getComponentsContexts(GatewayBoot.Mode.TRADITIONAL);
        Assert.assertEquals(4, contexts.size());
        Assert.assertTrue(contexts.contains("com/l7tech/server/resources/blahRuntimeContext.xml"));
        Assert.assertTrue(contexts.contains("com/l7tech/server/resources/blahAdminContext.xml"));
        Assert.assertTrue(contexts.contains("com/l7tech/server/resources/bloopRuntimeContext.xml"));
        Assert.assertTrue(contexts.contains("com/l7tech/server/resources/bloopAdminContext.xml"));

        SyspropUtil.setProperty(GatewayBootUtil.PROP_COMPONENTS, "bloop,blah");
        contexts = GatewayBootUtil.getComponentsContexts(GatewayBoot.Mode.RUNTIME);
        Assert.assertEquals(2, contexts.size());
        Assert.assertTrue(contexts.contains("com/l7tech/server/resources/blahRuntimeContext.xml"));
        Assert.assertFalse(contexts.contains("com/l7tech/server/resources/blahAdminContext.xml"));
        Assert.assertTrue(contexts.contains("com/l7tech/server/resources/bloopRuntimeContext.xml"));
        Assert.assertFalse(contexts.contains("com/l7tech/server/resources/bloopAdminContext.xml"));


        SyspropUtil.setProperty(GatewayBootUtil.PROP_COMPONENTS, "  bloop  , blah ");
        contexts = GatewayBootUtil.getComponentsContexts(GatewayBoot.Mode.TRADITIONAL);
        Assert.assertEquals(4, contexts.size());
        Assert.assertTrue(contexts.contains("com/l7tech/server/resources/blahRuntimeContext.xml"));
        Assert.assertTrue(contexts.contains("com/l7tech/server/resources/blahAdminContext.xml"));
        Assert.assertTrue(contexts.contains("com/l7tech/server/resources/bloopRuntimeContext.xml"));
        Assert.assertTrue(contexts.contains("com/l7tech/server/resources/bloopAdminContext.xml"));

        
        SyspropUtil.setProperty(GatewayBootUtil.PROP_COMPONENTS, "");
        contexts = GatewayBootUtil.getComponentsContexts(GatewayBoot.Mode.TRADITIONAL);
        Assert.assertEquals(0, contexts.size());


        SyspropUtil.setProperty(GatewayBootUtil.PROP_COMPONENTS, "   ");
        contexts = GatewayBootUtil.getComponentsContexts(GatewayBoot.Mode.TRADITIONAL);
        Assert.assertEquals(0, contexts.size());

    }
}