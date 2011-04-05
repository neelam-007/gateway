package com.l7tech.server.event.admin;

/**
 * Administrative event for errors in administrative APIs
 */
public class AdminError extends AdminEvent {

    public AdminError( final Object source, final String note ) {
        super( source, note );
    }
}
