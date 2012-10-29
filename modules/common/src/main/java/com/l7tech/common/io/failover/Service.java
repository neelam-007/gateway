package com.l7tech.common.io.failover;

import java.util.Map;

public class Service {
    
    private String name;
    private Map properties;

    public Service() {
    }

    public Service(String name, Map properties) {
        this.name = name;
        this.properties = properties;
    }

    public String getName() {

        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Map getProperties() {
        return properties;
    }

    public void setProperties(Map properties) {
        this.properties = properties;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Service)) return false;

        Service service = (Service) o;

        if (name != null ? !name.equals(service.name) : service.name != null) return false;
        if (properties != null ? !properties.equals(service.properties) : service.properties != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (properties != null ? properties.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return name;
    }
}
