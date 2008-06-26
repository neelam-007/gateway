package com.l7tech.common.uddi;

import java.util.logging.Logger;
import java.net.URI;
import java.lang.reflect.Constructor;

import com.l7tech.common.util.SyspropUtil;

/**
 * Factory for creation of UDDIClients.
 *
 * <p>This will create a Generic JAX-WS based UDDI client unless the Systinet
 * libraries are detected.</p>
 *
 * <p>Note that our Systinet client supports only a small subset of
 * functionality and is deprecated.</p>
 *
 * <p>The default policy attachment version can be overridden using the
 * system property <code>com.l7tech.common.uddi.defaultVersion</code>. See
 * {@link PolicyAttachmentVersion} for valid values</p>
 *
 * @author Steve Jones
 */
public class UDDIClientFactory {

    //- PUBLIC

    /**
     * Get the factory instance.
     *
     * @return The factory.
     */
    public static UDDIClientFactory getInstance() {
        return instance;
    }

    /**
     * Create a new UDDIClient for the specified registry type.
     *
     * @param url The URL for the inquiry web service or base url for all services
     * @param uddiRegistryInfo The type of registry (if null defaults will be used)
     * @param login The login to use (may be null)
     * @param password The password to use (may be null, required if a login is specified)
     * @param attachmentVersion The policy attachment version to use (if null the default version is used)
     * @return The UDDIClient
     */
    public UDDIClient newUDDIClient(final String url,
                                    final UDDIRegistryInfo uddiRegistryInfo,
                                    final String login,
                                    final String password,
                                    final PolicyAttachmentVersion attachmentVersion) {
        String inquiryUrl = url;
        String publishUrl = url;
        String securityUrl = url;

        if ( uddiRegistryInfo != null ) {
            String baseUrl = calculateBaseUrl(url, uddiRegistryInfo.getInquiry());
            inquiryUrl = buildUrl(baseUrl, uddiRegistryInfo.getInquiry());
            publishUrl = buildUrl(baseUrl, uddiRegistryInfo.getPublication());
            securityUrl = buildUrl(baseUrl, uddiRegistryInfo.getSecurityPolicy());

        }
        return newUDDIClient(
                inquiryUrl,
                publishUrl,
                securityUrl,
                login,
                password,
                attachmentVersion);
    }

    /**
     * Create a new UDDIClient.
     *
     * @param inquiryUrl The URL for the inquiry web service
     * @param publishUrl The URL for the publishing web service (may be null)
     * @param securityUrl The URL for the security web service (may be null)
     * @param login The login to use (may be null)
     * @param password The password to use (may be null, required if a login is specified)
     * @param attachmentVersion The policy attachment version to use (if null the default version is used)
     * @return The UDDIClient
     */
    public UDDIClient newUDDIClient(final String inquiryUrl,
                                    final String publishUrl,
                                    final String securityUrl,
                                    final String login,
                                    final String password,
                                    final PolicyAttachmentVersion attachmentVersion) {
        UDDIClient client;

        PolicyAttachmentVersion policyAttachmentVersion = attachmentVersion == null ?
                getDefaultPolicyAttachmentVersion() :
                attachmentVersion;

        try {
            Class genericUddiClass = Class.forName(clientClass);
            Constructor constructor = genericUddiClass.getConstructor(String.class, String.class, String.class, String.class, String.class, PolicyAttachmentVersion.class);
            client = (UDDIClient) constructor.newInstance(inquiryUrl, publishUrl, securityUrl, login, password, policyAttachmentVersion);
        } catch (Exception e) {
            throw new RuntimeException("Generic UDDI client error.", e);
        }

        return client;
    }

    //- PRIVATE

    private static final String SYSPROP_DEFAULT_VERSION = "com.l7tech.common.uddi.defaultVersion";
    private static final String SYSPROP_UDDICLIENT = "com.l7tech.common.uddi.client";
    private static final String DEFAULT_UDDICLIENT = "com.l7tech.common.uddi.GenericUDDIClient";
    private static final Logger logger = Logger.getLogger(UDDIClientFactory.class.getName());
    private static final UDDIClientFactory instance = new UDDIClientFactory();

    private final String clientClass;

    private UDDIClientFactory() {
        clientClass = SyspropUtil.getString(SYSPROP_UDDICLIENT, DEFAULT_UDDICLIENT);

        logger.config("Using UDDIClient implementation '"+clientClass+"'.");
    }

    private String calculateBaseUrl(String url, String suffix) {
        String base = url;

        if ( base.endsWith(suffix) ) {
            base = base.substring(0, base.length()-suffix.length());
        }

        if ( !base.endsWith("/") ) {
            base += "/";
        }

        return base;
    }

    private String buildUrl(String url, String suffix) {
        return URI.create(url).resolve(suffix).toString();
    }

    private static PolicyAttachmentVersion getDefaultPolicyAttachmentVersion() {
        String id = SyspropUtil.getString(
                SYSPROP_DEFAULT_VERSION,
                PolicyAttachmentVersion.v1_2.toString());
        return PolicyAttachmentVersion.valueOf(id);
    }
}
