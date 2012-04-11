package com.l7tech.server.transport.jms;

import javax.jms.Destination;

public class DestinationStub implements Destination {
    private final String destination;

    public DestinationStub(final String destination) {
        this.destination = destination;
    }

    @Override
    public String toString() {
        return destination;
    }
}
