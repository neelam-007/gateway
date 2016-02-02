package com.ca.siteminder;

import com.l7tech.util.Pair;

import java.util.List;

import static com.ca.siteminder.SiteMinderContext.SessionDef;

/**
 * @author Jamie Williams - jamie.williams2@ca.com
 */
public class SiteMinderAuthResponseDetails {

    private final SessionDef sessionDef;
    private final List<SiteMinderContext.Attribute> attrList;

    public SiteMinderAuthResponseDetails(SessionDef sessionDef, List<SiteMinderContext.Attribute> attrList) {
        this.sessionDef = sessionDef;
        this.attrList = attrList;
    }

    public SessionDef getSessionDef() {
        return sessionDef;
    }

    public List<SiteMinderContext.Attribute> getAttrList() {
        return attrList;
    }
}
