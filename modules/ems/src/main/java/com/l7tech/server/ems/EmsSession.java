package com.l7tech.server.ems;

import org.apache.wicket.protocol.http.WebSession;
import org.apache.wicket.Request;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.TimeZone;

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

    public DateFormat buildDateFormat() {
        SimpleDateFormat format = new SimpleDateFormat( getDateTimeFormatPattern() );
        if ( getTimeZoneId() != null ) {
            format.setTimeZone( TimeZone.getTimeZone( getTimeZoneId() ) );
        }
        return format;
    }
    //- PRIVATE

    private String dateTimeFormatPattern = EmsApplication.DEFAULT_DATE_FORMAT;
    private String timeZoneId;
    private String preferredPage;
}
