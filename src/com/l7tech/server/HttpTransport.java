/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server;

import java.net.InetAddress;

/**
 * @author alex
 * @version $Revision$
 */
public class HttpTransport implements Transport {

    public HttpTransport() {
    }

    public static class IpProtocolPort {
        public IpProtocolPort( InetAddress ip, TransportProtocol protocol, int port ) {
            _ip = ip;
            _protocol = protocol;
            _port = port;
        }

        public TransportProtocol getProtocol() {
            return _protocol;
        }

        public int getPort() {
            return _port;
        }

        public InetAddress getIp() {
            return _ip;
        }

        private InetAddress _ip;
        private TransportProtocol _protocol;
        private int _port;
    }
}
