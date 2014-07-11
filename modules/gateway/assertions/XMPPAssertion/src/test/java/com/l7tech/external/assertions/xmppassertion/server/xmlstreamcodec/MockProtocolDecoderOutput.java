package com.l7tech.external.assertions.xmppassertion.server.xmlstreamcodec;

import org.apache.mina.core.filterchain.IoFilter;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: njordan
 * Date: 19/03/12
 * Time: 2:54 PM
 * To change this template use File | Settings | File Templates.
 */
public class MockProtocolDecoderOutput implements ProtocolDecoderOutput {
    private List<Object> messages = new ArrayList<Object>();
    
    public MockProtocolDecoderOutput() {
    }
    
    public void flush(IoFilter.NextFilter nextFilter, IoSession session) {
    }
    
    public void write(Object message) {
        messages.add(message);
    }
    
    public List<Object> getMessages() {
        return messages;
    }
}
