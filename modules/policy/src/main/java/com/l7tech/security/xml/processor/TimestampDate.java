package com.l7tech.security.xml.processor;

import com.l7tech.util.DomUtils;
import com.l7tech.util.ISO8601Date;
import org.w3c.dom.Element;

import java.text.ParseException;
import java.util.Date;

/**
* Holds a date as represented in a Date element of a WSS timestamp.
*/
class TimestampDate extends ParsedElementImpl implements WssTimestampDate {
    Date date;
    String dateString;

    TimestampDate(Element createdOrExpiresElement) throws ParseException {
        super(createdOrExpiresElement);
        dateString = DomUtils.getTextValue(createdOrExpiresElement);
        date = ISO8601Date.parse(dateString);
    }

    @Override
    public long asTime() {
        return date.getTime();
    }

    @Override
    public String asIsoString() {
        return dateString;
    }
}
