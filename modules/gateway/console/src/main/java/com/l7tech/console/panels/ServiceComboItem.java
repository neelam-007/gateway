package com.l7tech.console.panels;

/**
 * Represents a published service entry in a {@link com.l7tech.console.panels.ServiceComboBox}.
*/
public class ServiceComboItem implements Comparable {
    ServiceComboItem(String name, long id) {
        serviceName = name;
        serviceID = id;
    }

    @Override
    public String toString() {
        return serviceName;
    }

    String serviceName;
    long serviceID;

    @Override
    @SuppressWarnings({ "RedundantIfStatement" })
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ServiceComboItem comboItem = (ServiceComboItem) o;

        if (serviceID != comboItem.serviceID) return false;
        if (serviceName != null ? !serviceName.equals(comboItem.serviceName) : comboItem.serviceName != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result;
        result = (serviceName != null ? serviceName.hashCode() : 0);
        result = 31 * result + (int) (serviceID ^ (serviceID >>> 32));
        return result;
    }

    @Override
    public int compareTo(Object o) {
        if (o == null || ! (o instanceof ServiceComboItem)) throw new IllegalArgumentException("The compared object must be a ComboItem.");
        String originalServiceName = this.serviceName;
        String comparedServiceName = ((ServiceComboItem)o).serviceName;
        if (originalServiceName == null || comparedServiceName == null) throw new NullPointerException("Service Name must not be null.");

        return originalServiceName.toLowerCase().compareTo(comparedServiceName.toLowerCase());
    }
}
