/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.event;

import java.util.EventListener;

/**
 * @author alex
 * @version $Revision$
 */
public interface GenericListener extends EventListener {
    void receive(Event event);
}
