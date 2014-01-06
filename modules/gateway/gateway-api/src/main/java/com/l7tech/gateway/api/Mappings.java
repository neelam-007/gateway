package com.l7tech.gateway.api;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import java.util.List;

/**
 * This is the mappings managed object. It is meant to hold a list of mappings.
 */
@XmlRootElement(name = "Mappings")
@XmlType(name = "MappingsType", propOrder = {"mappings"})
public class Mappings {

    private List<Mapping> mappings = null;

    Mappings() {
    }

    Mappings(List<Mapping> mappings) {
        this.mappings = mappings;
    }

    @XmlElement(name = "Mapping")
    public List<Mapping> getMappings() {
        return mappings;
    }

    public void setMappings(List<Mapping> mappings) {
        this.mappings = mappings;
    }
}