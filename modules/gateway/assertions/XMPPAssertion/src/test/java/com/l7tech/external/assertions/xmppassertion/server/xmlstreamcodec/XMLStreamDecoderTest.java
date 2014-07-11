package com.l7tech.external.assertions.xmppassertion.server.xmlstreamcodec;

import junit.framework.Assert;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.DummySession;
import org.junit.Test;

import java.util.logging.Logger;

/**
 * Created by IntelliJ IDEA.
 * User: njordan
 * Date: 19/03/12
 * Time: 2:53 PM
 * To change this template use File | Settings | File Templates.
 */
public class XMLStreamDecoderTest {
    private static final Logger log = Logger.getLogger(XMLStreamDecoderTest.class.getName());

    @Test
    public void test1() throws Exception {
        DummySession session = new DummySession();
        IoBuffer in = createIoBuffer("adfsadf");
        MockProtocolDecoderOutput out = new MockProtocolDecoderOutput();
        
        XMLStreamDecoder decoder = new XMLStreamDecoder(true);
        decoder.doDecode(session, in, out);

        Assert.assertEquals(0, out.getMessages().size());
    }
    
    @Test
    public void test2() throws Exception {
        DummySession session = new DummySession();
        IoBuffer in = createIoBuffer("<?xml version=");
        MockProtocolDecoderOutput out = new MockProtocolDecoderOutput();

        XMLStreamDecoder decoder = new XMLStreamDecoder(true);
        decoder.doDecode(session, in, out);

        Assert.assertEquals(0, out.getMessages().size());
    }

    @Test
    public void test3() throws Exception {
        DummySession session = new DummySession();
        IoBuffer in = createIoBuffer("<?xml version=");
        MockProtocolDecoderOutput out = new MockProtocolDecoderOutput();

        XMLStreamDecoder decoder = new XMLStreamDecoder(true);
        decoder.doDecode(session, in, out);

        Assert.assertEquals(0, out.getMessages().size());

        decoder.doDecode(session, createIoBuffer("\"1.0\"?>"), out);

        Assert.assertEquals(0, out.getMessages().size());
    }

    @Test
    public void test4() throws Exception {
        DummySession session = new DummySession();
        IoBuffer in = createIoBuffer("<?xml version=\"1.0\"?>");
        MockProtocolDecoderOutput out = new MockProtocolDecoderOutput();

        XMLStreamDecoder decoder = new XMLStreamDecoder(true);
        decoder.doDecode(session, in, out);

        Assert.assertEquals(0, out.getMessages().size());

        decoder.doDecode(session, createIoBuffer("<test>"), out);

        Assert.assertEquals(1, out.getMessages().size());
        Assert.assertEquals("<?xml version=\"1.0\"?><test>", new String((byte[])out.getMessages().get(0)));
    }

    @Test
    public void test5() throws Exception {
        DummySession session = new DummySession();
        IoBuffer in = createIoBuffer("<?xml version=\"1.0\"?>");
        MockProtocolDecoderOutput out = new MockProtocolDecoderOutput();

        XMLStreamDecoder decoder = new XMLStreamDecoder(true);
        decoder.doDecode(session, in, out);

        Assert.assertEquals(0, out.getMessages().size());

        decoder.doDecode(session, createIoBuffer("<test>"), out);

        Assert.assertEquals(1, out.getMessages().size());
        Assert.assertEquals("<?xml version=\"1.0\"?><test>", new String((byte[])out.getMessages().get(0)));

        decoder.doDecode(session, createIoBuffer("</test>"), out);

        Assert.assertEquals(2, out.getMessages().size());
        Assert.assertEquals("</test>", new String((byte[])out.getMessages().get(1)));
    }

    @Test
    public void test6() throws Exception {
        DummySession session = new DummySession();
        IoBuffer in = createIoBuffer("<?xml version=\"1.0\"?>");
        MockProtocolDecoderOutput out = new MockProtocolDecoderOutput();

        XMLStreamDecoder decoder = new XMLStreamDecoder(true);
        decoder.doDecode(session, in, out);

        Assert.assertEquals(0, out.getMessages().size());

        decoder.doDecode(session, createIoBuffer("<test><hello>"), out);

        Assert.assertEquals(1, out.getMessages().size());
        Assert.assertEquals("<?xml version=\"1.0\"?><test>", new String((byte[])out.getMessages().get(0)));

        decoder.doDecode(session, createIoBuffer("adf</hello></test>"), out);

        Assert.assertEquals(3, out.getMessages().size());
        Assert.assertEquals("<hello>adf</hello>", new String((byte[])out.getMessages().get(1)));
        Assert.assertEquals("</test>", new String((byte[])out.getMessages().get(2)));
    }

    private IoBuffer createIoBuffer(String text) {
        return IoBuffer.wrap(text.getBytes());
    }
}
