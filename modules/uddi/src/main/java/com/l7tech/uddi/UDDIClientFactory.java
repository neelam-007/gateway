package com.l7tech.uddi;

import java.util.logging.Logger;
import java.lang.reflect.Constructor;

import com.l7tech.util.SyspropUtil;

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
     * Create a new UDDIClient.
     *
     * @param config The configuration for the client.
     * @return The UDDIClient
     */
    public UDDIClient newUDDIClient( final UDDIClientConfig config ) {
        return newUDDIClient(
                config.getInquiryUrl(),
                config.getPublishUrl(),
                config.getSubscriptionUrl(),
                config.getSecurityUrl(),
                config.getLogin(),
                config.getPassword(),
                null,
                config.getTlsConfig());
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
                                    final String subscriptionUrl,
                                    final String securityUrl,
                                    final String login,
                                    final String password,
                                    final PolicyAttachmentVersion attachmentVersion,
                                    final UDDIClientTLSConfig tlsConfig ) {
        UDDIClient client;

        PolicyAttachmentVersion policyAttachmentVersion = attachmentVersion == null ?
                getDefaultPolicyAttachmentVersion() :
                attachmentVersion;

        try {
            Class genericUddiClass = Class.forName(clientClass);
            Constructor constructor = genericUddiClass.getDeclaredConstructor(
                    String.class, String.class, String.class, String.class, String.class, String.class,
                    PolicyAttachmentVersion.class, UDDIClientTLSConfig.class);
            client = (UDDIClient) constructor.newInstance(
                    inquiryUrl,
                    publishUrl,
                    subscriptionUrl,
                    securityUrl,
                    login,
                    password,
                    policyAttachmentVersion,
                    tlsConfig);
        } catch (Exception e) {
            throw new RuntimeException("Generic UDDI client error.", e);
        }

        return client;
    }

    //- PACKAGE


    static PolicyAttachmentVersion getDefaultPolicyAttachmentVersion() {
        String id = SyspropUtil.getString(
                SYSPROP_DEFAULT_VERSION,
                PolicyAttachmentVersion.v1_2.toString());
        return PolicyAttachmentVersion.valueOf(id);
    }

    //- PRIVATE

    private static final String SYSPROP_DEFAULT_VERSION = "com.l7tech.uddi.defaultVersion";
    private static final String SYSPROP_UDDICLIENT = "com.l7tech.uddi.client";
    private static final String DEFAULT_UDDICLIENT = "com.l7tech.uddi.GenericUDDIClient";
    private static final Logger logger = Logger.getLogger(UDDIClientFactory.class.getName());
    private static final UDDIClientFactory instance = new UDDIClientFactory();

    private final String clientClass;

    private UDDIClientFactory() {
        clientClass = SyspropUtil.getString(SYSPROP_UDDICLIENT, DEFAULT_UDDICLIENT);

        logger.config("Using UDDIClient implementation '"+clientClass+"'.");
    }

}