/**
 * LAYER 7 TECHNOLOGIES, INC<br/>
 *
 * User: flascell<br/>
 * Date: Apr 4, 2005<br/>
 */
package com.l7tech.server.sla;

import java.io.Serializable;

/**
 * This class serves the purpose of hibername mapping for a row in the counted_hits table. Which is in turn managed
 * through the CountedHitsManager.
 *
 * The counted_hits table records each hit that goes through a counter (ThroughputAssertion) at run time. It is used
 * to reconstitute teh CounterCache when the gateway reboots for example.
 *
 * @deprecated this is no longer needed
 * @author flascelles@layer7-tech.com
 * todo erase this class when refactor is complete
 */
public class CountedHitRecord implements Serializable {
    private long counterId;
    private long ts;
    private long hitId=-1; // this property is totally useless and is just there to allow hibernate mapping

    public long getCounterId() {
        return counterId;
    }

    public void setCounterId(long counterId) {
        this.counterId = counterId;
    }

    public long getTs() {
        return ts;
    }

    public void setTs(long ts) {
        this.ts = ts;
    }

    public long getHitId() {
        return hitId;
    }

    public void setHitId(long hitId) {
        this.hitId = hitId;
    }
}
