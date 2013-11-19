package com.l7tech.external.assertions.gatewaymanagement.server.rest;

/**
 * This interface specifies that the factory can produce templates
 *
 * @author Victor Kazakov
 */
public interface TemplateFactory<R> {
    /**
     * This will return a template resource. The template resource can be used as an example resource by api
     * developers.
     *
     * @return The template resource.
     */
    R getResourceTemplate();
}
