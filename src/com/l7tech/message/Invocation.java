package com.l7tech.message;

/**
 * @author alex
 * @version $Revision$
 */
public interface Invocation {
    void setRequest( Request request );
    Request getRequest();

    void setResponse( Response response );
    Response getResponse();
}
