package com.l7tech.console.event;

import java.util.EventObject;

/**
 * Event that is deliverded by the wizard components.
 * <p>
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 */
public class WizardEvent extends EventObject {
    /** Event sent when the user cancels the wizard. */
    static public int CANCELED = 1;
    /** Event sent when the user has finished the wizard. */
    static public int FINISHED = 2;
    /** Event sent after a new page has been selected. */
    static public int SELECTION_CHANGED = 3;

    private final int id;

    public WizardEvent(Object source, int id) {
        super(source);
        this.id = id;
    }

    public int getEventId() {
        return id;
    }
}
