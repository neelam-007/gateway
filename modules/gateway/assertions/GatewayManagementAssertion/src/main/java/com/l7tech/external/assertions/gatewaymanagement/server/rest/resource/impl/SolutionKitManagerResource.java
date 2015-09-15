package com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.impl;

/* NOTE: The java docs in this class get converted to API documentation seen by customers! */

import com.l7tech.common.io.XmlUtil;
import com.l7tech.external.assertions.gatewaymanagement.server.ServerRESTGatewayManagementAssertion;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.RestManVersion;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.Since;
import com.l7tech.gateway.api.Bundle;
import com.l7tech.gateway.api.Item;
import com.l7tech.gateway.api.Mapping;
import com.l7tech.gateway.api.impl.MarshallingUtils;
import com.l7tech.gateway.common.LicenseManager;
import com.l7tech.gateway.common.solutionkit.SkarProcessor;
import com.l7tech.gateway.common.solutionkit.SolutionKitCustomization;
import com.l7tech.gateway.common.solutionkit.SolutionKitUtils;
import com.l7tech.gateway.common.solutionkit.SolutionKitsConfig;
import com.l7tech.gateway.common.solutionkit.BadRequestException;
import com.l7tech.gateway.common.solutionkit.ForbiddenException;
import com.l7tech.gateway.common.solutionkit.*;
import com.l7tech.gateway.rest.SpringBean;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.solutionkit.SolutionKitManagerUi;
import com.l7tech.server.policy.bundle.ssgman.restman.RestmanMessage;
import com.l7tech.server.security.signer.SignatureVerifier;
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
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.inject.Singleton;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;
import javax.xml.transform.dom.DOMSource;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.security.SignatureException;
import java.text.MessageFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.l7tech.gateway.common.solutionkit.SolutionKitsConfig.MAPPING_PROPERTY_NAME_SK_ALLOW_MAPPING_OVERRIDE;
import static com.l7tech.gateway.common.solutionkit.SolutionKit.SK_PROP_ALLOW_ADDENDUM_KEY;
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

    private SignatureVerifier signatureVerifier;
    @SpringBean
    public void setSignatureVerifier(final SignatureVerifier signatureVerifier) {
        this.signatureVerifier = signatureVerifier;
    }

    public SolutionKitManagerResource() {}

    /**
     * Install or upgrade a SKAR file.
     *
     * <h5>To Install</h5>
     * <ul>
     * 	<li>Upload a SKAR file using a <code>POST</code> request.</li>
     * 	<li>Set the encoding to <code>multipart/form-data</code>.</li>
     * 	<li>Set the file upload form-field name as <code>file</code>.</li>
     * 	<li>
     * 	    Add other form-field name and value as needed.
     * 	    <ul>
     * 	        <li>
     * 	            <code>entityIdReplace</code>. Optional. To map one entity ID to another. Format <i>find_id</i>::<i>replace_with_id</i></>.
     * 	            <ul>
     *                  <li>The Solution Kit Manager will replace the entity ID to the left of the double colon (::) with the entity ID to the right. E.g. set <code>entityIdReplace</code> to <code>f1649a0664f1ebb6235ac238a6f71a6d::66461b24787941053fc65a626546e4bd</code>.</li>
     * 	                <li>Multiple values accepted (values from multiple fields with the same form-field name are treated as a list of values).</li>
     * 	                <li><b>Mapping must be overridable</b> from the solution kit author (e.g. mapping with srcId=<find_id> has <code>SK_AllowMappingOverride = true</code>). An error occurs when attempting to replace an non-overridable mapping.</li>
     * 	            </ul>
     * 	        </li>
     * 	        <li><code>solutionKitSelect</code>. Optional. To select which Solution Kit in the uploaded SKAR to install. A Solution Kit ID. If not provided, all Solution Kit(s) in the upload SKAR will be installed. Multiple values accepted (values from multiple fields with the same form-field name are treated as a list of values).</li>
     * 	        <li>Passing values to the Custom Callback. Optional. All form-fields not listed above will be passed to the Custom Callback.
     * 	    </ul>
     * 	</li>
     * </ul>
     *
     * <p>Here's a cURL example (note the use of the --insecure option for development only):</p>
     * <code>
     *     curl --user admin_user:the_password --insecure --form entityIdReplace=f1649a0664f1ebb6235ac238a6f71a6d::66461b24787941053fc65a626546e4bd --form entityIdReplace=0567c6a8f0c4cc2c9fb331cb03b4de6f::1e3299eab93e2935adafbf35860fc8d9 --form "file=@/<your_path>/SimpleSolutionKit-1.1-20150823.skar.signed" --form MyInputTextKey=Hello https://127.0.0.1:8443/restman/1.0/solutionKitManagers
     * </code>
     *
     * <h5>To Upgrade</h5>
     * Same as install above, and the following.
     * <ul>
     *     <li>Specify the previously installed Solution Kit ID to upgrade as a query parameter in the URL. For example:</li>
     * </ul>
     * <code>
     *     https://127.0.0.1:8443/restman/1.0/solutionKitManagers?id=33b16742-d62d-4095-8f8d-4db707e9ad52
     * </code>
     *
     * @param fileInputStream Input stream of the upload SKAR file.
     * @param instanceModifier For upgrade, the instance modifier of the to-be-upgraded solution kit.  For install, the instance modifier of the to-be-installed solution kit.
     * @param solutionKitSelects Which Solution Kit ID(s) in the uploaded SKAR to install/upgrade. If not provided, all Solution Kit(s) in the upload SKAR will be installed/upgraded.
     * @param entityIdReplaces Optional. To map one entity ID to another. Format <find_id>::<replace_with_id>.
     * @param upgradeGuid Optional, note this is a query parameter, not a form key-value. Select which Solution Kit ID(s) in the uploaded SKAR to upgrade.
     * @param formDataMultiPart See above.
     * @return Output from the Solution Kit Manager.
     */
    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response installOrUpgrade(final @FormDataParam("file") InputStream fileInputStream,
                                     final @FormDataParam("instanceModifier") String instanceModifier,
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
        final SolutionKitAdminHelper solutionKitAdminHelper = new SolutionKitAdminHelper(licenseManager, solutionKitManager, signatureVerifier);
        String warningMessage = null;

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

                if (isParent) {
                    solutionKitForUpgrade = resultOfCheckingParent.right;
                } else {
                    final SolutionKit tempSK = solutionKitManager.findBySolutionKitGuidAndIM(upgradeGuid, instanceModifier);
                    if (tempSK == null) {
                        //There is a requirement from the note in the story SSG-10996, Upgrade Solution Kit.
                        // - dis-allow upgrade if you don't have SK already installed (install only)
                        final String instanceModifierDisplayName = StringUtils.isBlank(instanceModifier)? "N/A" : instanceModifier;
                        final String warningMsg = "Upgrade failed: cannot find any existing solution kit (GUID = '" + upgradeGuid + "',  Instance Modifier = '" + instanceModifierDisplayName + "') for upgrade.";
                        logger.warning(warningMsg);
                        return status(NOT_FOUND).entity(warningMsg + lineSeparator()).build();
                    } else {
                        solutionKitForUpgrade = tempSK;
                    }
                }
                solutionKitsConfig.setSolutionKitsToUpgrade(
                    solutionKitAdminHelper.getSolutionKitsToUpgrade(solutionKitForUpgrade)
                );

                // find previously installed mappings where srcId differs from targetId (e.g. user resolved)
                solutionKitsConfig.onUpgradeResetPreviouslyInstalledMappings();
            }

            // load skar
            final SkarProcessor skarProcessor = new SkarProcessor(solutionKitsConfig);
            skarProcessor.load(
                fileInputStream,
                new Functions.BinaryVoidThrows<byte[], String, SignatureException>() {
                    @Override
                    public void call(final byte[] digest, final String signature) throws SignatureException {
                        solutionKitAdminHelper.verifySkarSignature(digest, signature);
                    }
                }
            );

            // handle any user selection(s) - child solution kits
            warningMessage = setUserSelections(solutionKitsConfig, solutionKitSelects, !isUpgrade, instanceModifier);

            // Note that these selected solution kits have been assigned with instance modifiers (default: empty/null)
            final Set<SolutionKit> selectedSolutionKits = solutionKitsConfig.getSelectedSolutionKits();

            // optionally apply addendum bundle(s)
            applyAddendumBundles(formDataMultiPart, solutionKitsConfig);

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

            // remap any entity id(s)
            remapEntityIds(solutionKitsConfig, entityIdReplaces);

            // pass in form fields as input parameters to customizations
            setCustomizationKeyValues(solutionKitsConfig.getCustomizations(), formDataMultiPart);

            // Test all selected (child) solution kit(s) before actual installation.
            // This step is to prevent partial installation/upgrade
            testBundleImports(solutionKitAdminHelper, solutionKitsConfig);

            // Check if the loaded skar is a collection of skars
            final SolutionKit parentSKFromLoad = solutionKitsConfig.getParentSolutionKitLoaded();
            Goid parentGoid = null;

            // Process parent solution kit first, if a parent solution kit is loaded.
            if (parentSKFromLoad != null) {
                // Case 1: Parent for upgrade
                if (isUpgrade) {
                    assert solutionKitsConfig.getSolutionKitsToUpgrade().size() > 0; // should always be grater then 0 as check is done above (early fail)
                    final SolutionKit parentSKFromDB = solutionKitsConfig.getSolutionKitsToUpgrade().get(0); // The first element is a real parent solution kit.

                    if (!parentSKFromLoad.getSolutionKitGuid().equalsIgnoreCase(parentSKFromDB.getSolutionKitGuid())) {
                        String warningMsg = "The query parameter 'id' (" + upgradeGuid + ") does not match the GUID (" + parentSKFromDB.getSolutionKitGuid() + ") of the loaded solution kit from file.";
                        logger.warning(warningMsg);

                        return status(NOT_FOUND).entity(warningMsg + lineSeparator()).build();
                    }

                    // Update the parent solution kit attributes
                    parentSKFromDB.setName(parentSKFromLoad.getName());
                    parentSKFromDB.setSolutionKitVersion(parentSKFromLoad.getSolutionKitVersion());
                    parentSKFromDB.setXmlProperties(parentSKFromLoad.getXmlProperties());

                    parentGoid = parentSKFromDB.getGoid();
                    solutionKitManager.update(parentSKFromDB);
                }
                // Case 2: Parent for install
                else {
                    final List<SolutionKit> solutionKitsExistingOnGateway = solutionKitManager.findBySolutionKitGuid(parentSKFromLoad.getSolutionKitGuid());
                    // Case 2.1: Find the parent already installed on gateway
                    if (solutionKitsExistingOnGateway.size() > 0) {
                        final SolutionKit parentExistingOnGateway = solutionKitsExistingOnGateway.get(0);
                        parentGoid = parentExistingOnGateway.getGoid();
                        solutionKitManager.update(parentExistingOnGateway);
                    }
                    // Case 2.2: No such parent installed on gateway
                    else {
                        parentGoid = solutionKitManager.save(parentSKFromLoad);
                    }
                }
            }

            // install or upgrade skars
            // After processing the parent, process selected solution kits if applicable.
            for (SolutionKit solutionKit : selectedSolutionKits) {
                // If the solution kit is under a parent solution kit, then set its parent goid before it gets saved.
                if (parentSKFromLoad != null) {
                    solutionKit.setParentGoid(parentGoid);
                }

                skarProcessor.invokeCustomCallback(solutionKit);

                Triple<SolutionKit, String, Boolean> triple = skarProcessor.installOrUpgrade(solutionKit);
                solutionKitAdminHelper.install(triple.left, triple.middle, triple.right);
            }
        } catch (SolutionKitManagerResourceException e) {
            logger.log(Level.WARNING, ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
            return e.getResponse();
        } catch (UntrustedSolutionKitException | SignatureException e) {
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

        return Response.ok().entity("Request completed successfully." + lineSeparator() + (StringUtils.isBlank(warningMessage) ? "" : warningMessage + lineSeparator())).build();
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

        if (SolutionKit.PARENT_SOLUTION_KIT_DUMMY_MAPPINGS.equals(parentCandidate.getMappings())) {
            return new Pair<>(true, parentCandidate);
        }

        return new Pair<>(false, null);
    }

    /**
     * Error Message Format during testBundleImports.
     */
    private static final String TEST_BUNDLE_IMPORT_ERROR_MESSAGE = "Test install/upgrade failed for solution kit: {0} ({1}). {2}";

    /**
     * Will attempt a dry-run (i.e. test bundle import) of the selected solution kits.<br/>
     * Method simply calls {@link SolutionKitManager#importBundle(String, SolutionKit, boolean)} and handles potential conflicts.
     *
     * @param solutionKitAdminHelper the helper class used to call validating solution kit for install or upgrade
     * @param solutionKitsConfig the SolutionKit config object.  Required and cannot be {@code null}.
     * @throws SolutionKitManagerResourceException if an error happens during dry-run, holding the response.
     */
    private void testBundleImports(@NotNull final SolutionKitAdminHelper solutionKitAdminHelper, @NotNull final SolutionKitsConfig solutionKitsConfig) throws SolutionKitManagerResourceException {
        final Collection<SolutionKit> selectedSolutionKits = solutionKitsConfig.getSelectedSolutionKits();
        final boolean isUpgrade = solutionKitsConfig.isUpgrade();

        for (final SolutionKit solutionKit: selectedSolutionKits) {

            // Check if the solution kit is upgradable.  If the solution kit attempts for upgrade, but its skar does not
            // contain UpgradeBundle.xml, then throw exception with warning
            if (isUpgrade && !solutionKitsConfig.isUpgradeInfoProvided(solutionKit)) {
                throw new SolutionKitManagerResourceException(status(BAD_REQUEST).entity("Solution kit '" + solutionKit.getName() +
                        "' cannot be used for upgrade due to that its SKAR file does not include UpgradeBundle.xml." + lineSeparator()).build());
            }

            // Check if a target solution kit is good for install or upgrade.
            // Install: if an existing solution kit is found as same as the target, fail install.
            // Upgrade: if the selected solution kit uses an instance modifier that other existing solution kit has been used, fail upgrade.
            try {
                solutionKitAdminHelper.validateSolutionKitForInstallOrUpgrade(
                    solutionKit,
                    isUpgrade,
                    (isUpgrade? SolutionKitUtils.findTargetInstanceModifier(solutionKit, solutionKitsConfig.getSolutionKitsToUpgrade()) : null)
                );
            }
            // This exception already has a well-info message containing name, GUID, and instance modifier.
            catch (BadRequestException e) {
                throw new SolutionKitManagerResourceException(status(BAD_REQUEST).entity(ExceptionUtils.getMessage(e) + lineSeparator()).build(), e);
            } catch (SolutionKitConflictException e) {
                throw new SolutionKitManagerResourceException(status(CONFLICT).entity(ExceptionUtils.getMessage(e) + lineSeparator()).build(), e);
            }

            // Update resolved mapping target IDs.
            try {
                solutionKitsConfig.updateResolvedMappingsIntoBundle(solutionKit);
            } catch (final ForbiddenException e) {
                throw new SolutionKitManagerResourceException(
                    status(FORBIDDEN).entity(
                        MessageFormat.format(
                            TEST_BUNDLE_IMPORT_ERROR_MESSAGE,
                            solutionKit.getName(),
                            solutionKit.getSolutionKitGuid(),
                            lineSeparator() + ExceptionUtils.getMessage(e) + lineSeparator()
                        )
                    ).build(),
                    e
                );
            }

            final String bundle = solutionKitsConfig.getBundleAsString(solutionKit);
            // make sense to process only if the solution kit has bundle
            if (StringUtils.isNotBlank(bundle)) {
                assert bundle != null; // intellij intellisense doesn't know how isNotBlank works
                final String mappingsStr;
                try {
                    mappingsStr = solutionKitManager.importBundle(bundle, solutionKit, true);
                } catch (final BadRequestException e) {
                    throw new SolutionKitManagerResourceException(
                        status(BAD_REQUEST).entity(
                            MessageFormat.format(
                                TEST_BUNDLE_IMPORT_ERROR_MESSAGE,
                                solutionKit.getName(),
                                solutionKit.getSolutionKitGuid(),
                                lineSeparator() + ExceptionUtils.getMessage(e) + lineSeparator()
                            )
                        ).build(),
                        e
                    );
                } catch (final Exception e) {
                    throw new SolutionKitManagerResourceException(
                        status(INTERNAL_SERVER_ERROR).entity(
                            MessageFormat.format(
                                TEST_BUNDLE_IMPORT_ERROR_MESSAGE,
                                solutionKit.getName(),
                                solutionKit.getSolutionKitGuid(),
                                lineSeparator() + ExceptionUtils.getMessage(e) + lineSeparator()
                            )
                        ).build(),
                        e
                    );
                }

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
                                    solutionKit.getName(),
                                    solutionKit.getSolutionKitGuid(),
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
                                    solutionKit.getName(),
                                    solutionKit.getSolutionKitGuid(),
                                    lineSeparator() + mappingsStr + lineSeparator()
                                )
                            ).build()
                        );
                    }
                }
            }
        }
    }

    /**
     * Uninstall the Solution Kit record and the entities installed from the original SKAR (if delete mappings were provided by the SKAR author).
     * <ul>
     *     <li>Use a <code>DELETE</code> request.</li>
     *     <li>Specify the Solution Kit ID as a query parameter in the URL.</li>
     * </ul>
     *
     * @param deleteGuidIM Solution Kit GUID and Instance Modifier to delete.
     * @param childGuidIMList GUID and Instance Modifier of child solution kits to delete
     * @return Output from the Solution Kit Manager.
     */
    @DELETE
    public Response uninstall(
        final @QueryParam("id") String deleteGuidIM,
        final @QueryParam("childId") List<String> childGuidIMList) {

        // Couldn't use Solution Kit ID in the URL to upgrade (i.e. @Path("{id}") and @PathParam("id")).
        //      ... com.l7tech.external.assertions.gatewaymanagement.tools.WadlTest.test(2)
        //              junit.framework.AssertionFailedError: Invalid doc for param 'id' on request on method with id: 'null' at resource path: {id} ...

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
                    final String instanceModifierDisplayName = StringUtils.isBlank(instanceModifier)? "N/A" : instanceModifier;
                    final String warningMsg = "Uninstall failed: cannot find any existing solution kit (GUID = '" + deleteGuid + "',  Instance Modifier = '" + instanceModifierDisplayName + "') for uninstall.";
                    logger.warning(warningMsg);
                    return status(NOT_FOUND).entity(warningMsg + lineSeparator()).build();
                } else {
                    solutionKitToUninstall = tempSK;
                }
            }

            final SolutionKitAdminHelper solutionKitAdminHelper = new SolutionKitAdminHelper(licenseManager, solutionKitManager, signatureVerifier);
            final Collection<SolutionKit> childrenList = solutionKitAdminHelper.findAllChildrenByParentGoid(solutionKitToUninstall.getGoid());

            // If the solution kit is a parent solution kit, then check if there are any child guids specified from query parameters.
            if (isParent) {
                int numOfUninstalled = 0;

                // No child list specified means uninstall all child solution kits.
                if (childGuidIMList == null || childGuidIMList.isEmpty()) {
                    for (SolutionKit child: childrenList) {
                        solutionKitAdminHelper.uninstall(child.getGoid());
                        numOfUninstalled++;
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
                            throw new SolutionKitManagerResourceException(status(NOT_FOUND).entity("Uninstall failed: Cannot any child solution kit matching the GUID '" + guid + "' specified in the parameter 'solutionKitSelect'" + lineSeparator()).build());
                        }

                        // Firstly check if individual instance modifier is specified.
                        // The individual instance modifier will override the global instance modifier.
                        String finalIM;
                        if (StringUtils.isNotBlank(individualInstanceModifier)) {
                            finalIM = individualInstanceModifier;
                        }
                        // Secondly check if global instance modifier is specified.
                        else if (StringUtils.isNotBlank(instanceModifier)) {
                            finalIM = instanceModifier;
                        }
                        // By default
                        else {
                            finalIM = null;
                        }
                        selectedSolutionKit = solutionKitManager.findBySolutionKitGuidAndIM(guid, finalIM);

                        if (selectedSolutionKit == null) {
                            final String instanceModifierDisplayName = StringUtils.isBlank(finalIM)? "N/A" : finalIM;
                            final String warningMsg = "Uninstall failed: cannot find any existing solution kit (GUID = '" + guid + "',  Instance Modifier = '" + instanceModifierDisplayName + "') for uninstall.";
                            logger.warning(warningMsg);
                            return status(NOT_FOUND).entity(warningMsg + lineSeparator()).build();
                        }


                        solutionKitAdminHelper.uninstall(selectedSolutionKit.getGoid());
                        numOfUninstalled++;
                    }
                }

                // Delete the parent solution kit, after all children are deleted at the above step.
                if (numOfUninstalled == childrenList.size()) {
                    solutionKitManager.delete(solutionKitToUninstall.getGoid());
                }
            }
            // Uninstall a non-parent solution kit
            else {
                solutionKitAdminHelper.uninstall(solutionKitToUninstall.getGoid());
            }
        } catch (SolutionKitManagerResourceException e) {
            logger.log(Level.WARNING, ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
            return e.getResponse();
        } catch (Exception e) {
            logger.log(Level.WARNING, e.getMessage(), e);    // log full exception for unexpected errors
            return status(INTERNAL_SERVER_ERROR).entity(e.getMessage() + lineSeparator()).build();
        }

        return Response.noContent().build();
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
     * @param globalInstanceModifier: in stall, the instance modifier is shared by all selected solution kits, but individual solution kit's instance modifier can override it.
     *
     * @return a message if there is any warning.
     * @throws SolutionKitManagerResourceException
     * @throws UnsupportedEncodingException
     */
    private String setUserSelections(
        @NotNull final SolutionKitsConfig solutionKitsConfig,
        @Nullable final List<FormDataBodyPart> solutionKitSelects,
        final boolean isInstall,
        @Nullable final String globalInstanceModifier) throws SolutionKitManagerResourceException, UnsupportedEncodingException {

        String warningMessage = null;

        // Collect information about loaded solution kits
        final Set<SolutionKit> loadedSolutionKits = new TreeSet<>(solutionKitsConfig.getLoadedSolutionKits().keySet());
        final Map<String, SolutionKit> loadedSolutionKitMap = new HashMap<>(loadedSolutionKits.size());
        for (SolutionKit loadedSolutionKit : loadedSolutionKits) {
            loadedSolutionKitMap.put(loadedSolutionKit.getSolutionKitGuid(), loadedSolutionKit);
        }

        // Collect information about solution kits for upgrade
        final List<SolutionKit> solutionKitsToUpgrade = solutionKitsConfig.getSolutionKitsToUpgrade();
        final Map<String, Set<String>> guidAndInstanceModifierMapFromUpgrade = SolutionKitUtils.getGuidAndInstanceModifierMapFromUpgrade(solutionKitsToUpgrade);

        // Check the list of form data parameter "solutionKitSelect"
        if (solutionKitSelects == null || solutionKitSelects.isEmpty()) {
            if (isInstall) {
                if (StringUtils.isNotBlank(globalInstanceModifier)) {
                    for (SolutionKit solutionKit: loadedSolutionKits) {
                        solutionKit.setProperty(SolutionKit.SK_PROP_INSTANCE_MODIFIER_KEY, globalInstanceModifier);
                    }
                }
                solutionKitsConfig.setSelectedSolutionKits(loadedSolutionKits);
            } else { // For upgrade
                final Set<SolutionKit> selectedSolutionKits = new TreeSet<>();

                // For upgrade, like UI (the method isEditableOrEnabledAt), filter out those selected solution kits not suitable for upgrade.
                // Case 1: If the selected solution kit is not in the upgrade list, then ignore the selected solution kit.
                // Case 2: If there are more than two solution kits from the upgrade list matching the selected solution kit, ignore the selected solution kit.
                SolutionKit loadedSK;
                Set<String> instanceModifierSet;
                for (String guidOfLoadedSK: loadedSolutionKitMap.keySet()) {
                    loadedSK = loadedSolutionKitMap.get(guidOfLoadedSK);
                    instanceModifierSet = guidAndInstanceModifierMapFromUpgrade.get(guidOfLoadedSK);
                    if (instanceModifierSet == null) {
                        continue;
                    } else if (instanceModifierSet.size() > 1) {
                        warningMessage = "Upgrade Warning: '" + loadedSK.getName() + "' cannot be used for upgrade, since there are two or more solution kit instances using it to upgrade" + lineSeparator();
                        continue;
                    }
                    selectedSolutionKits.add(loadedSK);
                }
                solutionKitsConfig.setSelectedSolutionKits(selectedSolutionKits);
            }
        } else {
            final Set<SolutionKit> selectedSolutionKits = new TreeSet<>();
            final boolean isUpgrade = !isInstall;
            String decodedStr, guid, individualInstanceModifier;
            SolutionKit selectedSolutionKit;

            for (FormDataBodyPart solutionKitSelect : solutionKitSelects) {
                decodedStr = URLDecoder.decode(solutionKitSelect.getValue(), CharEncoding.UTF_8);
                guid = substringBefore(decodedStr, PARAMETER_DELIMINATOR).trim();
                individualInstanceModifier = substringAfter(decodedStr, PARAMETER_DELIMINATOR).trim();

                if (!loadedSolutionKitMap.containsKey(guid)) {
                    throw new SolutionKitManagerResourceException(status(NOT_FOUND).entity(
                            "Solution Kit ID to install/upgrade: " + guid + " not found in the skar." + lineSeparator()).build());
                }

                selectedSolutionKit = loadedSolutionKitMap.get(guid);

                // For upgrade, like UI (the method isEditableOrEnabledAt), filter out those selected solution kits not suitable for upgrade.
                // Case 1: If the selected solution kit is not in the upgrade list, then ignore the selected solution kit.
                // Case 2: If there are more than two solution kits from the upgrade list matching the selected solution kit, ignore the selected solution kit.
                if (isUpgrade) {
                    final Set<String> instanceModifierSet = guidAndInstanceModifierMapFromUpgrade.get(selectedSolutionKit.getSolutionKitGuid());
                    if (instanceModifierSet == null) {
                        continue;
                    } else if (instanceModifierSet.size() > 1) {
                        warningMessage = "Upgrade Warning: '" + selectedSolutionKit.getName() + "' cannot be used for upgrade, since there are two or more solution kit instances using it to upgrade" + lineSeparator();
                        continue;
                    }
                }

                // Firstly check if individual instance modifier is specified.  In Install, this instance modifier will override the global instance modifier.
                if (StringUtils.isNotBlank(individualInstanceModifier)) {
                    selectedSolutionKit.setProperty(SolutionKit.SK_PROP_INSTANCE_MODIFIER_KEY, individualInstanceModifier);
                }
                // Secondly check if global instance modifier is specified.  This global value is only used in install, not applied for upgrade.
                else if (isInstall && StringUtils.isNotBlank(globalInstanceModifier)) {
                    selectedSolutionKit.setProperty(SolutionKit.SK_PROP_INSTANCE_MODIFIER_KEY, globalInstanceModifier);
                }

                selectedSolutionKits.add(selectedSolutionKit);
            }

            if (solutionKitSelects.isEmpty()) {
                throw new SolutionKitManagerResourceException(status(NOT_FOUND).entity(
                    "There are no any solution kits being selectable for install/upgrade." + lineSeparator()).build());
            }

            // set selection set for install
            solutionKitsConfig.setSelectedSolutionKits(selectedSolutionKits);
        }

        return warningMessage;
    }

    // remap any entity id(s)
    private void remapEntityIds(@NotNull final SolutionKitsConfig solutionKitsConfig, @Nullable final List<FormDataBodyPart> entityIdReplaces) throws UnsupportedEncodingException {
        if (entityIdReplaces != null) {
            Map<String, String> entityIdReplaceMap = new HashMap<>(entityIdReplaces.size());
            String entityIdReplaceStr;
            for (FormDataBodyPart entityIdReplace : entityIdReplaces) {
                entityIdReplaceStr = entityIdReplace.getValue();
                if (isNotEmpty(entityIdReplaceStr)) {
                    decodeSplitPut(entityIdReplaceStr, entityIdReplaceMap);
                }
            }

            Map<String, Pair<SolutionKit, Map<String, String>>> idRemapBySolutionKit = new HashMap<>();
            for (SolutionKit solutionKit: solutionKitsConfig.getSelectedSolutionKits()) {
                idRemapBySolutionKit.put(solutionKit.getSolutionKitGuid(), new Pair<>(solutionKit, entityIdReplaceMap));
            }
            solutionKitsConfig.setResolvedEntityIds(idRemapBySolutionKit);
        }
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

    /*
        Addendum Bundle: this is an undocumented "dark feature" that’s not QA’d.

        Prerequisites:
            - Requires that only one Solution Kit is in scope.  If using a collection of Solution Kits, select only one child Solution Kit for the request (e.g. specify a GUID in form-field name solutionKitSelect).
            - If Solution Kit supports upgrade, a separate upgrade addendum bundle is required (e.g. mapping action=“NewOrUpdate"instead of "AlwaysCreateNew")
            - The .skar file's bundle requires the <l7:Mappings> element (note the ending "s").
            - <l7:AllowAddendum> must be true in .skar file metadata (i.e. SolutionKit.xml)
            - The .skar file mapping(s) to change must set "SK_AllowMappingOverride" property to true

        Algorithm:
            Loop through addendum bundle mappings
                if skar bundle has mapping AND has override flag true
                    replace bundle mapping with addendum bundle mapping
                else
                    throw error (400 Bad Request or 403 Forbidden)

                if addendum bundle has reference item matching it's mapping srdId
                    if skar bundle has reference item with same id
                        replace skar bundle's reference item with addendum bundle reference item
                    else
                        add addendum bundle reference item

         TODO move code below to a class for better separation (possibly SolutionKitUtils or SolutionKitConfig)
     */
    void applyAddendumBundles(@NotNull final FormDataMultiPart formDataMultiPart,
                              @NotNull final SolutionKitsConfig solutionKitsConfig) throws IOException, SAXException, SolutionKitManagerResourceException {

        final Set<SolutionKit> selectedSolutionKits = solutionKitsConfig.getSelectedSolutionKits();
        final FormDataBodyPart addendumPart = formDataMultiPart.getField(FORM_FIELD_NAME_BUNDLE);
        if (addendumPart != null) {
            if (selectedSolutionKits.size() > 1) {
                throw new SolutionKitManagerResourceException(status(BAD_REQUEST).entity("Can't have more than one Solution Kit in scope when using form field named '" +
                        FORM_FIELD_NAME_BUNDLE + "'. If using a collection of Solution Kits, select only one child Solution Kit for the request." + lineSeparator()).build());
            }

            // future: handle collection of skars using field name "bundle" + <solution_kit_guid>
            SolutionKit solutionKit = selectedSolutionKits.iterator().next();

            // check if metadata allows Add flag to solution kit meta to allow author to control support for this dynamic bundle (e.g. allow or not allow dynamic bundle)
            final Boolean allowAddendum = Boolean.valueOf(solutionKit.getProperty(SK_PROP_ALLOW_ADDENDUM_KEY));
            if (!allowAddendum) {
                throw new SolutionKitManagerResourceException(status(FORBIDDEN).entity("The selected .skar file does not allow addendum bundle.  Form field named '" + FORM_FIELD_NAME_BUNDLE +
                        "' not allow unless the .skar file author has metadata element '" + SK_PROP_ALLOW_ADDENDUM_KEY + "' to true." + lineSeparator()).build());
            }

            final InputStream addendumInputStream = addendumPart.getValueAs(InputStream.class);
            final DOMSource addendumBundleSource = new DOMSource();
            final Document addendumBundleDoc = XmlUtil.parse(new ByteArrayInputStream(IOUtils.slurpStream(addendumInputStream)));
            final Element addendumBundleEle = addendumBundleDoc.getDocumentElement();
            addendumBundleSource.setNode(addendumBundleEle);
            final Bundle addendumBundle = MarshallingUtils.unmarshal(Bundle.class, addendumBundleSource, true);

            if (addendumBundle == null || addendumBundle.getMappings() == null) {
                throw new SolutionKitManagerResourceException(status(BAD_REQUEST).entity(
                        "The addendum bundle specified using form field named '" + FORM_FIELD_NAME_BUNDLE + "' can't be null (nor have null mappings)." + lineSeparator()).build());
            }

            // get the skar bundle
            final Bundle bundle = solutionKitsConfig.getBundle(solutionKit);
            if (bundle == null || bundle.getReferences() == null || bundle.getMappings() == null) {
                // skar bundle null shouldn't happen; just in case
                throw new SolutionKitManagerResourceException(status(BAD_REQUEST).entity("The .skar file bundle can't be null (nor have null references, nor null mappings) " +
                        "when addendum bundle has been specified (i.e. form field named '" + FORM_FIELD_NAME_BUNDLE + "' was provided)." + lineSeparator()).build());
            }

            // use skar bundle to build a map of srcId->mapping and map of id->reference
            final List<Item> referenceItems = bundle.getReferences();
            final Map<String, Item> idReferenceItemMap = new HashMap<>(referenceItems.size());
            for (Item referenceItem : referenceItems) {
                idReferenceItemMap.put(referenceItem.getId(), referenceItem);
            }
            final List<Mapping> mappings = bundle.getMappings();
            final Map<String, Mapping> srcIdMappingMap = new HashMap<>(mappings.size());
            for (Mapping mapping : mappings) {
                srcIdMappingMap.put(mapping.getSrcId(), mapping);
            }

            // use addendum bundle to build a map of id->reference
            final Map<String, Item> addendumIdReferenceMap = new HashMap<>(addendumBundle.getReferences().size());
            for (Item referenceItem : addendumBundle.getReferences()) {
                addendumIdReferenceMap.put(referenceItem.getId(), referenceItem);
            }

            // make decisions on addendum bundle mappings
            for (Mapping addendumMapping : addendumBundle.getMappings()) {

                // check if skar bundle has mapping AND has override flag true
                Mapping mapping = srcIdMappingMap.get(addendumMapping.getSrcId());
                if (mapping != null) {
                    final Boolean allowOverride = mapping.getProperty(MAPPING_PROPERTY_NAME_SK_ALLOW_MAPPING_OVERRIDE);
                    if (allowOverride == null || !allowOverride) {
                        throw new SolutionKitManagerResourceException(status(FORBIDDEN).entity("Unable to process addendum bundle for mapping with scrId=" + addendumMapping.getSrcId() +
                                ".  This requires the .skar file author to set mapping property '" + MAPPING_PROPERTY_NAME_SK_ALLOW_MAPPING_OVERRIDE + "' to true." + lineSeparator()).build());
                    }

                    // replace skar bundle mapping with addendum bundle mapping
                    mappings.set(mappings.indexOf(mapping), addendumMapping);

                    // check if addendum bundle has reference item with srdId
                    Item addendumReferenceItem = addendumIdReferenceMap.get(addendumMapping.getSrcId());
                    if (addendumReferenceItem != null) {
                        // check if skar bundle has reference item with same id
                        Item referenceItem = idReferenceItemMap.get(addendumReferenceItem.getId());
                        if (referenceItem != null) {
                            // replace skar bundle's reference item with addendum bundle reference item
                            referenceItems.set(referenceItems.indexOf(referenceItem), addendumReferenceItem);
                        } else {
                            // add addendum bundle reference item
                            referenceItems.add(addendumReferenceItem);
                        }
                    }
                }
            }
        }
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