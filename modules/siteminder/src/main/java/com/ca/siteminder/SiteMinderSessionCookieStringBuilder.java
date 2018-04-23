package com.ca.siteminder;

import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.TimeZone;

/**
 * SiteMinder SESSION Cookie String Builder.
 * Builds the SESSION cookie string using ACO details.
 * Example: <ssozone>SESSION=SSOTOKEN; Path=<path>; Domain=<domain>; Expires=<time in GMT>; <secure-flag>; <http-flag>
  */
public class SiteMinderSessionCookieStringBuilder {

    final SimpleDateFormat gmtDateFormat =
            new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z");

    public SiteMinderSessionCookieStringBuilder() {
        gmtDateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
    }

    public String build(@NotNull SiteMinderContext context) {
        final String ssoToken = context.getSsoToken();
        final Map<String, SiteMinderContext.Attribute> attrMap = buildAttrMap(context);

        if (StringUtils.isNotEmpty(ssoToken)) {
            StringBuilder cookieBuilder = new StringBuilder();

            cookieBuilder.append(computeSsoZoneName(attrMap, context) + "SESSION=" + ssoToken);
            cookieBuilder.append(";Path=" + computeCookiePath(attrMap, context));
            cookieBuilder.append(";Domain=" + computeCookieDomain(attrMap, context));

            if ("yes".equals(computePersistentCookies(attrMap, context))) {
                cookieBuilder.append(";Expires=" + computeExpiresTime(attrMap, context));
            }

            if ("yes".equals(computeUseSecureCookies(attrMap, context))) {
                cookieBuilder.append(";Secure");
            }

            if ("yes".equals(computeUseHttpOnly(attrMap, context))) {
                cookieBuilder.append(";HttpOnly");
            }

            return cookieBuilder.toString();
        }

        return null;
    }

    private Map<String, SiteMinderContext.Attribute> buildAttrMap(SiteMinderContext context) {
        final Map<String, SiteMinderContext.Attribute> attrMap = context.getAttrMap();

        insertMissingAttributeWithDefaultValue(attrMap, SiteMinderAgentConstants.ATTR_ACO_SSOZONE_NAME,
            SiteMinderAgentConstants.ATTR_ACO_SSOZONE_NAME_DEFAULT_VALUE);
        insertMissingAttributeWithDefaultValue(attrMap, SiteMinderAgentConstants.ATTR_ACO_COOKIE_PATH,
                SiteMinderAgentConstants.ATTR_ACO_COOKIE_PATH_DEFAULT_VALUE);
        insertMissingAttributeWithDefaultValue(attrMap, SiteMinderAgentConstants.ATTR_ACO_COOKIE_PATH_SCOPE,
                SiteMinderAgentConstants.ATTR_ACO_COOKIE_PATH_SCOPE_DEFAULT_VALUE);
        insertMissingAttributeWithDefaultValue(attrMap, SiteMinderAgentConstants.ATTR_ACO_COOKIE_DOMAIN,
                SiteMinderAgentConstants.ATTR_ACO_COOKIE_DOMAIN_DEFAULT_VALUE);
        insertMissingAttributeWithDefaultValue(attrMap, SiteMinderAgentConstants.ATTR_ACO_COOKIE_DOMAIN_SCOPE,
                SiteMinderAgentConstants.ATTR_ACO_COOKIE_DOMAIN_SCOPE_DEFAULT_VALUE);
        insertMissingAttributeWithDefaultValue(attrMap, SiteMinderAgentConstants.ATTR_ACO_USE_SECURE_COOKIES,
                SiteMinderAgentConstants.ATTR_ACO_USE_SECURE_COOKIES_DEFAULT_VALUE);
        insertMissingAttributeWithDefaultValue(attrMap, SiteMinderAgentConstants.ATTR_ACO_USE_HTTP_ONLY_COOKIES,
                SiteMinderAgentConstants.ATTR_ACO_USE_HTTP_ONLY_COOKIES_DEFAULT_VALUE);
        insertMissingAttributeWithDefaultValue(attrMap, SiteMinderAgentConstants.ATTR_ACO_PERSISTENT_COOKIES,
                SiteMinderAgentConstants.ATTR_ACO_PERSISTENT_COOKIES_DEFAULT_VALUE);
        insertMissingAttributeWithDefaultValue(attrMap, SiteMinderAgentConstants.ATTR_ACO_COOKIE_VALIDATION_PERIOD,
                SiteMinderAgentConstants.ATTR_ACO_COOKIE_VALIDATION_PERIOD_DEFAULT_VALUE);

        return attrMap;
    }

    private void insertMissingAttributeWithDefaultValue(Map<String, SiteMinderContext.Attribute> attrMap, String name, String value) {
        if (!attrMap.containsKey(name)) {
            attrMap.put(name, new SiteMinderContext.Attribute(name, value));
        }
    }

    private String computeExpiresTime(Map<String, SiteMinderContext.Attribute> attrMap, SiteMinderContext context) {
        long validationPeriod = attrMap.get(SiteMinderAgentConstants.ATTR_ACO_COOKIE_VALIDATION_PERIOD).getValueAsInt();

        if (validationPeriod <= 0) {
            validationPeriod =  context.getSessionDef().getMaxTimeout();
        }

        return gmtDateFormat.format(new Date(
                (context.getSessionDef().getSessionStartTime() + validationPeriod) * 1000));
    }

    private String computePersistentCookies(Map<String, SiteMinderContext.Attribute> attrMap, SiteMinderContext context) {
        return attrMap.get(SiteMinderAgentConstants.ATTR_ACO_PERSISTENT_COOKIES).getValueAsString().toLowerCase();
    }

    private String computeUseSecureCookies(Map<String, SiteMinderContext.Attribute> attrMap, SiteMinderContext context) {
        return attrMap.get(SiteMinderAgentConstants.ATTR_ACO_USE_SECURE_COOKIES).getValueAsString().toLowerCase();
    }

    private String computeUseHttpOnly(Map<String, SiteMinderContext.Attribute> attrMap, SiteMinderContext context) {
        return attrMap.get(SiteMinderAgentConstants.ATTR_ACO_USE_HTTP_ONLY_COOKIES).getValueAsString().toLowerCase();
    }

    private String computeSsoZoneName(Map<String, SiteMinderContext.Attribute> attrMap, SiteMinderContext context) {
        String ssoZone = context.getSsoZoneName();

        if (StringUtils.isEmpty(ssoZone)) {
            ssoZone = attrMap.get(SiteMinderAgentConstants.ATTR_ACO_SSOZONE_NAME).getValueAsString();
        }

        return ssoZone;
    }

    private String computeCookiePath(Map<String, SiteMinderContext.Attribute> attrMap, SiteMinderContext context) {
        final String PATH_SEPARATOR = "/";
        String cookiePath = attrMap.get(SiteMinderAgentConstants.ATTR_ACO_COOKIE_PATH).getValueAsString();
        int cookiePathScope = attrMap.get(SiteMinderAgentConstants.ATTR_ACO_COOKIE_PATH_SCOPE).getValueAsInt();
        String resource = context.getResContextDef().getResource();

        if (cookiePathScope > 0) {
            cookiePath = computeCookiePathByScope(resource, cookiePathScope, PATH_SEPARATOR.charAt(0));
        }

        if (!cookiePath.startsWith(PATH_SEPARATOR)) {
            cookiePath = PATH_SEPARATOR + cookiePath;
        }

        return cookiePath;
    }

    private String computeCookiePathByScope(final String resource, final int maxSections, final Character sectionSeparator) {
        int charIndex = (resource.charAt(0) == sectionSeparator) ? 1 : 0;
        int sectionIndex = 0;

        while (charIndex < resource.length()) {
            if (resource.charAt(charIndex) == sectionSeparator) {
                sectionIndex++;
                if (sectionIndex == maxSections) {
                    break;
                }
            }
            charIndex++;
        }

        return (charIndex > resource.length()) ? resource : resource.substring(0, charIndex);
    }

    private String computeCookieDomain(final Map<String, SiteMinderContext.Attribute> attrMap, final SiteMinderContext context) {
        final String DOMAIN_SEPARATOR = ".";
        String cookieDomain = attrMap.get(SiteMinderAgentConstants.ATTR_ACO_COOKIE_DOMAIN).getValueAsString();
        int cookieDomainScope = attrMap.get(SiteMinderAgentConstants.ATTR_ACO_COOKIE_DOMAIN_SCOPE).getValueAsInt();
        String serverName = context.getResContextDef().getServer();

        if (StringUtils.isNotEmpty(serverName) && cookieDomainScope > 1) {
            cookieDomain = computeCookieDomainByScope(serverName, cookieDomainScope, DOMAIN_SEPARATOR.charAt(0));
        }

        if (!cookieDomain.startsWith(DOMAIN_SEPARATOR)) {
            cookieDomain = DOMAIN_SEPARATOR + cookieDomain;
        }

        return cookieDomain;
    }

    private String computeCookieDomainByScope(final String serverName, final int maxSections, final char sectionSeparator) {
        int charIndex = serverName.length() - 1;
        int sectionIndex = 0;

        // read the chars from end; count the sections
        while (charIndex >= 0) {
            if (serverName.charAt(charIndex) == sectionSeparator) {
                sectionIndex++;
                if (sectionIndex == maxSections) {
                    break;
                }
            }
            charIndex--;
        }

        return charIndex < 0 ? serverName : serverName.substring(charIndex);
    }

}
