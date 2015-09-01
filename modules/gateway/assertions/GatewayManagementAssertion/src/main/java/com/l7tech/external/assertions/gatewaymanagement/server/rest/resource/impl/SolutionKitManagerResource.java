package com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.impl;

/* NOTE: The java docs in this class get converted to API documentation seen by customers! */

import com.l7tech.external.assertions.gatewaymanagement.server.ServerRESTGatewayManagementAssertion;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.RestManVersion;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.Since;
import com.l7tech.gateway.common.AsyncAdminMethods;
import com.l7tech.gateway.common.LicenseManager;
import com.l7tech.gateway.common.api.solutionkit.SkarProcessor;
import com.l7tech.gateway.common.api.solutionkit.SolutionKitCustomization;
import com.l7tech.gateway.common.api.solutionkit.SolutionKitUtils;
import com.l7tech.gateway.common.api.solutionkit.SolutionKitsConfig;
import com.l7tech.gateway.common.solutionkit.BadRequestException;
import com.l7tech.gateway.common.solutionkit.ForbiddenException;
import com.l7tech.gateway.common.solutionkit.*;
import com.l7tech.gateway.rest.SpringBean;
import com.l7tech.objectmodel.*;
import com.l7tech.policy.solutionkit.SolutionKitManagerUi;
import com.l7tech.server.security.signer.SignatureVerifier;
import com.l7tech.server.solutionkit.SolutionKitAdminImpl;
import com.l7tech.server.solutionkit.SolutionKitManager;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Pair;
import org.apache.commons.lang.CharEncoding;
import org.apache.commons.lang.StringUtils;
import org.glassfish.jersey.media.multipart.BodyPart;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.inject.Singleton;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;
import java.io.InputStream;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Constructor;
import java.net.URLDecoder;
import java.util.*;
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

    protected static final String ID_DELIMINATOR = "::";  // double colon separate ids; key_id :: value_id (e.g. f1649a0664f1ebb6235ac238a6f71a6d :: 66461b24787941053fc65a626546e4bd)

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
     * @param solutionKitSelects Which Solution Kit ID(s) in the uploaded SKAR to install. If not provided, all Solution Kit(s) in the upload SKAR will be installed.
     * @param entityIdReplaces Optional. To map one entity ID to another. Format <find_id>::<replace_with_id>.
     * @param upgradeGuid Optional, note this is a query parameter, not a form key-value. Select which Solution Kit ID(s) in the uploaded SKAR to upgrade.
     * @param formDataMultiPart See above.
     * @return Output from the Solution Kit Manager.
     */
    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response installOrUpgrade(final @FormDataParam("file") InputStream fileInputStream,
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
        try {
            validateParams(fileInputStream);

            // handle upgrade
            final SolutionKitAdmin solutionKitAdmin = new SolutionKitAdminImpl(licenseManager, solutionKitManager, signatureVerifier);
            if (upgradeGuid != null) {
                final List<SolutionKit> solutionKitsExistingOnGateway = solutionKitManager.findBySolutionKitGuid(upgradeGuid);
                if (solutionKitsExistingOnGateway.size() > 0) {
                    solutionKitsConfig.setSolutionKitsToUpgrade(
                            SolutionKitUtils.getListOfSolutionKitsToUpgrade(solutionKitAdmin, solutionKitsExistingOnGateway.get(0))
                    );
                }
            }

            // load skar
            final SkarProcessor skarProcessor = new SkarProcessor(solutionKitsConfig);
            skarProcessor.load(fileInputStream, solutionKitAdmin);

            // handle any user selection(s) - child solution kits
            setUserSelections(solutionKitsConfig, solutionKitSelects);
            final Set<SolutionKit> selectedSolutionKits = solutionKitsConfig.getSelectedSolutionKits();

            // Check instance modifier uniqueness for installing child solution kits.  If any child solution kits violate
            // the uniqueness rule, stop install any solution kits and do not allow any partial installation.
            // Note: This checking is only for install (not for upgrade).
            if (upgradeGuid == null) {
                final Map<String, List<String>> usedInstanceModifiersMap = SolutionKitUtils.getInstanceModifiers(solutionKitAdmin);
                for (SolutionKit solutionKit : selectedSolutionKits) {
                    if (!SolutionKitUtils.checkInstanceModifierUniqueness(solutionKit, usedInstanceModifiersMap)) {
                        // TODO: If in future, headless installation uses instance modifier, we should modify the below warning message to say the instance modifier is not unique and try other different instance modifier.
                        return status(CONFLICT).entity("The solution kit '" + solutionKit.getName() + "' has been installed on gateway already." + lineSeparator()).build();
                    }
                }
            }

            // Check if the loaded skar is a collection of skars
            final SolutionKit parentSKFromLoad = solutionKitsConfig.getParentSolutionKit();
            Goid parentGoid = null;

            // Process parent solution kit first, if a parent solution kit is loaded.
            if (parentSKFromLoad != null) {
                // Case 1: Parent for upgrade
                if (upgradeGuid != null) {
                    final SolutionKit parentSKFromDB = solutionKitsConfig.getSolutionKitsToUpgrade().get(0); // The first element is a real parent solution kit.

                    if (!upgradeGuid.equalsIgnoreCase(parentSKFromDB.getSolutionKitGuid())) {
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

            // remap any entity id(s)
            remapEntityIds(solutionKitsConfig, entityIdReplaces);

            // pass in form fields as input parameters to customizations
            setCustomizationKeyValues(solutionKitsConfig.getCustomizations(), formDataMultiPart);

            // install or upgrade skars
            // After processing the parent, process selected solution kits if applicable.
            for (SolutionKit solutionKit : selectedSolutionKits) {
                // If the solution kit is under a parent solution kit, then set its parent goid before it gets saved.
                if (parentSKFromLoad != null) {
                    solutionKit.setParentGoid(parentGoid);
                }

                skarProcessor.invokeCustomCallback(solutionKit);

                AsyncAdminMethods.JobId<Goid> jobId = skarProcessor.installOrUpgrade(solutionKitAdmin, solutionKit);
                processJobResult(solutionKitAdmin, jobId);
            }
        } catch (SolutionKitManagerResourceException e) {
            logger.log(Level.WARNING, ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
            return e.getResponse();
        } catch (UntrustedSolutionKitException e) {
            logger.log(Level.WARNING, ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
            return status(BAD_REQUEST).entity(e.getMessage() + lineSeparator()).build();
        } catch (ForbiddenException e) {
            logger.log(Level.WARNING, ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
            return Response.status(FORBIDDEN).entity(e.getMessage()).build();
        } catch (BadRequestException e) {
            logger.log(Level.WARNING, ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
            return Response.status(BAD_REQUEST).entity(e.getMessage()).build();
        } catch (SolutionKitException | UnsupportedEncodingException | InterruptedException |
                AsyncAdminMethods.UnknownJobException | AsyncAdminMethods.JobStillActiveException |
                SaveException | FindException | UpdateException e) {
            logger.log(Level.WARNING, ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
            return status(INTERNAL_SERVER_ERROR).entity(e.getMessage() + lineSeparator()).build();
        } finally {
            solutionKitsConfig.clear();
        }

        return Response.ok().entity("Request completed successfully." + lineSeparator()).build();
    }

    /**
     * Uninstall the Solution Kit record and the entities installed from the original SKAR (if delete mappings were provided by the SKAR author).
     * <ul>
     *     <li>Use a <code>DELETE</code> request.</li>
     *     <li>Specify the Solution Kit ID as a query parameter in the URL.</li>
     * </ul>
     *
     * @param deleteGuid Solution Kit GUID to delete.
     * @param childGuidsInQueryParam GUIDs of child solution kits to delete
     * @return Output from the Solution Kit Manager.
     */
    @DELETE
    public Response uninstall(
        final @QueryParam("id") String deleteGuid,
        final @QueryParam("childId") List<String> childGuidsInQueryParam) {

        // Couldn't use Solution Kit ID in the URL to upgrade (i.e. @Path("{id}") and @PathParam("id")).
        //      ... com.l7tech.external.assertions.gatewaymanagement.tools.WadlTest.test(2)
        //              junit.framework.AssertionFailedError: Invalid doc for param 'id' on request on method with id: 'null' at resource path: {id} ...

        try {
            if (StringUtils.isEmpty(deleteGuid)) {
                // HTTP DELETE, using 404 not found to be consistent with other restman resources
                throw new SolutionKitManagerResourceException(status(NOT_FOUND).entity("Solution Kit ID to uninstall is empty." + lineSeparator()).build());
            }

            final List<SolutionKit> solutionKitsExistingOnGateway = solutionKitManager.findBySolutionKitGuid(deleteGuid);
            if (solutionKitsExistingOnGateway.isEmpty()) {
                throw new SolutionKitManagerResourceException(status(NOT_FOUND).entity("No Solution Kit ID " + deleteGuid + " found for uninstall." + lineSeparator()).build());
            }

            final SolutionKitAdmin solutionKitAdmin = new SolutionKitAdminImpl(licenseManager, solutionKitManager, signatureVerifier);

            // Find the first solution kit matching the given guid, deleteGuid.
            // todo: if there are multiple solution kits with the same guid, in future probably need to introduce a new query parameter, instance modifier to unify a solution kit.
            final SolutionKit solutionKitToUninstall = solutionKitsExistingOnGateway.get(0);
            final Collection<SolutionKitHeader> childrenHeaders = solutionKitManager.findAllChildrenByParentGoid(solutionKitToUninstall.getGoid());

            // If the solution kit is a parent solution kit, then check if there are any child guids specified from query parameters.
            int numOfUninstalled = 0;
            if (! childrenHeaders.isEmpty()) {
                // There are no child guids specified in the query param, which means uninstall all child solution kits.
                if (childGuidsInQueryParam.isEmpty()) {
                    AsyncAdminMethods.JobId<String> jobId;
                    for (SolutionKitHeader childHeader: childrenHeaders) {
                        jobId = solutionKitAdmin.uninstall(childHeader.getGoid());
                        processJobResult(solutionKitAdmin, jobId);
                    }
                }
                // Otherwise, uninstall specified child solution kits.
                else {
                    Map<String, SolutionKitHeader> childSKMap = new HashMap<>(childrenHeaders.size());
                    for (SolutionKitHeader childHeader: childrenHeaders) {
                        childSKMap.put(childHeader.getSolutionKitGuid(), childHeader);
                    }

                    // Check if given child guild is valid or not.  If found any, stop uninstall. We do not allow partial uninstallation until fix the wrong guid.
                    // The reason of not allowing partial uninstallation is if we silently swallow invalid/misused guid, the user would think uninstallation
                    // was successful, but actually some expected solution kits are still existing.
                    Set<String> allChildGuids = childSKMap.keySet();
                    for (String givenChildGuid: childGuidsInQueryParam) {
                        if (! allChildGuids.contains(givenChildGuid)) {
                            throw new SolutionKitManagerResourceException(status(NOT_FOUND).entity("Child Solution Kit GUID " + givenChildGuid + " is not valid." + lineSeparator()).build());
                        }
                    }

                    // Uninstall each child solution kit
                    SolutionKitHeader childHeader;
                    AsyncAdminMethods.JobId<String> jobId;
                    for (String childGuidInParam: childGuidsInQueryParam) {
                        childHeader = childSKMap.get(childGuidInParam);
                        jobId = solutionKitAdmin.uninstall(childHeader.getGoid());
                        processJobResult(solutionKitAdmin, jobId);
                        numOfUninstalled++;
                    }
                }
            } else if (! childGuidsInQueryParam.isEmpty()) {
                // Just give an info and do not throw any error
                // Ignore redundant given child guids.
                logger.info("Redundant child solution kit guid(s) given, since there are not child solution kits associated with '" + solutionKitToUninstall.getName() + "'");
            }

            if (SolutionKit.PARENT_SOLUTION_KIT_DUMMY_MAPPINGS.equals(solutionKitToUninstall.getMappings())) {
                if (childGuidsInQueryParam.isEmpty() || numOfUninstalled == childrenHeaders.size()) {
                    // Delete the parent solution kit, after all children are deleted at the above step.
                    solutionKitManager.delete(solutionKitToUninstall.getGoid());
                }
            } else {
                // Uninstall a non-parent solution kit
                final AsyncAdminMethods.JobId<String> jobId = solutionKitAdmin.uninstall(solutionKitToUninstall.getGoid());
                processJobResult(solutionKitAdmin, jobId);
            }
        } catch (SolutionKitManagerResourceException e) {
            logger.log(Level.WARNING, ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
            return e.getResponse();
        } catch (SolutionKitException | InterruptedException | FindException | DeleteException|
                AsyncAdminMethods.UnknownJobException | AsyncAdminMethods.JobStillActiveException e) {
            logger.log(Level.WARNING, ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
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

    // set user selection(s)
    private void setUserSelections(@NotNull final SolutionKitsConfig solutionKitsConfig, @Nullable final List<FormDataBodyPart> solutionKitIdInstalls) throws SolutionKitManagerResourceException {
        final Set<SolutionKit> loadedSolutionKits = new TreeSet<>(solutionKitsConfig.getLoadedSolutionKits().keySet());
        if (solutionKitIdInstalls == null || solutionKitIdInstalls.isEmpty()) {
            // default install all
            solutionKitsConfig.setSelectedSolutionKits(loadedSolutionKits);
        } else {
            // build id -> loaded solution kit map
            final Map<String, SolutionKit> loadedSolutionKitMap = new HashMap<>(loadedSolutionKits.size());
            for (SolutionKit loadedSolutionKit : loadedSolutionKits) {
                loadedSolutionKitMap.put(loadedSolutionKit.getSolutionKitGuid(), loadedSolutionKit);
            }

            // verify the user selected id(s) and build selection set
            final Set<SolutionKit> selectedSolutionKitIdSet = new TreeSet<>();
            String selectedId;
            for (FormDataBodyPart solutionKitIdInstall : solutionKitIdInstalls) {
                selectedId = solutionKitIdInstall.getValue();
                if (!loadedSolutionKitMap.containsKey(selectedId)) {
                    throw new SolutionKitManagerResourceException(status(NOT_FOUND).entity(
                            "Solution Kit ID to install: " + selectedId + " not found in the skar." + lineSeparator()).build());
                }
                selectedSolutionKitIdSet.add(loadedSolutionKitMap.get(selectedId));
            }

            // set selection set for install
            solutionKitsConfig.setSelectedSolutionKits(selectedSolutionKitIdSet);
        }
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
        entityIdReplaceMap.put(substringBefore(entityIdReplaceDecoded, ID_DELIMINATOR).trim(), substringAfter(entityIdReplaceDecoded, ID_DELIMINATOR).trim());
    }

    private void processJobResult(final SolutionKitAdmin solutionKitAdmin,
                                  final AsyncAdminMethods.JobId<? extends Serializable> jobId) throws
            InterruptedException, SolutionKitException, AsyncAdminMethods.UnknownJobException, AsyncAdminMethods.JobStillActiveException {

        while( true ) {
            final String status = solutionKitAdmin.getJobStatus( jobId );
            if ( status == null ) {
                throw new SolutionKitException("Unknown jobid: " + jobId);
            } else if ( !status.startsWith( "a" ) ) {
                final AsyncAdminMethods.JobResult<? extends Serializable> jobResult = solutionKitAdmin.getJobResult( jobId );
                if ( jobResult.result != null ) {
                    break; // job done
                } else {
                    SolutionKitException solutionKitException;
                    try {
                        final Class throwableClass = this.getClass().getClassLoader().loadClass(jobResult.throwableClassname);
                        if (SolutionKitException.class.isAssignableFrom(throwableClass)) {
                            Constructor constructor = throwableClass.getDeclaredConstructor(String.class);
                            solutionKitException = (SolutionKitException) constructor.newInstance(jobResult.throwableMessage);
                        } else {
                            solutionKitException = new SolutionKitException(jobResult.throwableMessage);
                        }
                    } catch (Exception e) {
                        throw new SolutionKitException(jobResult.throwableMessage);
                    }
                    throw solutionKitException;
                }
            } else {
                Thread.sleep( 5000L );
            }
        }
    }

    private void setCustomizationKeyValues(@NotNull final Map<String, Pair<SolutionKit, SolutionKitCustomization>> customizations, @NotNull FormDataMultiPart formDataMultiPart) throws SolutionKitException, UnsupportedEncodingException {
        // pass on all unknown form fields as customization key-value input
        final List<BodyPart> bodyParts = formDataMultiPart.getBodyParts();
        final List<FormDataBodyPart> keyValues = new ArrayList<>(bodyParts.size());
        for (BodyPart bodyPart : bodyParts) {
            Map<String, String> parameters = bodyPart.getContentDisposition().getParameters();
            final String name = parameters.get("name");

            // NOTE: skip known form fields from method declaration above (strings not final static constants because Jersey annotations complains with error)
            if (!"file".equals(name) && !"solutionKitSelect".equals(name) && !"entityIdReplace".equals(name)) {
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
                        customizationValue = URLDecoder.decode(customizationKeyValue.getValue(), CharEncoding.UTF_8);
                        if (isNotEmpty(customizationKey) && isNotEmpty(customizationValue)) {
                            customUi.getContext().getKeyValues().put(customizationKey, customizationValue);
                        }
                    }
                }
            }
        }
    }

    private class SolutionKitManagerResourceException extends Exception {
        private Response response;

        public SolutionKitManagerResourceException(Response response) {
            super();
            this.response = response;
        }

        public Response getResponse() {
            return response;
        }
    }
}