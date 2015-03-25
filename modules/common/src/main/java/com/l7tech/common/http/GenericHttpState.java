package com.l7tech.common.http;

/**
 * User: steve
 * Date: Sep 21, 2005
 * Time: 2:02:49 PM
 * $Id$
 */
public class GenericHttpState {

    //- PUBLIC

    public GenericHttpState() {
    }

    public GenericHttpState(Object state) {
        this.state = state;
    }

    public Object getStateObject() {
        return state;
    }

    public void setStateObject(Object state) {
        this.state = state;
    }

    //- PRIVATE

    private Object state;
}
