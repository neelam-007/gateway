/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */
package com.l7tech.remote.rmi;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.security.SecureRandom;

/**
 * @author emil
 * @version Dec 7, 2004
 */
class SessionGenerator {
    static final Log logger = LogFactory.getLog(SessionGenerator.class);

    private static SecureRandom random = new SecureRandom();
    private static long lastTimeVal = 0;
    private static long count = 0;

    static final SessionHolder getOrCreate() {
        // join current session if one exists
        SessionHolder current = SessionHolder.getCurrent();
        if (current == null || current.getSessionToken() == null) {
            String nextSessionID = getNextSessionID();
            SessionHolder.setCurrent(new SessionHolder(nextSessionID));
            logger.warn("creating new session " + current);
        } else {
            logger.warn("Joining current session " + current);
        }
        return SessionHolder.getCurrent();
    }


    /*
      * we want to have a random string with a length of
      * 6 characters. Since we encode it BASE 36, we've to
      * modulo it with the following value:
      */
    private final static long maxRandomLen = 2176782336L; // 36 ** 6

    /*
      *  millisecons between different tics. So this means that the
      *  time string has a new value every 3 seconds:
      */
    private final static long ticDifference = 3000;

    /**
     * generate the next unique identifier
     *
     * @return the identifier
     */
    private static synchronized String getNextSessionID() {
        long n = random.nextLong();
        if (n < 0) n = -n;
        n %= maxRandomLen;
        n += maxRandomLen;

        StringBuffer sb = new StringBuffer();
        sb.append(Long.toString(n, Character.MAX_RADIX).substring(1));
        long timeVal = System.currentTimeMillis() / ticDifference;
        sb.append(Long.toString(timeVal, Character.MAX_RADIX).substring(1));

        /*
         * make the string unique: append the count since last time flip.
         */
        // count only within tics.
        if (lastTimeVal != timeVal) {
            lastTimeVal = timeVal;
            count = 0;
        }

        sb.append(Long.toString(++count, Character.MAX_RADIX));
        return sb.toString();

    }
}