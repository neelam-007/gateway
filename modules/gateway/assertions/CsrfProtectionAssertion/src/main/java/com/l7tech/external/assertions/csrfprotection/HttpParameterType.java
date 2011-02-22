package com.l7tech.external.assertions.csrfprotection;

/**
 * Created by IntelliJ IDEA.
 * User: njordan
 * Date: 18-Feb-2011
 * Time: 11:44:05 AM
 * To change this template use File | Settings | File Templates.
 */
public enum HttpParameterType {
    GET("GET"),
    POST("POST"),
    GET_AND_POST("GET and POST");

    private String displayName;
    private HttpParameterType(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
