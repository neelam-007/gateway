/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.common.http.cache;

import static com.l7tech.common.http.cache.HttpObjectCache.WAIT_LATEST;
import static com.l7tech.common.http.cache.HttpObjectCache.WAIT_NEVER;
import com.l7tech.common.http.*;
import com.l7tech.common.mime.ContentTypeHeader;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.net.URL;
import java.util.logging.Logger;

/**
 * Simple tests for HttpObjectCache.
 */
public class HttpObjectCacheTest extends TestCase {
    private static Logger log = Logger.getLogger(HttpObjectCacheTest.class.getName());

    private static final long TEST_POLL_AGE = 500; // in ms
    private static final long SHORT_DELAY = TEST_POLL_AGE / 5; // sleep that should give things time to happen, but avoid triggering poll_age
    private static final long POLL_DELAY = TEST_POLL_AGE * 2; // sleep that should cause POLL_AGE to expire

    public HttpObjectCacheTest(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(HttpObjectCacheTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    private static class UserObj {
        final String blat;

        public UserObj(String blat) {
            this.blat = blat;
        }

        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            final UserObj userObj = (UserObj)o;
            //noinspection RedundantIfStatement
            if (blat != null ? !blat.equals(userObj.blat) : userObj.blat != null) return false;
            return true;
        }

        public int hashCode() {
            return (blat != null ? blat.hashCode() : 0);
        }
    }

    public void testSingleThreaded() throws Exception {
        // Use accelerated time while testing with mock http client: poll if data more than TEST_POLL_AGE ms old
        HttpObjectCache httpObjectCache = new HttpObjectCache(500, TEST_POLL_AGE);

        final String userObjStr = "Howza!";
        final UserObj userObj = new UserObj(userObjStr);
        MockGenericHttpClient hc = new MockGenericHttpClient(200,
                                                             new GenericHttpHeaders(new HttpHeader[0]),
                                                             ContentTypeHeader.OCTET_STREAM_DEFAULT,
                                                             (long)userObjStr.getBytes().length,
                                                             userObjStr.getBytes());
        URL url = new URL("http://blat/");
        GenericHttpRequestParams params = new GenericHttpRequestParams(url);

        // Make sure we include a last modified header, although we'll just send garbage
        hc.setHeaders(new GenericHttpHeaders(new HttpHeader[] {new GenericHttpHeader(HttpConstants.HEADER_LAST_MODIFIED,
                                                                                     "blarglebliff")}));

        HttpObjectCache.UserObjectFactory factory = new HttpObjectCache.UserObjectFactory() {
            public Object createUserObject(String url, GenericHttpResponse response) {
                return new UserObj(userObjStr);
            }
        };

        hc.clearResponseCount();
        HttpObjectCache.FetchResult result = httpObjectCache.fetchCached(hc, params, WAIT_NEVER, factory);
        assertTrue(hc.getResponseCount() == 1);
        assertNotNull(result);
        assertNull(result.getException());
        final Object firstUo = result.getUserObject();
        assertNotNull(firstUo);
        assertEquals(userObj, firstUo);

        // Immediately try a second fetch and ensure the HTTP client is not contacted
        result = httpObjectCache.fetchCached(hc, params, WAIT_LATEST, factory);
        assertTrue(hc.getResponseCount() == 1);
        assertNotNull(result);
        assertNull(result.getException());
        assertNotNull(result.getUserObject());
        assertTrue(result.getUserObject() == firstUo);

        // Wait long enough to trigger a poll, then try a third request, and ensure that if-modified-since worked.
        Thread.sleep(POLL_DELAY);
        hc.setResponseStatus(HttpConstants.STATUS_NOT_MODIFIED);
        result = httpObjectCache.fetchCached(hc, params, WAIT_LATEST, factory);
        assertTrue(hc.getResponseCount() == 2);
        assertNotNull(result);
        assertNull(result.getException());
        assertNotNull(result.getUserObject());
        assertTrue(result.getUserObject() == firstUo); // must not have replaced the object

        // Immediately try a fourth request, which should not poll
        result = httpObjectCache.fetchCached(hc, params, WAIT_LATEST, factory);
        assertTrue(hc.getResponseCount() == 2);
        assertNotNull(result);
        assertNull(result.getException());
        assertNotNull(result.getUserObject());
        assertTrue(result.getUserObject() == firstUo); // must not have replaced the object

        // Wait long enough to trigger another poll, then try a fifth request, which should do a new download
        Thread.sleep(POLL_DELAY);
        hc.setResponseStatus(200);
        result = httpObjectCache.fetchCached(hc, params, WAIT_LATEST, factory);
        assertTrue(hc.getResponseCount() == 3);
        assertNotNull(result);
        assertNull(result.getException());
        assertNotNull(result.getUserObject());
        assertTrue(result.getUserObject() != firstUo); // must have replaced the object
    }


    /** Class that represents an execution context for the multithreaded test. */
    private static class TestThread implements Runnable {
        private static int nextNum = 1;
        private final int num = nextNum++;
        private int nextReq = 1;
        private final HttpObjectCache httpObjectCache;
        private GenericHttpClient client = null;
        private GenericHttpRequestParams params = null;
        private HttpObjectCache.WaitMode waitForNewestResult = WAIT_NEVER;
        private HttpObjectCache.UserObjectFactory factory = null;
        /** @noinspection FieldCanBeLocal*/
        private volatile boolean started = false;
        private volatile boolean running = false;
        private int numRequestsStarted = 0;
        private int[] numRequestsFinished = new int[] { 0, 0, 0, 0 }; // failed, downloading_now, success, cached
        private HttpObjectCache.FetchResult lastFetchResult = null;
        private Throwable lastException = null;
        private Thread thread = null;

        public TestThread(HttpObjectCache cache, GenericHttpClient client, GenericHttpRequestParams params, HttpObjectCache.WaitMode waitForNewestResult, HttpObjectCache.UserObjectFactory factory) {
            this.httpObjectCache = cache;
            this.client = client;
            this.params = params;
            this.waitForNewestResult = waitForNewestResult;
            this.factory = factory;
        }

        /**
         * Start a test request issuing in a newly-created thread.   Must call in main thread.
         * Does not return until test thread has started running.
         */
        public synchronized void startRequest() throws InterruptedException {
            started = running = false;
            thread = new Thread(this, "TestThread" + num + " (req #" + nextReq++ + ")");
            thread.start();
            while (!started) wait();
            log.info("Thread " + thread.getName() + " has started");
        }

        /** Called in test thread. */
        public void run() {
            try {
                synchronized (this) {
                    numRequestsStarted++;
                    started = true;
                    running = true;
                    notifyAll();
                }

                log.info("Thread " + Thread.currentThread().getName() + " entering fetchCached()");
                HttpObjectCache.FetchResult result =
                        httpObjectCache.fetchCached(client, params, waitForNewestResult, factory);
                log.info("Thread " + Thread.currentThread().getName() + " leaving fetchCached()");

                synchronized (this) {
                    numRequestsFinished[result.getResult()]++;
                    lastFetchResult = result;
                    lastException = null;
                    running = false;
                }

            } catch (Throwable t) {
                // Thread failed -- record why
                synchronized (this) {
                    lastFetchResult = null;
                    lastException = t;
                    running = false;
                }
            }
        }

        public void joinThread() throws InterruptedException {
            final Thread t;
            synchronized (this) {
                t = thread;
            }
            if (t != null)
                t.join();
        }

        public void close() {
            final Thread t;
            synchronized (this) {
                t = thread;
            }
            if (t != null)
                t.interrupt();
        }

        public synchronized int getNumRequestsStarted() { return numRequestsStarted; }
        public synchronized int getNumRequestsFailed() { return numRequestsFinished[HttpObjectCache.RESULT_DOWNLOAD_FAILED]; }
        public synchronized int getNumRequestsAsync() { return numRequestsFinished[HttpObjectCache.RESULT_DOWNLOADING_NOW]; }
        public synchronized int getNumRequestsSuccess() { return numRequestsFinished[HttpObjectCache.RESULT_DOWNLOAD_SUCCESS]; }
        public synchronized int getNumRequestsCached() { return numRequestsFinished[HttpObjectCache.RESULT_USED_CACHED]; }
        public synchronized int getNumRequestsFinished() {
            int total = 0;
            for (int aNumRequestsFinished : numRequestsFinished) {
                total += aNumRequestsFinished;
            }
            return total;
        }

        public synchronized void setClient(GenericHttpClient client) {
            this.client = client;
        }

        public synchronized void setParams(GenericHttpRequestParams params) {
            this.params = params;
        }

        public synchronized void setWaitForNewestResult(HttpObjectCache.WaitMode waitForNewestResult) {
            this.waitForNewestResult = waitForNewestResult;
        }

        public synchronized void setFactory(HttpObjectCache.UserObjectFactory factory) {
            this.factory = factory;
        }

        public static int getNextNum() {
            return nextNum;
        }

        public synchronized HttpObjectCache.FetchResult getLastFetchResult() {
            return lastFetchResult;
        }

        public synchronized Throwable getLastException() {
            return lastException;
        }

        /** @return true if the request thread has entered fetchCached() but has not yet returned. */
        public synchronized boolean isWaiting() {
            return running;
        }
    }

    public void testMultiThreaded() throws Exception {
        // Use accelerated time while testing with mock http client: poll if data more than TEST_POLL_AGE ms old
        HttpObjectCache httpObjectCache = new HttpObjectCache(500, TEST_POLL_AGE);

        final String userObjStr = "Howza!";
        final UserObj userObj = new UserObj(userObjStr);
        MockGenericHttpClient hc = new MockGenericHttpClient(200,
                                                             new GenericHttpHeaders(new HttpHeader[0]),
                                                             ContentTypeHeader.OCTET_STREAM_DEFAULT,
                                                             (long)userObjStr.getBytes().length,
                                                             userObjStr.getBytes());
        URL url = new URL("http://blatty44/");
        GenericHttpRequestParams params = new GenericHttpRequestParams(url);

        // Make sure we include a last modified header, although we'll just send garbage
        hc.setHeaders(new GenericHttpHeaders(new HttpHeader[] {new GenericHttpHeader(HttpConstants.HEADER_LAST_MODIFIED,
                                                                                     "blarglebliff")}));

        HttpObjectCache.UserObjectFactory factory = new HttpObjectCache.UserObjectFactory() {
            public Object createUserObject(String url, GenericHttpResponse response) {
                return new UserObj(userObjStr);
            }
        };

        // Make two test threads
        TestThread t1 = new TestThread(httpObjectCache, hc, params, WAIT_NEVER, factory);
        TestThread t2 = new TestThread(httpObjectCache, hc, params, WAIT_NEVER, factory);
        try {
            doTestMultiThreaded(t1, t2, hc, userObj);
        } finally {
            t1.close();
            t2.close();
        }
    }

    private void doTestMultiThreaded(TestThread t1, TestThread t2, MockGenericHttpClient hc, Object userObj)
            throws Exception
    {
        // Block HTTP requests until released
        hc.clearResponseCount();
        hc.setHoldResponses(true);

        // Thread 1 should begin downloading the URL
        t1.startRequest();
        Thread.sleep(SHORT_DELAY);
        assertNull(t1.getLastException());
        assertTrue(t1.getNumRequestsStarted() == 1);
        assertTrue(t1.isWaiting());
        assertTrue(t1.getNumRequestsFinished() == 0);

        // Thread 2 should immediately return DOWNLOADING_NOW, but without any cached info
        t2.setWaitForNewestResult(WAIT_NEVER);
        t2.startRequest();
        t2.joinThread();
        assertNull(t2.getLastException());
        assertTrue(t2.getNumRequestsStarted() == 1);
        assertFalse(t2.isWaiting());
        assertTrue(t2.getNumRequestsFinished() == 1);
        assertTrue(t2.getNumRequestsAsync() == 1);
        HttpObjectCache.FetchResult t2fr = t2.getLastFetchResult();
        assertNotNull(t2fr);
        assertTrue(t2fr.getResult() == HttpObjectCache.RESULT_DOWNLOADING_NOW);
        assertNull(t2fr.getUserObject());

        // Now tell thread 2 to wait for complete info
        t2.setWaitForNewestResult(WAIT_LATEST);
        t2.startRequest();
        Thread.sleep(SHORT_DELAY);
        assertNull(t2.getLastException());
        assertTrue(t2.getNumRequestsStarted() == 2);
        assertTrue(t2.isWaiting());
        assertTrue(t2.getNumRequestsFinished() == 1);
        assertTrue(t2.getNumRequestsAsync() == 1);

        // Unblock the download, and ensure both threads finish with the same up-to-date info
        hc.setHoldResponses(false);
        t1.joinThread();
        t2.joinThread();
        assertNull(t1.getLastException());
        assertNull(t2.getLastException());
        assertFalse(t1.isWaiting());
        assertFalse(t2.isWaiting());
        assertTrue(t1.getNumRequestsFinished() == 1);
        assertTrue(t1.getNumRequestsSuccess() == 1);
        assertTrue(t2.getNumRequestsFinished() == 2);
        assertTrue(t2.getNumRequestsSuccess() == 1);
        assertTrue(t2.getNumRequestsAsync() == 1);
        HttpObjectCache.FetchResult t1fr = t1.getLastFetchResult();
        assertNotNull(t1fr);
        assertTrue(t1fr.getResult() == HttpObjectCache.RESULT_DOWNLOAD_SUCCESS);
        t2fr = t2.getLastFetchResult();
        assertTrue(t2fr.getResult() == HttpObjectCache.RESULT_DOWNLOAD_SUCCESS);
        assertEquals(userObj, t1fr.getUserObject());
        assertEquals(userObj, t2fr.getUserObject());
        assertTrue(t1fr.getUserObject() == t2fr.getUserObject());
        assertTrue(userObj != t1fr.getUserObject());

        // Make sure the server was only hit by one of the threads
        assertTrue(hc.getResponseCount() == 1);
    }
}
