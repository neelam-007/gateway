package com.l7tech.server.ems.enterprise;

/**
 *
 */
public class JSONMessage extends JSONSupport {

    //- PUBLIC

    public JSONMessage( final String message ) {
        this.message = message;
    }

    @Override
    protected void writeJson() {
        add("message", message);
    }

    //- PRIVATE

    private final String message;

}
