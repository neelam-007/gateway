package com.ca.siteminder;

/**
 * Copyright: Layer 7 Technologies, 2013
 * User: ymoiseyenko
 * Date: 6/17/13
 */
public interface SiteMinderAgentConstants {
    public static final String DEFAULT_COOKIE_NAME = "SMSESSION";
    public static final String DEFAULT_ACTION = "POST";

    public static final String SITEMINDER_PREFIX = "siteminder";
    public static final String VAR_PREFIX = "siteminder.response.attribute.headerVar";
//    public static final String ATTR_SMSESSION = "smsession";
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
    public static final String ATTR_USERMSG = "ATTR_USERMSG";
    public static final String ATTR_IDENTITYSPEC = "ATTR_IDENTITYSPEC";
    public static final String ATTR_USERUNIVERSALID = "ATTR_USERUNIVERSALID";
    public static final String ATTR_SERVICE_DATA = "ATTR_SERVICE_DATA";
    public static final String ATTR_STATUS_MESSAGE = "ATTR_STATUS_MESSAGE";
    public static final String ATTR_AUTH_DIR_NAME = "ATTR_AUTH_DIR_NAME";
    public static final String ATTR_AUTH_DIR_NAMESPACE = "ATTR_AUTH_DIR_NAME";
    public static final String ATTR_AUTH_DIR_OID = "ATTR_AUTH_DIR_NAME";
    public static final String ATTR_AUTH_DIR_SERVER = "ATTR_AUTH_DIR_SERVER";

    public static final String SESS_DEF_REASON = "SESS_DEF_REASON";

    public static final String VAR_ATTR_USERDN = SITEMINDER_PREFIX + "." + ATTR_USERDN;
    public static final String VAR_ATTR_SESSIONSPEC = SITEMINDER_PREFIX + "." + ATTR_SESSIONSPEC;
    public static final String VAR_ATTR_SESSIONID = SITEMINDER_PREFIX + "." + ATTR_SESSIONID;
    public static final String VAR_ATTR_USERNAME = SITEMINDER_PREFIX + "." + ATTR_USERNAME;
    public static final String VAR_ATTR_CLIENTIP = SITEMINDER_PREFIX + "." + ATTR_CLIENTIP;
    public static final String VAR_ATTR_DEVICENAME = SITEMINDER_PREFIX + "." + ATTR_DEVICENAME;
    public static final String VAR_ATTR_IDLESESSIONTIMEOUT = SITEMINDER_PREFIX + "." + ATTR_IDLESESSIONTIMEOUT;
    public static final String VAR_ATTR_MAXSESSIONTIMEOUT = SITEMINDER_PREFIX + "." + ATTR_MAXSESSIONTIMEOUT;
    public static final String VAR_ATTR_STARTSESSIONTIME = SITEMINDER_PREFIX + "." + ATTR_STARTSESSIONTIME;
    public static final String VAR_ATTR_LASTSESSIONTIME = SITEMINDER_PREFIX + "." + ATTR_LASTSESSIONTIME;
    public static final String VAR_ATTR_USERMSG = SITEMINDER_PREFIX + "." + ATTR_USERMSG;
    public static final String VAR_SESS_DEF_REASON = SITEMINDER_PREFIX + "." + SESS_DEF_REASON;

    public static final String VAR_COOKIE_NAME = SITEMINDER_PREFIX + "." + "cookie.name";
    public static final String COOKIE_CONTEXT_VAR = SITEMINDER_PREFIX + "." + "smsession";
    //SiteMinder Agent API return codes
    public static final int SM_AGENT_API_SUCCESS = 0;
    public static final int SM_AGENT_API_YES = 1;
    public static final int SM_AGENT_API_NO = 2;
    public static final int SM_AGENT_API_CHALLENGE = 3;
    public static final int SM_AGENT_API_UNRESOLVED = 4;
    public static final int SM_AGENT_API_FAILURE = -1;
    public static final int SM_AGENT_API_TIMEOUT = -2;
    public static final int SM_AGENT_API_NOCONNECTION = -3;
    public static final int SM_AGENT_API_INVALID_RESCTXDEF = -50;
    public static final int SM_AGENT_API_INVALID_REALMDEF = -51;
    public static final int SM_AGENT_API_INVALID_SESSIONDEF = -53;
    public static final int SM_AGENT_API_INVALID_TSR = -54;
    public static final int SM_AGENT_API_INVALID_ATTRLIST = -55;
    public static final int SM_AGENT_API_INVALID_MGMTCTXDEF = -56;

    public static final int SM_AGENT_API_INVALID_SESSIONID = -57;
}
