package com.l7tech.proxy.datamodel;

import org.junit.Test;

import java.beans.ExceptionListener;
import java.io.File;
import java.util.Iterator;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 *
 */
public class SsgFinderTest {

    @Test
    public void testReadConfig() throws Exception {
        SsgFinderImpl.exceptionListener = new FatalExceptionListener();
        SsgFinderImpl ssgFinder = new SsgFinderImpl();
        ssgFinder.storePath = new File(getClass().getClassLoader().getResource("com/l7tech/proxy/datamodel/halibut_ssgs.xml").toURI()).getPath();


        final List ssgList = ssgFinder.getSsgList();
        assertNotNull(ssgList);
        assertTrue("not empty", !ssgList.isEmpty());
        Iterator it = ssgList.iterator();
        Ssg firstSsg = (Ssg) it.next();
        assertNotNull(firstSsg);
        assertEquals("data.l7tech.local", firstSsg.getHostname());
        assertEquals("/ssg/soap", firstSsg.getSsgFile());
        assertEquals(1L, firstSsg.getId());
        assertEquals(true, firstSsg.isSavePasswordToDisk());
    }

    static class FatalExceptionListener implements ExceptionListener {
        @Override
        public void exceptionThrown(Exception e) {
            if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            }
            throw new RuntimeException(e);
        }
    }
}
