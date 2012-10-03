package com.l7tech.external.assertions.apiportalintegration.server.apikey.manager;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.external.assertions.apiportalintegration.server.ApiKeyData;
import org.jetbrains.annotations.Nullable;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class ApiKeyXmlUtilTest {
    protected static final String KEY = "mykey";
    protected static final String PLAN = "default";
    protected static final String SECRET = "shhh";
    protected static final String SERVICE_ID = "12345";
    protected static final String STATUS = "active";
    protected static final int VERSION = 7;
    protected static final String LABEL = "someLabel";
    protected static final String PLATFORM = "somePlatform";
    protected static final String OAUTH_CALLBACK = "someOauthCallBackUrl";
    protected static final String OAUTH_SCOPE = "someOauthScope";
    protected static final String OAUTH_TYPE = "someOauthType";
    private static final String NEW_LINE = System.getProperty("line.separator");

    protected Map<String, String> serviceIdPlans;
    final ApiKeyData result = new ApiKeyData();

    @Before
    public void setup() {
        serviceIdPlans = new HashMap<String, String>();
        serviceIdPlans.put(SERVICE_ID, PLAN);
    }

    @Test(expected = IllegalArgumentException.class)
    public void elementToKeyDataNullData() throws Exception {
        ApiKeyXmlUtil.elementToKeyData(null, result);
    }

    @Test(expected = IllegalArgumentException.class)
    public void elementToKeyDataNullResult() throws Exception {
        final String xml = buildKeyXml(STATUS, KEY, SECRET, serviceIdPlans);
        ApiKeyXmlUtil.elementToKeyData(getElementFromXml(xml), null);
    }

    @Test
    public void elementToKeyData() throws Exception {
        final String xml = buildKeyXml(STATUS, KEY, SECRET, serviceIdPlans);

        ApiKeyXmlUtil.elementToKeyData(getElementFromXml(xml), result);

        assertEquals(KEY, result.getKey());
        assertEquals(SECRET, result.getSecret());
        assertEquals(STATUS, result.getStatus());
        assertEquals(1, result.getServiceIds().size());
        final Map.Entry<String, String> serviceIds = result.getServiceIds().entrySet().iterator().next();
        assertEquals(SERVICE_ID, serviceIds.getKey());
        assertEquals(PLAN, serviceIds.getValue());
    }

    @Test
    public void elementToKeyDataNoStatus() throws Exception {
        final String xml = buildKeyXml(null, KEY, SECRET, serviceIdPlans);

        ApiKeyXmlUtil.elementToKeyData(getElementFromXml(xml), result);

        assertEquals(KEY, result.getKey());
        assertEquals(SECRET, result.getSecret());
        assertEquals(ApiKeyXmlUtil.NOT_AVAILABLE, result.getStatus());
        assertEquals(1, result.getServiceIds().size());
        final Map.Entry<String, String> serviceIds = result.getServiceIds().entrySet().iterator().next();
        assertEquals(SERVICE_ID, serviceIds.getKey());
        assertEquals(PLAN, serviceIds.getValue());
    }

    @Test
    public void elementToKeyDataNoKey() throws Exception {
        final String xml = buildKeyXml(STATUS, null, SECRET, serviceIdPlans);

        ApiKeyXmlUtil.elementToKeyData(getElementFromXml(xml), result);

        assertNull(result.getKey());
        assertEquals(SECRET, result.getSecret());
        assertEquals(STATUS, result.getStatus());
        assertEquals(1, result.getServiceIds().size());
        final Map.Entry<String, String> serviceIds = result.getServiceIds().entrySet().iterator().next();
        assertEquals(SERVICE_ID, serviceIds.getKey());
        assertEquals(PLAN, serviceIds.getValue());
    }

    @Test
    public void elementToKeyDataNoServiceIds() throws Exception {
        final String xml = buildKeyXml(STATUS, KEY, SECRET, null);

        ApiKeyXmlUtil.elementToKeyData(getElementFromXml(xml), result);

        assertEquals(KEY, result.getKey());
        assertEquals(SECRET, result.getSecret());
        assertEquals(STATUS, result.getStatus());
        assertTrue(result.getServiceIds().isEmpty());
    }

    @Test
    public void elementToKeyDataEmptyServiceIds() throws Exception {
        final String xml = buildKeyXml(STATUS, KEY, SECRET, Collections.<String, String>emptyMap());

        ApiKeyXmlUtil.elementToKeyData(getElementFromXml(xml), result);

        assertEquals(KEY, result.getKey());
        assertEquals(SECRET, result.getSecret());
        assertEquals(STATUS, result.getStatus());
        assertTrue(result.getServiceIds().isEmpty());
    }

    @Test
    public void elementToKeyDataNoServicesElement() throws Exception {
        final String xml = buildKeyXml(STATUS, KEY, SECRET, null);
        final String modified = xml.replace("<l7:Services>", "").replace("</l7:Services>", "");

        ApiKeyXmlUtil.elementToKeyData(getElementFromXml(modified), result);

        assertEquals(KEY, result.getKey());
        assertEquals(SECRET, result.getSecret());
        assertEquals(STATUS, result.getStatus());
        assertTrue(result.getServiceIds().isEmpty());
    }

    @Test
    public void elementToKeyDataNoPlan() throws Exception {
        serviceIdPlans.clear();
        serviceIdPlans.put(SERVICE_ID, null);
        final String xml = buildKeyXml(STATUS, KEY, SECRET, serviceIdPlans);

        ApiKeyXmlUtil.elementToKeyData(getElementFromXml(xml), result);

        assertEquals(KEY, result.getKey());
        assertEquals(SECRET, result.getSecret());
        assertEquals(STATUS, result.getStatus());
        assertEquals(1, result.getServiceIds().size());
        final Map.Entry<String, String> serviceIds = result.getServiceIds().entrySet().iterator().next();
        assertEquals(SERVICE_ID, serviceIds.getKey());
        assertTrue(serviceIds.getValue().isEmpty());
    }

    @Test
    public void elementToKeyDataNoSecret() throws Exception {
        final String xml = buildKeyXml(STATUS, KEY, null, serviceIdPlans);

        ApiKeyXmlUtil.elementToKeyData(getElementFromXml(xml), result);

        assertEquals(KEY, result.getKey());
        assertNull(result.getSecret());
        assertEquals(STATUS, result.getStatus());
        assertEquals(1, result.getServiceIds().size());
        final Map.Entry<String, String> serviceIds = result.getServiceIds().entrySet().iterator().next();
        assertEquals(SERVICE_ID, serviceIds.getKey());
        assertEquals(PLAN, serviceIds.getValue());
    }

    @Test
    public void elementToKeyDataNoOauth() throws Exception {
        final String xml = buildKeyXml(STATUS, KEY, SECRET, null);

        ApiKeyXmlUtil.elementToKeyData(getElementFromXml(xml), result);

        assertEquals(KEY, result.getKey());
        assertEquals(SECRET, result.getSecret());
        assertEquals(STATUS, result.getStatus());
        assertTrue(result.getServiceIds().isEmpty());
        assertNull(result.getLabel());
        assertNull(result.getPlatform());
        assertNull(result.getOauthCallbackUrl());
        assertNull(result.getOauthScope());
        assertNull(result.getOauthType());
    }

    @Test
    public void elementToKeyDataWithOauth() throws Exception {
        final String xml = buildKeyXml(STATUS, KEY, SECRET, null, LABEL, PLATFORM, OAUTH_CALLBACK, OAUTH_SCOPE, OAUTH_TYPE);

        ApiKeyXmlUtil.elementToKeyData(getElementFromXml(xml), result);

        assertEquals(KEY, result.getKey());
        assertEquals(SECRET, result.getSecret());
        assertEquals(STATUS, result.getStatus());
        assertTrue(result.getServiceIds().isEmpty());
        assertEquals(LABEL, result.getLabel());
        assertEquals(PLATFORM, result.getPlatform());
        assertEquals(OAUTH_CALLBACK, result.getOauthCallbackUrl());
        assertEquals(OAUTH_SCOPE, result.getOauthScope());
        assertEquals(OAUTH_TYPE, result.getOauthType());
    }

    @Test
    public void elementToKeyDataWithPlatform() throws Exception {
        final String xml = buildKeyXml(STATUS, KEY, SECRET, null, null, PLATFORM, null, null, null);

        ApiKeyXmlUtil.elementToKeyData(getElementFromXml(xml), result);

        assertEquals(KEY, result.getKey());
        assertEquals(SECRET, result.getSecret());
        assertEquals(STATUS, result.getStatus());
        assertTrue(result.getServiceIds().isEmpty());
        assertEquals(PLATFORM, result.getPlatform());
        assertNull(result.getLabel());
        assertNull(result.getOauthCallbackUrl());
        assertNull(result.getOauthScope());
        assertNull(result.getOauthType());
    }

    @Test
    public void elementToKeyDataOauthLabelOnly() throws Exception {
        final String xml = buildKeyXml(STATUS, KEY, SECRET, null, LABEL, null, null, null, null);

        ApiKeyXmlUtil.elementToKeyData(getElementFromXml(xml), result);

        assertEquals(KEY, result.getKey());
        assertEquals(SECRET, result.getSecret());
        assertEquals(STATUS, result.getStatus());
        assertTrue(result.getServiceIds().isEmpty());
        assertNull(result.getPlatform());
        assertEquals(LABEL, result.getLabel());
        assertEquals("", result.getOauthCallbackUrl());
        assertEquals("", result.getOauthScope());
        assertEquals("", result.getOauthType());
    }

    @Test
    public void elementToKeyDataOauthCallBackOnly() throws Exception {
        final String xml = buildKeyXml(STATUS, KEY, SECRET, null, null, null, OAUTH_CALLBACK, null, null);

        ApiKeyXmlUtil.elementToKeyData(getElementFromXml(xml), result);

        assertEquals(KEY, result.getKey());
        assertEquals(SECRET, result.getSecret());
        assertEquals(STATUS, result.getStatus());
        assertTrue(result.getServiceIds().isEmpty());
        assertNull(result.getPlatform());
        assertEquals("", result.getLabel());
        assertEquals(OAUTH_CALLBACK, result.getOauthCallbackUrl());
        assertEquals("", result.getOauthScope());
        assertEquals("", result.getOauthType());
    }

    @Test
    public void elementToKeyDataOauthScopeOnly() throws Exception {
        final String xml = buildKeyXml(STATUS, KEY, SECRET, null, null, null, null, OAUTH_SCOPE, null);

        ApiKeyXmlUtil.elementToKeyData(getElementFromXml(xml), result);

        assertEquals(KEY, result.getKey());
        assertEquals(SECRET, result.getSecret());
        assertEquals(STATUS, result.getStatus());
        assertTrue(result.getServiceIds().isEmpty());
        assertNull(result.getPlatform());
        assertEquals("", result.getLabel());
        assertEquals("", result.getOauthCallbackUrl());
        assertEquals(OAUTH_SCOPE, result.getOauthScope());
        assertEquals("", result.getOauthType());
    }

    @Test
    public void elementToKeyDataOauthTypeOnly() throws Exception {
        final String xml = buildKeyXml(STATUS, KEY, SECRET, null, null, null, null, null, OAUTH_TYPE);

        ApiKeyXmlUtil.elementToKeyData(getElementFromXml(xml), result);

        assertEquals(KEY, result.getKey());
        assertEquals(SECRET, result.getSecret());
        assertEquals(STATUS, result.getStatus());
        assertTrue(result.getServiceIds().isEmpty());
        assertNull(result.getPlatform());
        assertEquals("", result.getLabel());
        assertEquals("", result.getOauthCallbackUrl());
        assertEquals("", result.getOauthScope());
        assertEquals(OAUTH_TYPE, result.getOauthType());
    }

    protected Element getElementFromXml(final String xml) throws SAXException {
        final Document document = XmlUtil.parse(xml);
        return document.getDocumentElement();
    }

    protected String buildKeyXml(@Nullable final String status, @Nullable final String key, @Nullable final String secret, @Nullable final Map<String, String> serviceIdPlans) {
        final StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("<l7:ApiKey enabled=\"true\" ");
        if (status != null) {
            stringBuilder.append("status=\"" + status + "\" ");
        }
        stringBuilder.append("xmlns:l7=\"http://ns.l7tech.com/2011/08/portal-api-keys\">");
        stringBuilder.append(NEW_LINE);
        if (key != null) {
            stringBuilder.append("<l7:Value>");
            stringBuilder.append(key);
            stringBuilder.append("</l7:Value>");
            stringBuilder.append(NEW_LINE);
        }
        stringBuilder.append("<l7:Services>");
        if (serviceIdPlans != null) {
            for (final Map.Entry<String, String> serviceIdPlan : serviceIdPlans.entrySet()) {
                stringBuilder.append("<l7:S");
                stringBuilder.append(" id=\"" + serviceIdPlan.getKey() + "\"");
                if (serviceIdPlan.getValue() != null) {
                    stringBuilder.append(" plan=\"" + serviceIdPlan.getValue() + "\"");
                }
                stringBuilder.append(" />");
                stringBuilder.append(NEW_LINE);
            }
        }
        stringBuilder.append("</l7:Services>");
        stringBuilder.append(NEW_LINE);
        if (secret != null) {
            stringBuilder.append("<l7:Secret>");
            stringBuilder.append(secret);
            stringBuilder.append("</l7:Secret>");
            stringBuilder.append(NEW_LINE);
        }
        stringBuilder.append("</l7:ApiKey>");
        return stringBuilder.toString();
    }

    protected String buildKeyXml(@Nullable final String status, @Nullable final String key, @Nullable final String secret, @Nullable final Map<String, String> serviceIdPlans,
                                 @Nullable final String label, @Nullable final String platform, @Nullable final String oauthCallBack,
                                 @Nullable final String oauthScope, @Nullable final String oauthType) {
        final StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("<l7:ApiKey enabled=\"true\" ");
        if (status != null) {
            stringBuilder.append("status=\"" + status + "\" ");
        }
        stringBuilder.append("xmlns:l7=\"http://ns.l7tech.com/2011/08/portal-api-keys\">");
        stringBuilder.append(NEW_LINE);
        if (key != null) {
            stringBuilder.append("<l7:Value>");
            stringBuilder.append(key);
            stringBuilder.append("</l7:Value>");
            stringBuilder.append(NEW_LINE);
        }
        stringBuilder.append("<l7:Services>");
        if (serviceIdPlans != null) {
            for (final Map.Entry<String, String> serviceIdPlan : serviceIdPlans.entrySet()) {
                stringBuilder.append("<l7:S");
                stringBuilder.append(" id=\"" + serviceIdPlan.getKey() + "\"");
                if (serviceIdPlan.getValue() != null) {
                    stringBuilder.append(" plan=\"" + serviceIdPlan.getValue() + "\"");
                }
                stringBuilder.append(" />");
                stringBuilder.append(NEW_LINE);
            }
        }
        stringBuilder.append("</l7:Services>");
        stringBuilder.append(NEW_LINE);
        if (secret != null) {
            stringBuilder.append("<l7:Secret>");
            stringBuilder.append(secret);
            stringBuilder.append("</l7:Secret>");
            stringBuilder.append(NEW_LINE);
        }
        if (oauthCallBack != null || oauthScope != null || oauthType != null || label != null) {
            stringBuilder.append("<l7:OAuth ");
            if (oauthCallBack != null) {
                stringBuilder.append(" callbackUrl=\"" + oauthCallBack + "\"");
            }
            if (oauthScope != null) {
                stringBuilder.append(" scope=\"" + oauthScope + "\"");
            }
            if (oauthType != null) {
                stringBuilder.append(" type=\"" + oauthType + "\"");
            }
            if (label != null) {
                stringBuilder.append(" label=\"" + label + "\"");
            }
            stringBuilder.append(" />");
            stringBuilder.append(NEW_LINE);
        }
        if (platform != null) {
            stringBuilder.append("<l7:Platform>");
            stringBuilder.append(platform);
            stringBuilder.append("</l7:Platform>");
            stringBuilder.append(NEW_LINE);
        }
        stringBuilder.append("</l7:ApiKey>");
        return stringBuilder.toString();
    }

}
