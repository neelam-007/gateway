package com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.ServerRESTGatewayManagementAssertion;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.BundleExporter;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.BundleImporter;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.RbacAccessService;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.URLAccessibleLocator;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.exceptions.InvalidArgumentException;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.ChoiceParam;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.ParameterValidationUtils;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.RestManVersion;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.Since;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.impl.BundleTransformer;
import com.l7tech.gateway.api.*;
import com.l7tech.gateway.rest.SpringBean;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.server.search.exceptions.CannotRetrieveDependenciesException;
import com.l7tech.util.Functions;
import org.glassfish.jersey.process.internal.RequestScoped;
import org.glassfish.jersey.server.ContainerRequest;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.*;

/*
 * Do not make this a @Provider the will make allow
 * @queryParam on fields. This will be added to the rest application using the application context. See
 * /com/l7tech/external/assertions/gatewaymanagement/server/gatewayManagementContext.xml:restAgent
 */
/* NOTE: The java docs in this class get converted to API documentation seen by customers!*/

/**
 * This resource is used to export and import bundles for migration. See <a href="migration.html">migration.html</a>
 * for
 * more documentation on migration and bundling.
 */
@Path(ServerRESTGatewayManagementAssertion.Version1_0_URI + "bundle")
@RequestScoped
public class BundleResource {
    @SpringBean
    private BundleImporter bundleImporter;

    @SpringBean
    private BundleExporter bundleExporter;

    @SpringBean
    private BundleTransformer transformer;

    @SpringBean
    private RbacAccessService rbacAccessService;

    @Context
    private UriInfo uriInfo;

    @Context
    private ContainerRequest containerRequest;

    public BundleResource() {
    }

    BundleResource(final BundleImporter bundleImporter, final BundleExporter bundleExporter,
                   final BundleTransformer transformer, final URLAccessibleLocator urlAccessibleLocator,
                   final RbacAccessService rbacAccessService, final UriInfo uriInfo,
                   final ContainerRequest containerRequest) {
        this.bundleImporter = bundleImporter;
        this.bundleExporter = bundleExporter;
        this.transformer = transformer;
        this.rbacAccessService = rbacAccessService;
        this.uriInfo = uriInfo;
        this.containerRequest = containerRequest;
    }

    //TODO:This method had a huge number of parameters! write it so it gets all its paramaters from uriInfo
    /**
     * Returns the bundle for the given resources. This API call is capable of returning a bundle created from multiple
     * resources.
     *
     * @param defaultAction                       Default bundling action. By default this is NewOrExisting
     * @param exportGatewayRestManagementService  If true the gateway management service will be exported too. False by
     *                                            default.
     * @param activeConnectorIds                  Active Connectors to export
     * @param cassandraConnectionIds              Cassandra Connections to export
     * @param trustedCertificateIds               Trusted Certificates to export
     * @param clusterPropertyIds                  Cluster properties to export
     * @param customKeyValueIds                   Custom Key Values to export
     * @param emailListenerIds                    Email listeners to export
     * @param encapsulatedAssertionIds            Encapsulated Assertions to export
     * @param firewallRuleIds                     Firewall rules to export
     * @param folderIds                           Folders to export
     * @param genericEntityIds                    Generic entities to export
     * @param httpConfigurationIds                Http Configurations to export
     * @param identityProviderIds                 Identity providers to export
     * @param interfaceTagIds                     Interface Tags to export
     * @param jdbcConnectionIds                   JDBC Connections to export
     * @param jmsDestinationIds                   JMS Destinations to Export
     * @param listenPortIds                       Listen Ports to export
     * @param policyIds                           Policies to export
     * @param policyAliasIds                      Policy Aliases to export
     * @param policyBackedServiceIds              Policy Backed Services to export
     * @param privateKeyIds                       Private Keys to export
     * @param serviceIds                          Services to export
     * @param serviceAliasIds                     Service Aliases to export
     * @param resourceIds                         Resources to export
     * @param revocationCheckingPolicyIds         Revocation Checking Policies to export
     * @param roleIds                             Roles to export
     * @param sampleMessageIds                    Sample Messages to export
     * @param scheduledTaskIds                    Scheduled Tasks to export
     * @param passwordIds                         Passwords to export
     * @param securityZoneIds                     Security Zones to export
     * @param serverModuleFileIds                 Server Modules Files to export
     * @param siteMinderConfigurationIds          Siteminder Configurations to export
     * @param workQueueIds                        Work Queues to export
     * @param requiredActiveConnectorIds          Marks these Active Connectors as required in the bundle (does not
     *                                            export their dependencies and FailOnNew is set to true)
     * @param requiredCassandraConnectionIds      Marks these Cassandra Connections as required in the bundle (does not
     *                                            export their dependencies and FailOnNew is set to true)
     * @param requiredTrustedCertificateIds       Marks these Trusted Certificates as required in the bundle (does not
     *                                            export their dependencies and FailOnNew is set to true)
     * @param requiredClusterPropertyIds          Marks these Cluster properties as required in the bundle (does not
     *                                            export their dependencies and FailOnNew is set to true)
     * @param requiredCustomKeyValueIds           Marks these Custom Key Values as required in the bundle (does not
     *                                            export their dependencies and FailOnNew is set to true)
     * @param requiredEmailListenerIds            Marks these Email listeners as required in the bundle (does not export
     *                                            their dependencies and FailOnNew is set to true)
     * @param requiredEncapsulatedAssertionIds    Marks these Encapsulated Assertions as required in the bundle (does
     *                                            not export their dependencies and FailOnNew is set to true)
     * @param requiredFirewallRuleIds             Marks these Firewall rules as required in the bundle (does not export
     *                                            their dependencies and FailOnNew is set to true)
     * @param requiredFolderIds                   Marks these Folders as required in the bundle (does not export their
     *                                            dependencies and FailOnNew is set to true)
     * @param requiredGenericEntityIds            Marks these Generic entities as required in the bundle (does not
     *                                            export their dependencies and FailOnNew is set to true)
     * @param requiredHttpConfigurationIds        Marks these Http Configurations as required in the bundle (does not
     *                                            export their dependencies and FailOnNew is set to true)
     * @param requiredIdentityProviderIds         Marks these Identity providers as required in the bundle (does not
     *                                            export their dependencies and FailOnNew is set to true)
     * @param requiredInterfaceTagIds             Marks these Interface Tags as required in the bundle (does not export
     *                                            their dependencies and FailOnNew is set to true)
     * @param requiredJdbcConnectionIds           Marks these JDBC Connections as required in the bundle (does not
     *                                            export their dependencies and FailOnNew is set to true)
     * @param requiredJmsDestinationIds           Marks these JMS Destinations as required in the bundle (does not
     *                                            export their dependencies and FailOnNew is set to true)
     * @param requiredListenPortIds               Marks these Listen Ports as required in the bundle (does not export
     *                                            their dependencies and FailOnNew is set to true)
     * @param requiredPolicyIds                   Marks these Policies as required in the bundle (does not export their
     *                                            dependencies and FailOnNew is set to true)
     * @param requiredPolicyAliasIds              Marks these Policy Aliases as required in the bundle (does not export
     *                                            their dependencies and FailOnNew is set to true)
     * @param requiredPolicyBackedServiceIds      Marks these Policy Backed Services as required in the bundle (does not
     *                                            export their dependencies and FailOnNew is set to true)
     * @param requiredPrivateKeyIds               Marks these Private Keys as required in the bundle (does not export
     *                                            their dependencies and FailOnNew is set to true)
     * @param requiredServiceIds                  Marks these Services as required in the bundle (does not export their
     *                                            dependencies and FailOnNew is set to true)
     * @param requiredServiceAliasIds             Marks these Service Aliases as required in the bundle (does not export
     *                                            their dependencies and FailOnNew is set to true)
     * @param requiredResourceIds                 Marks these Resources as required in the bundle (does not export their
     *                                            dependencies and FailOnNew is set to true)
     * @param requiredRevocationCheckingPolicyIds Marks these Revocation Checking Policies as required in the bundle
     *                                            (does not export their dependencies and FailOnNew is set to true)
     * @param requiredRoleIds                     Marks these Roles as required in the bundle (does not export their
     *                                            dependencies and FailOnNew is set to true)
     * @param requiredSampleMessageIds            Marks these Sample Messages as required in the bundle (does not export
     *                                            their dependencies and FailOnNew is set to true)
     * @param requiredScheduledTaskIds            Marks these Scheduled Tasks as required in the bundle (does not export
     *                                            their dependencies and FailOnNew is set to true)
     * @param requiredPasswordIds                 Marks these Passwords as required in the bundle (does not export their
     *                                            dependencies and FailOnNew is set to true)
     * @param requiredSecurityZoneIds             Marks these Security Zones as required in the bundle (does not export
     *                                            their dependencies and FailOnNew is set to true)
     * @param requiredServerModuleFileIds         Marks these Server Modules Files as required in the bundle (does not
     *                                            export their dependencies and FailOnNew is set to true)
     * @param requiredSiteMinderConfigurationIds  Marks these Siteminder Configurations as required in the bundle (does
     *                                            not export their dependencies and FailOnNew is set to true)
     * @param requiredWorkQueueIds                Marks these Work Queues as required in the bundle (does not export
     *                                            their dependencies and FailOnNew is set to true)
     * @param fullGateway                         True to export the full gateway. False by default
     * @param includeDependencies                 True to export with dependencies. False by default
     * @param encryptSecrets                      True to export with encrypted secrets. False by default.
     * @param encryptUsingClusterPassphrase       True to use the cluster passphrase if encrypting secrets. False by
     *                                            default.
     * @param encodedKeyPassphrase                The optional base-64 encoded passphrase to use for the encryption key
     *                                            when encrypting secrets.
     * @return The bundle for the resources
     */
    @GET
    public Item<Bundle> exportBundle(@QueryParam("defaultAction") @ChoiceParam({"NewOrExisting", "NewOrUpdate"}) @DefaultValue("NewOrExisting") String defaultAction,
                                     @QueryParam("exportGatewayRestManagementService") @DefaultValue("false") Boolean exportGatewayRestManagementService,
                                     //These are the entities to export
                                     @Since(RestManVersion.VERSION_1_0_2) @QueryParam("activeConnector") List<String> activeConnectorIds,
                                     @Since(RestManVersion.VERSION_1_0_2) @QueryParam("cassandraConnection") List<String> cassandraConnectionIds,
                                     @Since(RestManVersion.VERSION_1_0_2) @QueryParam("trustedCertificate") List<String> trustedCertificateIds,
                                     @Since(RestManVersion.VERSION_1_0_2) @QueryParam("clusterProperty") List<String> clusterPropertyIds,
                                     @Since(RestManVersion.VERSION_1_0_2) @QueryParam("customKeyValue") List<String> customKeyValueIds,
                                     @Since(RestManVersion.VERSION_1_0_2) @QueryParam("emailListener") List<String> emailListenerIds,
                                     @Since(RestManVersion.VERSION_1_0_2) @QueryParam("encapsulatedAssertion") List<String> encapsulatedAssertionIds,
                                     @Since(RestManVersion.VERSION_1_0_2) @QueryParam("firewallRule") List<String> firewallRuleIds,
                                     @QueryParam("folder") List<String> folderIds,
                                     @Since(RestManVersion.VERSION_1_0_2) @QueryParam("genericEntity") List<String> genericEntityIds,
                                     @Since(RestManVersion.VERSION_1_0_2) @QueryParam("httpConfiguration") List<String> httpConfigurationIds,
                                     @Since(RestManVersion.VERSION_1_0_2) @QueryParam("identityProvider") List<String> identityProviderIds,
                                     @Since(RestManVersion.VERSION_1_0_2) @QueryParam("interfaceTag") List<String> interfaceTagIds,
                                     @Since(RestManVersion.VERSION_1_0_2) @QueryParam("jdbcConnection") List<String> jdbcConnectionIds,
                                     @Since(RestManVersion.VERSION_1_0_2) @QueryParam("jmsDestination") List<String> jmsDestinationIds,
                                     @Since(RestManVersion.VERSION_1_0_2) @QueryParam("listenPort") List<String> listenPortIds,
                                     @QueryParam("policy") List<String> policyIds,
                                     @Since(RestManVersion.VERSION_1_0_2) @QueryParam("policyAlias") List<String> policyAliasIds,
                                     @Since(RestManVersion.VERSION_1_0_2) @QueryParam("policyBackedService") List<String> policyBackedServiceIds,
                                     @Since(RestManVersion.VERSION_1_0_2) @QueryParam("privateKey") List<String> privateKeyIds,
                                     @QueryParam("service") List<String> serviceIds,
                                     @Since(RestManVersion.VERSION_1_0_2) @QueryParam("serviceAlias") List<String> serviceAliasIds,
                                     @Since(RestManVersion.VERSION_1_0_2) @QueryParam("resource") List<String> resourceIds,
                                     @Since(RestManVersion.VERSION_1_0_2) @QueryParam("revocationCheckingPolicy") List<String> revocationCheckingPolicyIds,
                                     @Since(RestManVersion.VERSION_1_0_2) @QueryParam("role") List<String> roleIds,
                                     @Since(RestManVersion.VERSION_1_0_2) @QueryParam("sampleMessage") List<String> sampleMessageIds,
                                     @Since(RestManVersion.VERSION_1_0_2) @QueryParam("scheduledTask") List<String> scheduledTaskIds,
                                     @Since(RestManVersion.VERSION_1_0_2) @QueryParam("password") List<String> passwordIds,
                                     @Since(RestManVersion.VERSION_1_0_2) @QueryParam("securityZone") List<String> securityZoneIds,
                                     @Since(RestManVersion.VERSION_1_0_2) @QueryParam("serverModuleFile") List<String> serverModuleFileIds,
                                     @Since(RestManVersion.VERSION_1_0_2) @QueryParam("siteMinderConfiguration") List<String> siteMinderConfigurationIds,
                                     @Since(RestManVersion.VERSION_1_0_2) @QueryParam("workQueue") List<String> workQueueIds,
                                     //These are the entities that will be required on import
                                     @Since(RestManVersion.VERSION_1_0_2) @QueryParam("requireActiveConnector") List<String> requiredActiveConnectorIds,
                                     @Since(RestManVersion.VERSION_1_0_2) @QueryParam("requireCassandraConnection") List<String> requiredCassandraConnectionIds,
                                     @Since(RestManVersion.VERSION_1_0_2) @QueryParam("requireTrustedCertificate") List<String> requiredTrustedCertificateIds,
                                     @Since(RestManVersion.VERSION_1_0_2) @QueryParam("requireClusterProperty") List<String> requiredClusterPropertyIds,
                                     @Since(RestManVersion.VERSION_1_0_2) @QueryParam("requireCustomKeyValue") List<String> requiredCustomKeyValueIds,
                                     @Since(RestManVersion.VERSION_1_0_2) @QueryParam("requireEmailListener") List<String> requiredEmailListenerIds,
                                     @Since(RestManVersion.VERSION_1_0_2) @QueryParam("requireEncapsulatedAssertion") List<String> requiredEncapsulatedAssertionIds,
                                     @Since(RestManVersion.VERSION_1_0_2) @QueryParam("requireFirewallRule") List<String> requiredFirewallRuleIds,
                                     @Since(RestManVersion.VERSION_1_0_2) @QueryParam("requireFolder") List<String> requiredFolderIds,
                                     @Since(RestManVersion.VERSION_1_0_2) @QueryParam("requireGenericEntity") List<String> requiredGenericEntityIds,
                                     @Since(RestManVersion.VERSION_1_0_2) @QueryParam("requireHttpConfiguration") List<String> requiredHttpConfigurationIds,
                                     @Since(RestManVersion.VERSION_1_0_2) @QueryParam("requireIdentityProvider") List<String> requiredIdentityProviderIds,
                                     @Since(RestManVersion.VERSION_1_0_2) @QueryParam("requireInterfaceTag") List<String> requiredInterfaceTagIds,
                                     @Since(RestManVersion.VERSION_1_0_2) @QueryParam("requireJdbcConnection") List<String> requiredJdbcConnectionIds,
                                     @Since(RestManVersion.VERSION_1_0_2) @QueryParam("requireJmsDestination") List<String> requiredJmsDestinationIds,
                                     @Since(RestManVersion.VERSION_1_0_2) @QueryParam("requireListenPort") List<String> requiredListenPortIds,
                                     @Since(RestManVersion.VERSION_1_0_2) @QueryParam("requirePolicy") List<String> requiredPolicyIds,
                                     @Since(RestManVersion.VERSION_1_0_2) @QueryParam("requirePolicyAlias") List<String> requiredPolicyAliasIds,
                                     @Since(RestManVersion.VERSION_1_0_2) @QueryParam("requirePolicyBackedService") List<String> requiredPolicyBackedServiceIds,
                                     @Since(RestManVersion.VERSION_1_0_2) @QueryParam("requirePrivateKey") List<String> requiredPrivateKeyIds,
                                     @Since(RestManVersion.VERSION_1_0_2) @QueryParam("requireService") List<String> requiredServiceIds,
                                     @Since(RestManVersion.VERSION_1_0_2) @QueryParam("requireServiceAlias") List<String> requiredServiceAliasIds,
                                     @Since(RestManVersion.VERSION_1_0_2) @QueryParam("requireResource") List<String> requiredResourceIds,
                                     @Since(RestManVersion.VERSION_1_0_2) @QueryParam("requireRevocationCheckingPolicy") List<String> requiredRevocationCheckingPolicyIds,
                                     @Since(RestManVersion.VERSION_1_0_2) @QueryParam("requireRole") List<String> requiredRoleIds,
                                     @Since(RestManVersion.VERSION_1_0_2) @QueryParam("requireSampleMessage") List<String> requiredSampleMessageIds,
                                     @Since(RestManVersion.VERSION_1_0_2) @QueryParam("requireScheduledTask") List<String> requiredScheduledTaskIds,
                                     @Since(RestManVersion.VERSION_1_0_2) @QueryParam("requirePassword") List<String> requiredPasswordIds,
                                     @Since(RestManVersion.VERSION_1_0_2) @QueryParam("requireSecurityZone") List<String> requiredSecurityZoneIds,
                                     @Since(RestManVersion.VERSION_1_0_2) @QueryParam("requireServerModuleFile") List<String> requiredServerModuleFileIds,
                                     @Since(RestManVersion.VERSION_1_0_2) @QueryParam("requireSiteMinderConfiguration") List<String> requiredSiteMinderConfigurationIds,
                                     @Since(RestManVersion.VERSION_1_0_2) @QueryParam("requireWorkQueue") List<String> requiredWorkQueueIds,

                                     @QueryParam("all") @DefaultValue("false") @Since(RestManVersion.VERSION_1_0_1) Boolean fullGateway,
                                     @QueryParam("includeDependencies") @DefaultValue("false") @Since(RestManVersion.VERSION_1_0_1) Boolean includeDependencies,
                                     @QueryParam("encryptSecrets") @DefaultValue("false") @Since(RestManVersion.VERSION_1_0_1) Boolean encryptSecrets,
                                     @QueryParam("encryptUsingClusterPassphrase") @DefaultValue("false") @Since(RestManVersion.VERSION_1_0_1) Boolean encryptUsingClusterPassphrase,
                                     @HeaderParam("L7-key-passphrase") @Since(RestManVersion.VERSION_1_0_1) String encodedKeyPassphrase) throws IOException, ResourceFactory.ResourceNotFoundException, FindException, CannotRetrieveDependenciesException, GeneralSecurityException {
        rbacAccessService.validateFullAdministrator();
        ParameterValidationUtils.validateNoOtherQueryParams(uriInfo.getQueryParameters(), Arrays.asList("defaultAction", "exportGatewayRestManagementService",
                "activeConnector",
                "cassandraConnection",
                "trustedCertificate",
                "clusterProperty",
                "customKeyValue",
                "emailListener",
                "encapsulatedAssertion",
                "firewallRule",
                "folder",
                "genericEntity",
                "httpConfiguration",
                "identityProvider",
                "interfaceTag",
                "jdbcConnection",
                "jmsDestination",
                "listenPort",
                "policy",
                "policyAlias",
                "policyBackedService",
                "privateKey",
                "service",
                "serviceAlias",
                "resource",
                "revocationCheckingPolicy",
                "role",
                "sampleMessage",
                "scheduledTask",
                "password",
                "securityZone",
                "serverModuleFile",
                "siteMinderConfiguration",
                "workQueue",

                "requireActiveConnector",
                "requireCassandraConnection",
                "requireTrustedCertificate",
                "requireClusterProperty",
                "requireCustomKeyValue",
                "requireEmailListener",
                "requireEncapsulatedAssertion",
                "requireFirewallRule",
                "requireFolder",
                "requireGenericEntity",
                "requireHttpConfiguration",
                "requireIdentityProvider",
                "requireInterfaceTag",
                "requireJdbcConnection",
                "requireJmsDestination",
                "requireListenPort",
                "requirePolicy",
                "requirePolicyAlias",
                "requirePolicyBackedService",
                "requirePrivateKey",
                "requireService",
                "requireServiceAlias",
                "requireResource",
                "requireRevocationCheckingPolicy",
                "requireRole",
                "requireSampleMessage",
                "requireScheduledTask",
                "requirePassword",
                "requireSecurityZone",
                "requireServerModuleFile",
                "requireSiteMinderConfiguration",
                "requireWorkQueue",
                "all", "includeDependencies", "encryptSecrets", "encryptUsingClusterPassphrase"));
        final String encodedPassphrase = getEncryptionPassphrase(encryptSecrets, encryptUsingClusterPassphrase, encodedKeyPassphrase);
        //validate that something is being exported
        if (activeConnectorIds.isEmpty() && cassandraConnectionIds.isEmpty() && trustedCertificateIds.isEmpty() && clusterPropertyIds.isEmpty() && customKeyValueIds.isEmpty() && emailListenerIds.isEmpty() && encapsulatedAssertionIds.isEmpty() &&
                firewallRuleIds.isEmpty() && folderIds.isEmpty() && genericEntityIds.isEmpty() && httpConfigurationIds.isEmpty() && identityProviderIds.isEmpty() && interfaceTagIds.isEmpty() && jdbcConnectionIds.isEmpty() && jmsDestinationIds.isEmpty() &&
                listenPortIds.isEmpty() && policyIds.isEmpty() && policyAliasIds.isEmpty() && policyBackedServiceIds.isEmpty() && privateKeyIds.isEmpty() && serviceIds.isEmpty() && serviceAliasIds.isEmpty() && resourceIds.isEmpty() && revocationCheckingPolicyIds.isEmpty() && roleIds.isEmpty() &&
                sampleMessageIds.isEmpty() && scheduledTaskIds.isEmpty() && passwordIds.isEmpty() && securityZoneIds.isEmpty() && serverModuleFileIds.isEmpty() && siteMinderConfigurationIds.isEmpty() && workQueueIds.isEmpty() && !fullGateway) {
            throw new InvalidArgumentException("Must specify at least one entity to export");
        }

        List<EntityHeader> entityHeadersToExport = new ArrayList<>();

        buildEntityHeaders(activeConnectorIds, entityHeadersToExport, EntityType.SSG_ACTIVE_CONNECTOR);
        buildEntityHeaders(cassandraConnectionIds, entityHeadersToExport, EntityType.CASSANDRA_CONFIGURATION);
        buildEntityHeaders(trustedCertificateIds, entityHeadersToExport, EntityType.TRUSTED_CERT);
        buildEntityHeaders(clusterPropertyIds, entityHeadersToExport, EntityType.CLUSTER_PROPERTY);
        buildEntityHeaders(customKeyValueIds, entityHeadersToExport, EntityType.CUSTOM_KEY_VALUE_STORE);
        buildEntityHeaders(emailListenerIds, entityHeadersToExport, EntityType.EMAIL_LISTENER);
        buildEntityHeaders(encapsulatedAssertionIds, entityHeadersToExport, EntityType.ENCAPSULATED_ASSERTION);
        buildEntityHeaders(firewallRuleIds, entityHeadersToExport, EntityType.FIREWALL_RULE);
        buildEntityHeaders(folderIds, entityHeadersToExport, EntityType.FOLDER);
        buildEntityHeaders(genericEntityIds, entityHeadersToExport, EntityType.GENERIC);
        buildEntityHeaders(httpConfigurationIds, entityHeadersToExport, EntityType.HTTP_CONFIGURATION);
        buildEntityHeaders(identityProviderIds, entityHeadersToExport, EntityType.ID_PROVIDER_CONFIG);
        buildEntityHeaders(interfaceTagIds, entityHeadersToExport, EntityType.INTERFACE_TAG);
        buildEntityHeaders(jdbcConnectionIds, entityHeadersToExport, EntityType.JDBC_CONNECTION);
        buildEntityHeaders(jmsDestinationIds, entityHeadersToExport, EntityType.JMS_ENDPOINT);
        buildEntityHeaders(listenPortIds, entityHeadersToExport, EntityType.SSG_CONNECTOR);
        buildEntityHeaders(policyIds, entityHeadersToExport, EntityType.POLICY);
        buildEntityHeaders(policyAliasIds, entityHeadersToExport, EntityType.POLICY_ALIAS);
        buildEntityHeaders(policyBackedServiceIds, entityHeadersToExport, EntityType.POLICY_BACKED_SERVICE);
        buildEntityHeaders(privateKeyIds, entityHeadersToExport, EntityType.SSG_KEY_ENTRY);
        buildEntityHeaders(serviceIds, entityHeadersToExport, EntityType.SERVICE);
        buildEntityHeaders(serviceAliasIds, entityHeadersToExport, EntityType.SERVICE_ALIAS);
        buildEntityHeaders(resourceIds, entityHeadersToExport, EntityType.RESOURCE_ENTRY);
        buildEntityHeaders(revocationCheckingPolicyIds, entityHeadersToExport, EntityType.REVOCATION_CHECK_POLICY);
        buildEntityHeaders(roleIds, entityHeadersToExport, EntityType.RBAC_ROLE);
        buildEntityHeaders(sampleMessageIds, entityHeadersToExport, EntityType.SAMPLE_MESSAGE);
        buildEntityHeaders(scheduledTaskIds, entityHeadersToExport, EntityType.SCHEDULED_TASK);
        buildEntityHeaders(passwordIds, entityHeadersToExport, EntityType.SECURE_PASSWORD);
        buildEntityHeaders(securityZoneIds, entityHeadersToExport, EntityType.SECURITY_ZONE);
        buildEntityHeaders(serverModuleFileIds, entityHeadersToExport, EntityType.SERVER_MODULE_FILE);
        buildEntityHeaders(siteMinderConfigurationIds, entityHeadersToExport, EntityType.SITEMINDER_CONFIGURATION);
        buildEntityHeaders(workQueueIds, entityHeadersToExport, EntityType.WORK_QUEUE);

        List<EntityHeader> entityHeadersToIgnoreDependencies = new ArrayList<>();
        buildEntityHeaders(requiredActiveConnectorIds, entityHeadersToIgnoreDependencies, EntityType.SSG_ACTIVE_CONNECTOR);
        buildEntityHeaders(requiredCassandraConnectionIds, entityHeadersToIgnoreDependencies, EntityType.CASSANDRA_CONFIGURATION);
        buildEntityHeaders(requiredTrustedCertificateIds, entityHeadersToIgnoreDependencies, EntityType.TRUSTED_CERT);
        buildEntityHeaders(requiredClusterPropertyIds, entityHeadersToIgnoreDependencies, EntityType.CLUSTER_PROPERTY);
        buildEntityHeaders(requiredCustomKeyValueIds, entityHeadersToIgnoreDependencies, EntityType.CUSTOM_KEY_VALUE_STORE);
        buildEntityHeaders(requiredEmailListenerIds, entityHeadersToIgnoreDependencies, EntityType.EMAIL_LISTENER);
        buildEntityHeaders(requiredEncapsulatedAssertionIds, entityHeadersToIgnoreDependencies, EntityType.ENCAPSULATED_ASSERTION);
        buildEntityHeaders(requiredFirewallRuleIds, entityHeadersToIgnoreDependencies, EntityType.FIREWALL_RULE);
        buildEntityHeaders(requiredFolderIds, entityHeadersToIgnoreDependencies, EntityType.FOLDER);
        buildEntityHeaders(requiredGenericEntityIds, entityHeadersToIgnoreDependencies, EntityType.GENERIC);
        buildEntityHeaders(requiredHttpConfigurationIds, entityHeadersToIgnoreDependencies, EntityType.HTTP_CONFIGURATION);
        buildEntityHeaders(requiredIdentityProviderIds, entityHeadersToIgnoreDependencies, EntityType.ID_PROVIDER_CONFIG);
        buildEntityHeaders(requiredInterfaceTagIds, entityHeadersToIgnoreDependencies, EntityType.INTERFACE_TAG);
        buildEntityHeaders(requiredJdbcConnectionIds, entityHeadersToIgnoreDependencies, EntityType.JDBC_CONNECTION);
        buildEntityHeaders(requiredJmsDestinationIds, entityHeadersToIgnoreDependencies, EntityType.JMS_ENDPOINT);
        buildEntityHeaders(requiredListenPortIds, entityHeadersToIgnoreDependencies, EntityType.SSG_CONNECTOR);
        buildEntityHeaders(requiredPolicyIds, entityHeadersToIgnoreDependencies, EntityType.POLICY);
        buildEntityHeaders(requiredPolicyAliasIds, entityHeadersToIgnoreDependencies, EntityType.POLICY_ALIAS);
        buildEntityHeaders(requiredPolicyBackedServiceIds, entityHeadersToIgnoreDependencies, EntityType.POLICY_BACKED_SERVICE);
        buildEntityHeaders(requiredPrivateKeyIds, entityHeadersToIgnoreDependencies, EntityType.SSG_KEY_ENTRY);
        buildEntityHeaders(requiredServiceIds, entityHeadersToIgnoreDependencies, EntityType.SERVICE);
        buildEntityHeaders(requiredServiceAliasIds, entityHeadersToIgnoreDependencies, EntityType.SERVICE_ALIAS);
        buildEntityHeaders(requiredResourceIds, entityHeadersToIgnoreDependencies, EntityType.RESOURCE_ENTRY);
        buildEntityHeaders(requiredRevocationCheckingPolicyIds, entityHeadersToIgnoreDependencies, EntityType.REVOCATION_CHECK_POLICY);
        buildEntityHeaders(requiredRoleIds, entityHeadersToIgnoreDependencies, EntityType.RBAC_ROLE);
        buildEntityHeaders(requiredSampleMessageIds, entityHeadersToIgnoreDependencies, EntityType.SAMPLE_MESSAGE);
        buildEntityHeaders(requiredScheduledTaskIds, entityHeadersToIgnoreDependencies, EntityType.SCHEDULED_TASK);
        buildEntityHeaders(requiredPasswordIds, entityHeadersToIgnoreDependencies, EntityType.SECURE_PASSWORD);
        buildEntityHeaders(requiredSecurityZoneIds, entityHeadersToIgnoreDependencies, EntityType.SECURITY_ZONE);
        buildEntityHeaders(requiredServerModuleFileIds, entityHeadersToIgnoreDependencies, EntityType.SERVER_MODULE_FILE);
        buildEntityHeaders(requiredSiteMinderConfigurationIds, entityHeadersToIgnoreDependencies, EntityType.SITEMINDER_CONFIGURATION);
        buildEntityHeaders(requiredWorkQueueIds, entityHeadersToIgnoreDependencies, EntityType.WORK_QUEUE);

        if (fullGateway && !entityHeadersToExport.isEmpty()) {
            throw new InvalidArgumentException("If specifying full gateway export (all=true) do not give any other entity id's");
        }

        final Bundle bundle = createBundle(true, Mapping.Action.valueOf(defaultAction), "id",
                exportGatewayRestManagementService, includeDependencies, encryptSecrets, encodedPassphrase, entityHeadersToIgnoreDependencies,
                entityHeadersToExport.toArray(new EntityHeader[entityHeadersToExport.size()]));
        return new ItemBuilder<>(transformer.convertToItem(bundle))
                .addLink(ManagedObjectFactory.createLink(Link.LINK_REL_SELF, uriInfo.getRequestUri().toString()))
                .build();
    }

    private static void buildEntityHeaders(@NotNull final List<String> ids, @NotNull final List<EntityHeader> entityHeaders, @NotNull final EntityType entityType) {
        for (final String id : ids) {
            entityHeaders.add(new EntityHeader(id, entityType, null, null));
        }
    }

    /**
     * Returns the bundle for the given resource type. The resource type is either a policy, service, or folder
     *
     * @param resourceType                       Resource type. Either folder, service or policy
     * @param id                                 ID of the resource to bundle
     * @param defaultAction                      Default bundling action. By default this is NewOrExisting
     * @param defaultMapBy                       Default map by action.
     * @param includeRequestFolder               For a folder export, specifies whether to include the folder in the
     *                                           bundle or just its contents.
     * @param exportGatewayRestManagementService If true the gateway management service will be exported too. False by
     *                                           default.
     * @param includeDependencies                True to export with dependencies. False by default
     * @param encryptSecrets                     True to export with encrypted secrets. False by default.
     * @param encryptUsingClusterPassphrase      True to use the cluster passphrase if encrypting secrets. False by
     *                                           default
     * @param encodedKeyPassphrase               The optional base-64 encoded passphrase to use for the encryption key
     *                                           when encrypting secrets.
     * @return The bundle for the resource
     * @title Folder, Service or Policy Export
     */
    @GET
    @Path("{resourceType}/{id}")
    public Item<Bundle> exportFolderServiceOrPolicyBundle(@PathParam("resourceType") @ChoiceParam({"folder", "policy", "service"}) String resourceType,
                                                          @PathParam("id") Goid id,
                                                          @QueryParam("defaultAction") @ChoiceParam({"NewOrExisting", "NewOrUpdate"}) @DefaultValue("NewOrExisting") String defaultAction,
                                                          @QueryParam("defaultMapBy") @DefaultValue("id") @ChoiceParam({"id", "name", "guid"}) String defaultMapBy,
                                                          @QueryParam("includeRequestFolder") @DefaultValue("false") Boolean includeRequestFolder,
                                                          @QueryParam("exportGatewayRestManagementService") @DefaultValue("false") Boolean exportGatewayRestManagementService,
                                                          @QueryParam("includeDependencies") @DefaultValue("false") @Since(RestManVersion.VERSION_1_0_1) Boolean includeDependencies,
                                                          @QueryParam("encryptSecrets") @DefaultValue("false") @Since(RestManVersion.VERSION_1_0_1) Boolean encryptSecrets,
                                                          @QueryParam("encryptUsingClusterPassphrase") @DefaultValue("false") @Since(RestManVersion.VERSION_1_0_1) Boolean encryptUsingClusterPassphrase,
                                                          @HeaderParam("L7-key-passphrase") @Since(RestManVersion.VERSION_1_0_1) String encodedKeyPassphrase) throws IOException, ResourceFactory.ResourceNotFoundException, FindException, CannotRetrieveDependenciesException, GeneralSecurityException {
        rbacAccessService.validateFullAdministrator();
        ParameterValidationUtils.validateNoOtherQueryParams(uriInfo.getQueryParameters(), Arrays.asList("defaultAction", "defaultMapBy", "includeRequestFolder", "exportGatewayRestManagementService", "includeDependencies", "encryptSecrets", "encryptUsingClusterPassphrase"));
        final String encodedPassphrase = getEncryptionPassphrase(encryptSecrets, encryptUsingClusterPassphrase, encodedKeyPassphrase);
        final EntityType entityType;
        switch (resourceType) {
            case "folder":
                entityType = EntityType.FOLDER;
                break;
            case "policy":
                entityType = EntityType.POLICY;
                break;
            case "service":
                entityType = EntityType.SERVICE;
                break;
            default:
                throw new IllegalArgumentException("Illegal resourceType. Can only generate bundles for folders, policies, or resources.");
        }

        EntityHeader header = new EntityHeader(id, entityType, null, null);
        final Bundle bundle = createBundle(includeRequestFolder, Mapping.Action.valueOf(defaultAction), defaultMapBy,
                exportGatewayRestManagementService, includeDependencies, encryptSecrets, encodedPassphrase, Collections.<EntityHeader>emptyList(), header);
        return new ItemBuilder<>(transformer.convertToItem(bundle))
                .addLink(ManagedObjectFactory.createLink(Link.LINK_REL_SELF, uriInfo.getRequestUri().toString()))
                .build();
    }

    /**
     * This will import a bundle.
     *
     * @param test                 If true the bundle import will be tested no changes will be made to the gateway
     * @param activate             False to not activate the updated services and policies.
     * @param versionComment       The comment to set for updated/created services and policies
     * @param encodedKeyPassphrase The optional base-64 encoded passphrase to uadmimnse for the encryption key when
     *                             encrypting passwords.
     * @param bundle               The bundle to import
     * @return The mappings performed during the bundle import
     */
    @PUT
    public Response importBundle(@QueryParam("test") @DefaultValue("false") final boolean test,
                                 @QueryParam("activate") @DefaultValue("true") final boolean activate,
                                 @QueryParam("versionComment") final String versionComment,
                                 @HeaderParam("L7-key-passphrase") @Since(RestManVersion.VERSION_1_0_1) String encodedKeyPassphrase,
                                 final Bundle bundle) throws Exception {
        rbacAccessService.validateFullAdministrator();
        ParameterValidationUtils.validateNoOtherQueryParams(uriInfo.getQueryParameters(), Arrays.asList("test", "activate", "versionComment"));

        List<Mapping> mappings =
                bundleImporter.importBundle(bundle, test, activate, versionComment, encodedKeyPassphrase);

        Item<Mappings> item = new ItemBuilder<Mappings>("Bundle mappings", "BUNDLE MAPPINGS")
                .addLink(ManagedObjectFactory.createLink(Link.LINK_REL_SELF, uriInfo.getRequestUri().toString()))
                .setContent(ManagedObjectFactory.createMappings(mappings))
                .build();
        return containsErrors(mappings) ? Response.status(Response.Status.CONFLICT).entity(item).build() : Response.ok(item).build();
    }

    /**
     * Checks if there are errors in the mappings list. If there is any error it will fail.
     *
     * @param mappings The list of mappings to check for errors
     * @return true if there is an error in the mappings list
     */
    private boolean containsErrors(List<Mapping> mappings) {
        return Functions.exists(mappings, new Functions.Unary<Boolean, Mapping>() {
            @Override
            public Boolean call(Mapping mapping) {
                return mapping.getErrorType() != null;
            }
        });
    }

    /**
     * Creates a bundle from the entity headers given
     *
     * @param includeRequestFolder true to include the request folder
     * @param defaultAction        The default mapping action to take
     * @param defaultMapBy         The default map by property
     * @param includeDependencies  true to include dependencies
     * @param headers              The header to bundle a bundle for
     * @return The bundle from the headers
     */
    @SuppressWarnings("unchecked")
    @NotNull
    private Bundle createBundle(boolean includeRequestFolder,
                                @NotNull final Mapping.Action defaultAction,
                                @NotNull final String defaultMapBy,
                                boolean exportGatewayRestManagementService,
                                boolean includeDependencies,
                                boolean encryptSecrets,
                                @Nullable final String encodedKeyPassphrase,
                                @NotNull final List<EntityHeader> ignoreEntityDependencies,
                                @NotNull final EntityHeader... headers) throws FindException, CannotRetrieveDependenciesException, FileNotFoundException, GeneralSecurityException {
        //build the bundling properties
        final Properties bundleOptionsBuilder = new Properties();
        bundleOptionsBuilder.setProperty(BundleExporter.IncludeRequestFolderOption, String.valueOf(includeRequestFolder));
        bundleOptionsBuilder.setProperty(BundleExporter.DefaultMappingActionOption, defaultAction.toString());
        bundleOptionsBuilder.setProperty(BundleExporter.DefaultMapByOption, defaultMapBy);
        bundleOptionsBuilder.setProperty(BundleExporter.EncryptSecrets, Boolean.toString(encryptSecrets));

        //ignore the rest man service so it is not exported
        if (containerRequest.getProperty("ServiceId") != null && !exportGatewayRestManagementService) {
            bundleOptionsBuilder.setProperty(BundleExporter.IgnoredEntityIdsOption, containerRequest.getProperty("ServiceId").toString());
        }
        if (containerRequest.getProperty("ServiceId") != null) {
            bundleOptionsBuilder.setProperty(BundleExporter.ServiceUsed, containerRequest.getProperty("ServiceId").toString());
        }
        //TODO: This is ugly! change the properties to a map
        bundleOptionsBuilder.put(BundleExporter.IgnoreDependenciesOption, ignoreEntityDependencies);
        //create the bundle export
        return bundleExporter.exportBundle(bundleOptionsBuilder, includeDependencies, encryptSecrets, encryptSecrets ? encodedKeyPassphrase : null, headers);
    }

    private String getEncryptionPassphrase(final Boolean encrypt, final Boolean useClusterPassphrase, final String customPassphrase) {
        String passphrase = null;
        if (encrypt) {
            if (!useClusterPassphrase && customPassphrase != null) {
                passphrase = customPassphrase;
            } else if (!useClusterPassphrase && customPassphrase == null) {
                throw new InvalidArgumentException("Passphrase is required for encryption");
            }
        }
        return passphrase;
    }
}
