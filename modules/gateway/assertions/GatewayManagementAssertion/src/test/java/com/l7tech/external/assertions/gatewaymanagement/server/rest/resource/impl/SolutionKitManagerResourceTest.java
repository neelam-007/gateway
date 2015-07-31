package com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.impl;

import org.apache.commons.lang.CharEncoding;
import org.junit.Test;

import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

import static com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.impl.SolutionKitManagerResource.ID_DELIMINATOR;
import static org.junit.Assert.assertEquals;

/**
 * Solution Kit Manager Resource tests
 */
public class SolutionKitManagerResourceTest {

    @Test
    public void entityIdDecodeAndSplit() throws Exception {
        Map<String, String> entityIdReplaceMap = new HashMap<>(2);

        String entityIdOld = "f1649a0664f1ebb6235ac238a6f71a6d";
        String entityIdNew = "66461b24787941053fc65a626546e4bd";
        SolutionKitManagerResource.decodeSplitPut(entityIdOld + ID_DELIMINATOR + entityIdNew, entityIdReplaceMap);
        assertEquals(entityIdNew, entityIdReplaceMap.get(entityIdOld));

        entityIdOld = "0567c6a8f0c4cc2c9fb331cb03b4de6f";
        entityIdNew = "1e3299eab93e2935adafbf35860fc8d9";
        SolutionKitManagerResource.decodeSplitPut(entityIdOld + URLEncoder.encode(ID_DELIMINATOR, CharEncoding.UTF_8) + entityIdNew, entityIdReplaceMap);
        assertEquals(entityIdNew, entityIdReplaceMap.get(entityIdOld));
    }

    // TODO more tests
}
