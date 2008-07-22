package com.l7tech.console.event;

import java.util.EventListener;

/**
 * <p> Copyright (C) 2005 Layer 7 Technologies Inc.</p>
 * <p> @author fpang </p>
 * $Id$
 */
public interface WsdlListener extends EventListener {
    /**
     * Fired when a WSDL is selected.
     *
     * @param event describing the action
     */
    public void wsdlSelected(WsdlEvent event);
}
