package com.l7tech.server.log;

import java.util.logging.LogRecord;
import java.util.concurrent.atomic.AtomicReference;
import java.util.Collections;
import java.util.List;
import java.util.Collection;
import java.util.ArrayList;

/**
 * MessageSink that dispatches to other MessageSinks.
 *
 * @author Steve Jones
 */
public class DispatchingMessageSink implements MessageSink {

    //- PUBLIC

    /**
     * Create a dispatching sink with no initial clients.
     */
    public DispatchingMessageSink() {

    }

    /**
     * Set the sink list to the given collection.
     *
     * <p>Any null items and duplicates will be ignored.</p>
     *
     * @param sinks The new list of message sinks
     */
    public void setMessageSinks(Collection<MessageSink> sinks) {
        ArrayList<MessageSink> internal = new ArrayList();

        if ( sinks != null ) {
            internal.ensureCapacity(sinks.size());

            for ( MessageSink sink : sinks ) {
                if ( sink != null && !internal.contains(sink) ) {
                    internal.add(sink);
                }
            }
        }

        list.set(Collections.unmodifiableList(internal));
    }

    /**
     * Dispatch the given message to the current sink list.
     *
     * @param category The message category
     * @param record The message details
     */
    public void message(final MessageCategory category, final LogRecord record) {
        List<MessageSink> sinks = list.get();

        for ( MessageSink sink : sinks ) {
            try {
                sink.message(category, record);
            } catch (Exception exception) {
                // not a good idea to log here ...
                exception.printStackTrace();
            }
        }
    }

    //- PRIVATE

    private final AtomicReference<List<MessageSink>> list = new AtomicReference(Collections.EMPTY_LIST);
}
