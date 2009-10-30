/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 * @author darmstrong
 */
package com.l7tech.uddi;

import org.junit.Test;
import org.junit.Assert;

public class WsdlPortInfoTest {
    @Test
    public void testUriConstant(){
        Assert.assertEquals("Constant cannot be changed. Will break the SSM", "http://layer7-tech.com/flag/maxed_out_search_result", WsdlPortInfo.MAXED_OUT_UDDI_RESULTS_URL);
    }
}
