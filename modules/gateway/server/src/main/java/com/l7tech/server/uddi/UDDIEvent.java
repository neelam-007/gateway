package com.l7tech.server.uddi;

/**
 * Base class for UDDI events.
 */
abstract class UDDIEvent {

    //- PROTECTED

    protected UDDIEvent() {
        this(true);
    }

    protected UDDIEvent( final boolean masterOnly ) {
        this.masterOnly = masterOnly;        
    }

    protected boolean isMasterOnly() {
        return masterOnly;
    }

    //- PRIVATE

    final boolean masterOnly;
}
