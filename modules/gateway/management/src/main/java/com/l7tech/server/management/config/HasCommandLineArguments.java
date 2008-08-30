/**
 * Copyright (C) 2008 Layer 7 Technologies Inc.
 */
package com.l7tech.server.management.config;

/** @author alex */
public interface HasCommandLineArguments {
    /**
     * Return the extra command-line arguments that this feature would like to add to the SSG startup command
     * TODO what about features that have conflicting or overridding arguments? 
     */
    String[] getArguments();
}
