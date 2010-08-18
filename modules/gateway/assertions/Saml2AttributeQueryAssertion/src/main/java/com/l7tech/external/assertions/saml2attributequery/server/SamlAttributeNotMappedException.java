package com.l7tech.external.assertions.saml2attributequery.server;

/**
 * Created by IntelliJ IDEA.
 * User: njordan
 * Date: 9-Feb-2009
 * Time: 11:58:37 PM
 * To change this template use File | Settings | File Templates.
 */
public class SamlAttributeNotMappedException extends Exception {
    private String attributeName;

    public SamlAttributeNotMappedException(String attributeName) {
        this.attributeName = attributeName;
    }
}
