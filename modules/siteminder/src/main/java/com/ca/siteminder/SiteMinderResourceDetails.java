package com.ca.siteminder;

import java.util.List;

import static com.ca.siteminder.SiteMinderContext.AuthenticationScheme;
import static com.ca.siteminder.SiteMinderContext.RealmDef;
import static com.ca.siteminder.SiteMinderContext.ResourceContextDef;

/**
 * @author Jamie Williams - jamie.williams2@ca.com
 */
public class SiteMinderResourceDetails {

    private final ResourceContextDef resContextDef;
    private final RealmDef realmDef;
    private final List<AuthenticationScheme> authSchemes;
    private final boolean resourceProtected;
    private final long createdTimeStamp;

    public SiteMinderResourceDetails(boolean resourceProtected, ResourceContextDef resContextDef, RealmDef realmDef,
                                     List<AuthenticationScheme> authSchemes) {
        this.resourceProtected = resourceProtected;
        this.resContextDef = resContextDef;
        this.realmDef = realmDef;
        this.authSchemes = authSchemes;
        this.createdTimeStamp = System.currentTimeMillis();
    }

    public ResourceContextDef getResContextDef() {
        return resContextDef;
    }

    public RealmDef getRealmDef() {
        return realmDef;
    }

    public List<AuthenticationScheme> getAuthSchemes() {
        return authSchemes;
    }

    public boolean isResourceProtected() {
        return resourceProtected;
    }

    public long getTimeStamp() {
        return createdTimeStamp;
    }


}
