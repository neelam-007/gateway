package com.l7tech.external.assertions.xmppassertion.server.xmlstreamcodec;

import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFactory;
import org.apache.mina.filter.codec.ProtocolDecoder;
import org.apache.mina.filter.codec.ProtocolEncoder;

/**
 * Created by IntelliJ IDEA.
 * User: njordan
 * Date: 12/15/11
 * Time: 10:52 AM
 * To change this template use File | Settings | File Templates.
 */
public class XMLStreamCodecFactory implements ProtocolCodecFactory {
    private final XMLStreamCodecConfiguration configuration;
    private final boolean inbound;

    public XMLStreamCodecFactory(XMLStreamCodecConfiguration configuration, boolean inbound) {
        this.configuration = configuration;
        this.inbound = inbound;
    }

    @Override
    public ProtocolEncoder getEncoder(IoSession session) throws Exception {
        return new XMLStreamEncoder();
    }

    @Override
    public ProtocolDecoder getDecoder(IoSession session) throws Exception {
        return new XMLStreamDecoder(inbound);
    }
}
