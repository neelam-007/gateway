package com.l7tech.external.assertions.authandmgmtserviceinstaller;

/**
 * Constants used by Api Portal Authentication and Management Service Installer package
 *
 * @author ghuang
 */
public class ApiPortalAuthAndMgmtServiceInstallerConstants {
    public static final long USER_ACCOUNT_CONTROL_MIN = 1;
    public static final long USER_ACCOUNT_CONTROL_MAX = 100613115;

    public final static String MSAD_LDAP_TEMPLATE_NAME = "MicrosoftActiveDirectory";
    public final static String GENERIC_LDAP_TEMPLATE_NAME = "GenericLDAP";

    public static final String INSTALLER_PUBLISHED_SERVICE_NAME =  "API Portal Authentication and Management Service";
    public static final String INSTALLER_NAME =  "API Portal Authentication and Management Service Installer";
    public static final String NS_INSTALLER_VERSION = "http://ns.l7tech.com/2013/02/api-portal-auth-service-installer-bundle";
    public static final String INSTALLER_BUNDLE_INFO_FILE_PATH = "/com/l7tech/external/assertions/authandmgmtserviceinstaller/bundle/ApiPortalAuthServiceInstallerBundleInfo.xml";
    public static final String POLICY_RESOURCE_BASE_NAME = "com/l7tech/external/assertions/authandmgmtserviceinstaller/resources/";
    public static final String AUTHENTICATION_MAIN_POLICY_FILE_NAME = "api-portal_authentication-main-policy.xml";
    public static final String MSAD_LDAP_AUTH_SECTION_POLICY_FILE_NAME = "api-portal_authentication-msad-ldap-auth-section-policy.xml";
    public static final String OPEN_LDAP_AUTH_SECTION_POLICY_FILE_NAME = "api-portal_authentication-open-ldap-auth-section-policy.xml";

    public static final String DEFAULT_RESOLUTION_URI = "/portalAuth/*";

    // Default values for Authentication configuration
    public static final String AUTH_MSAD_LDAP_DEFAULT_SEARCH_FILTER = "cn=~";
    public static final String AUTH_OPEN_LDAP_DEFAULT_USER_SEARCH_FILTER = "(uid=~)";
    public static final String AUTH_OPEN_LDAP_DEFAULT_GROUP_SEARCH_FILTER = "(uniqueMember=uid=~,{0})";

    // Default values for Management configuration
    public static final String MGMT_MSAD_DEFAULT_USER_ACCOUNT_CONTROL = "66048";
    public static final String MGMT_MSAD_DEFAULT_GROUP_NAME = "";
    public static final String MGMT_MSAD_DEFAULT_GROUP_NAME_FOR_ORGADMIN = "";
    public static final String MGMT_MSAD_DEFAULT_USER_DN_CREATE_FORMAT = "cn=~,{0}";
    public static final String MGMT_MSAD_DEFAULT_USER_DN_MEMBER_FORMAT = "cn=~,{0}";
    public static final String MGMT_MSAD_DEFAULT_GROUP_DN = "{0}";
    public static final String MGMT_MSAD_DEFAULT_USER_SEARCH_FILTER = "cn=~";
    public static final String MGMT_MSAD_DEFAULT_GROUP_ATTRIBUTE = "member";
    public static final String MGMT_MSAD_DEFAULT_USERNAME_ATTRIBUTE = "sAMAccountName";
    public static final String MGMT_MSAD_DEFAULT_PASSWORD_ATTRIBUTE = "unicodePwd";
    public static final String MGMT_MSAD_DEFAULT_OBJECTCLASS = "user";

    public static final String MGMT_OPENLDAP_DEFAULT_GROUP_NAME = "";
    public static final String MGMT_OPENLDAP_DEFAULT_GROUP_NAME_FOR_ORGADMIN = "";
    public static final String MGMT_OPENLDAP_DEFAULT_USER_DN_CREATE_FORMAT = "cn=~,{0}";
    public static final String MGMT_OPENLDAP_DEFAULT_USER_DN_MEMBER_FORMAT = "uid=~,{0}";
    public static final String MGMT_OPENLDAP_DEFAULT_GROUP_DN = "{0}";
    public static final String MGMT_OPENLDAP_DEFAULT_USER_SEARCH_FILTER = "(uid=~)";
    public static final String MGMT_OPENLDAP_DEFAULT_GROUP_ATTRIBUTE = "uniqueMember";
    public static final String MGMT_OPENLDAP_DEFAULT_USERNAME_ATTRIBUTE = "cn";
    public static final String MGMT_OPENLDAP_DEFAULT_PASSWORD_ATTRIBUTE = "userPassword";
    public static final String MGMT_OPENLDAP_DEFAULT_OBJECTCLASS = "top, person, inetOrgPerson, organizationalPerson";

    // Left Comment names for searching purpose in the MSAD LDAP Auth section policy file
    public static final String LEFT_COMMENT_NAME_AUTH_MSAD_LDAP_SEARCH_FILTER ="microsoft_ad_ldap_search_filter";

    // Left Comment names for searching purpose in the OPEN LDAP Auth section policy file
    public static final String LEFT_COMMENT_NAME_AUTH_OPEN_LDAP_USER_SEARCH_FILTER ="open_ldap_user_search_filter";
    public static final String LEFT_COMMENT_NAME_AUTH_OPEN_LDAP_GROUP_SEARCH_FILTER ="open_ldap_group_search_filter";

    // Left Comment names for searching purpose in the main policy file
    public static final String LEFT_COMMENT_NAME_PREFIX = "prefix";

    public static final String LEFT_COMMENT_NAME_SSG_LDAP_PROVIDER_NAME = "ssg_ldap_provider_name";
    public static final String LEFT_COMMENT_NAME_MGMT_BASE_DN ="base_dn";
    public static final String LEFT_COMMENT_NAME_MGMT_USER_ACCOUNT_CONTROL ="userAccountControl";
    public static final String LEFT_COMMENT_NAME_MGMT_GROUP_NAME ="groupName";
    public static final String LEFT_COMMENT_NAME_MGMT_GROUP_NAME_FOR_ORGADMIN ="groupNameForOrgAdmin";
    public static final String LEFT_COMMENT_NAME_MGMT_USER_DN_CREATE_FORMAT ="user_dn_create_format";
    public static final String LEFT_COMMENT_NAME_MGMT_USER_DN_MEMBER_FORMAT ="user_dn_member_format";
    public static final String LEFT_COMMENT_NAME_MGMT_GROUP_DN ="group_dn";
    public static final String LEFT_COMMENT_NAME_MGMT_USER_SEARCH_FILTER ="user_search_filter";
    public static final String LEFT_COMMENT_NAME_MGMT_GROUP_ATTRIBUTE ="group_attribute";
    public static final String LEFT_COMMENT_NAME_MGMT_USERNAME_ATTRIBUTE ="username_attribute";
    public static final String LEFT_COMMENT_NAME_MGMT_PASSWORD_ATTRIBUTE ="password_attribute";
    public static final String LEFT_COMMENT_NAME_MGMT_OBJECTCLASS ="objectClassList";
    public static final String LEFT_COMMENT_NAME_MGMT_HAS_UID_ATTRIBUTES ="has_uid_attribute";

    public static final String LEFT_COMMENT_NAME_AUTHENTICATION_PROVIDER = "Authentication Provider(s)";
    public static final String LEFT_COMMENT_NAME_MANAGEMENT_HANDLER = "management()";
    public static final String LEFT_COMMENT_NAME_MGMT_QUERY_LDAP = "query_ldap_mgmt";
}