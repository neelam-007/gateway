package com.l7tech.server.event.metrics;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionMetrics;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.concurrent.atomic.AtomicReference;

/**
 */
@RunWith(MockitoJUnitRunner.class)
public class AssertionFinishedTest {

    @Mock
    Assertion assertion;

    @Test
    public void testThreadOwnership() throws Exception {

        final AtomicReference<AssertionFinished> assertionFinishedRef = new AtomicReference<>();
        final AtomicReference<Throwable> potentialErrorRef = new AtomicReference<>();

        // create a new thread per property
        final Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    // create the event in a new thread
                    final AssertionFinished assertionFinished = new AssertionFinished(
                            PolicyEnforcementContextFactory.createPolicyEnforcementContext(null, null),
                            assertion,
                            new AssertionMetrics(System.currentTimeMillis(), System.currentTimeMillis() + 100)
                    );
                    // this thread should be able to access the content
                    assertionFinished.getAssertion();
                    assertionFinished.getAssertionMetrics();
                    assertionFinished.getContext();

                    assertionFinishedRef.set(assertionFinished);

                } catch (Throwable ex) {
                    potentialErrorRef.set(ex);
                }
            }
        });
        thread.start();
        thread.join(10000); // should be more than reasonable time to finish!!!!

        // make sure the thread finishes without errors
        Assert.assertNull(potentialErrorRef.get());


        final AssertionFinished assertionFinished = assertionFinishedRef.get();
        Assert.assertNotNull(assertionFinished);

        try {
            assertionFinished.getAssertion();
            Assert.fail("Accessing getAssertion from different thread is not allowed");
        } catch (final IllegalStateException ignore) {
            // this is expected
        }
        try {
            assertionFinished.getContext();
            Assert.fail("Accessing getContext from different thread is not allowed");
        } catch (final IllegalStateException ignore) {
            // this is expected
        }
        // assertion metrics are fine to be accessed from different threads
        assertionFinished.getAssertionMetrics();

    }

}