package com.l7tech.server.processcontroller;

import com.l7tech.common.io.ProcResult;
import java.io.IOException;
import junit.framework.Assert;
import org.junit.Test;
import org.junit.Ignore;

/**
 * @author jbufu
 */
public class OSApiTest {

    @Test
    @Ignore("Until the full PC / OS API are deployed for testing")
    public void testExec() throws Exception {
        OSApi api = CxfApiTestUtil.makeSslApiStub("https://localhost:8765/services/osApi", OSApi.class);
        ProcResult result = api.execute("/tmp", "ls", new String[] {"-1", "/tmp"}, null, 5000);
        Assert.assertEquals("ls -1 /tmp should return success", result.getExitStatus(), 0);
    }

    @Test
    @Ignore("Until the full PC / OS API are deployed for testing")
    public void testSleepTimeout() throws Exception {
        OSApi api = CxfApiTestUtil.makeSslApiStub("https://localhost:8765/services/osApi", OSApi.class);
        try {
            api.execute("/tmp", "sleep", new String[] {"10"}, null, 2000);
        } catch (IOException expected) {
            return;
        }
        Assert.fail("Sleep should have been interrupted after the requested execution timeout");
    }

    @Test
    @Ignore("Until the full PC / OS API are deployed for testing")
    public void testBigOutput() throws Exception {
        OSApi api = CxfApiTestUtil.makeSslApiStub("https://localhost:8765/services/osApi", OSApi.class);
        try {
            api.execute("/tmp", "cat", new String[] {"usr.list"}, null, 30000);
        } catch (IOException expected) {
            return;
        }
        Assert.fail("Sleep should have been interrupted after the requested execution timeout");
    }
}
