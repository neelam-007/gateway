package com.l7tech.gateway.common;

import org.junit.Test;
import org.junit.Assert;

/**
 * [class_desc]
 *
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Jan 13, 2004<br/>
 */
public class RequestIdTest {

    @Test
    public void testParsing() {
        RequestId redid1 = new RequestId(123, 456);
        RequestId redid2 = new RequestId(redid1.toString());
        Assert.assertTrue(redid1.equals(redid2));
        redid1 = new RequestId(0, 0);
        redid2 = new RequestId(redid1.toString());
        Assert.assertTrue(redid1.equals(redid2));
    }

    @Test
    public void testExtremeParsing() {
        RequestId redid1 = new RequestId(0, Long.MAX_VALUE);
        RequestId redid2 = new RequestId(redid1.toString());
        Assert.assertTrue(redid1.equals(redid2));
        redid1 = new RequestId(Long.MAX_VALUE, 0);
        redid2 = new RequestId(redid1.toString());
        Assert.assertTrue(redid1.equals(redid2));
    }
}
