/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.util;

import com.l7tech.common.util.HexUtils;
import com.l7tech.common.util.UptimeMetrics;
import com.l7tech.server.ServerConfig;

import java.io.BufferedInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Run the system's "uptime" utility in a background thread.
 *
 * User: mike
 * Date: Sep 16, 2003
 * Time: 5:45:56 PM
 */
public class UptimeMonitor {
    private static final Logger logger = Logger.getLogger(UptimeMonitor.class.getName());
    private static final int REFRESH_DELAY = 1000 * 30;
    private static final String[] UPTIME_PATHS = new String[] {
        "/usr/bin/uptime",
        "/bin/uptime",
        "c:/cygwin/bin/uptime",
        "c:/opt/cygwin/bin/uptime"
    };
    private static String foundUptime = null;

    private Thread thread = null;
    private UptimeMetrics result = null;

    private static class InstanceHolder {
        private static final UptimeMonitor INSTANCE = new UptimeMonitor();
    }

    private static synchronized UptimeMonitor getInstance() {
        return InstanceHolder.INSTANCE;
    }

    private UptimeMonitor() {
        startMonitor();
    }

    private void startMonitor() {
        this.result = findUptime(UPTIME_PATHS);
        if (foundUptime != null) {
            this.thread = new Thread(new Runnable() { public void run() { runMonitorThread(); }}, "Uptime Monitor");
            thread.setDaemon(true);
            thread.start();
        }
    }

    /**
     * Given a list of candidate paths, find the path that uptime is actually at, and collect
     * it's initial output.
     *
     * @param uptimePaths  An array of paths to search.
     * @return The output of the uptime command we found, or null if we didn't find out.
     */
    private static UptimeMetrics findUptime(String[] uptimePaths) {
        for (int i = 0; i < uptimePaths.length; i++) {
            String uptimePath = uptimePaths[i];
            Process up = null;
            try {
                UptimeMetrics snapshot = runUptime(uptimePath);
                up = Runtime.getRuntime().exec(uptimePath);
                up.waitFor();
                foundUptime = uptimePath;
                logger.info("Using uptime executable: " + foundUptime);
                return snapshot;
            } catch (IOException e) {
                // loop around and try the next one
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
            } finally {
                if (up != null)
                    up.destroy();
            }
        }
        logger.warning("Did not find uptime executable on this system");
        return null;
    }

    /**
     * Query the last uptime gathered by the system.
     *
     * @return The last gathered uptime information, or null if no information has been gathered yet.
     * @throws java.io.FileNotFoundException  if uptime is not available here
     * @throws java.lang.IllegalStateException  if the monitor thread has been shut down
     */
    public static UptimeMetrics getLastUptime() throws FileNotFoundException, IllegalStateException {
        return getInstance().doGetLastUptime();
    }

    /**
     * Check if uptime metrics are available on this system.  If not, any call to getLastUptime()
     * will throw FileNotFoundException.
     *
     * @return
     */
    public static boolean isUptimeMetricsAvailable() {
        return getInstance().thread != null;
    }

    /**
     * Query the last uptime gathered by the system.
     *
     * @return The last gathered uptime information, or null if no information has been gathered yet.
     * @throws java.io.FileNotFoundException  if uptime is not available here
     * @throws java.lang.IllegalStateException  if the monitor thread has been shut down
     */
    private synchronized UptimeMetrics doGetLastUptime() throws FileNotFoundException, IllegalStateException {
        if (foundUptime == null)
            throw new FileNotFoundException("The uptime executable was not found on this sytem.");
        if (thread == null)
            throw new IllegalStateException("The UptimeMonitor has been shut down.");
        return result;
    }

    /**
     * Shut down the uptime monitor thread.  It is not possible to bring it back after
     * it has been shut down, so only do this if you are exiting.
     */
    public static void shutdownMonitorThread() {
        getInstance().doShutdown();
    }

    private synchronized void doShutdown() {
        if (thread != null) {
            thread.interrupt();
            thread = null;
        }
    }

    /**
     * Attempt to run uptime at the provided path, and update our uptime metrics if successful.
     * @param uptimePath    the path to try
     * @return the string output by the uptime program, limited to 512 bytes
     * @throws java.io.IOException   if the program could not be executed
     * @throws java.lang.InterruptedException  if this thread was interrupted
     */
    private static UptimeMetrics runUptime(String uptimePath) throws IOException, InterruptedException {
        Process up = null;
        InputStream got = null;
        try {
            up = Runtime.getRuntime().exec(uptimePath);
            got = new BufferedInputStream(up.getInputStream());
            byte[] buff = HexUtils.slurpStream(got, 512);
            String uptimeOutput = new String(buff);
            UptimeMetrics snapshot = new UptimeMetrics(uptimeOutput, ServerConfig.getInstance().getServerBootTime() );
            up.waitFor();
            return snapshot;
        } finally {
            if (got != null)
                got.close();
            if (up != null)
                up.destroy();
        }
    }

    /**
     * Runs in own thread.  Gather uptime information every 30 seconds.
     */
    private void runMonitorThread() {
        logger.info("Uptime monitor thread is starting");
        long failuresInRow = 0;
        for (;;) {
            try {
                UptimeMetrics snapshot = runUptime(foundUptime);
                synchronized (this) {
                    this.result = snapshot;
                }
                failuresInRow = 0;
            } catch (IOException e) {
                if (failuresInRow < 5)
                    logger.log(Level.WARNING, "Unable to get system uptime from the uptime executable", e);
                failuresInRow++;
            } catch (InterruptedException e) {
                logger.info("Uptime monitor thread is shutting down");
                return;
            }

            // Do sleep in a different catch block, since it should always happen
            try {
                Thread.sleep(REFRESH_DELAY);
            } catch (InterruptedException e) {
                logger.info("Uptime monitor thread is shutting down");
                return;
            }
        }
    }
}
