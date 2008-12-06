/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 * User: darmstrong
 * Date: Dec 5, 2008
 * Time: 3:57:15 PM
 */
package com.l7tech.gateway.standardreports;

/**
 * Intended to be thrown from within Utilities. The functions within Utilities are called by the Jasper Reports
 * engine when a report is being filled. It's not possible to add a checked exception to interface of the report filling
 * classes, so when a JRException is caught from the filling process, we can see if it was caused by this class,
 * and if so return the message to the user via the api exception. This is useful as some report exceptions cannot
 * be prevented before a report is filled, as Clusters can have different settings e.g. hourly bin retention period
 */
public class UtilityConstraintException extends RuntimeException{

    public UtilityConstraintException(String message) {
        super(message);
    }
}
