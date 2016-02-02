package com.ca.siteminder;

import java.util.List;

import static com.ca.siteminder.SiteMinderContext.SessionDef;

/**
 * @author Jamie Williams - jamie.williams2@ca.com
 */
public class SiteMinderAuthResponseDetails {

    private final SessionDef sessionDef;
    private final List<SiteMinderContext.Attribute> attrList;
    private final long createdTimeStamp;

    public SiteMinderAuthResponseDetails(SessionDef sessionDef, List<SiteMinderContext.Attribute> attrList) {
        this.sessionDef = sessionDef;
        this.attrList = attrList;
        this.createdTimeStamp = System.currentTimeMillis();
    }

    public SessionDef getSessionDef() {
        return sessionDef;
    }

    public List<SiteMinderContext.Attribute> getAttrList() {
        return attrList;
    }

    public long getCreatedTimeStamp() {
        return createdTimeStamp;
    }
}
