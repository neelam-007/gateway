package com.l7tech.server;

/**
 * @deprecated Please use {@link java.util.Timer} and {@link PeriodicVersionCheck} instead!
 * @author alex
 * @version $Revision$
 */
public interface PeriodicTask extends Runnable {
    long getFrequency();
}
