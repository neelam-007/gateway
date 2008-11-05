package com.l7tech.server.ems.pages;

import org.mortbay.util.ajax.JSON;

import java.io.Serializable;

/**
 * Interface to be implemented by sources of JSON data.
 */
public interface JsonDataProvider extends Serializable {

    /**
     * Get JSON data.
     *
     * <p>Get the data from this provider, the returned Objects must all
     * be convertible into a JSON format. This means that they are either
     * one of the basic supported types or that they implement the
     * {@link JSON.Convertible} interface.</p>
     *
     * @return The JSON convertible data.
     * @see JSON.Convertible
     * @see JsonInteraction
     */
    Object getData();
}
