package com.l7tech.gateway.common.stepdebug;

/**
 * Enumeration of policy debug states.
 */
public enum DebugState {
    /**
     * Debugging not started or stopped.
     */
    STOPPED,

    /**
     * Debugging started. Waiting for request message to arrive.
     */
    STARTED,

    /**
     * Break at next line. (ie. Step in)
     */
    BREAK_AT_NEXT_LINE,

    /**
     * Break at next breakpoint. (ie. Resume, step over, and step out.)
     * For step over and step out cases, the next assertion line to break
     * is specified by SSM.
     */
    BREAK_AT_NEXT_BREAKPOINT,

    /**
     * Breakpoint reached
     */
    AT_BREAKPOINT
}