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

    public SiteMinderResourceDetails(ResourceContextDef resContextDef, RealmDef realmDef,
                                     List<AuthenticationScheme> authSchemes) {
        this.resContextDef = resContextDef;
        this.realmDef = realmDef;
        this.authSchemes = authSchemes;
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
}
