/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.util;

import com.l7tech.logging.LogManager;

import java.io.BufferedInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Run the system's "uptime" utility in a background thread.
 *
 * User: mike
 * Date: Sep 16, 2003
 * Time: 5:45:56 PM
 */
public class UptimeMonitor {
    private static final Logger logger = LogManager.getInstance().getSystemLogger();
    private static final int REFRESH_DELAY = 1000 * 30;
    private static final String[] UPTIME_PATHS = new String[] {
        "/usr/bin/uptime",
        "/bin/uptime",
        "c:/cygwin/bin/uptime",
    };
    private String foundUptime = null;
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
        foundUptime = findUptime(UPTIME_PATHS);
        if (foundUptime != null) {
            this.thread = new Thread(new Runnable() { public void run() { runMonitorThread(); }});
            thread.setDaemon(true);
            thread.start();
        }
    }

    /**
     * Given a list of candidate paths, find the path that uptime is actually at.
     *
     * @param uptimePaths  An array of paths to search.
     * @return The path of the uptime executable on this system, or null if it wasn't found.
     */
    private static String findUptime(String[] uptimePaths) {
        String found = null;
        for (int i = 0; i < uptimePaths.length; i++) {
            String uptimePath = uptimePaths[i];
            try {
                Process up = Runtime.getRuntime().exec(uptimePath);
                up.waitFor();
                found = uptimePath;
                logger.info("Using uptime executable: " + found);
                break;
            } catch (IOException e) {
                // loop around and try the next one
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return found;
            }
        }
        return found;
    }

    /**
     * Query the last uptime gathered by the system.
     *
     * @return The last gathered uptime information, or null if no information has been gathered yet.
     * @throws FileNotFoundException  if uptime is not available here
     * @throws IllegalStateException  if the monitor thread has been shut down
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
     * @throws FileNotFoundException  if uptime is not available here
     * @throws IllegalStateException  if the monitor thread has been shut down
     */
    private UptimeMetrics doGetLastUptime() throws FileNotFoundException, IllegalStateException {
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
     * Runs in own thread.  Gather uptime information every 30 seconds.
     */
    private void runMonitorThread() {
        logger.info("Uptime monitor thread is starting");
        for (;;) {
            Process up;
            try {
                up = Runtime.getRuntime().exec(foundUptime);
                up.waitFor();
                InputStream got = new BufferedInputStream(up.getInputStream());
                byte[] buff = HexUtils.slurpStream(got, 512);
                String result = new String(buff);
                UptimeMetrics snapshot = new UptimeMetrics(result);
                synchronized (this) {
                    this.result = snapshot;
                }
            } catch (IOException e) {
                logger.log(Level.WARNING, "Unable to get system uptime from the uptime executable", e);
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
