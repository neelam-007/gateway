package com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.ServerRESTGatewayManagementAssertion;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.impl.PrivateKeyAPIResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.ChoiceParam;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.ParameterValidationUtils;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.RestEntityResource;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.impl.PrivateKeyTransformer;
import com.l7tech.gateway.api.*;
import com.l7tech.gateway.api.impl.*;
import com.l7tech.gateway.rest.SpringBean;
import com.l7tech.objectmodel.FindException;
import com.l7tech.util.CollectionUtils;
import org.glassfish.jersey.message.XmlHeader;

import javax.inject.Singleton;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import javax.ws.rs.ext.Provider;
import java.math.BigInteger;
import java.net.URI;
import java.util.Arrays;
import java.util.List;

/**
 * The private key resource
 */
@Provider
@Path(PrivateKeyResource.Version_URI + PrivateKeyResource.privateKey_URI)
@Singleton
public class PrivateKeyResource extends RestEntityResource<PrivateKeyMO, PrivateKeyAPIResourceFactory, PrivateKeyTransformer> {

    protected static final String Version_URI = ServerRESTGatewayManagementAssertion.Version1_0_URI;

    @Context
    protected UriInfo uriInfo;

    protected static final String privateKey_URI = "privateKeys";

    @Override
    @SpringBean
    public void setFactory(PrivateKeyAPIResourceFactory factory) {
        super.factory = factory;
    }

    @Override
    @SpringBean
    public void setTransformer(PrivateKeyTransformer transformer) {
        super.transformer = transformer;
    }

    /**
     * Creates a new private key given a PrivateKeyCreationContext. The PrivateKeyCreationContext is used specify how
     * the private key should be created. The PrivateKeyCreationContext dn is the domain name to create the key with.
     * The properties are optional but you may specify: <table> <tr><th>Key</th><th>Type</th><th>Description</th></tr>
     * <tr><td>ecName</td><td>String</td><td>This the Elliptic Curve key type to use. If it is not specified an RSA key
     * type is used.</td></tr> <tr><td>rsaKeySize</td><td>Integer</td><td>This is the rsa key size to use. This is only
     * applicable if an ecName is not specified. Defaults to 2048</td></tr> <tr><td>daysUntilExpiry</td><td>Integer</td><td>Specify
     * the number of days until the key expires. Defaults to 5 years.</td></tr> <tr><td>caCapable</td><td>Boolean</td><td>Specify
     * if the certificate should be CA capable. Defaults to false</td></tr> <tr><td>signatureHashAlgorithm</td><td>String</td><td>The
     * algorithm used for the signature hash.</td></tr> </table>
     * <p/>
     * Example request:
<pre>
     &lt;l7:PrivateKeyCreationContext xmlns:l7=&quot;http://ns.l7tech.com/2010/04/gateway-management&quot;&gt;
         &lt;l7:Dn&gt;CN=srcAlias&lt;/l7:Dn&gt;
         &lt;l7:Properties&gt;
             &lt;l7:Property key=&quot;signatureHashAlgorithm&quot;&gt;
                &lt;l7:StringValue&gt;SHA384&lt;/l7:StringValue&gt;
             &lt;/l7:Property&gt;
             &lt;l7:Property key=&quot;rsaKeySize&quot;&gt;
                &lt;l7:IntegerValue&gt;516&lt;/l7:IntegerValue&gt;
             &lt;/l7:Property&gt;
             &lt;l7:Property key=&quot;ecName&quot;&gt;
                &lt;l7:StringValue&gt;secp384r1&lt;/l7:StringValue&gt;
             &lt;/l7:Property&gt;
             &lt;l7:Property key=&quot;daysUntilExpiry&quot;&gt;
                &lt;l7:IntegerValue&gt;2&lt;/l7:IntegerValue&gt;
             &lt;/l7:Property&gt;
                &lt;l7:Property key=&quot;caCapable&quot;&gt;
             &lt;l7:BooleanValue&gt;true&lt;/l7:BooleanValue&gt;
         &lt;/l7:Property&gt;
         &lt;/l7:Properties&gt;
     &lt;/l7:PrivateKeyCreationContext&gt;
</pre>
     * <p/>
     * This responds with a reference to the newly created private key.
     *
     * @param privateKeyCreationContext This specifies how to create the private key.
     * @param id                        The identity of the private key to create in the form of [keystore id]:[alias]
     * @return A reference to the newly created private key
     * @throws ResourceFactory.ResourceNotFoundException
     * @throws ResourceFactory.InvalidResourceException
     */
    @POST
    @Path("{id}")
    @XmlHeader(XslStyleSheetResource.DEFAULT_STYLESHEET_HEADER)
    public Response createResource(PrivateKeyCreationContext privateKeyCreationContext, @PathParam("id") String id) throws ResourceFactory.ResourceNotFoundException, ResourceFactory.InvalidResourceException {

        PrivateKeyMO mo = factory.createPrivateKey(privateKeyCreationContext, id);
        UriBuilder ub = uriInfo.getAbsolutePathBuilder().path(mo.getId());
        final URI uri = ub.build();
        return Response.created(uri).entity(new ItemBuilder<>(
                transformer.convertToItem(mo))
                .setContent(null)
                .addLink(getLink(mo))
                .addLinks(getRelatedLinks(mo))
                .build())
                .build();
    }

    /**
     * Retrieve a private key by its ID. The ID is a combination of the keystoreId and the key alias seperated by a ':'.
     * For example 00000000000000000000000000000002:mykey
     *
     * @param id The identity of the key to select
     * @return The selected private key.
     * @throws ResourceFactory.ResourceNotFoundException
     */
    @GET
    @Path("{id}")
    public Item<PrivateKeyMO> get(@PathParam("id") String id) throws ResourceFactory.ResourceNotFoundException {
        return super.get(id);
    }

    /**
     * This will return a list of private keys. A sort can be specified to allow the resulting list to be sorted in
     * either ascending or descending order. Other params given will be used as search values. Examples:
     * <p/>
     * /restman/1.0/privateKeys?alias=mykey
     * <p/>
     * Returns the private key with alias "mykey"
     * <p/>
     * /restman/1.0/privateKeys?alias=mykey&alias=myotherkey
     * <p/>
     * Returns the private keys with alias "mykey" and "myotherkey"
     * <p/>
     * If a parameter is not a valid search value an error will be returned.
     *
     * @param sort    The key to sort the list by. Currently only 'id' is supported.
     * @param order   The order to sort the list. true for ascending, false for descending. null implies ascending
     * @param aliases The alias filter
     * @return A list of entities. If the list is empty then no entities were found.
     */
    @SuppressWarnings("unchecked")
    @GET
    @Produces(MediaType.APPLICATION_XML)
    public ItemsList<PrivateKeyMO> list(
            @QueryParam("sort") @ChoiceParam({"id"}) String sort,
            @QueryParam("order") @ChoiceParam({"asc", "desc"}) String order,
            @QueryParam("alias") List<String> aliases) {
        Boolean ascendingSort = ParameterValidationUtils.convertSortOrder(order);
        ParameterValidationUtils.validateNoOtherQueryParamsIncludeDefaults(uriInfo.getQueryParameters(), Arrays.asList("alias"));

        CollectionUtils.MapBuilder<String, List<Object>> filters = CollectionUtils.MapBuilder.builder();
        if (aliases != null && !aliases.isEmpty()) {
            filters.put("alias", (List) aliases);
        }
        return super.list(sort, ascendingSort,
                filters.map());
    }

    /**
     * Updates an existing private key. This api call can be used to replace a keys certificate chain or change its
     * security zone.
     *
     * @param resource The updated private key.
     * @param id       The id of the private key to update
     * @return A reference to the newly updated private key.
     * @throws ResourceFactory.ResourceNotFoundException
     * @throws ResourceFactory.InvalidResourceException
     */
    @PUT
    @Path("{id}")
    public Response update(PrivateKeyMO resource, @PathParam("id") String id) throws ResourceFactory.ResourceFactoryException {
        return super.update(resource, id);
    }

    /**
     * Export a private key.
     *
     * @param id                      The id of the key to export
     * @param privateKeyExportContext The export key context. This contains a password to secure the exported key with.
     *                                The password must always be specified but can be the empty string to specify no
     *                                password. This can also specify the alias to export the private key with. If no
     *                                alias is specified the key will be exported with its current alias.
     * @return The exported private key
     * @throws ResourceFactory.ResourceNotFoundException
     * @throws FindException
     */
    @PUT
    @XmlHeader(XslStyleSheetResource.DEFAULT_STYLESHEET_HEADER)
    @Path("{id}/export")
    public Item<PrivateKeyExportResult> exportPrivateKey(@PathParam("id") final String id, final PrivateKeyExportContext privateKeyExportContext) throws ResourceFactory.ResourceNotFoundException, FindException {
        final PrivateKeyExportResult exportResult = factory.exportResource(id, privateKeyExportContext.getPassword(), privateKeyExportContext.getAlias());
        final PrivateKeyExportResult restExport = new PrivateKeyExportResult();
        restExport.setPkcs12Data(exportResult.getPkcs12Data());
        return new ItemBuilder<PrivateKeyExportResult>(id + " Export", id, getResourceType() + "Export")
                .setContent(restExport)
                .addLink(ManagedObjectFactory.createLink("self", uriInfo.getRequestUri().toString()))
                .addLink(ManagedObjectFactory.createLink("privateKey", getUrlString(id)))
                .build();
    }

    /**
     * Import a private key.
     *
     * @param id                      The id to import the key into
     * @param privateKeyImportContext The private key to import. This contains the key data. It contains the password
     *                                that the key is secured with. An Alias can also be given to specify which alias to
     *                                use if the key given contains more then one alias. By default the first alias in
     *                                the given key is used.
     * @return A reference to the newly imported private key.
     * @throws ResourceFactory.ResourceNotFoundException
     * @throws FindException
     * @throws ResourceFactory.InvalidResourceException
     */
    @POST
    @XmlHeader(XslStyleSheetResource.DEFAULT_STYLESHEET_HEADER)
    @Path("{id}/import")
    public Item<PrivateKeyMO> importPrivateKey(@PathParam("id") String id, PrivateKeyImportContext privateKeyImportContext) throws ResourceFactory.ResourceNotFoundException, FindException, ResourceFactory.InvalidResourceException {
        PrivateKeyMO importResult = factory.importResource(id, privateKeyImportContext.getAlias(), privateKeyImportContext.getPkcs12Data(), privateKeyImportContext.getPassword());
        return new ItemBuilder<>(transformer.convertToItem(importResult))
                .addLink(getLink(importResult))
                .addLinks(getRelatedLinks(importResult))
                .build();
    }

    /**
     * Mark a private key for a special special purpose
     *
     * @param id      The id of the key to mark for special purpose
     * @param purpose The special purpose to mark the key with. Can specify more then one special purposes.
     * @return A reference to the newly updated private key.
     * @throws ResourceFactory.ResourceNotFoundException
     * @throws FindException
     * @throws ResourceFactory.InvalidResourceException
     */
    @PUT
    @XmlHeader(XslStyleSheetResource.DEFAULT_STYLESHEET_HEADER)
    @Path("{id}/specialPurpose")
    public Item<PrivateKeyMO> markSpecialPurpose(@PathParam("id") String id, @QueryParam("purpose") @ChoiceParam({"SSL", "CA", "AUDIT_VIEWER", "AUDIT_SIGNING"}) List<String> purpose) throws ResourceFactory.ResourceNotFoundException, FindException, ResourceFactory.InvalidResourceException {
        PrivateKeyMO resource = factory.setSpecialPurpose(id, purpose);
        return new ItemBuilder<>(transformer.convertToItem(resource))
                .addLink(getLink(resource))
                .addLinks(getRelatedLinks(resource))
                .build();
    }

    /**
     * Generate a certificate signing request for this private key.
     *
     * @param id            The id of the key to generate the CSR from
     * @param dn            The CSR subject dn to use. It defaults to the key's subject dn if none is specified.
     * @param signatureHash The signature hash to use. Defaults to 'Automatic'
     * @return The CSR data.
     * @throws ResourceFactory.ResourceNotFoundException
     * @throws ResourceFactory.InvalidResourceException
     */
    @GET
    @Path("{id}/generateCSR")
    public Item<PrivateKeyGenerateCsrResult> generateCSR(@PathParam("id") String id, @QueryParam("csrSubjectDN") String dn, @QueryParam("signatureHash") @ChoiceParam({"SHA1", "SHA256", "SHA384", "SHA512"}) String signatureHash) throws ResourceFactory.ResourceNotFoundException, ResourceFactory.InvalidResourceException {
        PrivateKeyGenerateCsrResult privateKeyGenerateCsrResult = factory.generateCSR(id, dn, signatureHash);
        return new ItemBuilder<PrivateKeyGenerateCsrResult>(id + " CSR", id, getResourceType() + "CSR")
                .setContent(privateKeyGenerateCsrResult)
                .addLink(ManagedObjectFactory.createLink("self", uriInfo.getRequestUri().toString()))
                .addLink(ManagedObjectFactory.createLink("privateKey", getUrlString(id)))
                .build();
    }

    /**
     * Deletes an existing private key.
     *
     * @param id The id of the private key to delete.
     * @throws ResourceFactory.ResourceNotFoundException
     */
    @DELETE
    @Path("{id}")
    @Override
    public void delete(@PathParam("id") String id) throws ResourceFactory.ResourceNotFoundException {
        super.delete(id);
    }

    /**
     * Signs a csr pem file with the specified key.
     *
     * @param id            The id of the key to sign the certificate with
     * @param subjectDN     The subject DN to set on the signed certificate
     * @param expiryAge     The expiry age of the certificate
     * @param signatureHash The signature hash to use. Defaults to 'Automatic'
     * @param certificate   The certificate csr to sign
     * @return The signed certificate.
     * @throws ResourceFactory.ResourceNotFoundException
     * @throws ResourceFactory.InvalidResourceException
     */
    @PUT
    @Path("{id}/signCert")
    @Consumes("application/x-pem-file")
    public Item<PrivateKeySignCsrResult> signCert(@PathParam("id") String id, @QueryParam("subjectDN") String subjectDN, @QueryParam("expiryAge") @DefaultValue("730") Integer expiryAge, @QueryParam("signatureHash") @ChoiceParam({"Automatic", "SHA1", "SHA256", "SHA384", "SHA512"}) @DefaultValue("Automatic") String signatureHash, String certificate) throws ResourceFactory.ResourceNotFoundException, ResourceFactory.InvalidResourceException {
        PrivateKeySignCsrResult privateKeySignCsrResult = factory.signCert(id, subjectDN, expiryAge, signatureHash, certificate);
        return new ItemBuilder<PrivateKeySignCsrResult>("Signed Cert", id, getResourceType() + "SignedCert")
                .setContent(privateKeySignCsrResult)
                .addLink(ManagedObjectFactory.createLink("self", uriInfo.getRequestUri().toString()))
                .addLink(ManagedObjectFactory.createLink("privateKey", getUrlString(id)))
                .build();
    }

    /**
     * This will return a template, example private key that can be used as a reference for what private key objects
     * should look like.
     *
     * @return The template private key.
     */
    @GET
    @Path("template")
    public Item<PrivateKeyMO> template() {
        PrivateKeyMO privateKeyMO = ManagedObjectFactory.createPrivateKey();
        privateKeyMO.setAlias("TemplateAlias");
        privateKeyMO.setKeystoreId("TemplateKeystoreID");
        privateKeyMO.setProperties(CollectionUtils.MapBuilder.<String, Object>builder().put("keyAlgorithm", "RSA").map());
        CertificateData certificateData = ManagedObjectFactory.createCertificateData();
        certificateData.setSubjectName("CN=subjectName");
        certificateData.setIssuerName("CN=issuerName");
        certificateData.setSerialNumber(new BigInteger("123"));
        certificateData.setEncoded("encoded".getBytes());
        privateKeyMO.setCertificateChain(Arrays.asList(certificateData));
        return super.createTemplateItem(privateKeyMO);
    }

    /**
     * This will return a template import context.
     *
     * @return The template private key import context.
     */
    @GET
    @Path("template/privatekeyimportcontext")
    public Item<PrivateKeyImportContext> templatePrivateKeyImportContext() {
        PrivateKeyImportContext privateKeyImportContext = new PrivateKeyImportContext();
        privateKeyImportContext.setPkcs12Data("keyData".getBytes());
        privateKeyImportContext.setPassword("password");
        privateKeyImportContext.setAlias("keyAlias");
        return new ItemBuilder<PrivateKeyImportContext>("PrivateKeyImportContext Template", "PrivateKeyImportContext")
                .addLink(ManagedObjectFactory.createLink("self", getUrlString("template/privatekeyimportcontext")))
                .setContent(privateKeyImportContext)
                .build();
    }

    /**
     * This will return a template export context.
     *
     * @return The template private key export context.
     */
    @GET
    @Path("template/privatekeyexportcontext")
    public Item<PrivateKeyExportContext> templatePrivateKeyExportContext() {
        PrivateKeyExportContext privateKeyExportContext = new PrivateKeyExportContext();
        privateKeyExportContext.setPassword("password");
        privateKeyExportContext.setAlias("keyAlias");
        return new ItemBuilder<PrivateKeyExportContext>("PrivateKeyExportContext Template", "PrivateKeyExportContext")
                .addLink(ManagedObjectFactory.createLink("self", getUrlString("template/privatekeyexportcontext")))
                .setContent(privateKeyExportContext)
                .build();
    }

    /**
     * This will return a template key creation context.
     *
     * @return The template private key creation context.
     */
    @GET
    @Path("template/privatekeycreationcontext")
    public Item<PrivateKeyCreationContext> templatePrivateKeyCreationContext() {
        PrivateKeyCreationContext privateKeyCreationContext = ManagedObjectFactory.createPrivateKeyCreationContext();
        privateKeyCreationContext.setDn("CN=keyAlias");
        privateKeyCreationContext.setProperties(CollectionUtils.MapBuilder.<String, Object>builder()
                .put("ecName", "secp384r1")
                .put("rsaKeySize", 516)
                .put("daysUntilExpiry", 2)
                .put("caCapable", true)
                .put("signatureHashAlgorithm", "SHA384")
                .map());
        return new ItemBuilder<PrivateKeyCreationContext>("PrivateKeyCreationContext Template", "PrivateKeyCreationContext")
                .addLink(ManagedObjectFactory.createLink("self", getUrlString("template/privatekeycreationcontext")))
                .setContent(privateKeyCreationContext)
                .build();
    }
}
