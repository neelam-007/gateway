/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 * @author darmstrong
 */
package com.l7tech.json;

import org.junit.Test;

public class JacksonJsonDataTest {

    @Test(expected = InvalidJsonException.class)
    public void testException() throws Exception{

        final JSONFactory instance = JSONFactory.getInstance();
        final JSONData jsonData = instance.newJsonData("{\"result\"\"success\"}");
        jsonData.getJsonObject();
    }
}
