package com.ca.siteminder;

import java.util.List;

/**
 * Represents Agent Config Object details for cache
 */
public class SiteMinderAcoDetails {

    private final String name;
    private final List<SiteMinderContext.Attribute> attrList;
    private final long createdTimeStamp;

    public SiteMinderAcoDetails(final String name, final List<SiteMinderContext.Attribute> attrList) {
        this.name = name;
        this.attrList = attrList;
        this.createdTimeStamp = System.currentTimeMillis();
    }

    public String getName() {
        return name;
    }

    public List<SiteMinderContext.Attribute> getAttrList() {
        return attrList;
    }

    public long getCreatedTimeStamp() {
        return createdTimeStamp;
    }
}
