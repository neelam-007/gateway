package com.l7tech.server.ems;

import org.apache.wicket.protocol.http.WebSession;
import org.apache.wicket.Request;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.TimeZone;

/**
 * Extended session with EMS specific properties.
 */
public class EsmSession extends WebSession {

    //- PUBLIC

    public EsmSession(Request request) {
        super(request);
    }

    public String getDateFormatPattern() {
        return dateFormatPattern;
    }

    public void setDateFormatPattern( final String pattern ) {
        dateFormatPattern = pattern;
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
        return buildDateFormat( true );
    }

    public DateFormat buildDateFormat( boolean datetime ) {
        SimpleDateFormat format = new SimpleDateFormat( datetime ? getDateTimeFormatPattern() : getDateFormatPattern() );
        if ( getTimeZoneId() != null ) {
            format.setTimeZone( TimeZone.getTimeZone( getTimeZoneId() ) );
        }
        return format;
    }
    //- PRIVATE

    private String dateTimeFormatPattern = EsmApplication.DEFAULT_DATETIME_FORMAT;
    private String dateFormatPattern = EsmApplication.DEFAULT_DATE_FORMAT;
    private String timeZoneId;
    private String preferredPage;
}
