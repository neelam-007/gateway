package com.l7tech.console.event;

import java.util.EventListener;

/**
 * Listener to the action for a certificate.
 * <p/>
 * <p> Copyright (C) 2004 Layer 7 Technologies Inc.</p>
 * <p> @author fpang </p>
 * $Id$
 */
public interface CertListener extends EventListener {

    /**
     * Fired when a cert is selected.
     *
     * @param ce event describing the action
     */
    public void certSelected(CertEvent ce);

}


