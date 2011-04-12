package com.l7tech.server.event.admin;

import com.l7tech.util.TextUtils;

import java.text.MessageFormat;
import java.util.logging.Level;

/**
 * Event fired when administrative passwords are reset on a provider
 */
public class AdministrativePasswordsResetEvent extends AdminEvent {

    private static final String RESET_MESSAGE = "Force administrative user password reset in {0}";

    public AdministrativePasswordsResetEvent( final Object source,
                                              final String providerDetail ) {
        super( source, formatMessage( providerDetail ), Level.WARNING );
    }

    private static String formatMessage( final String providerDetail ) {
        final int maxDetailLength = MESSAGE_MAX_LENGTH - RESET_MESSAGE.length();
        return MessageFormat.format( RESET_MESSAGE, TextUtils.truncateStringAtEnd( providerDetail, maxDetailLength ) );
    }
}
