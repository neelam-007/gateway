package com.l7tech.external.assertions.remotecacheassertion.server;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.common.mime.StashManager;
import com.l7tech.message.Message;
import com.l7tech.server.StashManagerFactory;
import com.l7tech.util.HexUtils;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


public class CachedMessageDataTest {

    private StashManagerFactory stashManagerFactory;
    private StashManager stashManager;
    private Message msg;
    private final String xmlBody = "<myrequest/>";

    @Before
    public void setup() throws IOException, NoSuchPartException {
        stashManagerFactory = mock(StashManagerFactory.class);
        stashManager = mock(StashManager.class);
        when(stashManagerFactory.createStashManager()).thenReturn(stashManager);
        InputStream is = new ByteArrayInputStream(xmlBody.getBytes());

        when(stashManager.recall(0)).thenReturn(is);
        when(stashManager.getSize(0)).thenReturn((long) xmlBody.getBytes().length);

        msg = new Message();
        msg.initialize(XmlUtil.stringAsDocument(xmlBody));
    }

    /**
     * Test CachedMessageData of type Json is created successfully. validate the content type and the body are correct.
     *
     * @throws Exception
     */
    @Test
    public void testCreateCachedMessageDataOfJsonType() throws Exception {
        String header = msg.getMimeKnob().getOuterContentType().getFullValue();
        String encodedData = HexUtils.encodeBase64(xmlBody.getBytes());
        String cachedData = "{\"mimeType\":\"" + header + "\", \"body\":\"" + encodedData + "\"}";

        CachedMessageData cachedMessageData = new CachedMessageData(msg, stashManagerFactory, "JSON");

        assertEquals(header, cachedMessageData.getContentType());
        assertEquals(xmlBody, new String(cachedMessageData.getBodyBytes()));
        assertEquals(CachedMessageData.ValueType.JSON, cachedMessageData.getValueType());
        assertEquals(cachedData, cachedMessageData.getCacheMessageData(CachedMessageData.ValueType.JSON));
        assertEquals((4 + header.getBytes().length + cachedData.getBytes().length), cachedMessageData.sizeInBytes(CachedMessageData.ValueType.JSON));
    }

    /**
     * Test CachedMessageData of type byte[] is created successfully. validate the content type and the body are correct.
     *
     * @throws Exception
     */
    @Test
    public void testCreateCachedMessageDataOfByteType() throws Exception {
        String header = msg.getMimeKnob().getOuterContentType().getFullValue();
        CachedMessageData cachedMessageData = new CachedMessageData(msg, stashManagerFactory, "BYTE_ARRAY");
        byte[] bytes = (byte[]) cachedMessageData.getCacheMessageData(CachedMessageData.ValueType.BYTE_ARRAY);

        assertEquals(header, cachedMessageData.getContentType());
        assertEquals(xmlBody, new String(cachedMessageData.getBodyBytes()));
        assertEquals(CachedMessageData.ValueType.BYTE_ARRAY, cachedMessageData.getValueType());
        assertTrue(Arrays.equals(xmlBody.getBytes(), bytes));
        assertEquals((4 + header.getBytes().length + xmlBody.getBytes().length), cachedMessageData.sizeInBytes(CachedMessageData.ValueType.BYTE_ARRAY));

    }

    /**
     * Test CachedMessageData is created given a byte array
     *
     * @throws Exception
     */
    @Test
    public void testCreateByteCachedMessageData() throws Exception {
        String body = "\u0000\u0000\u0000\u0017text/xml; charset=utf-8<myrequest/>";
        CachedMessageData cachedMessageData = new CachedMessageData(body.getBytes());
        byte[] bytes = cachedMessageData.toByteArray();

        assertEquals("text/xml; charset=utf-8", cachedMessageData.getContentType());
        assertEquals(xmlBody, new String(cachedMessageData.getBodyBytes()));
        assertTrue(Arrays.equals(body.getBytes(), bytes));
    }

    /**
     * Test creation of CachedMessageData fails when the byte[] array provided is missing content type
     *
     * @throws Exception
     */
    @Test(expected = Exception.class)
    public void testCreateByteCachedMessageDataThrowsExceptionWhenHeaderMissing() throws Exception {
        String body = "\u0000\u0000\u0000\u0017<myrequest/>";
        CachedMessageData cachedMessageData = new CachedMessageData(body.getBytes());
        cachedMessageData.toByteArray();
    }

    /**
     * Test CachedMessageData is created given a json string
     *
     * @throws Exception
     */
    @Test
    public void testCreateJsonCachedMessageData() throws Exception {
        String body = "{data:value}";
        CachedMessageData cachedMessageData = new CachedMessageData(body);

        assertEquals("application/json; charset=utf-8", cachedMessageData.getContentType());
        assertEquals(body, new String(cachedMessageData.getBodyBytes()));
    }
}
