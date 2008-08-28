package com.l7tech.server.ems;

import org.apache.wicket.protocol.http.WebSession;
import org.apache.wicket.Request;

/**
 * Extended session with EMS specific properties.
 */
public class EmsSession extends WebSession {

    //- PUBLIC

    public EmsSession(Request request) {
        super(request);
    }

    public String getDateTimeFormatPattern() {
        return dateTimeFormatPattern;
    }

    public void setDateTimeFormatPattern( final String pattern ) {
        dateTimeFormatPattern = pattern;
    }

    public String getTimeZoneId() {
        return timeZoneId;
    }

    public void setTimeZoneId(String timeZoneId) {
        this.timeZoneId = timeZoneId;
    }
    
    //- PRIVATE

    private String dateTimeFormatPattern = EmsApplication.DEFAULT_DATE_FORMAT;
    private String timeZoneId;
}
