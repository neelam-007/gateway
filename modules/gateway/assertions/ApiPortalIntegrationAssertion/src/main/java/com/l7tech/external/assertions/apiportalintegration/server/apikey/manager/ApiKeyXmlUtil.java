package com.l7tech.external.assertions.apiportalintegration.server.apikey.manager;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.external.assertions.apiportalintegration.server.ApiKeyData;
import com.l7tech.external.assertions.apiportalintegration.server.ModuleConstants;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.jetbrains.annotations.NotNull;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Utility class for handling API key xml.
 *
 * @author alee
 */
public final class ApiKeyXmlUtil {
    private static final Logger logger = Logger.getLogger(ApiKeyXmlUtil.class.getName());
    static final String NOT_AVAILABLE = "n/a";

    private ApiKeyXmlUtil() {
        // do not construct me
    }

    /**
     * Expected element format:
     * <l7:ApiKey enabled="true" status="active">
     * <l7:Value>l7xxff7107b646054f089f66ade9fca8bc8f</l7:Value>
     * <l7:Services>
     * <l7:S id="1111" plan="2222" />
     * <l7:S id="3333" plan="4444" />
     * </l7:Services>
     * <l7:Secret>fe438b347f2c42678eddb6d2842c8ae2</l7:Secret>
     * </l7:ApiKey>
     *
     * @param keyElem  the Element to parse
     * @param newEntry entry to receive data
     * @throws java.io.IOException when an error is encountered parsing the element
     */
    public static void elementToKeyData(@NotNull final Element keyElem, @NotNull ApiKeyData newEntry) throws IOException {
        Validate.notNull(keyElem);
        final String status = keyElem.getAttribute(ModuleConstants.STATUS_ATTRIBUTE_NAME);
        if (StringUtils.isNotBlank(status)) {
            newEntry.setStatus(status);
        } else {
            newEntry.setStatus(NOT_AVAILABLE);
        }
        newEntry.setKey(getElementValue(XmlUtil.findFirstChildElementByName(keyElem, ModuleConstants.NAMESPACE_API_KEYS, ModuleConstants.VALUE_ELEMENT_NAME)));
        newEntry.setSecret(getElementValue(XmlUtil.findFirstChildElementByName(keyElem, ModuleConstants.NAMESPACE_API_KEYS, ModuleConstants.SECRET_ELEMENT_NAME)));
        final Element servicesElement = XmlUtil.findFirstChildElementByName(keyElem, ModuleConstants.NAMESPACE_API_KEYS, ModuleConstants.SERVICES_ELEMENT_NAME);
        if (servicesElement != null) {
            final List<Element> serviceElements = XmlUtil.findChildElementsByName(servicesElement, ModuleConstants.NAMESPACE_API_KEYS, ModuleConstants.SERVICE_ELEMENT_NAME);
            final Map<String, String> serviceIds = new HashMap<String, String>();
            for (final Element service : serviceElements) {
                final String id = service.getAttribute(ModuleConstants.ID_ATTRIBUTE_NAME);
                final String plan = service.getAttribute(ModuleConstants.PLAN_ATTRIBUTE_NAME);
                serviceIds.put(id, plan);
            }
            newEntry.setServiceIds(serviceIds);
        } else {
            logger.warning("Unable to parse service ids because no " + ModuleConstants.SERVICES_ELEMENT_NAME + " element was found.");
        }
        newEntry.setPlatform(getElementValue(XmlUtil.findFirstChildElementByName(keyElem, ModuleConstants.NAMESPACE_API_KEYS, ModuleConstants.PLATFORM_ELEMENT_NAME)));
        final Element oauthElement = XmlUtil.findFirstChildElementByName(keyElem, ModuleConstants.NAMESPACE_API_KEYS, ModuleConstants.OAUTH_ELEMENT_NAME);
        if (oauthElement != null) {
            newEntry.setLabel(oauthElement.getAttribute(ModuleConstants.LABEL_ATTRIBUTE_NAME));
            newEntry.setOauthCallbackUrl(oauthElement.getAttribute(ModuleConstants.OAUTHCALLBACKURL_ATTRIBUTE_NAME));
            newEntry.setOauthScope(oauthElement.getAttribute(ModuleConstants.OAUTHSCOPE_ATTRIBUTE_NAME));
            newEntry.setOauthType(oauthElement.getAttribute(ModuleConstants.OAUTHTYPE_ATTRIBUTE_NAME));
        } else {
            logger.warning("Unable to parse oauth because no " + ModuleConstants.OAUTH_ELEMENT_NAME + " element was found.");
        }
    }

    private static String getElementValue(final Element element) {
        return element == null ? null : element.getTextContent();
    }

    public static void updateApiKeyDataFromXml(ApiKeyData data, String xmlRepresentation) {
        if (StringUtils.isNotBlank(xmlRepresentation)) {
            try {
                final Document document = XmlUtil.parse(xmlRepresentation);
                elementToKeyData(document.getDocumentElement(), data);
            } catch (final Exception e) {
                logger.warning("Cannot set data. Error parsing api key element: " + e.getMessage());
            }
        } else {
            logger.warning("Cannot set data because key xml is null or empty.");
        }
    }
}
