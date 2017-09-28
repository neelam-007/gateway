package com.l7tech.gateway.api;

import org.jetbrains.annotations.NotNull;

import javax.xml.bind.annotation.*;
import java.util.ArrayList;
import java.util.List;

/**
 * The managed object holds a list of Bundle objects.
 */
@XmlRootElement(name = "Bundles")
@XmlType(name = "Bundles", propOrder = {"bundles"})
public class BundleList {
    private List<Bundle> bundles = new ArrayList<>();

    BundleList() {}

    @XmlElement(name = "Bundle")
    @NotNull
    public List<Bundle> getBundles() {
        return bundles;
    }

    public void setBundles(List<Bundle> bundles) {
        this.bundles = bundles;
    }
}
