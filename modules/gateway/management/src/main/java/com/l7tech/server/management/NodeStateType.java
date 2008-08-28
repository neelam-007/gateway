/**
 * Copyright (C) 2008 Layer 7 Technologies Inc.
 */
package com.l7tech.server.management;

import javax.xml.bind.annotation.XmlEnum;

/**
 * The different states a cluster node can be in, as understood by the process controller. 
 * @author alex
 */
@XmlEnum(String.class)
public enum NodeStateType {
    /** Initial state: the status of the node is not yet known */
    UNKNOWN,

    /** It was previously determined that the node is not running; the PC has started it. */
    STARTING,

    /** It couldn't be started--try again after awhile */
    WONT_START,

    /** The node was started and is apparently fully operational. */
    RUNNING,

    /** The node was running but has crashed. The PC should restart it ASAP. Equivalent to {@link #UNKNOWN} from an operational standpoint. */
    CRASHED,

    /** The node has been asked to shutdown but it may not have finished doing so yet. */
    STOPPING,

    /** The node was stopped and will not be automatically restarted */
    STOPPED,;
}
