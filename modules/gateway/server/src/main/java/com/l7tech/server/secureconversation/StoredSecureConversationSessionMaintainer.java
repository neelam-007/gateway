package com.l7tech.server.secureconversation;

import com.l7tech.util.Config;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.ValidatedConfig;

import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Performs maintenance tasks for stored WS-SecureConversation sessions.
 */
public class StoredSecureConversationSessionMaintainer {

    //- PUBLIC

    public StoredSecureConversationSessionMaintainer( final Config config,
                                                      final StoredSecureConversationSessionManager storedSecureConversationSessionManager,
                                                      final Timer timer ) {
        this.config = validated( config );
        this.storedSecureConversationSessionManager = storedSecureConversationSessionManager;
        this.timer = timer;
    }

    public void init() {
        long interval = config.getTimeUnitProperty( "wss.secureConversation.clusterSessionsCleanupInterval", 33407L );
        if ( interval > 0L ) {
            timer.schedule( new TimerTask() {
                @Override
                public void run() {
                    doMaintenance();
                }
            }, interval * 2L, interval );
        }
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger( StoredSecureConversationSessionMaintainer.class.getName() );

    private final Config config;
    private final StoredSecureConversationSessionManager storedSecureConversationSessionManager;
    private final Timer timer;

    private void doMaintenance() {
        try {
            if ( storeSessions() ) {
                logger.fine( "Deleting stale stored secure conversation sessions." );
                storedSecureConversationSessionManager.deleteStale( System.currentTimeMillis() );
                logger.fine( "Deleted stale stored secure conversation sessions." );
            } else {
                logger.fine( "Deleting all stored secure conversation sessions." );
                storedSecureConversationSessionManager.deleteAll();
                logger.fine( "Deleted all stored secure conversation sessions." );
            }
        } catch( Exception e ) {
            logger.log(
                    Level.WARNING,
                    "Error during secure conversation session maintenance: " + ExceptionUtils.getMessage( e ),
                    ExceptionUtils.getDebugException( e ) );
        }
    }

    private boolean storeSessions(){
        return config.getBooleanProperty( "wss.secureConversation.clusterSessions", false );
    }

    private static Config validated( final Config config ) {
        final ValidatedConfig validated = new ValidatedConfig( config, logger );

        validated.setMinimumValue( "wss.secureConversation.clusterSessionsCleanupInterval", 0 );

        return validated;
    }
}
