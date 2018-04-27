package com.ca.siteminder;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class SiteMinderSessionCookieStringBuilderTest {

    private SiteMinderSessionCookieStringBuilder cookieBuilder;
    private SiteMinderContext context;

    @Before
    public void setup() {
        cookieBuilder = new SiteMinderSessionCookieStringBuilder();
        context = new SiteMinderContext();
        context.setSsoToken("ABC1234");

        List<SiteMinderContext.Attribute> attrList = new ArrayList<>();
        attrList.add(new SiteMinderContext.Attribute(SiteMinderAgentConstants.ATTR_ACO_SSOZONE_NAME,
                SiteMinderAgentConstants.ATTR_ACO_SSOZONE_NAME_DEFAULT_VALUE));
        attrList.add(new SiteMinderContext.Attribute(SiteMinderAgentConstants.ATTR_ACO_COOKIE_PATH,
                SiteMinderAgentConstants.ATTR_ACO_COOKIE_PATH_DEFAULT_VALUE));
        attrList.add(new SiteMinderContext.Attribute(SiteMinderAgentConstants.ATTR_ACO_COOKIE_PATH_SCOPE,
                SiteMinderAgentConstants.ATTR_ACO_COOKIE_PATH_SCOPE_DEFAULT_VALUE));
        attrList.add(new SiteMinderContext.Attribute(SiteMinderAgentConstants.ATTR_ACO_COOKIE_DOMAIN,
                SiteMinderAgentConstants.ATTR_ACO_COOKIE_DOMAIN_DEFAULT_VALUE));
        attrList.add(new SiteMinderContext.Attribute(SiteMinderAgentConstants.ATTR_ACO_COOKIE_DOMAIN_SCOPE,
                SiteMinderAgentConstants.ATTR_ACO_COOKIE_DOMAIN_SCOPE_DEFAULT_VALUE));
        attrList.add(new SiteMinderContext.Attribute(SiteMinderAgentConstants.ATTR_ACO_USE_SECURE_COOKIES,
                SiteMinderAgentConstants.ATTR_ACO_USE_SECURE_COOKIES_DEFAULT_VALUE));
        attrList.add(new SiteMinderContext.Attribute(SiteMinderAgentConstants.ATTR_ACO_USE_HTTP_ONLY_COOKIES,
                SiteMinderAgentConstants.ATTR_ACO_USE_HTTP_ONLY_COOKIES_DEFAULT_VALUE));
        attrList.add(new SiteMinderContext.Attribute(SiteMinderAgentConstants.ATTR_ACO_PERSISTENT_COOKIES,
                SiteMinderAgentConstants.ATTR_ACO_PERSISTENT_COOKIES_DEFAULT_VALUE));
        attrList.add(new SiteMinderContext.Attribute(SiteMinderAgentConstants.ATTR_ACO_COOKIE_VALIDATION_PERIOD,
                SiteMinderAgentConstants.ATTR_ACO_COOKIE_VALIDATION_PERIOD_DEFAULT_VALUE));
        context.setAttrList(attrList);

        context.setResContextDef(new SiteMinderContext.ResourceContextDef());
        context.getResContextDef().setServer("myserver.example.com");
        context.getResContextDef().setResource("/app/hi.html");

        context.setSessionDef(new SiteMinderContext.SessionDef());
        context.getSessionDef().setSessionStartTime(1510919807);
        context.getSessionDef().setMaxTimeout(3600);
    }

    @Test
    public void testDefaultCookieString() {
        assertEquals("SMSESSION=ABC1234;Path=/;Domain=.", cookieBuilder.build(context));
    }

    @Test
    public void testCookieStringWithFullFlags() {
        Map<String, SiteMinderContext.Attribute> attrMap = context.getAttrMap();
        attrMap.get(SiteMinderAgentConstants.ATTR_ACO_USE_HTTP_ONLY_COOKIES).setValue("yes");
        attrMap.get(SiteMinderAgentConstants.ATTR_ACO_USE_SECURE_COOKIES).setValue("yes");
        assertEquals("SMSESSION=ABC1234;Path=/;Domain=.;Secure;HttpOnly", cookieBuilder.build(context));
    }

    @Test
    public void testCookieStringWithFullFlagsIncludingExpires() {
        Map<String, SiteMinderContext.Attribute> attrMap = context.getAttrMap();
        attrMap.get(SiteMinderAgentConstants.ATTR_ACO_USE_HTTP_ONLY_COOKIES).setValue("yes");
        attrMap.get(SiteMinderAgentConstants.ATTR_ACO_USE_SECURE_COOKIES).setValue("yes");
        attrMap.get(SiteMinderAgentConstants.ATTR_ACO_PERSISTENT_COOKIES).setValue("yes");
        assertEquals("SMSESSION=ABC1234;Path=/;Domain=.;Expires=Fri, 17 Nov 2017 12:56:47 GMT;Secure;HttpOnly", cookieBuilder.build(context));

        attrMap.get(SiteMinderAgentConstants.ATTR_ACO_COOKIE_VALIDATION_PERIOD).setValue("7200");
        assertEquals("SMSESSION=ABC1234;Path=/;Domain=.;Expires=Fri, 17 Nov 2017 13:56:47 GMT;Secure;HttpOnly", cookieBuilder.build(context));
    }

    @Test
    public void testCookieStringWithNonDefaultDomainScope() {
        Map<String, SiteMinderContext.Attribute> attrMap = context.getAttrMap();

        attrMap.get(SiteMinderAgentConstants.ATTR_ACO_COOKIE_DOMAIN_SCOPE).setValue("2");
        assertEquals("With DomainScope more than 1",
                "SMSESSION=ABC1234;Path=/;Domain=.example.com", cookieBuilder.build(context));

        attrMap.get(SiteMinderAgentConstants.ATTR_ACO_COOKIE_DOMAIN_SCOPE).setValue("-1");
        assertEquals("With invalid DomainScope",
                "SMSESSION=ABC1234;Path=/;Domain=.", cookieBuilder.build(context));

        attrMap.get(SiteMinderAgentConstants.ATTR_ACO_COOKIE_DOMAIN_SCOPE).setValue("1");
        assertEquals("With valid DomainScope 1; but not permitted by HTTP standards",
                "SMSESSION=ABC1234;Path=/;Domain=.", cookieBuilder.build(context));

        attrMap.get(SiteMinderAgentConstants.ATTR_ACO_COOKIE_DOMAIN_SCOPE).setValue("3");
        assertEquals("With valid DomainScope",
                "SMSESSION=ABC1234;Path=/;Domain=.myserver.example.com", cookieBuilder.build(context));

        attrMap.get(SiteMinderAgentConstants.ATTR_ACO_COOKIE_DOMAIN_SCOPE).setValue("4");
        assertEquals("With valid DomainScope but more than the available sections",
                "SMSESSION=ABC1234;Path=/;Domain=.myserver.example.com", cookieBuilder.build(context));

        context.getResContextDef().setServer("myserver");
        attrMap.get(SiteMinderAgentConstants.ATTR_ACO_COOKIE_DOMAIN_SCOPE).setValue("2");
        assertEquals("With valid DomainScope but less than the available sections",
                "SMSESSION=ABC1234;Path=/;Domain=.myserver", cookieBuilder.build(context));

        context.getResContextDef().setServer("");
        attrMap.get(SiteMinderAgentConstants.ATTR_ACO_COOKIE_DOMAIN_SCOPE).setValue("2");
        assertEquals("With valid DomainScope but no server details",
                "SMSESSION=ABC1234;Path=/;Domain=.", cookieBuilder.build(context));
    }

    @Test
    public void testCookieStringWithNonDefaultPathScope() {
        Map<String, SiteMinderContext.Attribute> attrMap = context.getAttrMap();

        context.getResContextDef().setResource("/app1/app2/hi.html");
        attrMap.get(SiteMinderAgentConstants.ATTR_ACO_COOKIE_PATH_SCOPE).setValue("-2");
        assertEquals("SMSESSION=ABC1234;Path=/;Domain=.", cookieBuilder.build(context));

        context.getResContextDef().setResource("/app1/app2/hi.html");
        attrMap.get(SiteMinderAgentConstants.ATTR_ACO_COOKIE_PATH_SCOPE).setValue("0");
        assertEquals("SMSESSION=ABC1234;Path=/;Domain=.", cookieBuilder.build(context));

        context.getResContextDef().setResource("/app1/app2/hi.html");
        attrMap.get(SiteMinderAgentConstants.ATTR_ACO_COOKIE_PATH_SCOPE).setValue("1");
        assertEquals("SMSESSION=ABC1234;Path=/app1;Domain=.", cookieBuilder.build(context));

        context.getResContextDef().setResource("/app1/app2/hi.html");
        attrMap.get(SiteMinderAgentConstants.ATTR_ACO_COOKIE_PATH_SCOPE).setValue("2");
        assertEquals("SMSESSION=ABC1234;Path=/app1/app2;Domain=.", cookieBuilder.build(context));

        context.getResContextDef().setResource("app1/app2/hi.html");
        attrMap.get(SiteMinderAgentConstants.ATTR_ACO_COOKIE_PATH_SCOPE).setValue("2");
        assertEquals("SMSESSION=ABC1234;Path=/app1/app2;Domain=.", cookieBuilder.build(context));

        context.getResContextDef().setResource("/app1/app2/hi.html");
        attrMap.get(SiteMinderAgentConstants.ATTR_ACO_COOKIE_PATH_SCOPE).setValue("3");
        assertEquals("SMSESSION=ABC1234;Path=/app1/app2/hi.html;Domain=.", cookieBuilder.build(context));

        context.getResContextDef().setResource("/app1/app2/hi.html");
        attrMap.get(SiteMinderAgentConstants.ATTR_ACO_COOKIE_PATH_SCOPE).setValue("4");
        assertEquals("SMSESSION=ABC1234;Path=/app1/app2/hi.html;Domain=.", cookieBuilder.build(context));
    }

}
