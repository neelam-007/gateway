package com.ca.siteminder;

/**
 * Copyright: Layer 7 Technologies, 2013
 * User: ymoiseyenko
 * Date: 6/17/13
 */
public interface SiteMinderAgentConstants {
    public static final String SITEMINDER_PREFIX = "siteminder";
    public static final String ATTR_USERDN = "ATTR_USERDN";
    public static final String ATTR_SESSIONSPEC = "ATTR_SESSIONSPEC";
    public static final String ATTR_SESSIONID = "ATTR_SESSIONID";
    public static final String ATTR_USERNAME = "ATTR_USERNAME";
    public static final String ATTR_CLIENTIP = "ATTR_CLIENTIP";
    public static final String ATTR_DEVICENAME = "ATTR_DEVICENAME";
    public static final String ATTR_IDLESESSIONTIMEOUT = "ATTR_IDLESESSIONTIMEOUT";
    public static final String ATTR_MAXSESSIONTIMEOUT = "ATTR_MAXSESSIONTIMEOUT";
    public static final String ATTR_STARTSESSIONTIME = "ATTR_STARTSESSIONTIME";
    public static final String ATTR_LASTSESSIONTIME = "ATTR_LASTSESSIONTIME";
    public static final String ATTR_CURRENTSERVERTIME = "ATTR_CURRENTSERVERTIME";
    public static final String ATTR_USERMSG = "ATTR_USERMSG";
    public static final String ATTR_IDENTITYSPEC = "ATTR_IDENTITYSPEC";
    public static final String ATTR_USERUNIVERSALID = "ATTR_USERUNIVERSALID";
    public static final String ATTR_SERVICE_DATA = "ATTR_SERVICE_DATA";
    public static final String ATTR_STATUS_MESSAGE = "ATTR_STATUS_MESSAGE";
    public static final String ATTR_AUTH_DIR_NAME = "ATTR_AUTH_DIR_NAME";
    public static final String ATTR_AUTH_DIR_NAMESPACE = "ATTR_AUTH_DIR_NAMESPACE";
    public static final String ATTR_AUTH_DIR_OID = "ATTR_AUTH_DIR_OID";
    public static final String ATTR_AUTH_DIR_SERVER = "ATTR_AUTH_DIR_SERVER";
    public static final String ATTR_SESSIONDRIFT = "ATTR_SESSIONDRIFT";
    public static final String SESS_DEF_REASON = "SESS_DEF_REASON";
    public static final String ATTR_ONACCEPTTEXT = "ATTR_ONACCEPTTEXT";
    public static final String ATTR_ONACCEPTREDIRECT = "ATTR_ONACCEPTREDIRECT";
    public static final String ATTR_HTTP_COOKIE_VALUE = "ATTR_HTTP_COOKIE_VALUE";
    public static final String ATTR_DENIED_REDIRECT_NAME = "ATTR_227";
    public static final String ATTR_SSOZONE = "ATTR_SSOZONE";
    public static final String ATTR_SESSION_COOKIE_STRING = "ATTR_SESSION_COOKIE_STRING";

    public static final String ATTR_ACO_SSOZONE_NAME = "ATTR_ACO_SSOZoneName";
    public static final String ATTR_ACO_COOKIE_PATH = "ATTR_ACO_CookiePath";
    public static final String ATTR_ACO_COOKIE_PATH_SCOPE = "ATTR_ACO_CookiePathScope";
    public static final String ATTR_ACO_COOKIE_DOMAIN = "ATTR_ACO_CookieDomain";
    public static final String ATTR_ACO_COOKIE_DOMAIN_SCOPE = "ATTR_ACO_CookieDomainScope";
    public static final String ATTR_ACO_USE_SECURE_COOKIES = "ATTR_ACO_UseSecureCookies";
    public static final String ATTR_ACO_USE_HTTP_ONLY_COOKIES = "ATTR_ACO_UseHTTPOnlyCookies";
    public static final String ATTR_ACO_PERSISTENT_COOKIES = "ATTR_ACO_PersistentCookies";
    public static final String ATTR_ACO_COOKIE_VALIDATION_PERIOD = "ATTR_ACO_CookieValidationPeriod";

    public static final String ATTR_ACO_SSOZONE_NAME_DEFAULT_VALUE = "SM";
    public static final String ATTR_ACO_COOKIE_PATH_DEFAULT_VALUE = "/";
    public static final String ATTR_ACO_COOKIE_PATH_SCOPE_DEFAULT_VALUE = "0";
    public static final String ATTR_ACO_COOKIE_DOMAIN_DEFAULT_VALUE = ".";
    public static final String ATTR_ACO_COOKIE_DOMAIN_SCOPE_DEFAULT_VALUE = "0";
    public static final String ATTR_ACO_USE_SECURE_COOKIES_DEFAULT_VALUE = "no";
    public static final String ATTR_ACO_USE_HTTP_ONLY_COOKIES_DEFAULT_VALUE = "no";
    public static final String ATTR_ACO_PERSISTENT_COOKIES_DEFAULT_VALUE = "no";
    public static final String ATTR_ACO_COOKIE_VALIDATION_PERIOD_DEFAULT_VALUE = "0";

    public static final int SM_AGENT_API_INVALID_SESSIONID = -57;
    public static final int ATTR_CURRENTAGENTTIME = 153; //Added as the AgentAPI list of constants do not include the ATTR_CURRENTSERVERTIME;
    public static final int ATTR_SESSIONAGENTDRIFT = 167; //Added as the AgentAPI list of constants don't include the ATTR_SESSIONDRIFT;
    public static final int ATTR_DENIED_REDIRECT_CODE = 227;
}
