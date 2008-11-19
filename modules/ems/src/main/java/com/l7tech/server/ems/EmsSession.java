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

    public void setTimeZoneId( final String timeZoneId ) {
        this.timeZoneId = timeZoneId;
    }

    public String getPreferredPage() {
        return preferredPage;
    }

    public void setPreferredPage( final String preferredPage ) {
        this.preferredPage = preferredPage;
    }

    //- PRIVATE

    private String dateTimeFormatPattern = EmsApplication.DEFAULT_DATE_FORMAT;
    private String timeZoneId;
    private String preferredPage;
}
