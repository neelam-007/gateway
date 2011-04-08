package com.l7tech.gateway.common.audit;

import com.l7tech.util.Pair;

import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 */
public class MessagesUtil {
    static {
        registerWellKnownMessages();
    }

    // - PUBLIC

    /**
     * Get an audit allowing for any registered filter to be ran before returning the Audit.
     *
     * @param id message id
     * @return a Pair of Boolean and AuditDetailMessage. If the boolean is true, the the message exists in static
     * configuration. If false, then the requester asked for a message id which does not exist on the system. If true
     * and the AuditDetailMessage is null, then it was filtered. Otherwise the retunred AuditDetailMessage may or may
     * not contain a Level which was modified by the filter. 
     */
    public static Pair<Boolean, AuditDetailMessage> getAuditDetailMessageByIdWithFilter(int id) {
        AuditDetailMessage message = Messages.getAuditDetailMessageById(id);
        if(message == null){
            //does not exist
            return new Pair<Boolean, AuditDetailMessage>(false, null);
        }

        final AuditDetailLevelFilter levelFilter = auditMessageLevelFilter.get();
        if (levelFilter != null){
            final Level level = levelFilter.filterLevelForAuditDetailMessage(id, message.getLevel());
            if(level != null){
                if(!message.getLevel().equals(level)){
                    final Set<AuditDetailMessage.Hint> hints = message.getHints();
                    message = new AuditDetailMessage(id, level, message.getMessage());
                    for (AuditDetailMessage.Hint hint : hints) {
                        message.getHints().add(AuditDetailMessage.Hint.getHint(hint));
                    }
                }
            } else {
                //message has been filtered to NEVER
                message = null;
            }
        }
        return new Pair<Boolean, AuditDetailMessage>(true, message);
    }

    public static AuditDetailMessage getAuditDetailMessageById(int id) {
        return Messages.getAuditDetailMessageById(id);
    }

    /**
     * Register a filter which can modify the <code>Level</code> returned by getAuditDetailMessageById(int).
     * Only one filter can be set.
     *
     * @param filter filter to use
     * @throws IllegalStateException if more than one filter is registered.
     */
    public static void registerMessageLevelFilter(AuditDetailLevelFilter filter){
        final AuditDetailLevelFilter oldFilter = auditMessageLevelFilter.getAndSet(filter);
        if(oldFilter != null){
            logger.log(Level.WARNING, "Only a single audit detail level filter may be registered at a time in MessagesUtil");
        }
    }

    public static interface AuditDetailLevelFilter {
        /**
         * Filter the level for the audit message with the specified id.
         *
         * <ul>
         * <li>If the audit detail should not be audited, then null should be returned.
         * <li>If the audit detail Level should be changed, then return the new level.
         * <li>If no change is required, then return the defaultLevel supplied. This is passed for convenience so that it
         * does not need to be looked up and cause a potential loop.
         * </ul>
         *
         *
         * @param id Audit detail id.
         * @param defaultLevel default Level for the supplied id. Implementations do not need to check that this is
         * the correct level for the id. This is used to signal that no change was required. Required.
         * @return the new Level if the filter changed it, null if the Audit should not be audited, otherwise the Level supplied.
         */
        public Level filterLevelForAuditDetailMessage(int id, Level defaultLevel);
    }

    // - PRIVATE

    final private static AtomicReference<AuditDetailLevelFilter> auditMessageLevelFilter = new AtomicReference<AuditDetailLevelFilter>();
    final private static Logger logger = Logger.getLogger(MessagesUtil.class.getName());

    // Make sure these always get loaded, so the static intializers run (1.5 safe)
    private static void registerWellKnownMessages() {
        new CommonMessages();
        new AssertionMessages();
        new BootMessages();
        new AdminMessages();
        new MessageProcessingMessages();
        new SystemMessages();
        new ServiceMessages();
    }
}