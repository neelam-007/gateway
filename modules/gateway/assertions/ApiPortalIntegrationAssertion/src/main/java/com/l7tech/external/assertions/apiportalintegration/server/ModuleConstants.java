package com.l7tech.external.assertions.apiportalintegration.server;

/**
 * Contains constants for the api portal integration module.
 */
public final class ModuleConstants {
    private ModuleConstants() {
        // do not construct me
    }

    public static final String NAMESPACE_API_KEYS = "http://ns.l7tech.com/2011/08/portal-api-keys";
    public static final String VALUE_ELEMENT_NAME = "Value";
    public static final String SERVICES_ELEMENT_NAME = "Services";
    public static final String SERVICE_ELEMENT_NAME = "S";
    public static final String SECRET_ELEMENT_NAME = "Secret";
    public static final String ID_ATTRIBUTE_NAME = "id";
    public static final String PLAN_ATTRIBUTE_NAME = "plan";
    public static final String STATUS_ATTRIBUTE_NAME = "status";
    public static final String LABEL_ATTRIBUTE_NAME = "label";
    public static final String PLATFORM_ELEMENT_NAME = "Platform";
    public static final String OAUTH_ELEMENT_NAME = "OAuth";
    public static final String OAUTHCALLBACKURL_ATTRIBUTE_NAME = "callbackUrl";
    public static final String OAUTHSCOPE_ATTRIBUTE_NAME = "scope";
    public static final String OAUTHTYPE_ATTRIBUTE_NAME = "type";
    public static final String ACCOUNTPLANMAPPINGID_ELEMENT_NAME = "AccountPlanMappingId";
    public static final String CUSTOMMETADATA_ELEMENT_NAME = "CustomMetaData";
    public static final String APPLICATIONID_ELEMENT_NAME = "ApplicationId";
    public static final String API_ID_SERVICE_PROPERTY_NAME = "portalID";

    public static final String PORTAL_API_PLANS_UI_PROPERTY = "portal.apiplans"; // should really be portal.api.plans
    public static final String TEMP_PORTAL_MANAGED_SERVICE_INDICATOR = "L7p:ApiPortalManagedServiceAssertion";
    public static final String TEMP_PORTAL_MANAGED_ENCASS_INDICATOR = "L7p:ApiPortalManagedEncassAssertion";
    public static final String PORTAL_MANAGED_SERVICE_INDICATOR = "L7p:ApiPortalIntegration";
    public static final String PORTAL_MANAGED_ENCASS_INDICATOR = "L7p:ApiPortalEncassIntegration";
    public static final String PORTAL_MANAGED_SERVICES_UI_PROPERTY = "portal.managed.services";

    // Cluster-Property for API Portal Integration required by publish API wizard.
    public static final String API_DELETED_FOLDER_ID = "portal.deleted.apis.folderId";
    public static final String OAUTH1X_FRAGMENT_GUID = "portal.oauth1.fragment.guid";
    public static final String OAUTH20_FRAGMENT_GUID = "portal.oauth2.fragment.guid";
    public static final String API_PLANS_FRAGMENT_GUID = "portal.api.plans.fragment.guid";
    public static final String ACCOUNT_PLANS_FRAGMENT_GUID = "portal.account.plans.fragment.guid";
    public static final String NOT_INSTALLED_VALUE = "not installed";
}
//
