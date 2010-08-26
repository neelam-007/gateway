package com.l7tech.external.assertions.rawtcp.server;

import java.net.*;

/**
 * A fake Socket that can be used for testing.
 */
class StubSocket extends Socket {
    StubSocket(SocketImpl socketImpl) throws SocketException {
        super(socketImpl);
    }
}
