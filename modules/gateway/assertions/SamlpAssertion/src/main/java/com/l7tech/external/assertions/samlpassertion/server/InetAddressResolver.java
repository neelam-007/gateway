package com.l7tech.external.assertions.samlpassertion.server;

import com.l7tech.external.assertions.samlpassertion.SamlProtocolAssertion;

import java.net.InetAddress;

/**
 * @author: vchan
 */
public abstract class InetAddressResolver {

    protected final SamlProtocolAssertion assertion;
    protected InetAddress address;

    protected InetAddressResolver(final SamlProtocolAssertion assertion) {
        this.assertion = assertion;

        parse();
    }

    public InetAddress getAddress() {
        return address;
    }

    protected abstract void parse();
}