package com.l7tech.objectmodel;

import org.junit.Assert;
import org.junit.Test;

import java.util.Random;

/**
 * This was created: 6/27/13 as 1:45 PM
 *
 * @author Victor Kazakov
 */
public class GoidAdapterTest {

    @Test
    public void test() throws Exception {
        Random random = new Random();
        byte[] bytes = new byte[16];
        random.nextBytes(bytes);
        Goid goid = new Goid(bytes);

        GoidAdapter adapter = new GoidAdapter();

        Assert.assertEquals(goid, adapter.unmarshal(adapter.marshal(goid)));
    }
}
