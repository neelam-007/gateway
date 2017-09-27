package com.l7tech.gateway.api;

import javax.xml.bind.annotation.*;
import java.util.ArrayList;
import java.util.List;

/**
 * The managed object holds a list of Bundle objects.
 */
@XmlRootElement(name = "Bundles")
@XmlType(name = "Bundles")
public class BundleList {
    private List<Bundle> bundles = new ArrayList<>();

    @XmlElement(name = "Bundle")
    public List<Bundle> getBundles() {
        return bundles;
    }

    public void setBundles(List<Bundle> bundles) {
        this.bundles = bundles;
    }
}
