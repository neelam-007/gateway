/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 * @author darmstrong
 */
package com.l7tech.json;

import org.junit.Test;

public class JacksonJsonDataTest {

    @Test(expected = InvalidJsonException.class)
    public void testException() throws Exception{

        final JSONData jsonData = JSONFactory.INSTANCE.newJsonData("{\"result\"\"success\"}");
        jsonData.getJsonObject();
    }
}
