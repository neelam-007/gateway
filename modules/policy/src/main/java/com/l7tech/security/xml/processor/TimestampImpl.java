package com.l7tech.security.xml.processor;

import org.w3c.dom.Element;

/**
* Holds a WSS timestamp.
*/
public class TimestampImpl extends ParsedElementImpl implements WssTimestamp {
    private final TimestampDate createdTimestampDate;
    private final TimestampDate expiresTimestampDate;

    TimestampImpl(TimestampDate createdTimestampDate, TimestampDate expiresTimestampDate, Element timestampElement) {
        super(timestampElement);
        this.createdTimestampDate = createdTimestampDate;
        this.expiresTimestampDate = expiresTimestampDate;
    }

    @Override
    public WssTimestampDate getCreated() {
        return createdTimestampDate;
    }

    @Override
    public WssTimestampDate getExpires() {
        return expiresTimestampDate;
    }
}
