package com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.impl;

/* NOTE: The java docs in this class get converted to API documentation seen by customers! */

import com.l7tech.external.assertions.gatewaymanagement.server.ServerRESTGatewayManagementAssertion;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.RestManVersion;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.Since;
import com.l7tech.gateway.common.LicenseManager;
import com.l7tech.gateway.common.security.signer.SignatureVerifierHelper;
import com.l7tech.gateway.common.security.signer.SignerUtils;
import com.l7tech.gateway.common.solutionkit.BadRequestException;
import com.l7tech.gateway.common.solutionkit.ForbiddenException;
import com.l7tech.gateway.common.solutionkit.*;
import com.l7tech.gateway.rest.SpringBean;
import com.l7tech.identity.IdentityProviderConfigManager;
import com.l7tech.objectmodel.FindException;
import com.l7tech.policy.solutionkit.SolutionKitManagerUi;
import com.l7tech.server.policy.bundle.ssgman.restman.RestmanMessage;
import com.l7tech.server.security.signer.SignatureVerifierServer;
import com.l7tech.server.solutionkit.AddendumBundleHandler;
import com.l7tech.server.solutionkit.SolutionKitAdminHelper;
import com.l7tech.server.solutionkit.SolutionKitManager;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Functions;
import com.l7tech.util.Pair;
import com.l7tech.util.Triple;
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
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.security.SignatureException;
import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.l7tech.gateway.common.solutionkit.SolutionKit.SK_PROP_INSTANCE_MODIFIER_KEY;
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

    protected static final String FORM_FIELD_NAME_BUNDLE = "bundle";
    protected static final String PARAMETER_DELIMINATOR = "::";  // double colon separate ids; key_id :: value_id (e.g. f1649a0664f1ebb6235ac238a6f71a6d :: 66461b24787941053fc65a626546e4bd)

    private SolutionKitManager solutionKitManager;
    @SpringBean
    public void setSolutionKitManager(final SolutionKitManager solutionKitManager) {
        this.solutionKitManager = solutionKitManager;
    }

    private LicenseManager licenseManager;
    @SpringBean
    public void setLicenseManager(final LicenseManager licenseManager) {
        this.licenseManager = licenseManager;
    }

    private SignatureVerifierServer signatureVerifier;
    @SpringBean
    @Named("signatureVerifier")
    public void setSignatureVerifier(final SignatureVerifierServer signatureVerifier) {
        this.signatureVerifier = signatureVerifier;
    }

    private IdentityProviderConfigManager identityProviderConfigManager;
    @SpringBean
    public void setIdentityProviderConfigManager(final IdentityProviderConfigManager identityProviderConfigManager) {
        this.identityProviderConfigManager = identityProviderConfigManager;
    }

    public SolutionKitManagerResource() {}

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
     * 	            <code>entityIdReplace</code>. Optional. To map one entity ID to another. Format [find_id]::[replace_with_id]</>.
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
     *      --form "file=@/<your_path>/SimpleSolutionKit-1.1-20150823.skar.signed" --form MyInputTextKey=Hello
     *      https://127.0.0.1:8443/restman/1.0/solutionKitManagers
     * </code>
     *
     * <h5>To Upgrade</h5>
     * Same as install above except the formats of <code>solutionKitSelect</code> and <code>instanceModifier</code><br>
     *     Specify the previously installed Solution Kit ID to upgrade as a query parameter in the URL. The ID can be either a parent solution kit's GUID or a non-parent solution kit's GUID.<br>
     * For example:
     * <code>
     *     https://127.0.0.1:8443/restman/1.0/solutionKitManagers?id=33b16742-d62d-4095-8f8d-4db707e9ad52
     * </code>
     * Note: we're using HTML POST to upgrade since HTML PUT does not support forms and therefore does not support multipart file upload.
     * <ul>
     *     <li><code>solutionKitSelect</code>: to select an individual child solution kit for upgrade and/or update the current instance modifier using a new instance modifier
     *          <ul>
     *              The value format is [ID]::[Current_IM]::[New_IM]
     *              <li>[ID] is the GUID of a selected solution kit (e.g, a non-parent solution kit)</li>
     *              <li>[Current_IM] is the current instance modifier used by a selected solution kit</li>
     *              <li>[New_IM] is a new instance modifier, which the selected solution kit use to upgrade the current instance modifier.</li>
     *              <li>Note: the value of each instance modifier should not include "::", since "::" is a reserved delimiter.</li>
     *          </ul>
     *     </li>
     *     <li>
     *         <code>instanceModifier</code>: to set a global current and new instance modifiers for upgrading all selected solution kits (non-parent solution kits)
     *         <ul>
     *              The value format is [Global_Current_IM]::[Global_New_IM]
     *              <li>[Global_Current_IM] is the current instance modifier used by all selected solution kits</li>
     *              <li>[Global_New_IM] is a new instance modifier, which all selected solution kits use to upgrade the current instance modifier.</li>
     *              <li>Note: the value of each instance modifier should not include "::", since "::" is a reserved delimiter.</li>
     *         </ul>
     *     </li>
     *     <li>
     *         Usage of Solution Kit ID, <code>instanceModifier</code>, and <code>solutionKitSelect</code><br>
     *         - If ID is a parent solution kit GUID, then <code>instanceModifier</code> and <code>solutionKitSelect</code> accepted.<br>
     *         - If ID is a non-parent solution kit GUID, <code>instanceModifier</code> accepted and <code>solutionKitSelect</code> not accepted.
     *     </li>
     *     <li>
     *         <code>instanceModifier</code> combines with <code>solutionKitSelect</code>: The global instance modifiers will be applied on the selected solution kits specified by a list of <code>solutionKitSelect</code>
     *         <ul>
     *              <li>Individual [Current_IM] will overwrite [Global_Current_IM] for a particular selected solution kit.</li>
     *              <li>Individual [New_IM] will overwrite [Global_New_IM] for a particular selected solution kit.</li>
     *              <li>If individual solution kits do not specify [Current_IM] and [New_IM], then [Global_Current_IM] and [Global_New_IM] will be used.</li>
     *         </ul>
     *     </li>
     *     <li>
     *         Use <code>instanceModifier</code> only without <code>solutionKitSelect</code>
     *         <ul>
     *              <li>[Global_Current_IM] will be used to find all child solution kits, whose current instance modifier is [Global_Current_IM].</li>
     *              <li>[Global_New_IM] will be applied to change the current instance modifier during upgrading the above found solution kits.</li>
     *          </ul>
     *     </li>
     * </ul>
     *
     * @param fileInputStream Input stream of the upload SKAR file.
     * @param instanceModifierParameter Global instance modifiers of to-be-upgraded/installed solution kit(s).
     * @param solutionKitSelects Which Solution Kit(s) (found by ID and Instance Modifier) in the uploaded SKAR to install/upgrade. If not provided, all Solution Kit(s) in the upload SKAR will be installed/upgraded.
     * @param entityIdReplaces Optional. To map one entity ID to another. Format [find_id]::[replace_with_id].
     * @param upgradeGuid Optional, note this is a query parameter, not a form key-value. Select which Solution Kit ID(s) in the uploaded SKAR to upgrade.
     * @param formDataMultiPart See above.
     * @return Output from the Solution Kit Manager.
     */
    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response installOrUpgrade(final @FormDataParam("file") InputStream fileInputStream,
                                     final @FormDataParam("instanceModifier") String instanceModifierParameter,
                                     final @FormDataParam("solutionKitSelect") List<FormDataBodyPart> solutionKitSelects,
                                     final @FormDataParam("entityIdReplace") List<FormDataBodyPart> entityIdReplaces,
                                     final @QueryParam("id") String upgradeGuid,
                                     final FormDataMultiPart formDataMultiPart) {

        // NOTE: if changing a @FormDataParam("name") name in method declaration above, also need to change in setCustomizationKeyValues() below
        // Strings can't be made final static constants because Jersey annotations complains with error

        // Using POST to upgrade since HTML PUT does not support forms (and therefore does not support multipart file upload).

        // Couldn't use Solution Kit ID in the URL to upgrade (i.e. @Path("{id}") and @PathParam("id")).
        //      ... com.l7tech.external.assertions.gatewaymanagement.tools.WadlTest.test(2)
        //              junit.framework.AssertionFailedError: Invalid doc for param 'id' on request on method with id: 'null' at resource path: {id} ...

        final SolutionKitsConfig solutionKitsConfig = new SolutionKitsConfig();
        final SolutionKitAdminHelper solutionKitAdminHelper = new SolutionKitAdminHelper(licenseManager, solutionKitManager, identityProviderConfigManager);

        try {
            validateParams(fileInputStream);

            // handle upgrade
            final boolean isUpgrade = StringUtils.isNotBlank(upgradeGuid);
            if (isUpgrade) {
                // Check if the query parameter 'id' (i.e. GUID) is for a parent, child, or other (i.e., neither parent nor child).
                // Continue to validate two parameters 'id' and 'instanceModifier", which are combined to find a matched solution kit.
                final Pair<Boolean, SolutionKit> resultOfCheckingParent = isParentSolutionKit(upgradeGuid);
                final boolean isParent = resultOfCheckingParent.left;
                final SolutionKit solutionKitForUpgrade;

                // Find the solution kit to be upgrade, based on what kind of id is given, such as parent id, child id, or single sk id.
                if (isParent) {
                    solutionKitForUpgrade = resultOfCheckingParent.right;
                } else {
                    final String currentIM = instanceModifierParameter == null?
                            null : substringBefore(URLDecoder.decode(instanceModifierParameter, CharEncoding.UTF_8), PARAMETER_DELIMINATOR).trim();

                    final SolutionKit tempSK = solutionKitManager.findBySolutionKitGuidAndIM(upgradeGuid, currentIM);
                    if (tempSK == null) {
                        //There is a requirement from the note in the story SSG-10996, Upgrade Solution Kit.
                        // - dis-allow upgrade if you don't have SK already installed (install only)
                        final String warningMsg = "Upgrade failed: cannot find any existing solution kit (GUID = '" + upgradeGuid +
                                "',  Instance Modifier = '" + InstanceModifier.getDisplayName(currentIM) + "') for upgrade.";
                        logger.warning(warningMsg);
                        return status(NOT_FOUND).entity(warningMsg + lineSeparator()).build();
                    } else {
                        solutionKitForUpgrade = tempSK;
                    }
                }
                // Get the upgrade list.  However, this list has not applied user selection on solution kits based on parameters such id and solutionKitSelect.
                solutionKitsConfig.setSolutionKitsToUpgrade(solutionKitAdminHelper.getSolutionKitsToUpgrade(solutionKitForUpgrade));

                // This help info must be done before skarProcessor runs.
                setSelectedGuidAndImForHeadlessUpgrade(isParent, upgradeGuid, solutionKitsConfig, instanceModifierParameter, solutionKitSelects);

                // Update the upgrade list again after the above setSelectedGuidAndImForHeadlessUpgrade is done.
                updateSolutionKitsToUpgradeBasedOnGivenParameters(solutionKitsConfig);

                // find previously installed IDs to resolve
                solutionKitsConfig.setPreviouslyResolvedIds();
            }

            // verify signed skar signature and load the skar afterwards
            final SkarProcessor skarProcessor;
            try (final SignerUtils.SignedZipContent zipContent = new SignatureVerifierHelper(signatureVerifier).verifyZip(fileInputStream)) {
                skarProcessor = new SkarProcessor(solutionKitsConfig);
                skarProcessor.load(zipContent.getDataStream());
            }

            // handle any user selection(s) - child solution kits
            setUserSelections(solutionKitsConfig, solutionKitSelects, !isUpgrade, instanceModifierParameter);

            // Note that these selected solution kits have been assigned with instance modifiers (default: empty/null)
            final Set<SolutionKit> selectedSolutionKits = solutionKitsConfig.getSelectedSolutionKits();

            // optionally apply addendum bundle(s)
            new AddendumBundleHandler(formDataMultiPart, solutionKitsConfig, FORM_FIELD_NAME_BUNDLE).apply();

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

            // Test all selected (child) solution kit(s) before actual installation.
            // This step is to prevent partial installation/upgrade
            final SolutionKitProcessor solutionKitProcessor = new SolutionKitProcessor(solutionKitsConfig, solutionKitAdminHelper, skarProcessor);
            testInstallOrUpgrade(solutionKitProcessor, solutionKitAdminHelper);

            // install or upgrade
            solutionKitProcessor.installOrUpgrade();

        } catch (AddendumBundleHandler.AddendumBundleException e) {
            logger.log(Level.WARNING, ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
            return e.getResponse();
        } catch (SolutionKitManagerResourceException e) {
            logger.log(Level.WARNING, ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
            return e.getResponse();
        } catch (SignatureException e) {
            logger.log(Level.WARNING, ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
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

    protected void updateSolutionKitsToUpgradeBasedOnGivenParameters(@NotNull final SolutionKitsConfig solutionKitsConfig) throws FindException {
        // Check precondition: the map must be filled already based on parameters given by user
        final Map<String, Pair<String, String>> selectedGuidAndImForHeadlessUpgrade = solutionKitsConfig.getSelectedGuidAndImForHeadlessUpgrade();
        if (selectedGuidAndImForHeadlessUpgrade.isEmpty()) {
            throw new IllegalArgumentException("A map of guid and instance modifier for selected to-be-upgraded solution kits has not been initialized.");
        }

        final List<SolutionKit> finalUpgradeList = new ArrayList<>();
        final List<SolutionKit> solutionKitsToUpgrade = solutionKitsConfig.getSolutionKitsToUpgrade();

        // Add the parent first if the first element is a parent.
        if (solutionKitsToUpgrade.size() > 0) {
            final SolutionKit parentCandidate = solutionKitsToUpgrade.get(0);
            if (SolutionKitUtils.isParentSolutionKit(parentCandidate)) {
                finalUpgradeList.add(parentCandidate);
            }
        }

        // Get the final upgrade list
        String currentIM, tempIM, tempGuid;
        for (String guid: selectedGuidAndImForHeadlessUpgrade.keySet()) {
            currentIM = selectedGuidAndImForHeadlessUpgrade.get(guid).left;

            for (SolutionKit solutionKit: solutionKitsToUpgrade) {
                tempGuid = solutionKit.getSolutionKitGuid();
                tempIM = solutionKit.getProperty(SK_PROP_INSTANCE_MODIFIER_KEY);

                if (guid.equals(tempGuid) && InstanceModifier.isSame(currentIM, tempIM)) {
                    finalUpgradeList.add(solutionKit);
                }
            }
        }

        solutionKitsConfig.setSolutionKitsToUpgrade(finalUpgradeList);
    }

    private Pair<Boolean, SolutionKit> isParentSolutionKit(String solutionKitGuid) throws FindException, SolutionKitManagerResourceException {
        final List<SolutionKit> solutionKits = solutionKitManager.findBySolutionKitGuid(solutionKitGuid);
        final int size = solutionKits.size();

        if (size == 0) {
            throw new SolutionKitManagerResourceException(status(NOT_FOUND).entity("There does not exist any solution kit matching the GUID (" + solutionKitGuid + ")" + lineSeparator()).build());
        } else if (size > 1) {
            // Every parent is unique.  If found more than one, guarantee it is not a parent.
            return new Pair<>(false, null);
        }

        final SolutionKit parentCandidate = solutionKits.get(0);

        if (SolutionKitUtils.isParentSolutionKit(parentCandidate)) {
            return new Pair<>(true, parentCandidate);
        }

        return new Pair<>(false, null);
    }

    /**
     * Error Message Format during testBundleImports.
     */
    private static final String TEST_BUNDLE_IMPORT_ERROR_MESSAGE = "Test install/upgrade failed for solution kit: {0} ({1}). {2}";
    private static final String TEST_BUNDLE_IMPORT_ERROR_MESSAGE_NO_NAME_GUID = "Test install/upgrade failed for solution kit: {0}";

    /**
     * Attempt test install or upgrade (i.e. a dry run without committing) of the selected solution kits; handles potential conflicts.
     *
     * @param solutionKitProcessor class containing the test install / upgrade logic
     * @param solutionKitAdminHelper the helper class used to call validating solution kit for install or upgrade
     * @throws SolutionKitManagerResourceException if an error happens during dry-run, holding the response.
     */
    private void testInstallOrUpgrade(@NotNull final SolutionKitProcessor solutionKitProcessor,
                                      @NotNull final SolutionKitAdminHelper solutionKitAdminHelper) throws SolutionKitManagerResourceException {

        final AtomicReference<SolutionKit> solutionKitReference = new AtomicReference<>();
        try {
            solutionKitProcessor.testInstallOrUpgrade(new Functions.UnaryVoidThrows<Triple<SolutionKit, String, Boolean>, Throwable>() {
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
     * Uninstall the Solution Kit record and the entities installed from the original SKAR (if delete mappings were provided by the SKAR author).
     * <ul>
     *     <li>Use a <code>DELETE</code> request.</li>
     *     <li>Specify the Solution Kit ID as a query parameter in the URL. The ID consists of two parts, solution kit GUID and instance modifier used. The two parts are separated by "::".</li>
     *     <li>Four types of ID URL format:
     *          <ul>
     *              <li>https://localhost:8443/restman/1.0/solutionKitManagers?id=[Non-Parent_GUID]::[IM]</li>
     *              <li>https://localhost:8443/restman/1.0/solutionKitManagers?id=[Parent_GUID]::[IM]</li>
     *              <li>https://localhost:8443/restman/1.0/solutionKitManagers?id=[Parent_GUID]</li>
     *              <li>https://localhost:8443/restman/1.0/solutionKitManagers?id=[Parent_GUID]::[IM]&childId=[Child_1_GUID]::[IM1]...&childId=[Child_n_GUID]::[IMn]</li>
     *          </ul>
     *     </li>
     * </ul>
     * In the URL, [IM] is an instance modifier, which combines with a GUID to find a unique solution kit. If [IM] is missed, then a default instance modifier (empty value) will be used.
     * <ul>
     *     <li>Use the type (1), if deleting a single non-parent solution kit.</li>
     *     <li>Use the type (2), if deleting all child solution kits with the specified instance modifier.</li>
     *     <li>Use the type (3), if deleting all child solution kits.</li>
     *     <li>Use the type (4), if deleting some of child solution kits. Each individual instance modifier [IMx] will overwrite the global instance modifier [IM]. If individual solution kits don't specify [IMx], then the global instance modifier will be used.</li>
     * </ul>
     *
     * @param deleteGuidIM Solution kit GUID and instance modifier of a single solution kit or a collection of solution kits to delete.
     * @param childGuidIMList GUID and instance modifier of child solution kits to delete
     * @return Output from the Solution Kit Manager.
     */
    @DELETE
    public Response uninstall(
            final @QueryParam("id") String deleteGuidIM,
            final @QueryParam("childId") List<String> childGuidIMList) {

        // Couldn't use Solution Kit ID in the URL to upgrade (i.e. @Path("{id}") and @PathParam("id")).
        //      ... com.l7tech.external.assertions.gatewaymanagement.tools.WadlTest.test(2)
        //              junit.framework.AssertionFailedError: Invalid doc for param 'id' on request on method with id: 'null' at resource path: {id} ...

        final List<String> uninstallSuccessMessages = new ArrayList<>();
        final List<String> errorMessages = new ArrayList<>();
        StringBuilder message = new StringBuilder();
        String currentSolutionKitName = "";
        String currentSolutionKitIM = "";
        String currentSolutionKitGuid = "";

        try {
            if (StringUtils.isEmpty(deleteGuidIM)) {
                // HTTP DELETE, using 404 not found to be consistent with other restman resources
                throw new SolutionKitManagerResourceException(status(NOT_FOUND).entity("Solution Kit ID to uninstall is empty." + lineSeparator()).build());
            }

            final String deleteGuid = substringBefore(deleteGuidIM, PARAMETER_DELIMINATOR).trim();
            final String instanceModifier = substringAfter(deleteGuidIM, PARAMETER_DELIMINATOR).trim();

            final Pair<Boolean, SolutionKit> resultOfCheckingParent = isParentSolutionKit(deleteGuid);
            final boolean isParent = resultOfCheckingParent.left;
            final SolutionKit solutionKitToUninstall;

            if (isParent) {
                solutionKitToUninstall = resultOfCheckingParent.right;
            } else {
                final SolutionKit tempSK = solutionKitManager.findBySolutionKitGuidAndIM(deleteGuid, instanceModifier);
                if (tempSK == null) {
                    // There is a requirement from the note in the story SSG-10996, Upgrade Solution Kit.
                    // - dis-allow upgrade if you don't have SK already installed (install only)
                    final String warningMsg = "Uninstall failed: cannot find any existing solution kit (GUID = '" + deleteGuid + "', "+
                            printIM(instanceModifier) + ") for uninstall.";
                    logger.warning(warningMsg);
                    return status(NOT_FOUND).entity(warningMsg + lineSeparator()).build();
                } else {
                    solutionKitToUninstall = tempSK;
                }
            }

            final SolutionKitAdminHelper solutionKitAdminHelper = new SolutionKitAdminHelper(licenseManager, solutionKitManager, identityProviderConfigManager);
            final Collection<SolutionKit> childrenList = solutionKitAdminHelper.find(solutionKitToUninstall.getGoid());

            // If the solution kit is a parent solution kit, then check if there are any child guids specified from query parameters.
            if (isParent) {
                // No child list and no instance modifier specified means uninstall all child solution kits.
                if ((childGuidIMList == null || childGuidIMList.isEmpty()) && !deleteGuidIM.contains(PARAMETER_DELIMINATOR)) {
                    for (SolutionKit child: childrenList) {
                        currentSolutionKitName = child.getName();
                        currentSolutionKitIM = child.getProperty(SK_PROP_INSTANCE_MODIFIER_KEY);
                        currentSolutionKitGuid = child.getSolutionKitGuid();
                        solutionKitAdminHelper.uninstall(child.getGoid());
                        uninstallSuccessMessages.add("- '"+ currentSolutionKitName+ "' (GUID = '" + currentSolutionKitGuid +
                                "', "+ printIM(currentSolutionKitIM) + ")" + lineSeparator());
                    }
                }
                // No child list and instance modifier specified means uninstall all child kits with the instance modifier
                else if (childGuidIMList == null || childGuidIMList.isEmpty()) {
                    for (SolutionKit child: childrenList) {
                        currentSolutionKitIM = child.getProperty(SK_PROP_INSTANCE_MODIFIER_KEY);
                        if ((StringUtils.isBlank(currentSolutionKitIM) && StringUtils.isBlank(instanceModifier)) || StringUtils.equals(currentSolutionKitIM,instanceModifier)) {
                            currentSolutionKitName = child.getName();
                            currentSolutionKitGuid = child.getSolutionKitGuid();
                            solutionKitAdminHelper.uninstall(child.getGoid());
                            uninstallSuccessMessages.add("- '"+ currentSolutionKitName + "' (GUID = '" + currentSolutionKitGuid +
                                    "', " + printIM(currentSolutionKitIM) + ")" + lineSeparator());
                        }
                    }

                    // if no child solution kits were uninstalled given the IM, then return 404 error
                    if (uninstallSuccessMessages.isEmpty()) {
                        message.append("Uninstall failed. There were no child solution kits under parent solution kit with (GUID = '").append(deleteGuid)
                                .append(", ").append(printIM(instanceModifier)).append(")").append(lineSeparator());
                        throw new SolutionKitManagerResourceException(status(NOT_FOUND).entity(message.toString()).build());
                    }
                }

                // Otherwise, uninstall specified child solution kits.
                else {
                    final Set<String> childGuids = new HashSet<>(childrenList.size());
                    for (SolutionKit child: childrenList) {
                        childGuids.add(child.getSolutionKitGuid());
                    }

                    String guid, individualInstanceModifier;
                    SolutionKit selectedSolutionKit;

                    for (String guidIM: childGuidIMList) {
                        guid = substringBefore(guidIM, PARAMETER_DELIMINATOR).trim();
                        individualInstanceModifier = substringAfter(guidIM, PARAMETER_DELIMINATOR).trim();

                        // If the solutionKitSelect specifies an invalid guid, then report this error
                        if (! childGuids.contains(guid)) {
                            errorMessages.add("Uninstall failed: Cannot find any child solution kit matching the GUID = '" + guid + "'" + lineSeparator() );
                            continue;
                        }

                        String finalIM = useInstanceModifier(instanceModifier, individualInstanceModifier);
                        selectedSolutionKit = solutionKitManager.findBySolutionKitGuidAndIM(guid, finalIM);

                        if (selectedSolutionKit == null) {
                            final String warningMsg = "Uninstall failed: Cannot find any existing solution kit (GUID = '" + guid +
                                    "', " + printIM(finalIM) + ") for uninstall." + lineSeparator();
                            logger.warning(warningMsg);
                            errorMessages.add(warningMsg);
                        } else {
                            currentSolutionKitName = selectedSolutionKit.getName();
                            currentSolutionKitIM = finalIM;
                            currentSolutionKitGuid = selectedSolutionKit.getSolutionKitGuid();
                            solutionKitAdminHelper.uninstall(selectedSolutionKit.getGoid());
                            String uninstallMessage = "- '" + currentSolutionKitName + "' (GUID = '" + currentSolutionKitGuid + "', " +
                                    printIM(finalIM) + ")" + lineSeparator();
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

            }
            // Uninstall a non-parent solution kit
            else {
                currentSolutionKitName = solutionKitToUninstall.getName();
                currentSolutionKitIM = solutionKitToUninstall.getProperty(SK_PROP_INSTANCE_MODIFIER_KEY);
                currentSolutionKitGuid = solutionKitToUninstall.getSolutionKitGuid();
                solutionKitAdminHelper.uninstall(solutionKitToUninstall.getGoid());
                uninstallSuccessMessages.add("- '" + currentSolutionKitName + "' (GUID = '" + solutionKitToUninstall.getSolutionKitGuid() + "', " +
                        printIM(currentSolutionKitIM) + ")" + lineSeparator());
            }

            // Delete the parent solution kit, after all children are deleted at the above step.
            if (uninstallSuccessMessages.size() == childrenList.size()) {
                solutionKitManager.delete(solutionKitToUninstall.getGoid());
                uninstallSuccessMessages.add("- " + solutionKitToUninstall.getName() + " (GUID = '" +
                        solutionKitToUninstall.getSolutionKitGuid() + "')"+ lineSeparator());
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
                    .append(printIM(currentSolutionKitIM)).append(")")
                            .append(lineSeparator()).append(lineSeparator())
                    .append("Please see below for more details").append(lineSeparator()).append("--------------------");
            logger.log(Level.WARNING, e.getMessage(), e);    // log full exception for unexpected errors
            return status(INTERNAL_SERVER_ERROR).entity(message.toString() + lineSeparator() + e.getMessage() + lineSeparator()).build();
        }

        //Return a response with noContent() if all the uninstalls were successful
        return Response.noContent().build();
    }

    public String printIM(String instanceModifier) {
        return (StringUtils.isBlank(instanceModifier)? "without instance modifier" :
                ("instance modifier = '"+ instanceModifier + "'"));
    }

    private StringBuilder makeUninstallMessage(List<String> uninstallSuccessMessages, StringBuilder message) {
        message.append("Uninstalled solution kits:").append(lineSeparator());
        for (String success : uninstallSuccessMessages) {
            message.append(success);
        }
        message.append(lineSeparator()).append("Total Solution Kits deleted: ").append(uninstallSuccessMessages.size()).append(lineSeparator());
        return message;
    }

    private String useInstanceModifier(String globalInstanceModifier, String individualInstanceModifier) {  // TODO (TL refactor) move to InstanceModifier class?
        // Firstly check if individual instance modifier is specified.
        // The individual instance modifier will override the global instance modifier.
        String finalIM;
        if (StringUtils.isNotBlank(individualInstanceModifier)) {
            finalIM = individualInstanceModifier;
        }
        // Secondly check if global instance modifier is specified.
        else if (StringUtils.isNotBlank(globalInstanceModifier)) {
            finalIM = globalInstanceModifier;
        }
        // By default
        else {
            finalIM = null;
        }
        return finalIM;
    }

    // validate input params
    private void validateParams(@Nullable final InputStream fileInputStream) throws SolutionKitManagerResourceException {
        if (fileInputStream == null) {
            throw new SolutionKitManagerResourceException(status(BAD_REQUEST).entity(
                    "Missing mandatory upload file.  Choose a file to upload with form data field named: 'file'." + lineSeparator()).build());
        }
    }

    // Each item of solutionKitSelects could specify instance modifier split by "::".

    /**
     * Set user selection(s) based on form data parameters "solutionKitSelect" specifying a list of solution kits with
     * GUID and instance modifier, which is delimited by "::".
     *
     * This method is shared by install and upgrade.  If the parameter "isInstall" is true, then this method should be
     * used in the install context and the parameter globalInstanceModifier will be used to set those selected solution
     * kits which do not specify own instance modifier in the parameter "solutionKitSelects".  If this method is used in
     * upgrade context, then isInstall should be set to false and globalInstanceModifier should be ignored, since selected
     * solution kits must set own changeable instance modifier.
     *
     * @param solutionKitsConfig: used to get loaded solution kits and set selected solution kits.
     * @param solutionKitSelects: the form data parameter to specify a list of solution kits
     * @param isInstall: true if install is executed; false means upgrade.
     * @param instanceModifierParameter: the instance modifier is shared by all selected solution kits, but individual solution kit's instance modifier can override it.
     *
     * @throws SolutionKitManagerResourceException
     * @throws UnsupportedEncodingException
     */
    private void setUserSelections(
            @NotNull final SolutionKitsConfig solutionKitsConfig,
            @Nullable final List<FormDataBodyPart> solutionKitSelects,
            final boolean isInstall,
            @Nullable final String instanceModifierParameter) throws SolutionKitManagerResourceException, UnsupportedEncodingException {

        // Case 1: For Install
        if (isInstall) {
            selectSolutionKitsForInstall(solutionKitsConfig, instanceModifierParameter, solutionKitSelects);
        }
        // Case 2: For Upgrade
        else {
            selectSolutionKitsForUpgrade(solutionKitsConfig);
        }
    }

    /**
     * Set a list of solution kits for upgrade
     */
    protected void selectSolutionKitsForInstall(@NotNull final SolutionKitsConfig solutionKitsConfig,
                                                @Nullable final String instanceModifierParameter,
                                                @Nullable final List<FormDataBodyPart> solutionKitSelects) throws UnsupportedEncodingException, SolutionKitManagerResourceException {

        final Set<SolutionKit> loadedSolutionKits = new TreeSet<>(solutionKitsConfig.getLoadedSolutionKits().keySet());

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
            solutionKitsConfig.setSelectedSolutionKits(loadedSolutionKits);
        }
        // Case 2: There are some "solutionKitSelect" parameter(s) specified.
        else {
            // Create a map of guid and solution kit object for all loaded solution kits.
            final Map<String, SolutionKit> loadedSolutionKitMap = new HashMap<>(loadedSolutionKits.size());
            for (SolutionKit loadedSolutionKit : loadedSolutionKits) {
                loadedSolutionKitMap.put(loadedSolutionKit.getSolutionKitGuid(), loadedSolutionKit);
            }

            final Set<SolutionKit> selectedSolutionKits = new TreeSet<>();
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

            solutionKitsConfig.setSelectedSolutionKits(selectedSolutionKits);
        }
    }

    /**
     * Set a list of solution kits for upgrade
     * Precondition: The method setSelectedGuidAndImForHeadlessUpgrade must be called before this method is called.
     */
    protected void selectSolutionKitsForUpgrade(@NotNull final SolutionKitsConfig solutionKitsConfig) throws SolutionKitManagerResourceException {
        // Check the precondition:
        final Map<String, Pair<String, String>> selectedGuidAndImForHeadlessUpgrade = solutionKitsConfig.getSelectedGuidAndImForHeadlessUpgrade();
        if (selectedGuidAndImForHeadlessUpgrade.isEmpty()) {
            throw new IllegalArgumentException("A map of guid and instance modifier for selected to-be-upgraded solution kits has not been initialized.");
        }

        // Create a map of guid and solution kit object for all loaded solution kits.
        final Set<SolutionKit> loadedSolutionKits = new TreeSet<>(solutionKitsConfig.getLoadedSolutionKits().keySet());
        final Map<String, SolutionKit> loadedSolutionKitMap = new HashMap<>(loadedSolutionKits.size());
        for (SolutionKit loadedSolutionKit : loadedSolutionKits) {
            loadedSolutionKitMap.put(loadedSolutionKit.getSolutionKitGuid(), loadedSolutionKit);
        }

        SolutionKit loadedSK;
        Pair<String, String> instanceModifierPair;
        String newInstanceModifier;
        final Set<SolutionKit> selectedSolutionKits = new TreeSet<>();
        final Set<String> selectedGuidSet = selectedGuidAndImForHeadlessUpgrade.keySet();
        final Set<String> loadedGuidSet = loadedSolutionKitMap.keySet();

        for (String selectedGuid: selectedGuidSet) {
            if (! loadedGuidSet.contains(selectedGuid)) {
                throw new SolutionKitManagerResourceException(status(NOT_FOUND).entity("Solution Kit ID to upgrade: " +
                        selectedGuid + " not found in the skar." + lineSeparator()).build());
            }
        }

        for (String guidOfLoadedSK: loadedGuidSet) {
            // Ignore the loaded solution kit not matching selected guid from upgrade list
            if (! selectedGuidSet.contains(guidOfLoadedSK)) {
                continue;
            }

            instanceModifierPair = selectedGuidAndImForHeadlessUpgrade.get(guidOfLoadedSK);
            newInstanceModifier = instanceModifierPair.right;

            loadedSK = loadedSolutionKitMap.get(guidOfLoadedSK);
            loadedSK.setProperty(SolutionKit.SK_PROP_INSTANCE_MODIFIER_KEY, newInstanceModifier);
            InstanceModifier.setCustomContext(solutionKitsConfig, loadedSK);

            selectedSolutionKits.add(loadedSK);
        }

        //TODO: redundant condition?
        if (selectedSolutionKits.isEmpty()) {
            throw new SolutionKitManagerResourceException(status(NOT_FOUND).entity(
                    "There are no any solution kits being selectable for upgrade." + lineSeparator()).build());
        }

        solutionKitsConfig.setSelectedSolutionKits(selectedSolutionKits);
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

            // NOTE: skip known form fields from method declaration above (strings not final static constants because Jersey annotations complains with error)
            if (name != null && !name.startsWith(FORM_FIELD_NAME_BUNDLE) && !"file".equals(name) && !"solutionKitSelect".equals(name) && !"entityIdReplace".equals(name)) {
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
     * In headless upgrade, find all mappings for guid and instance modifier for selected solution kits, based on two parameters, "instanceModifier" and "solutionKitSelect".
     */
    protected void setSelectedGuidAndImForHeadlessUpgrade(final boolean isParent,
                                                          @NotNull final String upgradeGuid,
                                                          @NotNull final SolutionKitsConfig solutionKitsConfig,
                                                          @Nullable final String instanceModifierParameter,
                                                          @Nullable final List<FormDataBodyPart> solutionKitSelects) throws UnsupportedEncodingException, SolutionKitManagerResourceException, FindException {
        // Don't create a new return map.  Just use a map from SolutionKitConfig, so the result of this map can be shared thru accessing SolutionKitConfig.
        final Map<String, Pair<String, String>> selectedGuidAndImForHeadlessUpgrade = solutionKitsConfig.getSelectedGuidAndImForHeadlessUpgrade();  // it should be emtpy at beginning.
        final Pair<String, String> globalIMPair = processGlobalInstanceModifiers(instanceModifierParameter);
        final String currentGlobalIM = globalIMPair.left;
        final String newGlobalIM = globalIMPair.right;

        // Case 1: upgradeGuid is a parent solution kit GUID.
        if (isParent) {
            // Case 1.1: No "solutionKitSelect" parameters specified.
            if (solutionKitSelects == null || solutionKitSelects.isEmpty()) {
                final List<SolutionKit> kitList = solutionKitManager.findBySolutionKitGuid(upgradeGuid);
                if (kitList == null || kitList.isEmpty()) {
                    final String warningMsg = "Upgrade failed: cannot find a parent solution kit with GUID,  '" + upgradeGuid + "'";
                    logger.warning(warningMsg);
                    throw new SolutionKitManagerResourceException(status(NOT_FOUND).entity(warningMsg + lineSeparator()).build());
                }
                final SolutionKit parent = kitList.get(0); // parent is always unique.

                String childGuid;
                final Collection<SolutionKit> children = solutionKitManager.findAllChildrenByParentGoid(parent.getGoid());
                for (SolutionKit child: children) {
                    // Case 1: If there is no "instanceModifier" specified, then every child will be added.
                    // Case 2: If "instanceModifier" exists, then the children matching global instance modifier will be added.
                    if (instanceModifierParameter == null || InstanceModifier.isSame(currentGlobalIM, child.getProperty(SK_PROP_INSTANCE_MODIFIER_KEY))) {
                        childGuid = child.getSolutionKitGuid();
                        if (selectedGuidAndImForHeadlessUpgrade.keySet().contains(childGuid)) {
                            throw new SolutionKitManagerResourceException(status(CONFLICT).entity(
                                    "Upgrade failed: at least two child solution kits with a same GUID (" + childGuid + ") are selected for upgrade at same time."  + lineSeparator()).build());
                        }

                        selectedGuidAndImForHeadlessUpgrade.put(child.getSolutionKitGuid(), new Pair<>(currentGlobalIM, newGlobalIM));
                    }
                }

                // If the parent has children, but no any children were found by global instance modifier, then report error.
                if (children.size() > 0 && selectedGuidAndImForHeadlessUpgrade.isEmpty()) {
                    throw new SolutionKitManagerResourceException(status(NOT_FOUND).entity(
                            "Cannot find any to-be-upgraded solution kit(s), which matches the instance modifier (" +
                                    InstanceModifier.getDisplayName(currentGlobalIM) + ") specified by the parameter 'instanceModifier'" + lineSeparator()).build());
                }
            }
            // Case 1.2: There are some im2 "solutionKitSelect" parameters specified
            else {
                // Collect information of to-be-upgraded solution kits
                final List<SolutionKit> solutionKitsToUpgrade = solutionKitsConfig.getSolutionKitsToUpgrade();
                final Map<String, Set<String>> guidAndInstanceModifierMapFromUpgrade = SolutionKitUtils.getGuidAndInstanceModifierMapFromUpgrade(solutionKitsToUpgrade);

                Set<String> instanceModifierSetFromUpgrade, guidSet;
                String decodedStr, givenGuidFromPara, individualInstanceModifiers, currentIM, newIM;
                int numOfDeliminator;

                for (FormDataBodyPart solutionKitSelect : solutionKitSelects) {
                    decodedStr = URLDecoder.decode(solutionKitSelect.getValue(), CharEncoding.UTF_8);
                    givenGuidFromPara = substringBefore(decodedStr, PARAMETER_DELIMINATOR).trim();

                    // Do not allow two solutionKitSelect parameters specifying a same GUID, since two solution kit instances cannot be upgraded at the same time.
                    guidSet = selectedGuidAndImForHeadlessUpgrade.keySet();
                    if (guidSet.contains(givenGuidFromPara)) {
                        throw new SolutionKitManagerResourceException(status(NOT_FOUND).entity(
                                "Upgrade failed: at least two 'solutionKitSelect' parameters specify a same GUID (" +
                                        givenGuidFromPara + "), since two solution kit instances cannot be upgraded at the same time."  + lineSeparator()).build());
                    }

                    numOfDeliminator = StringUtils.countMatches(decodedStr, PARAMETER_DELIMINATOR);
                    if (numOfDeliminator == 0) {
                        currentIM = currentGlobalIM;
                        newIM = newGlobalIM;
                    } else {
                        individualInstanceModifiers = substringAfter(decodedStr, PARAMETER_DELIMINATOR).trim();
                        currentIM = substringBefore(individualInstanceModifiers, PARAMETER_DELIMINATOR).trim();
                        if (StringUtils.isBlank(currentIM)) currentIM = null;

                        if (numOfDeliminator == 1) {
                            newIM = currentIM;
                        } else {
                            newIM = substringAfter(individualInstanceModifiers, PARAMETER_DELIMINATOR).trim();
                            if (StringUtils.isBlank(newIM)) newIM = null;
                        }
                    }

                    // Do not allow the guid specified from the parameter, but not matched any solution kit from the upgrade list.
                    if (! guidAndInstanceModifierMapFromUpgrade.keySet().contains(givenGuidFromPara)) {
                        throw new SolutionKitManagerResourceException(status(NOT_FOUND).entity(
                                "Cannot find any to-be-upgraded solution kit, whose GUID matches to the given GUID (" +
                                        givenGuidFromPara + ") specified from the parameter 'solutionKitSelect'" + lineSeparator()).build());
                    }

                    instanceModifierSetFromUpgrade = guidAndInstanceModifierMapFromUpgrade.get(givenGuidFromPara);
                    boolean matched = false;

                    for (String instanceModifierFromUpgrade: instanceModifierSetFromUpgrade) {
                        matched = InstanceModifier.isSame(currentIM, instanceModifierFromUpgrade);
                        if (matched) {
                            selectedGuidAndImForHeadlessUpgrade.put(givenGuidFromPara, new Pair<>(currentIM, newIM));
                            break;
                        }
                    }
                    if (! matched) {
                        throw new SolutionKitManagerResourceException(status(NOT_FOUND).entity(
                                "Cannot find any to-be-upgraded solution kit, which matches the given GUID (" + givenGuidFromPara +
                                        ") and the given Instance Modifier (" + InstanceModifier.getDisplayName(currentIM) + ")" + lineSeparator()).build());
                    }
                }
            }
        }
        // Case 2: upgradeGuid is a non-parent solution kit GUID (e.g., child solution kit or single solution kit)
        else {
            selectedGuidAndImForHeadlessUpgrade.put(upgradeGuid, new Pair<>(currentGlobalIM, newGlobalIM));
        }
    }

    private Pair<String, String> processGlobalInstanceModifiers(@Nullable final String instanceModifierParameter) throws UnsupportedEncodingException {
        String currentGlobalIM = null;
        String newGlobalIM = null;
        if (instanceModifierParameter != null) {
            String decodedStr = URLDecoder.decode(instanceModifierParameter, CharEncoding.UTF_8);
            currentGlobalIM = substringBefore(decodedStr, PARAMETER_DELIMINATOR).trim();

            final int numOfDeliminator = StringUtils.countMatches(decodedStr, PARAMETER_DELIMINATOR);
            if (numOfDeliminator == 0) {
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