package com.l7tech.external.assertions.circuitbreaker.server;

import com.l7tech.util.TestTimeSource;
import com.l7tech.util.TimeSource;
import net.sf.saxon.functions.Count;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Basic tests for Counter.
 *
 * @author Jamie Williams - jamie.williams2@ca.com
 */
public class CounterTest {

    @Test
    public void getCountSinceTimestamp() throws Exception {
        TestTimeSource timeSource = new TestTimeSource();
        timeSource.sync();

        Counter counter = new Counter();

        counter.recordFailure(timeSource.getCurrentTimeMillis() + 10);
        counter.recordFailure(timeSource.getCurrentTimeMillis() + 20);
        counter.recordFailure(timeSource.getCurrentTimeMillis() + 30);

        assertEquals(3, counter.getCountSinceTimestamp(timeSource.getCurrentTimeMillis() + 10));
        assertEquals(2, counter.getCountSinceTimestamp(timeSource.getCurrentTimeMillis() + 20));
        assertEquals(1, counter.getCountSinceTimestamp(timeSource.getCurrentTimeMillis() + 30));
        assertEquals(0, counter.getCountSinceTimestamp(timeSource.getCurrentTimeMillis() + 40));
    }

}