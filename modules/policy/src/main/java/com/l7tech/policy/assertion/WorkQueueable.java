package com.l7tech.policy.assertion;

import com.l7tech.objectmodel.Goid;

/**
 * This is an interface for assertions that use WorkQueue entities.
 */
public interface WorkQueueable {
    public Goid getWorkQueueGoid();

    public void setWorkQueueGoid(Goid id);
}
