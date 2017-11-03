package com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.impl;

/* NOTE: The java docs in this class get converted to API documentation seen by customers! */

import com.l7tech.external.assertions.gatewaymanagement.server.ServerRESTGatewayManagementAssertion;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.RestManVersion;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.Since;
import com.l7tech.gateway.common.LicenseManager;
import com.l7tech.gateway.common.security.signer.SignerUtils;
import com.l7tech.gateway.common.security.signer.TrustedSignerCertsHelper;
import com.l7tech.gateway.common.security.signer.TrustedSignerCertsManager;
import com.l7tech.gateway.common.solutionkit.BadRequestException;
import com.l7tech.gateway.common.solutionkit.ForbiddenException;
import com.l7tech.gateway.common.solutionkit.*;
import com.l7tech.gateway.rest.SpringBean;
import com.l7tech.identity.IdentityProviderConfigManager;
import com.l7tech.objectmodel.FindException;
import com.l7tech.policy.solutionkit.SolutionKitManagerUi;
import com.l7tech.server.policy.bundle.ssgman.restman.RestmanMessage;
import com.l7tech.server.solutionkit.AddendumBundleHandler;
import com.l7tech.server.solutionkit.SolutionKitAdminHelper;
import com.l7tech.server.solutionkit.SolutionKitManager;
import com.l7tech.util.*;
import org.apache.commons.lang.CharEncoding;
import org.apache.commons.lang.StringUtils;
import org.glassfish.jersey.media.multipart.BodyPart;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.xml.sax.SAXException;

import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.security.SignatureException;
import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.lang.System.lineSeparator;
import static javax.ws.rs.core.Response.Status.*;
import static javax.ws.rs.core.Response.status;
import static org.apache.commons.lang.StringUtils.*;

/**
 * <p>This resource exposes the Solution Kit Manager for no-GUI use cases (e.g. support auto provisioning scripting to upload and manage a .skar file).</p>
 * <p>To interact with this resource use a tool like Advanced Rest Client (for Chrome) or cURL. Note the Solution Kit ID is the GUID provided in SolutionKit.xml.</p>
 */
@Provider
@Path(ServerRESTGatewayManagementAssertion.Version1_0_URI + "solutionKitManagers")
@Singleton
@Since(RestManVersion.VERSION_1_0_2)
public class SolutionKitManagerResource {
    private static final Logger logger = Logger.getLogger(SolutionKitManagerResource.class.getName());

    protected static final String PARAMETER_DELIMINATOR = "::";  // double colon separate ids; key_id :: value_id (e.g. f1649a0664f1ebb6235ac238a6f71a6d :: 66461b24787941053fc65a626546e4bd)

    private SolutionKitManager solutionKitManager;
    @SpringBean
    public void setSolutionKitManager(final SolutionKitManager solutionKitManager) {
        this.solutionKitManager = solutionKitManager;
        setSolutionKitAdminHelper();
    }

    private LicenseManager licenseManager;
    @SpringBean
    public void setLicenseManager(final LicenseManager licenseManager) {
        this.licenseManager = licenseManager;
        setSolutionKitAdminHelper();
    }

    private TrustedSignerCertsManager trustedSignerCertsManager;
    @SpringBean
    @Named("trustedSignerCertsManager")
    public void setTrustedSignerCertsManager(final TrustedSignerCertsManager trustedSignerCertsManager) {
        this.trustedSignerCertsManager = trustedSignerCertsManager;
    }

    private IdentityProviderConfigManager identityProviderConfigManager;
    @SpringBean
    public void setIdentityProviderConfigManager(final IdentityProviderConfigManager identityProviderConfigManager) {
        this.identityProviderConfigManager = identityProviderConfigManager;
        setSolutionKitAdminHelper();
    }

    private SolutionKitAdminHelper solutionKitAdminHelper;

    public SolutionKitManagerResource() {}

    void setSolutionKitAdminHelper(@NotNull final SolutionKitAdminHelper solutionKitAdminHelper) {
        this.solutionKitAdminHelper = solutionKitAdminHelper;
    }

    // After three beans are initialized, if they are not null, then initialize solutionKitAdminHelper.
    private void setSolutionKitAdminHelper() {
        if (licenseManager != null && solutionKitManager != null && identityProviderConfigManager != null) {
            solutionKitAdminHelper = new SolutionKitAdminHelper(licenseManager, solutionKitManager, identityProviderConfigManager);
        }
    }

    /**
     * Utility method to create a {@code HashSet} from the specified {@code String} value array.
     * Note that the method throws {@code Error} if there is a duplicate value.
     *
     * @param values    values to be added into the set.  Required and cannot be {@code null}.
     * @throws Error when {@code values} array contains a duplicate {@code string}.
     */
    @NotNull
    private static Set<String> toUniqueHashSet(@NotNull final String ... values) {
        final Set<String> set = new HashSet<>();
        for (final String value : values) {
            if (!set.add(value)) {
                throw new Error("Element '" + value + "' already exists in the set");
            }
        }
        return set;
    }

    /**
     * for installOrUpgrade method add new form and/or query parameters here
     * When adding new values or modifying exiting ones values make sure the unit test
     * {@code SolutionKitManagerResourceTest#testMethodParams} is changed accordingly.
     */
    @SuppressWarnings("UnusedDeclaration")
    static interface InstallOrUpgradeParams {
        // Form Params go here
        static interface Form {
            // Form Params
            static final String file = "file";
            static final String instanceModifier = "instanceModifier";
            static final String failOnExist = "failOnExist";
            static final String solutionKitSelect = "solutionKitSelect";
            static final String entityIdReplace = "entityIdReplace";
            static final String bundle = "bundle";
            // array of all known form params
            static final Set<String> all = Collections.unmodifiableSet(toUniqueHashSet(
                    file,
                    instanceModifier,
                    failOnExist,
                    solutionKitSelect,
                    entityIdReplace,
                    bundle
            ));
        }

        // Query Params go here
        static interface Query {
            //Query Params
            static final String id = "id";
            // array of all known query params
            static final Set<String> all = Collections.unmodifiableSet(toUniqueHashSet(
                    id
            ));
        }
    }

    /**
     * Install or upgrade a SKAR file.
     *
     * <h5>To Install</h5>
     * <ul>
     * 	<li>Upload a SKAR file using a <code>POST</code> request.</li>
     * 	<li>Set the encoding to <code>multipart/form-data</code>.</li>
     * 	<li>Set the file upload form-field name as <code>file</code>.  The uploaded file has to be signed (*.sskar format).</li>
     * 	<li>
     * 	    Add other form-field name and value as needed.
     * 	    <ul>
     * 	        <li>
     * 	            <code>entityIdReplace</code>. Optional. To map one entity ID (from the SKAR) to an existing gateway entity ID to resolve entity conflict. Format [find_id]::[replace_with_id]</>.
     * 	            <ul>
     *                  <li>The Solution Kit Manager will replace the entity ID to the left of the double colon (::) with the entity ID to the right. E.g. set <code>entityIdReplace</code> to <code>f1649a0664f1ebb6235ac238a6f71a6d::66461b24787941053fc65a626546e4bd</code>.</li>
     * 	                <li>Multiple values accepted (values from multiple fields with the same form-field name are treated as a list of values).</li>
     * 	                <li><b>Mapping must be overridable</b> from the solution kit author (e.g. mapping with srcId=[find_id] has <code>SK_AllowMappingOverride = true</code>). An error occurs when attempting to replace an non-overridable mapping.</li>
     * 	            </ul>
     * 	        </li>
     * 	        <li>
     * 	            <code>solutionKitSelect</code>: Optional. To select which solution kit in the uploaded SKAR will be installed and what instance modifier will be applied to the installed instance.
     *              <ul>
     *                  <li>The value format of <code>solutionKitSelect</code> is [ID]::[IM], where <i>"::"</i> is a deliminator, [ID] is the GUID of the installed solution kit, and [IM] is the instance modifier applied to the installed instance. If [IM] is not specified, or the value of [IM] is empty, then the default instance modifier (empty value) is used.</li>
     * 	                <li>If no <code>solutionKitSelect</code> provided, all solution kit(s) in the upload SKAR will be installed.</li>
     * 	                <li>Multiple <code>solutionKitSelect</code> allowed: to specify multiple solution kits to install.</li>
     * 	                <li>Note: the value of each instance modifier should not include "::", since "::" is a reserved delimiter.</li>
     * 	            </ul>
     * 	         </li>
     * 	         <li>
     * 	             <code>instanceModifier</code>. Optional. To specify an instance modifier applied to all installed solution kits. By default, if this form field is not specified or its value is empty, then this instance modifier uses a default value (empty value).
     * 	             <ul>
     * 	                 <li>Applying <code>instanceModifier</code> is a quick way to apply a same instance modifier to a list of solution kits that have a same instance modifier. However, this instance modifier is only applied in the following two scenarios:</li>
     * 	                 <li>(1) No any <code>solutionKitSelect</code> specified (i.e., all solution kit(s) in the SKAR will be installed.)</li>
     * 	                 <li>(2) Some <code>solutionKitSelect</code> specified, but without [IM] specified.</li>
     * 	                 <li>Note: the value of each instance modifier should not include "::", since "::" is a reserved delimiter.</li>
     * 	             </ul>
     * 	         </li>
     * 	        <li>Passing values to the Custom Callback. Optional. All form-fields not listed above will be passed to the Custom Callback.</li>
     * 	    </ul>
     * 	</li>
     * </ul>
     *
     * <p>Here's a cURL example (note the use of the --insecure option for development only):</p>
     * <code>
     *      curl --user admin_user:the_password --insecure
     *      --form entityIdReplace=f1649a0664f1ebb6235ac238a6f71a6d::66461b24787941053fc65a626546e4bd
     *      --form entityIdReplace=0567c6a8f0c4cc2c9fb331cb03b4de6f::1e3299eab93e2935adafbf35860fc8d9
     *      --form instanceModifier=AAA
     *      --form solutionKitSelect=33b16742-d62d-4095-8f8d-4db707e9ad52
     *      --form solutionKitSelect=33b16742-d62d-4095-8f8d-4db707e9ad53::BBB
     *      --form "file=@/<your_path>/SimpleSolutionKit-1.1-20150823.sskar" --form MyInputTextKey=Hello
     *      https://127.0.0.1:8443/restman/1.0/solutionKitManagers
     * </code>
     *
     * <h5>To Upgrade</h5>
     * Same as Install above except introducing a new query parameter <code></code>'id'</code> and changing the formats of <code>'instanceModifier'</code> and <code>'solutionKitSelect'</code>:
     * <ul>
     *     <li>
     *         Specify a query parameter 'id' for a previous installed Solution Kit GUID in the request URL. The ID can be either a parent solution kit's GUID or a non-parent solution kit's GUID.
     *         <br>For example:<code>https://127.0.0.1:8443/restman/1.0/solutionKitManagers?id=33b16742-d62d-4095-8f8d-4db707e9ad52</code>
     *     </li>
     *     <li>
     * 	        <code>instanceModifier</code>. Optional. To specify an existing instance modifier used to combine with the id query parameter to identify a unique solution kit for upgrade.  The value format of <code>instanceModifier</code> is [Current Instance Modifier].  If this parameter is not specified, a default instance modifier will be used (default value: empty string or null).
     * 	   </li>
     * 	   <li>
     * 	        <code>solutionKitSelect</code>: Optional. To select which child solution kits in the uploaded SKAR will be selected for upgrade. The value format of <code>solutionKitSelect</code> is [ID].
     * 	   </li>
     * </ul>
     *
     * <p>Here's a cURL example (note the use of the --insecure option for development only):</p>
     * <code>
     *      curl --user admin_user:the_password --insecure
     *      --form instanceModifier=AA
     *      --form solutionKitSelect=33b16742-d62d-4095-8f8d-4db707e9ad52
     *      --form solutionKitSelect=33b16742-d62d-4095-8f8d-4db707e9ad53
     *      --form file=@/<your_path>/SampleSolutionKit-upgrade-version.sskar
     *      https://127.0.0.1:8443/restman/1.0/solutionKitManagers?id=33b16742-d62d-4095-8f8d-4db707e9ad52
     * </code>
     *
     * @param fileInputStream Input stream of the upload SKAR file.
     * @param instanceModifierParameter Global instance modifiers of to-be-upgraded/installed solution kit(s).
     * @param failOnExistParameter Configures if Solution Kit installation should fail if one is already installed with the same ID and version. Default is true.
     * @param solutionKitSelects Which Solution Kit(s) (found by ID and Instance Modifier) in the uploaded SKAR to install/upgrade. If not provided, all Solution Kit(s) in the upload SKAR will be installed/upgraded.
     * @param entityIdReplaces Optional. To map one entity ID (from the SKAR) to an existing gateway entity ID to resolve entity conflict.  Format [find_id]::[replace_with_id].
     * @param upgradeGuid Optional, note this is a query parameter, not a form key-value. Select which Solution Kit ID(s) in the uploaded SKAR to upgrade.
     * @param formDataMultiPart See above.
     * @return Output from the Solution Kit Manager.
     */
    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response installOrUpgrade(final @FormDataParam(InstallOrUpgradeParams.Form.file) InputStream fileInputStream,
                                     final @FormDataParam(InstallOrUpgradeParams.Form.instanceModifier) String instanceModifierParameter,
                                     final @FormDataParam(InstallOrUpgradeParams.Form.failOnExist) String failOnExistParameter,
                                     final @FormDataParam(InstallOrUpgradeParams.Form.solutionKitSelect) List<FormDataBodyPart> solutionKitSelects,
                                     final @FormDataParam(InstallOrUpgradeParams.Form.entityIdReplace) List<FormDataBodyPart> entityIdReplaces,
                                     final @QueryParam(InstallOrUpgradeParams.Query.id) String upgradeGuid,
                                     final FormDataMultiPart formDataMultiPart) {
        // DEVELOPER NOTE: when adding new @FormDataParam or @QueryParam (or even modifying existing ones)
        // make sure they are added as constants inside InstallOrUpgradeParams as well as adding them into
        // corresponding InstallOrUpgradeParams#Form#all and InstallOrUpgradeParams#Query#all arrays.

        // Using POST to upgrade since HTML PUT does not support forms (and therefore does not support multipart file upload).

        // Couldn't use Solution Kit ID in the URL to upgrade (i.e. @Path("{id}") and @PathParam("id")).
        //      ... com.l7tech.external.assertions.gatewaymanagement.tools.WadlTest.test(2)
        //              junit.framework.AssertionFailedError: Invalid doc for param 'id' on request on method with id: 'null' at resource path: {id} ...

        final SolutionKitsConfig solutionKitsConfig = new SolutionKitsConfig();

        try {
            // This parameters validation is shared by install and upgrade.  Note: upgrade has separate own validation.
            validateParams(fileInputStream);

            String instanceModifierForUpgrade = null;
            List<String> selectedGuidList = null;
            SolutionKit foundByGuidAndIM = null;

            final boolean isUpgrade = StringUtils.isNotBlank(upgradeGuid);
            if (isUpgrade) {
                // Continue to validate the upgrade parameters, since parameters have been changed since 9.3 gateway.
                // Also new parameters must comply with backwards compatibility in pre-9.3 gateways.
                final Pair<String, List<String>> imAndGuidsPair = getValidatedUpgradeInfo(upgradeGuid, instanceModifierParameter, solutionKitSelects);
                instanceModifierForUpgrade = imAndGuidsPair.left; // Pair Left: instance modifier used in upgrade
                selectedGuidList = imAndGuidsPair.right;          // Pair Right: the guid list of selected solution kits for upgrade

                // Find a unique solution kit by GUID + Instance Modifier
                foundByGuidAndIM = solutionKitManager.findBySolutionKitGuidAndIM(upgradeGuid, instanceModifierForUpgrade);
                if (foundByGuidAndIM == null) {
                    throw new SolutionKitManagerResourceException(status(NOT_FOUND).entity("Upgrade failed: "+
                            cannotFindSolutionKitMessage(instanceModifierForUpgrade, upgradeGuid)+" for upgrade." + lineSeparator()).build()
                    );
                }

                // Set an upgrade candidate list in solutionKitsConfig.  This list will be used in setPreviouslyResolvedIds() and SkarPayload.process().
                solutionKitsConfig.setSolutionKitsToUpgrade(solutionKitAdminHelper.getSolutionKitsToUpgrade(foundByGuidAndIM));

                // Find previously installed IDs to resolve.
                solutionKitsConfig.setPreviouslyResolvedIds();
            }

            // Verify skar signature and create a SkarPayload to load the skar
            final SignerUtils.SignedZip signedZip = new SignerUtils.SignedZip(TrustedSignerCertsHelper.getTrustedCertificates(trustedSignerCertsManager));
            try (final SkarPayload payload = signedZip.load(fileInputStream, new SkarPayloadFactory(solutionKitsConfig))) {
                payload.process();
            } catch (final IOException e) {
                throw new SignatureException("Invalid signed Zip: " + ExceptionUtils.getMessage(e), e);
            }

            final boolean failOnExist;
            failOnExist = failOnExistParameter == null || !failOnExistParameter.equalsIgnoreCase("false");

            // Handle user selection and set selected solution kits for install or upgrade in solutionKitsConfig
            if (isUpgrade) {
                setSelectedSolutionKitsForUpgrade(foundByGuidAndIM, instanceModifierForUpgrade, selectedGuidList, solutionKitsConfig);
            } else {
                setSelectedSolutionKitsForInstall(solutionKitsConfig, instanceModifierParameter, solutionKitSelects, failOnExist);
            }

            // Note that these selected solution kits have been assigned with instance modifiers (default: empty/null)
            final Set<SolutionKit> selectedSolutionKits = solutionKitsConfig.getSelectedSolutionKits();

            // optionally apply addendum bundle(s)
            new AddendumBundleHandler(formDataMultiPart, solutionKitsConfig, InstallOrUpgradeParams.Form.bundle).apply();

            // (SSG-11727) Check if any two selected solution kits have same GUID and instance modifier. If so, return
            // a conflict error response and stop installation.  This checking is applied to install and upgrade.
            // This checking will detect a following error case:
            // - "id" (a parent guid) is specified and there are no "solutionKitSelects" specified.
            // - The skar file contains some solution kits with same guid.
            // - In this case, the above child solution kits have same guid and same instance modifier (default value).
            final String errorReport = SolutionKitUtils.haveDuplicateSelectedSolutionKits(selectedSolutionKits);
            if (StringUtils.isNotBlank(errorReport)) {
                return status(CONFLICT).entity(
                        "There are more than two selected solution kits having same GUID and Instance Modifier." + lineSeparator() + errorReport + lineSeparator()
                ).build();
            }

            // remap any entity id(s) (e.g. user configurable entity)
            remapEntityIds(solutionKitsConfig, entityIdReplaces);

            // pass in form fields as input parameters to customizations
            setCustomizationKeyValues(solutionKitsConfig.getCustomizations(), formDataMultiPart);

            final SolutionKitProcessor solutionKitProcessor = new SolutionKitProcessor(solutionKitsConfig, solutionKitAdminHelper);

            if (isUpgrade) {
                testUpgrade(solutionKitProcessor);
                solutionKitProcessor.upgrade(null);
            } else {
                testInstall(solutionKitProcessor);
                solutionKitProcessor.install(null,null);
            }

        } catch (AddendumBundleHandler.AddendumBundleException e) {
            logger.log(Level.WARNING, ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
            return e.getResponse();
        } catch (SolutionKitManagerResourceException e) {
            logger.log(Level.WARNING, ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
            return e.getResponse();
        } catch (SignatureException e) {
            logger.log(Level.WARNING, ExceptionUtils.getMessageWithCause(e), ExceptionUtils.getDebugException(e));
            return status(BAD_REQUEST).entity(e.getMessage() + lineSeparator()).build();
        } catch (ForbiddenException e) {
            logger.log(Level.WARNING, ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
            return Response.status(FORBIDDEN).entity(e.getMessage()).build();
        } catch (BadRequestException e) {
            logger.log(Level.WARNING, ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
            return Response.status(BAD_REQUEST).entity(e.getMessage()).build();
        } catch (Exception e) {
            logger.log(Level.WARNING, e.getMessage(), e);   // log full exception for unexpected errors
            return status(INTERNAL_SERVER_ERROR).entity(e.getMessage() + lineSeparator()).build();
        } finally {
            solutionKitsConfig.clear();
        }

        return Response.ok().entity("Request completed successfully." + lineSeparator()).build();
    }

    /**
     * Error Message Format during testBundleImports.
     */
    private static final String TEST_BUNDLE_IMPORT_ERROR_MESSAGE = "Test install/upgrade failed for solution kit: {0} ({1}). {2}";
    private static final String TEST_BUNDLE_IMPORT_ERROR_MESSAGE_NO_NAME_GUID = "Test install/upgrade failed for solution kit: {0}";

    /**
     * Attempt test install (i.e. a dry run without committing) of the selected solution kits; handles potential conflicts.
     *
     * @param solutionKitProcessor class containing the test install / upgrade logic
     * @throws SolutionKitManagerResourceException if an error happens during dry-run, holding the response.
     */
    private void testInstall(@NotNull final SolutionKitProcessor solutionKitProcessor) throws SolutionKitManagerResourceException {
        final AtomicReference<SolutionKit> solutionKitReference = new AtomicReference<>();
        try {
            solutionKitProcessor.testInstall(new Functions.UnaryVoidThrows<Triple<SolutionKit, String, Boolean>, Throwable>() {
                @Override
                public void call(Triple<SolutionKit, String, Boolean> loaded) throws Throwable {
                    solutionKitReference.set(loaded.left);

                    final String mappingsStr = solutionKitAdminHelper.testInstall(loaded.left, loaded.middle, loaded.right);

                    // no mappings; looks like there are no errors
                    if (StringUtils.isNotBlank(mappingsStr)) {
                        // create a RestmanMessage in order to parse error mappings
                        final RestmanMessage message;
                        try {
                            message = new RestmanMessage(mappingsStr);
                        } catch (final SAXException e) {
                            throw new SolutionKitManagerResourceException(
                                    status(INTERNAL_SERVER_ERROR).entity(
                                            MessageFormat.format(
                                                    TEST_BUNDLE_IMPORT_ERROR_MESSAGE,
                                                    loaded.left.getName(),
                                                    loaded.left.getSolutionKitGuid(),
                                                    lineSeparator() + ExceptionUtils.getMessage(e) + lineSeparator()
                                            )
                                    ).build(),
                                    e
                            );
                        }
                        if (message.hasMappingError()) {
                            throw new SolutionKitManagerResourceException(
                                    status(CONFLICT).entity(
                                            MessageFormat.format(
                                                    TEST_BUNDLE_IMPORT_ERROR_MESSAGE,
                                                    loaded.left.getName(),
                                                    loaded.left.getSolutionKitGuid(),
                                                    lineSeparator() + mappingsStr + lineSeparator()
                                            )
                                    ).build()
                            );
                        }
                    }
                }

            });
        } catch (final ForbiddenException e) {
            throw new SolutionKitManagerResourceException(status(FORBIDDEN).entity(getTestBundleImportErrorMessage(solutionKitReference.get(), e)).build(), e);
        } catch (final BadRequestException e) {
            throw new SolutionKitManagerResourceException(status(BAD_REQUEST).entity(getTestBundleImportErrorMessage(solutionKitReference.get(), e)).build(), e);
        } catch (final SolutionKitConflictException e) {
            throw new SolutionKitManagerResourceException(status(CONFLICT).entity(getTestBundleImportErrorMessage(solutionKitReference.get(), e)).build(), e);
        } catch (final SolutionKitManagerResourceException e) {
            throw e;
        } catch (final Throwable e) {
            throw new SolutionKitManagerResourceException(status(INTERNAL_SERVER_ERROR).entity(getTestBundleImportErrorMessage(solutionKitReference.get(), e)).build(), e);
        }
    }

    /**
     * Attempt test upgrade (i.e. a dry run without committing) of the selected solution kits; handles potential conflicts.
     *
     * @param solutionKitProcessor class containing the test install / upgrade logic
     * @throws SolutionKitManagerResourceException if an error happens during dry-run, holding the response.
     */
    private void testUpgrade(@NotNull final SolutionKitProcessor solutionKitProcessor) throws SolutionKitManagerResourceException {

        final AtomicReference<SolutionKit> solutionKitReference = new AtomicReference<>();
        try {
            solutionKitProcessor.testUpgrade(new Functions.UnaryVoidThrows<SolutionKitImportInfo, Throwable>() {
                @Override
                public void call(SolutionKitImportInfo loaded) throws Throwable {
                    final String mappingsStr = solutionKitAdminHelper.testUpgrade(loaded);
                    solutionKitReference.set(SolutionKitUtils.solutionKitToDisplayForUpgrade(loaded.getSolutionKitsToInstall().keySet(), loaded.getParentSolutionKit()));

                    // no mappings; looks like there are no errors
                    if (StringUtils.isNotBlank(mappingsStr)) {
                        // create a RestmanMessage in order to parse error mappings
                        final RestmanMessage message;
                        try {
                            message = new RestmanMessage(mappingsStr);
                        } catch (final SAXException e) {
                            throw new SolutionKitManagerResourceException(
                                    status(INTERNAL_SERVER_ERROR).entity(
                                            MessageFormat.format(
                                                    TEST_BUNDLE_IMPORT_ERROR_MESSAGE,
                                                    solutionKitReference.get().getName(),
                                                    solutionKitReference.get().getSolutionKitGuid(),
                                                    lineSeparator() + ExceptionUtils.getMessage(e) + lineSeparator()
                                            )
                                    ).build(),
                                    e
                            );
                        }
                        if (message.hasMappingErrorFromBundles()) {
                            throw new SolutionKitManagerResourceException(
                                    status(CONFLICT).entity(
                                            MessageFormat.format(
                                                    TEST_BUNDLE_IMPORT_ERROR_MESSAGE,
                                                    solutionKitReference.get().getName(),
                                                    solutionKitReference.get().getSolutionKitGuid(),
                                                    lineSeparator() + mappingsStr + lineSeparator()
                                            )
                                    ).build()
                            );
                        }
                    }
                }

            });
        } catch (final ForbiddenException e) {
            throw new SolutionKitManagerResourceException(status(FORBIDDEN).entity(getTestBundleImportErrorMessage(solutionKitReference.get(), e)).build(), e);
        } catch (final BadRequestException e) {
            throw new SolutionKitManagerResourceException(status(BAD_REQUEST).entity(getTestBundleImportErrorMessage(solutionKitReference.get(), e)).build(), e);
        } catch (final SolutionKitConflictException e) {
            throw new SolutionKitManagerResourceException(status(CONFLICT).entity(getTestBundleImportErrorMessage(solutionKitReference.get(), e)).build(), e);
        } catch (final SolutionKitManagerResourceException e) {
            throw e;
        } catch (final Throwable e) {
            throw new SolutionKitManagerResourceException(status(INTERNAL_SERVER_ERROR).entity(getTestBundleImportErrorMessage(solutionKitReference.get(), e)).build(), e);
        }
    }

    private String getTestBundleImportErrorMessage(@Nullable final SolutionKit solutionKit, @NotNull Throwable t) {
        if (solutionKit == null) {
            return MessageFormat.format(
                    TEST_BUNDLE_IMPORT_ERROR_MESSAGE_NO_NAME_GUID,
                    lineSeparator() + ExceptionUtils.getMessage(t) + lineSeparator()
            );
        } else {
            return MessageFormat.format(
                    TEST_BUNDLE_IMPORT_ERROR_MESSAGE,
                    solutionKit.getName(),
                    solutionKit.getSolutionKitGuid(),
                    lineSeparator() + ExceptionUtils.getMessage(t) + lineSeparator()
            );
        }
    }

    /**
     * for uninstall method add new form and/or query parameters here
     * When adding new values or modifying exiting ones values make sure the unit test
     * {@code SolutionKitManagerResourceTest#testMethodParams} is changed accordingly.
     */
    @SuppressWarnings("UnusedDeclaration")
    static interface UninstallParams {
        // Form Params go here
        static interface Form {
            // add potential form params here
            static final Set<String> all = Collections.unmodifiableSet(new HashSet<String>());
        }

        // Query Params go here
        static interface Query {
            //Query Params
            static final String id = "id";
            static final String childId = "childId";
            // array of all known query params
            static final Set<String> all = Collections.unmodifiableSet(toUniqueHashSet(
                    id,
                    childId
            ));
        }
    }

    /**
     * Uninstall the Solution Kit record and the entities installed from the original SKAR (if delete mappings were provided by the SKAR author).
     * <ul>
     *     <li>Use a <code>DELETE</code> request.</li>
     *     <li>Specify the Solution Kit ID as a query parameter in the URL. The ID consists of two parts, solution kit GUID and instance modifier used. The two parts are separated by "::".</li>
     *     <li>Four types of ID URL format:
     *          <ul>
     *              <li>https://localhost:8443/restman/1.0/solutionKitManagers?id=[Non-Parent_GUID]::[IM]</li>
     *              <li>https://localhost:8443/restman/1.0/solutionKitManagers?id=[Parent_GUID]::[IM]</li>
     *              <li>https://localhost:8443/restman/1.0/solutionKitManagers?id=[Parent_GUID]</li>
     *              <li>https://localhost:8443/restman/1.0/solutionKitManagers?id=[Parent_GUID]::[IM]&childId=[Child_1_GUID]...&childId=[Child_n_GUID]</li>
     *          </ul>
     *     </li>
     * </ul>
     * In the URL, [IM] is an instance modifier, which combines with a GUID to find a unique solution kit. If [IM] is missed, then a default instance modifier (empty value) will be used.
     * <ul>
     *     <li>Use the type (1), if deleting a single non-parent solution kit.</li>
     *     <li>Use the type (2), if deleting all child solution kits with the specified instance modifier.</li>
     *     <li>Use the type (3), if deleting all child solution kits without an instance modifier.</li>
     *     <li>Use the type (4), if deleting some of child solution kits specified by an instance modifier [IM]. Omit "::[IM]" part of the request to delete some child solution kits without an instance modifier.</li>
     * </ul>
     *
     * @param deleteGuidIM Solution kit GUID and instance modifier of a single solution kit or a collection of solution kits to delete.
     * @param childGuidIMList GUID and instance modifier of child solution kits to delete
     * @return Output from the Solution Kit Manager.
     */
    @DELETE
    public Response uninstall(
            final @QueryParam(UninstallParams.Query.id) String deleteGuidIM,
            final @QueryParam(UninstallParams.Query.childId) List<String> childGuidIMList) {

        // Couldn't use Solution Kit ID in the URL to upgrade (i.e. @Path("{id}") and @PathParam("id")).
        //      ... com.l7tech.external.assertions.gatewaymanagement.tools.WadlTest.test(2)
        //              junit.framework.AssertionFailedError: Invalid doc for param 'id' on request on method with id: 'null' at resource path: {id} ...

        final List<String> uninstallSuccessMessages = new ArrayList<>();
        final List<String> errorMessages = new ArrayList<>();
        StringBuilder message = new StringBuilder();
        String currentSolutionKitName = "";
        String currentSolutionKitGuid = "";
        String instanceModifier = "";

        try {
            if (StringUtils.isEmpty(deleteGuidIM)) {
                // HTTP DELETE, using 404 not found to be consistent with other restman resources
                throw new SolutionKitManagerResourceException(status(NOT_FOUND).entity("Solution Kit ID to uninstall is empty." + lineSeparator()).build());
            }

            final String deleteGuid = substringBefore(deleteGuidIM, PARAMETER_DELIMINATOR).trim();
            instanceModifier = getValidDeleteInstanceModifier(deleteGuidIM, childGuidIMList);

            final SolutionKit solutionKitToUninstall = solutionKitManager.findBySolutionKitGuidAndIM(deleteGuid, instanceModifier);
            if (solutionKitToUninstall == null) {
                final String warningMsg = "Uninstall failed: "+ cannotFindSolutionKitMessage(instanceModifier, deleteGuid) + " for uninstall.";
                logger.warning(warningMsg);
                throw new SolutionKitManagerResourceException(status(NOT_FOUND).entity(warningMsg + lineSeparator()).build());
            }

            final boolean isParent = SolutionKitUtils.isParentSolutionKit(solutionKitToUninstall);

            // If the solution kit is a parent solution kit, then check if there are any child guids specified from query parameters.
            if (isParent) {
                final Collection<SolutionKit> childrenList = solutionKitAdminHelper.find(solutionKitToUninstall.getGoid());
                // No child list specified means uninstall all child solution kits with the specified instance modifier.
                if (childGuidIMList.isEmpty()) {
                    for (SolutionKit child: childrenList) {
                        currentSolutionKitName = child.getName();
                        currentSolutionKitGuid = child.getSolutionKitGuid();
                        solutionKitAdminHelper.uninstall(child.getGoid());
                        uninstallSuccessMessages.add("- '"+ currentSolutionKitName+ "' (GUID = '" + currentSolutionKitGuid +
                                "', and Instance Modifier = '" + InstanceModifier.getDisplayName(instanceModifier) + "')" + lineSeparator());
                    }
                }
                // Otherwise, uninstall specified child solution kits.
                else {
                    final Set<String> childGuids = new HashSet<>(childrenList.size());
                    for (SolutionKit child: childrenList) {
                        childGuids.add(child.getSolutionKitGuid());
                    }

                    for (final String guidIM: childGuidIMList) {
                        final String guid = substringBefore(guidIM, PARAMETER_DELIMINATOR).trim();
                        // If the solutionKitSelect specifies an invalid guid, then report this error
                        if (! childGuids.contains(guid)) {
                            errorMessages.add("Uninstall failed: Cannot find any child solution kit matching the GUID = '" + guid + "'" + lineSeparator() );
                            continue;
                        }

                        final SolutionKit selectedSolutionKit = solutionKitManager.findBySolutionKitGuidAndIM(guid, instanceModifier);

                        if (selectedSolutionKit == null) {
                            final String warningMsg = "Uninstall failed: " + cannotFindSolutionKitMessage(instanceModifier, guid) + " for uninstall." + lineSeparator();
                            logger.warning(warningMsg);
                            errorMessages.add(warningMsg);
                        } else {
                            currentSolutionKitName = selectedSolutionKit.getName();
                            currentSolutionKitGuid = selectedSolutionKit.getSolutionKitGuid();
                            solutionKitAdminHelper.uninstall(selectedSolutionKit.getGoid());
                            String uninstallMessage = "- '" + currentSolutionKitName + "' (GUID = '" + currentSolutionKitGuid +
                                    "', and Instance Modifier = '" + InstanceModifier.getDisplayName(instanceModifier) + "')" + lineSeparator();
                            uninstallSuccessMessages.add(uninstallMessage);
                        }
                    }

                    // if no child selected solution kits were uninstalled, return 404 error
                    if (uninstallSuccessMessages.isEmpty()) {
                        message.append("UNINSTALL ERRORS:").append(lineSeparator());
                        for(String error : errorMessages) {
                            message.append(error);
                        }
                        throw new SolutionKitManagerResourceException(status(NOT_FOUND).entity(message.toString()).build());
                    }
                }

                // Delete the parent solution kit, after all children are deleted at the above step.
                if (uninstallSuccessMessages.size() == childrenList.size()) {
                    solutionKitManager.delete(solutionKitToUninstall.getGoid());
                    uninstallSuccessMessages.add("- " + solutionKitToUninstall.getName() + " (GUID = '" +
                            solutionKitToUninstall.getSolutionKitGuid() + "')"+ lineSeparator());
                }
            }
            // Uninstall a non-parent solution kit
            else {
                currentSolutionKitName = solutionKitToUninstall.getName();
                currentSolutionKitGuid = solutionKitToUninstall.getSolutionKitGuid();
                solutionKitAdminHelper.uninstall(solutionKitToUninstall.getGoid());
                uninstallSuccessMessages.add("- '" + currentSolutionKitName + "' (GUID = '" + currentSolutionKitGuid +
                        "', and Instance Modifier = '" + InstanceModifier.getDisplayName(instanceModifier) + "')" + lineSeparator());
            }

            //response 202 in the case where selected child kit does not exist
            if (!uninstallSuccessMessages.isEmpty() && !errorMessages.isEmpty()) {
                //Some solution kits uninstalled
                message = makeUninstallMessage(uninstallSuccessMessages, message);

                //Some solution kits where uninstall failed
                message.append(lineSeparator()).append("Solution kits selected for uninstall that failed:").append(lineSeparator());
                for (String error : errorMessages) {
                    message.append(error);
                }
                //Display kits that were successfully uninstalled and kits that failed at uninstall
                throw new SolutionKitManagerResourceException(status(ACCEPTED).entity(message.toString()).build());
            }

        } catch (SolutionKitManagerResourceException e) {
            logger.log(Level.WARNING, ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
            return e.getResponse();
        } catch (Exception e) {
            //case where there is an error in uninstallation (ie: wrong mapping)
            //Some solution kits uninstalled successfully
            if (uninstallSuccessMessages.size()>0) {
                message = makeUninstallMessage(uninstallSuccessMessages, message);
                message.append(lineSeparator());
            }
            message.append("ERROR IN SOLUTION KIT UNINSTALLATION WHEN PROCESSING '").append(currentSolutionKitName)
                    .append("' (GUID = '").append(currentSolutionKitGuid).append("', ")
                    .append(" and Instance Modifier = '").append(InstanceModifier.getDisplayName(instanceModifier)).append("')")
                    .append(lineSeparator()).append(lineSeparator())
                    .append("Please see below for more details").append(lineSeparator()).append("--------------------");
            logger.log(Level.WARNING, e.getMessage(), e);    // log full exception for unexpected errors
            return status(INTERNAL_SERVER_ERROR).entity(message.toString() + lineSeparator() + e.getMessage() + lineSeparator()).build();
        }

        //Return a response with noContent() if all the uninstalls were successful
        return Response.noContent().build();
    }

    /**
     * Error message to display when solution kit is not found.
     * @param instanceModifier Solution kit instance modifier.
     * @param guid Solution kit guid
     * @return The error message
     */
    private String cannotFindSolutionKitMessage(@Nullable final String instanceModifier, @NotNull final String guid) {
        return "Cannot find any existing solution kit (GUID = '" + guid +
                "', and Instance Modifier = '" + InstanceModifier.getDisplayName(instanceModifier) + "')";
    }

    /**
     * Validate instance modifiers for uninstall for the following rules:
     * if the parentIM is specified
     *   - if children IM is specified, it must be the same as the parent or children IM must be unspecified
     * if the parent IM is unspecified
     *   - all children IM must be the same as each other
     *
     * Note:"<child_guid>" and "<child_guid>::" both have an instance modifier of ""
     *
     * @param parentGuidIM the parent guid and instance modifier
     * @param childGuidIMList the list of child and their instance modifiers.
     * @return the instanceModifier determined by childGuidIM or by the parentGuidIM
     * @throws SolutionKitManagerResourceException Exception thrown when parameters are not valid
     */
    private @Nullable String getValidDeleteInstanceModifier(final @NotNull String parentGuidIM,
                                                  final @NotNull List<String> childGuidIMList) throws SolutionKitManagerResourceException {
        if (!parentGuidIM.contains(PARAMETER_DELIMINATOR)) {
            // If parent IM not specified, all children need to have same IM as another or no IM
            final String instanceModifier = childGuidIMList.isEmpty()? "" :
                    substringAfter(childGuidIMList.get(0), PARAMETER_DELIMINATOR).trim();
            for (String childGuidIm : childGuidIMList) {
                //Set the first child instanceModifier seen
                if (!instanceModifier.equals(substringAfter(childGuidIm, PARAMETER_DELIMINATOR).trim())) {
                    throw new SolutionKitManagerResourceException(status(CONFLICT).entity("Error: all child solution kit " +
                            "instance modifiers must be the same." +
                            lineSeparator() + "--------------------" +
                            lineSeparator() +
                            "List of child solution kits:" + lineSeparator() + StringUtils.join(childGuidIMList, lineSeparator())).build());
                }
            }
            // if childGuidIMList is empty, return a default IM, otherwise return the unique child IM (should only be one)
            return instanceModifier;

        } else {
            // if parent IM specified, All children need to have IM the same or no IM
            final String parentIM = substringAfter(parentGuidIM,PARAMETER_DELIMINATOR);
            for (String childGuidIm : childGuidIMList) {
                if (!parentIM.equals(substringAfter(childGuidIm, PARAMETER_DELIMINATOR).trim()) &&
                        childGuidIm.contains(PARAMETER_DELIMINATOR)) {
                    throw new SolutionKitManagerResourceException(status(CONFLICT).entity("Error: if child solution kit " +
                            "instance modifiers are specified, it must be the same as parent instance modifier." +
                            lineSeparator() + "--------------------" + lineSeparator() +
                            "Parent Solution Kit Instance Modifier: " + InstanceModifier.getDisplayName(parentIM) + lineSeparator() +
                            "List of child solution kits:" + lineSeparator() + StringUtils.join(childGuidIMList, lineSeparator())).build());
                }
            }
            return parentIM;
        }

    }

    private StringBuilder makeUninstallMessage(List<String> uninstallSuccessMessages, StringBuilder message) {
        message.append("Uninstalled solution kits:").append(lineSeparator());
        for (String success : uninstallSuccessMessages) {
            message.append(success);
        }
        message.append(lineSeparator()).append("Total Solution Kits deleted: ").append(uninstallSuccessMessages.size()).append(lineSeparator());
        return message;
    }

    // validate input params
    private void validateParams(@Nullable final InputStream fileInputStream) throws SolutionKitManagerResourceException {
        if (fileInputStream == null) {
            throw new SolutionKitManagerResourceException(status(BAD_REQUEST).entity(
                    "Missing mandatory upload file.  Choose a file to upload with form data field named: 'file'." + lineSeparator()).build());
        }
    }

    /**
     * Find a list of solution kits for install, selected by user based on two parameters, instanceModifierParameter and solutionKitSelects.
     * This method doesn't return this list, but will set this list in solutionKitsConfig.
     *
     * @param solutionKitsConfig: used to get loaded solution kits and set selected solution kits.
     * @param instanceModifierParameter: the instance modifier is shared by all selected solution kits, but individual solution kit's instance modifier can override it.
     * @param solutionKitSelects: the form data parameter to specify a list of solution kits
     * @param failOnExist the value of FailOnExist property
     *
     * @throws FindException thrown if error occurs during solutionKitAdminHelper attempts to find solution kits
     * @throws UnsupportedEncodingException thrown if character encoding needs to be consulted, but named character encoding is not supported.
     * @throws SolutionKitManagerResourceException thrown when NOT_FOUND cases happen during verifying GUID existence.
     */
    protected void setSelectedSolutionKitsForInstall(@NotNull final SolutionKitsConfig solutionKitsConfig,
                                                     @Nullable final String instanceModifierParameter,
                                                     @Nullable final List<FormDataBodyPart> solutionKitSelects,
                                                     final boolean failOnExist)
        throws FindException, UnsupportedEncodingException, SolutionKitManagerResourceException {

        final Set<SolutionKit> loadedSolutionKits = new TreeSet<>(solutionKitsConfig.getLoadedSolutionKits().keySet());
        final Set<SolutionKit> selectedSolutionKits = new TreeSet<>();

        String globalInstanceModifier = null; // Global instance modifier shared by all children
        if (instanceModifierParameter != null) {
            String decodedStr = URLDecoder.decode(instanceModifierParameter, CharEncoding.UTF_8);
            globalInstanceModifier = substringBefore(decodedStr, PARAMETER_DELIMINATOR).trim();
        }

        // Case 1: There are no "solutionKitSelect" parameters specified.
        if (solutionKitSelects == null || solutionKitSelects.isEmpty()) {
            if (StringUtils.isNotBlank(globalInstanceModifier)) {
                for (SolutionKit solutionKit: loadedSolutionKits) {
                    solutionKit.setProperty(SolutionKit.SK_PROP_INSTANCE_MODIFIER_KEY, globalInstanceModifier);
                    InstanceModifier.setCustomContext(solutionKitsConfig, solutionKit);
                }
            }
            selectedSolutionKits.addAll(loadedSolutionKits);
        }
        // Case 2: There are some "solutionKitSelect" parameter(s) specified.
        else {
            // Create a map of guid and solution kit object for all loaded solution kits.
            final Map<String, SolutionKit> loadedSolutionKitMap = new HashMap<>(loadedSolutionKits.size());
            for (SolutionKit loadedSolutionKit : loadedSolutionKits) {
                loadedSolutionKitMap.put(loadedSolutionKit.getSolutionKitGuid(), loadedSolutionKit);
            }

            String decodedStr, guid, individualInstanceModifier;
            SolutionKit selectedSolutionKit;

            for (FormDataBodyPart solutionKitSelect : solutionKitSelects) {
                decodedStr = URLDecoder.decode(solutionKitSelect.getValue(), CharEncoding.UTF_8);
                guid = substringBefore(decodedStr, PARAMETER_DELIMINATOR).trim();
                individualInstanceModifier = substringAfter(decodedStr, PARAMETER_DELIMINATOR).trim();

                if (!loadedSolutionKitMap.containsKey(guid)) {
                    throw new SolutionKitManagerResourceException(status(NOT_FOUND).entity("Solution Kit ID to install: " +
                            guid + " not found in the skar." + lineSeparator()).build());
                }

                selectedSolutionKit = loadedSolutionKitMap.get(guid);

                // Firstly check if individual instance modifier is specified.  In Install, this instance modifier will override the global instance modifier.
                if (StringUtils.isNotBlank(individualInstanceModifier)) {
                    selectedSolutionKit.setProperty(SolutionKit.SK_PROP_INSTANCE_MODIFIER_KEY, individualInstanceModifier);
                    InstanceModifier.setCustomContext(solutionKitsConfig, selectedSolutionKit);
                }
                // Secondly check if global instance modifier is specified.  This global value is only used in install, not applied for upgrade.
                else if (StringUtils.isNotBlank(globalInstanceModifier)) {
                    selectedSolutionKit.setProperty(SolutionKit.SK_PROP_INSTANCE_MODIFIER_KEY, globalInstanceModifier);
                    InstanceModifier.setCustomContext(solutionKitsConfig, selectedSolutionKit);
                }

                selectedSolutionKits.add(selectedSolutionKit);
            }

            //TODO: redundant condition?
            if (selectedSolutionKits.isEmpty()) {
                throw new SolutionKitManagerResourceException(status(NOT_FOUND).entity(
                        "There are no any solution kits being selectable for install." + lineSeparator()).build());
            }
        }

        // remove selected solution kits if fail on existing is false an they are already installed.
        if (!failOnExist) {
            final Set<SolutionKit> filteredSolutionKits = new TreeSet<>();
            for (final SolutionKit solutionKit : selectedSolutionKits) {
                final Collection<SolutionKit> existingSolutionKit = solutionKitAdminHelper.find(solutionKit.getSolutionKitGuid());
                if (existingSolutionKit.size() != 1 ||
                        !existingSolutionKit.iterator().next().getSolutionKitVersion().equals(solutionKit.getSolutionKitVersion())) {
                    filteredSolutionKits.add(solutionKit);
                }
            }
            solutionKitsConfig.setSelectedSolutionKits(filteredSolutionKits);
        } else {
            solutionKitsConfig.setSelectedSolutionKits(selectedSolutionKits);
        }
    }

    /**
     * Find a list of solution kits for upgrade, selected by user based on two parameters, instanceModifier and selectedGuidList.
     * This method doesn't return this list, but will set this list in solutionKitsConfig.
     *
     * @param solutionKitForUpgrade the solution kit identified by id (SK GUID) and instance modifier from the caller.
     * @param instanceModifier the instance modifier specified by user in instanceModifierParameter
     * @param selectedGuidList the GUID list of solution kits selected by user using solutionKitSelect parameters
     * @param solutionKitsConfig the solution kit information container
     * @throws SolutionKitManagerResourceException thrown when some NOT_ACCEPTABLE and NOT_FOUND cases happen.
     */
    void setSelectedSolutionKitsForUpgrade(@NotNull final SolutionKit solutionKitForUpgrade,
                                           @Nullable final String instanceModifier,
                                           @NotNull final List<String> selectedGuidList,
                                           @NotNull final SolutionKitsConfig solutionKitsConfig) throws SolutionKitManagerResourceException {
        final List<SolutionKit> solutionKitsToUpgrade = solutionKitsConfig.getSolutionKitsToUpgrade();
        final List<String> candidateGuidList = new ArrayList<>(solutionKitsToUpgrade.size());
        final List<String> finalSelectedGuidList = new ArrayList<>();

        for (final SolutionKit solutionKit: solutionKitsToUpgrade) {
            candidateGuidList.add(solutionKit.getSolutionKitGuid());
        }

        // Generate a selected and loaded solution kit list, based on user selection and the skar file uploaded.
        final Map<String, SolutionKit> loadedSolutionKitMap = new HashMap<>(); // A map to keep references of GUID and uploaded SolutionKit
        for (final SolutionKit loadedSolutionKit : solutionKitsConfig.getLoadedSolutionKits().keySet()) {
            loadedSolutionKitMap.put(loadedSolutionKit.getSolutionKitGuid(), loadedSolutionKit);
        }

        final boolean isParent = SolutionKitUtils.isParentSolutionKit(solutionKitForUpgrade);

        // Case: Choose a parent and some child solution kits to upgrade
        if (isParent && !selectedGuidList.isEmpty()) {
            finalSelectedGuidList.addAll(selectedGuidList);
        }
        // Case: Choose a parent with all children to upgrade
        else if (isParent) {
            // All loaded SK list will be a selected list.
            finalSelectedGuidList.addAll(loadedSolutionKitMap.keySet());
        }
        // Case: Choose a child solution kit to upgrade
        else if (candidateGuidList.size() == 2) {
            finalSelectedGuidList.add(candidateGuidList.get(1)); // b/c the first element in the upgrade candidate list is a parent.
        }
        // Case: Choose a non-parent and non-child solution kit to upgrade
        else {
            finalSelectedGuidList.addAll(candidateGuidList); // In this case, candidateGuidList should have one element.
        }

        // Use selected solution kits to update the loaded solution kits with new attributes such as instance modifier and custom context.
        final Set<SolutionKit> selectedLoadedSolutionKits = new TreeSet<>();
        final Set<String> loadedGuidSet = loadedSolutionKitMap.keySet();
        for (final String selectedGuid: finalSelectedGuidList) {
            // If any solution kit from the uploaded skar couldn't be found to match the selected solution kit for upgrade, throw exception.
            if (! loadedGuidSet.contains(selectedGuid)) {
                throw new SolutionKitManagerResourceException(status(NOT_FOUND).entity("There isn't any solution kit in " +
                    "the uploaded skar to match a selected solution kit (GUID='" + selectedGuid + "', Instance " +
                    "Modifier='" + InstanceModifier.getDisplayName(instanceModifier) + "')" + lineSeparator()).build());
            }

            final SolutionKit loadedSK = loadedSolutionKitMap.get(selectedGuid);
            loadedSK.setProperty(SolutionKit.SK_PROP_INSTANCE_MODIFIER_KEY, instanceModifier);
            InstanceModifier.setCustomContext(solutionKitsConfig, loadedSK);
            selectedLoadedSolutionKits.add(loadedSK);
        }

        // Finally set the selected list in solutionKitsConfig for upgrade.
        solutionKitsConfig.setSelectedSolutionKits(selectedLoadedSolutionKits);
    }

    // remap any entity id(s) (e.g. user configurable entity)
    private void remapEntityIds(@NotNull final SolutionKitsConfig solutionKitsConfig, @Nullable final List<FormDataBodyPart> entityIdReplaces) throws UnsupportedEncodingException {
        final Map<String, String> entityIdReplaceMap = new HashMap<>(entityIdReplaces == null? 0 : entityIdReplaces.size());

        if (entityIdReplaces != null) {
            String entityIdReplaceStr;
            for (FormDataBodyPart entityIdReplace : entityIdReplaces) {
                entityIdReplaceStr = entityIdReplace.getValue();
                if (isNotEmpty(entityIdReplaceStr)) {
                    decodeSplitPut(entityIdReplaceStr, entityIdReplaceMap);
                }
            }
        }

        Map<String, Pair<SolutionKit, Map<String, String>>> idRemapBySolutionKit = new HashMap<>();
        for (SolutionKit solutionKit: solutionKitsConfig.getSelectedSolutionKits()) {
            idRemapBySolutionKit.put(solutionKit.getSolutionKitGuid(), new Pair<>(solutionKit, entityIdReplaceMap));
        }

        solutionKitsConfig.setResolvedEntityIds(idRemapBySolutionKit);
    }

    protected static void decodeSplitPut(@NotNull final String entityIdReplaceStr, @NotNull final Map<String, String> entityIdReplaceMap) throws UnsupportedEncodingException {
        final String entityIdReplaceDecoded = URLDecoder.decode(entityIdReplaceStr, CharEncoding.UTF_8);
        entityIdReplaceMap.put(substringBefore(entityIdReplaceDecoded, PARAMETER_DELIMINATOR).trim(), substringAfter(entityIdReplaceDecoded, PARAMETER_DELIMINATOR).trim());
    }

    private void setCustomizationKeyValues(@NotNull final Map<String, Pair<SolutionKit, SolutionKitCustomization>> customizations,
                                           @NotNull FormDataMultiPart formDataMultiPart) throws SolutionKitException, SolutionKitManagerResourceException, UnsupportedEncodingException {
        // pass on all unknown form fields as customization key-value input
        final List<BodyPart> bodyParts = formDataMultiPart.getBodyParts();
        final List<FormDataBodyPart> keyValues = new ArrayList<>(bodyParts.size());
        for (BodyPart bodyPart : bodyParts) {
            Map<String, String> parameters = bodyPart.getContentDisposition().getParameters();
            final String name = parameters.get("name");

            // NOTE: skip known form fields from method declaration above
            if (name != null && !InstallOrUpgradeParams.Form.all.contains(name)) {
                keyValues.add(formDataMultiPart.getField(name));
            }
        }

        // pass in any input key-values to the customizations
        for (String solutionKitGuid : customizations.keySet()) {
            Pair<SolutionKit, SolutionKitCustomization> customization = customizations.get(solutionKitGuid);
            if (customization != null && customization.right != null) {
                final SolutionKitManagerUi customUi = customization.right.getCustomUi();
                if (customUi != null) {
                    String customizationKey, customizationValue;
                    for (FormDataBodyPart customizationKeyValue : keyValues) {
                        customizationKey = URLDecoder.decode(customizationKeyValue.getName(), CharEncoding.UTF_8);
                        try {
                            customizationValue = URLDecoder.decode(customizationKeyValue.getValue(), CharEncoding.UTF_8);
                        } catch (IllegalStateException e) {
                            logger.log(Level.WARNING, ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
                            throw new SolutionKitManagerResourceException(status(BAD_REQUEST).entity(
                                    "Unable to pass form-field value for '" + customizationKey + "' to the Custom Callback. " + e.getMessage() + lineSeparator()).build());
                        }
                        if (isNotEmpty(customizationKey) && isNotEmpty(customizationValue)) {
                            customUi.getContext().getKeyValues().put(customizationKey, customizationValue);
                        }
                    }
                }
            }
        }
    }

    /**
     * Validate the upgrade parameters, since parameters have been changed since 9.3 gateway. Also new parameters must
     * comply with backwards compatibility in pre-9.3 gateways.  After validation is successfully passed, return valid
     * upgrade information such as a unique instance modifier and a GUID list of selected solution kits.
     *
     * Note: this method does not include any logic to verify whether selected solution kits are acceptable for upgrade.
     * Please see such logic to verify acceptance in {@link #setSelectedSolutionKitsForUpgrade}.
     *
     * @param upgradeGuid the @QueryParam parameter, upgradeGuid
     * @param instanceModifierParameter the @FormDataParam parameter, instanceModifierParameter
     * @param solutionKitSelects the @FormDataParam parameter, solutionKitSelects
     *
     * @return a pair of instance modifier used in upgrade and the guid list of selected solution kits for upgrade.
     *
     * @throws UnsupportedEncodingException thrown if character encoding needs to be consulted, but named character encoding is not supported.
     * @throws SolutionKitManagerResourceException thrown when INTERNAL_SERVER_ERROR and NOT_ACCEPTABLE cases happen during validating parameters.
     */
    @NotNull
    Pair<String, List<String>> getValidatedUpgradeInfo(@NotNull final String upgradeGuid,
                                                       @Nullable final String instanceModifierParameter,
                                                       @Nullable final List<FormDataBodyPart> solutionKitSelects)
        throws UnsupportedEncodingException, SolutionKitManagerResourceException {

        final Pair<String, String> globalIMPair = processGlobalInstanceModifiers(instanceModifierParameter); // The global instance modifier specified by instanceModifierParameter
        final String currentGlobalIM = globalIMPair.left; // Pair Left: current instance modifier
        final String newGlobalIM = globalIMPair.right;    // Pair Right: new instance modifier (Not supported in 9.3+).
        final List<String> selectedGuidList = new ArrayList<>();
        final List<String> imList = new ArrayList<>(); // Hold all given instance modifiers

        // Case: There are some child solution kits explicitly selected by user.
        if (solutionKitSelects != null && !solutionKitSelects.isEmpty()) {
            for (int i = 0; i < solutionKitSelects.size(); i++) {
                final FormDataBodyPart solutionKitSelect = solutionKitSelects.get(i);
                final String decodedStr = URLDecoder.decode(solutionKitSelect.getValue(), CharEncoding.UTF_8);

                // Get the GUID from the parameter string and add it into the GUID list
                final String guid = substringBefore(decodedStr, PARAMETER_DELIMINATOR).trim();
                selectedGuidList.add(guid);

                // Analyze instance modifiers
                String currentIM, newIM;
                final int numOfDeliminator = StringUtils.countMatches(decodedStr, PARAMETER_DELIMINATOR);
                if (numOfDeliminator == 0) {
                    currentIM = currentGlobalIM;
                    newIM = newGlobalIM;
                } else {
                    final String instanceModifiersStr = substringAfter(decodedStr, PARAMETER_DELIMINATOR).trim();
                    currentIM = substringBefore(instanceModifiersStr, PARAMETER_DELIMINATOR).trim();
                    if (StringUtils.isBlank(currentIM)) currentIM = null;

                    if (numOfDeliminator == 1) {
                        newIM = currentIM;
                    } else {
                        newIM = substringAfter(instanceModifiersStr, PARAMETER_DELIMINATOR).trim();
                        if (StringUtils.isBlank(newIM)) newIM = null;
                    }
                }

                // Call the below method 'checkSameInstanceModifiers' to check whether both instance modifiers are same.
                // If not same, the check method throws exception immediately.  Otherwise, store their value into imList.
                checkSameInstanceModifiers(currentIM, newIM, guid, i + 1);
                imList.add(currentIM);
            }
        }
        // Case: there are no solution kits selected by user.  Then, just use the global instance modifier setting.
        else {
            // Call the below method 'checkSameInstanceModifiers' to check whether both instance modifiers are same.
            // If not same, the check method throws exception immediately.  Otherwise, store their value into imList.
            checkSameInstanceModifiers(currentGlobalIM, newGlobalIM, upgradeGuid, 1);
            imList.add(currentGlobalIM);
        }

        // imList must contain at least one element.
        if (imList.size() < 1) {
            throw new SolutionKitManagerResourceException(status(INTERNAL_SERVER_ERROR).entity(
                "Unexpected errors happen: Instance modifier information not found!" + lineSeparator()).build());
        }
        // Validate whether all instance modifiers are same.
        // Use the first element in imList to compare all other given instance modifiers.
        final String instanceModifier = imList.get(0);
        for (int i = 1; i < imList.size(); i++) {
            final String im = imList.get(i);
            if (! InstanceModifier.isSame(instanceModifier, im)) {
                throw new SolutionKitManagerResourceException(status(NOT_ACCEPTABLE).entity(
                    "Cannot upgrade child solution kits with different instance modifiers specified." + lineSeparator() +
                     "Failure detail: Solution Kit 1 (ID: " + selectedGuidList.get(0) + ") has instance modifier '" + InstanceModifier.getDisplayName(instanceModifier) + "' specified." + lineSeparator() +
                     "                Solution Kit " + (i + 1) + " (ID: " + selectedGuidList.get(i) + ") has instance modifier '" + InstanceModifier.getDisplayName(im) + "' specified." + lineSeparator()
                ).build());
            }
        }

        // When the above validation is finished and passed, all instance modifiers in imList must have the same value
        // as the value of the variable 'instanceModifier'.  Then return instanceModifier and selectedGuidList.
        return new Pair<>(instanceModifier, selectedGuidList);
    }

    /**
     * Check whether two instance modifiers (currentIM and newIM) are same.  If same, validation is passed and silently
     * ends.  Otherwise, throw SolutionKitManagerResourceException.
     *
     * To comply with instance modifier parameter rule in 9.3, for each pair of current instance modifier and new instance
     * modifier in pre-9.3, both instance modifiers must be same, since solution kit upgrade function doesn't allow new
     * instance modifier to update the current instance modifier in 9.3+.
     *
     * @param currentIM the instance modifier currently used by installed solution kit
     * @param newIM the new instance modifier attempts to update the current instance modifier in pre-9.3.  It is not allowed in 9.3+.
     * @param guid the GUID of a selected solution kit
     * @param index the index of a selected solution kit in the selection list.
     * @throws SolutionKitManagerResourceException thrown if currentIM is not same as newIM.
     */
    private void checkSameInstanceModifiers(@Nullable final String currentIM, @Nullable final String newIM,
                                            @NotNull final String guid, final int index) throws SolutionKitManagerResourceException {
        if (! InstanceModifier.isSame(currentIM, newIM)) {
            throw new SolutionKitManagerResourceException(status(NOT_ACCEPTABLE).entity(
                "Cannot upgrade a solution kit and change its instance modifier at the same time." + lineSeparator() +
                "Failure detail: Solution Kit " + index + " (ID: " + guid + ") currently has instance modifier '" +
                InstanceModifier.getDisplayName(currentIM) + "', which cannot be changed to '" + InstanceModifier.getDisplayName(newIM) + "'." + lineSeparator()
            ).build());
        }
    }

    private Pair<String, String> processGlobalInstanceModifiers(@Nullable final String instanceModifierParameter) throws UnsupportedEncodingException {
        String currentGlobalIM = null;
        String newGlobalIM = null;
        if (instanceModifierParameter != null) {
            String decodedStr = URLDecoder.decode(instanceModifierParameter, CharEncoding.UTF_8);
            currentGlobalIM = substringBefore(decodedStr, PARAMETER_DELIMINATOR).trim();

            if (! decodedStr.contains(PARAMETER_DELIMINATOR)) {
                newGlobalIM = currentGlobalIM;
            } else {
                newGlobalIM = substringAfter(decodedStr, PARAMETER_DELIMINATOR).trim();
                if (StringUtils.isBlank(newGlobalIM)) newGlobalIM = null;
            }
        }

        return new Pair<>(currentGlobalIM, newGlobalIM);
    }

    class SolutionKitManagerResourceException extends Exception {
        @NotNull
        private Response response;

        public SolutionKitManagerResourceException(@NotNull final Response response) {
            super();
            this.response = response;
        }

        public SolutionKitManagerResourceException(@NotNull final Response response, final Throwable cause) {
            super(cause);
            this.response = response;
        }

        @NotNull
        public Response getResponse() {
            return response;
        }
    }
}
