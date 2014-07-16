package com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.impl.CertificateAPIResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.ChoiceParam;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.ParameterValidationUtils;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.RestEntityResource;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.impl.CertificateTransformer;
import com.l7tech.gateway.api.*;
import com.l7tech.gateway.rest.SpringBean;
import com.l7tech.objectmodel.Goid;
import com.l7tech.util.CollectionUtils;

import javax.inject.Singleton;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;

/* NOTE: The java docs in this class get converted to API documentation seen by customers!*/

/**
 * Certificates are either HTTPS and LDAPS certificates.This resource enables the management of those certificates.
 */
@Provider
@Path(RestEntityResource.RestEntityResource_version_URI + CertificateResource.trustedCertificate_URI)
@Singleton
public class CertificateResource extends RestEntityResource<TrustedCertificateMO, CertificateAPIResourceFactory, CertificateTransformer> {

    protected static final String trustedCertificate_URI = "trustedCertificates";

    @Override
    @SpringBean
    public void setFactory(CertificateAPIResourceFactory factory) {
        super.factory = factory;
    }

    @Override
    @SpringBean
    public void setTransformer(CertificateTransformer transformer) {
        super.transformer = transformer;
    }

    /**
     * Creates a new certificate
     *
     * @param resource The certificate to create
     * @return A reference to the newly created certificate
     * @throws ResourceFactory.ResourceNotFoundException
     * @throws ResourceFactory.InvalidResourceException
     */
    @POST
    public Response create(TrustedCertificateMO resource) throws ResourceFactory.ResourceNotFoundException, ResourceFactory.InvalidResourceException {
        return super.create(resource);
    }

    /**
     * Retrieves a certificate.
     *
     * @param id The ID of the certificate to return
     * @return The certificate
     * @throws ResourceFactory.ResourceNotFoundException
     */
    @GET
    @Path("{id}")
    public Item<TrustedCertificateMO> get(@PathParam("id") String id) throws ResourceFactory.ResourceNotFoundException {
        return super.get(id);
    }

    /**
     * <p>Returns a list of certificates. Can optionally sort the resulting list in ascending or
     * descending order. Other params given will be used as search values.</p>
     * <p class="italicize">Examples:</p>
     * <div class="code indent"><pre>/restman/1.0/trustedCertificates?name=MyCertificate</pre></div>
     * <p>Returns certificates with name "MyCertificate".</p>
     * <p>If a parameter is not a valid search value a bad request error will be returned.</p>
     *
     * @param sort            Key to sort the list by.
     * @param order           Sort order for the list; 'true'=ascending, 'false'=descending; defaults to
     *                        ascending if not specified
     * @param names           Name filter
     * @param securityZoneIds Security zone ID filter
     * @return A list of certificates. If the list is empty then no certificates were found.
     */
    @SuppressWarnings("unchecked")
    @GET
    public ItemsList<TrustedCertificateMO> list(
            @QueryParam("sort") @ChoiceParam({"id", "name"}) String sort,
            @QueryParam("order") @ChoiceParam({"asc", "desc"}) String order,
            @QueryParam("name") List<String> names,
            @QueryParam("securityZone.id") List<Goid> securityZoneIds) {
        Boolean ascendingSort = ParameterValidationUtils.convertSortOrder(order);
        ParameterValidationUtils.validateNoOtherQueryParamsIncludeDefaults(uriInfo.getQueryParameters(), Arrays.asList("name", "securityZone.id"));

        CollectionUtils.MapBuilder<String, List<Object>> filters = CollectionUtils.MapBuilder.builder();
        if (names != null && !names.isEmpty()) {
            filters.put("name", (List) names);
        }
        if (securityZoneIds != null && !securityZoneIds.isEmpty()) {
            filters.put("securityZone.id", (List) securityZoneIds);
        }
        return super.list(sort, ascendingSort,
                filters.map());
    }

    /**
     * Creates or Updates an existing certificate. If a certificate with the given ID does not exist one
     * will be created, otherwise the existing one will be updated.
     *
     * @param resource Certificate to create or update
     * @param id       ID of the certificate to create or update
     * @return A reference to the newly created or updated certificate.
     * @throws ResourceFactory.ResourceNotFoundException
     * @throws ResourceFactory.InvalidResourceException
     */
    @PUT
    @Path("{id}")
    public Response createOrUpdate(TrustedCertificateMO resource, @PathParam("id") String id) throws ResourceFactory.ResourceFactoryException {
        return super.update(resource, id);
    }

    /**
     * Deletes an existing certificate.
     *
     * @param id The ID of the certificate to delete.
     * @throws com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory.ResourceNotFoundException
     */
    @DELETE
    @Path("{id}")
    @Override
    public void delete(@PathParam("id") String id) throws ResourceFactory.ResourceNotFoundException {
        super.delete(id);
    }

    /**
     * Returns a template, which is an example certificate that can be used as a reference for what certificate objects should look like.
     *
     * @return The template certificate.
     */
    @GET
    @Path("template")
    public Item<TrustedCertificateMO> template() {
        TrustedCertificateMO trustedCertificateMO = ManagedObjectFactory.createTrustedCertificate();
        trustedCertificateMO.setName("Trusted Certificate Template");
        trustedCertificateMO.setRevocationCheckingPolicyId(new Goid(0, 1).toString());
        trustedCertificateMO.setProperties(CollectionUtils.MapBuilder.<String, Object>builder()
                .put("trustedForSigningClientCerts", true)
                .put("trustedForSigningServerCerts", true)
                .put("trustedAsSamlAttestingEntity", true)
                .put("trustedAsSamlIssuer", true)
                .put("trustedForSsl", true)
                .put("trustAnchor", true)
                .put("verifyHostname", true)
                .put("revocationCheckingEnabled", true)
                .map());
        CertificateData certificateData = ManagedObjectFactory.createCertificateData();
        certificateData.setEncoded("Encoded Data".getBytes());
        certificateData.setIssuerName("cn=issuerdn");
        certificateData.setSubjectName("cn=subjectdn");
        certificateData.setSerialNumber(new BigInteger("123"));
        trustedCertificateMO.setCertificateData(certificateData);
        return super.createTemplateItem(trustedCertificateMO);
    }
}
