package com.l7tech.external.assertions.gatewaymanagement.server.rest;

import com.l7tech.gateway.common.transport.InterfaceTag;
import com.l7tech.objectmodel.Entity;

import java.util.Set;

/**
 * This is needed because Interface tag is not an Entity.
 */
//TODO: make interface tag an entity. I don't know of any reason why it cannot be one.
public class InterfaceTagWrapper extends InterfaceTag implements Entity {

    /**
     * create a new interface tag wrapper.
     */
    private InterfaceTagWrapper(String name, Set<String> ipPatterns) {
        super(name, ipPatterns);
    }

    /**
     * create a new interface tag wrapper from an interface tag.
     */
    public static InterfaceTagWrapper fromInterfaceTag(InterfaceTag interfaceTag) {
        return new InterfaceTagWrapper(interfaceTag.getName(), interfaceTag.getIpPatterns());
    }

    /**
     * returns the interface tags name as its id (this is supposed to be unique amoung interface tags)
     *
     * @return The name of the interface tag
     */
    @Override
    public String getId() {
        return getName();
    }
}
