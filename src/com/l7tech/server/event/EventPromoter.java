/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.event;


/**
 * Used by {@link EventManager} to exchange a generic {@link Event} for a more specific one in a data-driven way 
 * @author alex
 * @version $Revision$
 */
public interface EventPromoter {
    Event promote(Event event);
}
