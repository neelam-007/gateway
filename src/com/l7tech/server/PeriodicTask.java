package com.l7tech.server;

/**
 * @author alex
 * @version $Revision$
 */
public interface PeriodicTask extends Runnable {
    long getFrequency();
}
