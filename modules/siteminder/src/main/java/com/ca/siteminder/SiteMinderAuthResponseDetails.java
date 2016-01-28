package com.ca.siteminder;

import com.l7tech.util.Pair;

import java.util.List;

import static com.ca.siteminder.SiteMinderContext.SessionDef;

/**
 * @author Jamie Williams - jamie.williams2@ca.com
 */
public class SiteMinderAuthResponseDetails {

    private final SessionDef sessionDef;
    private final List<Pair<String, Object>> attrList;

    public SiteMinderAuthResponseDetails(SessionDef sessionDef, List<Pair<String, Object>> attrList) {
        this.sessionDef = sessionDef;
        this.attrList = attrList;
    }

    public SessionDef getSessionDef() {
        return sessionDef;
    }

    public List<Pair<String, Object>> getAttrList() {
        return attrList;
    }
}
