package com.l7tech.common.io;

import com.l7tech.test.BugId;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import java.util.NoSuchElementException;

import static com.l7tech.common.io.DocumentBuilderPool.CONFIG;
import static org.apache.commons.pool.impl.GenericObjectPool.WHEN_EXHAUSTED_FAIL;
import static org.mockito.Mockito.when;

/**
 */
public class DocumentBuilderPoolTest {

    @BugId("DE369448")
    @Test(timeout = 1000)
    public void documentBuilderGetAlwaysGrowDefault() throws Exception {
        // happy path
        getMoreDocumentBuilders();

        // failure case, must be done in the same test or else static settings may affect happy path test
        CONFIG.whenExhaustedAction = WHEN_EXHAUSTED_FAIL;
        try {
            getMoreDocumentBuilders();
            Assert.fail("Exception should be thrown");
        } catch (Exception e) {
            Assert.assertEquals(NoSuchElementException.class, e.getClass());
            Assert.assertEquals("Pool exhausted", e.getMessage());
        }
    }

    private void getMoreDocumentBuilders() throws Exception {

        DocumentBuilderFactory factory = Mockito.mock(DocumentBuilderFactory.class);
        DocumentBuilder builder = Mockito.mock(DocumentBuilder.class);
        when(factory.newDocumentBuilder()).thenReturn(builder);
        DocumentBuilderPool pool = new DocumentBuilderPool(factory);
        for (int i = 0; i <= DocumentBuilderPool.DEFAULT_POOL_SIZE + 2; i++) {
            pool.borrowObject();
        }
    }
}
