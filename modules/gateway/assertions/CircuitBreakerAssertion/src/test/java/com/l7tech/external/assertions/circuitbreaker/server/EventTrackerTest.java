package com.l7tech.external.assertions.circuitbreaker.server;

import com.l7tech.util.TestTimeSource;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Basic tests for EventTracker.
 *
 * @author Ekta Khandelwal - khaek01@ca.com
 */
public class EventTrackerTest {

    @Test
    public void getCountSinceTimestamp() throws Exception {
        TestTimeSource timeSource = new TestTimeSource();
        timeSource.sync();

        EventTracker eventTracker = new EventTracker();

        eventTracker.recordEvent(timeSource.getCurrentTimeMillis() + 10);
        eventTracker.recordEvent(timeSource.getCurrentTimeMillis() + 20);
        eventTracker.recordEvent(timeSource.getCurrentTimeMillis() + 30);

        assertEquals(3, eventTracker.getCountSinceTimestamp(timeSource.getCurrentTimeMillis() + 10));
        assertEquals(2, eventTracker.getCountSinceTimestamp(timeSource.getCurrentTimeMillis() + 20));
        assertEquals(1, eventTracker.getCountSinceTimestamp(timeSource.getCurrentTimeMillis() + 30));
        assertEquals(0, eventTracker.getCountSinceTimestamp(timeSource.getCurrentTimeMillis() + 40));
    }

}