package com.l7tech.console.event;

/**
 * An abstract adapter class for receiving cert events. The methods in
 * this class are empty. This class exists as convenience for creating
 * listener objects.
 * <p/>
 * <p> Copyright (C) 2004 Layer 7 Technologies Inc.</p>
 * <p> @author fpang </p>
 * $Id$
 */
public abstract class CertListenerAdapter implements CertListener {

    /**
     * Fired when a cert is selected.
     *
     * @param ce event describing the action
     */
    public void certSelected(CertEvent ce) {
    }

}
