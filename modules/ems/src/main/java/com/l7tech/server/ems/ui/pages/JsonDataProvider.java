package com.l7tech.server.ems.ui.pages;

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


    /**
     * Set the data on this provider. Note the data beign set, is intended to be the payload of uploaded JSON
     * data from a client. As a result the jsonData object is either this String OR a JSONException, if any exception
     * was thrown whilst trying to retrieve the json data from the request.
     * @param jsonData this object can be either 1) a JSON formatted String or 2) a JSONException
     * @throws IllegalArgumentException If the jsonData object does not fall into one of the 2 above categories, then
     *  this exception should be thrown
     */
    void setData(Object jsonData);
}
