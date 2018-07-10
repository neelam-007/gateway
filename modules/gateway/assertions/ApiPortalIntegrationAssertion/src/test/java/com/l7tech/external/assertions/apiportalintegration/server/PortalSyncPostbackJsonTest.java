package com.l7tech.external.assertions.apiportalintegration.server;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.l7tech.external.assertions.apiportalintegration.IncrementPostBackAssertion;
import com.l7tech.external.assertions.apiportalintegration.server.resource.PortalSyncPostbackJson;
import com.l7tech.server.jdbc.JdbcConnectionPoolManager;
import com.l7tech.server.jdbc.JdbcQueryingManager;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.Map;

import static org.mockito.Mockito.when;

public class PortalSyncPostbackJsonTest {

    @Test
    public void validatePostbackTest01() throws IOException {
        PortalSyncPostbackJson postback = new PortalSyncPostbackJson();
        postback.setIncrementStart(12L);
        postback.setBulkSync("true");
        postback.setIncrementEnd(235324L);
        postback.setEntityType(ServerIncrementalSyncCommon.ENTITY_TYPE_APPLICATION);
        postback.setErrorMessage("errorMessage");
        postback.setSyncLog("{cron * * *");
        postback.setIncrementStatus(PortalSyncPostbackJson.SYNC_STATUS_OK);
        postback.setEntityErrors(
                Lists.<Map<String, String>>newArrayList(ImmutableMap.of(ServerIncrementPostBackAssertion.ERROR_ENTITY_ID_LABEL, "error 1", ServerIncrementPostBackAssertion.ERROR_ENTITY_MSG_LABEL, "error 1  log"),
                        ImmutableMap.of(ServerIncrementPostBackAssertion.ERROR_ENTITY_ID_LABEL, "error 2", ServerIncrementPostBackAssertion.ERROR_ENTITY_MSG_LABEL, "error 2 log"))
        );
        postback.validate();
    }

    @Test(expected = IOException.class)
    public void validatePostbackTest02() throws IOException {
        PortalSyncPostbackJson postback = new PortalSyncPostbackJson();
        postback.setIncrementStart(12L);
        postback.setBulkSync(ServerIncrementalSyncCommon.BULK_SYNC_TRUE);
        postback.setIncrementEnd(235324L);
        postback.setEntityType(ServerIncrementalSyncCommon.ENTITY_TYPE_APPLICATION);
        postback.setSyncLog("{cron * * *");
        postback.setIncrementStatus(PortalSyncPostbackJson.SYNC_STATUS_ERROR);
        postback.setEntityErrors(
                Lists.<Map<String, String>>newArrayList(ImmutableMap.of(ServerIncrementPostBackAssertion.ERROR_ENTITY_ID_LABEL, "error 1", ServerIncrementPostBackAssertion.ERROR_ENTITY_MSG_LABEL, "error 1  log"),
                        ImmutableMap.of(ServerIncrementPostBackAssertion.ERROR_ENTITY_ID_LABEL, "error 2", ServerIncrementPostBackAssertion.ERROR_ENTITY_MSG_LABEL, "error 2 log"))
        );

        postback.validate();
        Assert.fail("Should throw IOException");

    }

    @Test(expected = IOException.class)
    public void validatePostbackTest03() throws IOException {
        PortalSyncPostbackJson postback = new PortalSyncPostbackJson();
        postback.setIncrementStart(12L);
        postback.setBulkSync(ServerIncrementalSyncCommon.BULK_SYNC_TRUE);
        postback.setIncrementEnd(235324L);
        postback.setEntityType(ServerIncrementalSyncCommon.ENTITY_TYPE_APPLICATION);
        postback.setErrorMessage("errorMessage");
        postback.setSyncLog("{cron * * *");
        postback.setIncrementStatus(PortalSyncPostbackJson.SYNC_STATUS_PARTIAL);

        postback.validate();
        Assert.fail("Should throw IOException");

    }
}