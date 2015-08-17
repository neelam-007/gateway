package com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.impl;

/* NOTE: The java docs in this class get converted to API documentation seen by customers! */

import com.l7tech.external.assertions.gatewaymanagement.server.ServerRESTGatewayManagementAssertion;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.RestManVersion;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.Since;
import com.l7tech.gateway.common.AsyncAdminMethods;
import com.l7tech.gateway.common.LicenseManager;
import com.l7tech.gateway.common.api.solutionkit.SkarProcessor;
import com.l7tech.gateway.common.api.solutionkit.SolutionKitsConfig;
import com.l7tech.gateway.common.solutionkit.SolutionKit;
import com.l7tech.gateway.common.solutionkit.SolutionKitAdmin;
import com.l7tech.gateway.common.solutionkit.SolutionKitException;
import com.l7tech.gateway.rest.SpringBean;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.server.solutionkit.SolutionKitAdminImpl;
import com.l7tech.server.solutionkit.SolutionKitManager;
import com.l7tech.util.ExceptionUtils;
import org.apache.commons.lang.CharEncoding;
import org.apache.commons.lang.StringUtils;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
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
import java.net.URLDecoder;
import java.util.*;
import java.util.logging.Logger;

import static java.lang.System.lineSeparator;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.status;
import static org.apache.commons.lang.StringUtils.*;

/**
 * This resource exposes the Solution Kit Manager for no-GUI use cases (e.g. support auto provisioning scripting to upload and manage a .skar file).
 */
@Provider
@Path(ServerRESTGatewayManagementAssertion.Version1_0_URI + "solutionKitManagers")
@Singleton
@Since(RestManVersion.VERSION_1_0_2)
public class SolutionKitManagerResource {
    private static final Logger logger = Logger.getLogger(SolutionKitManagerResource.class.getName());

    protected static final String ID_DELIMINATOR = "::";  // double colon separate ids; key_id :: value_id (e.g. f1649a0664f1ebb6235ac238a6f71a6d :: 66461b24787941053fc65a626546e4bd)

    @SpringBean
    private SolutionKitManager solutionKitManager;
    @SpringBean
    private LicenseManager licenseManager;

    public SolutionKitManagerResource() {}

    /**
     * Install or upgrade a SKAR file.
     * @param fileInputStream Input stream of the upload SKAR file.
     * @param solutionKitSelects Which Solution Kit ID(s) in the uploaded SKAR to install. If not provided, all Solution Kit(s) in the upload SKAR will be installed.
     * @param entityIdReplaces Optional. To map one entity ID to another. Format <find_id>:<replace_with_id>.
     * @param upgradeGuid Optional. Select which Solution Kit ID(s) in the uploaded SKAR to upgrade.
     * @return TODO
     */
    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response installOrUpgrade(final @FormDataParam("file") InputStream fileInputStream,
                                     final @FormDataParam("solutionKitSelect") List<FormDataBodyPart> solutionKitSelects,
                                     final @FormDataParam("entityIdReplace") List<FormDataBodyPart> entityIdReplaces,
                                     final @QueryParam("id") String upgradeGuid) {

        // Using POST to upgrade since HTML PUT does not support forms (and therefore does not support multipart file upload).

        // Couldn't use Solution Kit ID in the URL to upgrade (i.e. @Path("{id}") and @PathParam("id")).
        //      ... com.l7tech.external.assertions.gatewaymanagement.tools.WadlTest.test(2)
        //              junit.framework.AssertionFailedError: Invalid doc for param 'id' on request on method with id: 'null' at resource path: {id} ...

        try {
            // handle upgrade
            final SolutionKitsConfig solutionKitsConfig = new SolutionKitsConfig();
            handleUpgrade(solutionKitsConfig, upgradeGuid);

            // load skar
            final SkarProcessor skarProcessor = new SkarProcessor(solutionKitsConfig);
            skarProcessor.load(fileInputStream);

            // handle any user selection(s)
            setUserSelections(solutionKitsConfig, solutionKitSelects);

            // remap any entity id(s)
            // TODO express with restman mapping syntax instead?
            //todo:ms Multiple skars modification: entityIdReplaces should be a map keyed by SolutionKit like this Map<SolutionKit, List<FormDataBodyPart>
            remapEntityIds(solutionKitsConfig, entityIdReplaces);

            // install or upgrade skars
            final SolutionKitAdmin solutionKitAdmin = new SolutionKitAdminImpl(licenseManager, solutionKitManager);
            for (SolutionKit solutionKit: solutionKitsConfig.getSelectedSolutionKits()) {

                // TODO pass in customized data to the callback method; for all Java primitive wrappers (e.g. Boolean, Integer, etc)? > 1 goes into a List.
                skarProcessor.invokeCustomCallback(solutionKit);

                AsyncAdminMethods.JobId<Goid> jobId = skarProcessor.installOrUpgrade(solutionKitAdmin, solutionKit);
                processJobResult(solutionKitAdmin, jobId);
            }
        } catch (SolutionKitManagerResourceException e) {
            return e.getResponse();
        } catch (SolutionKitException | UnsupportedEncodingException | InterruptedException |
                AsyncAdminMethods.UnknownJobException | AsyncAdminMethods.JobStillActiveException e) {
            return status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getMessage() + lineSeparator()).build();
        }

        // TODO delete customization jar

        return Response.ok().entity("Request completed successfully." + lineSeparator()).build();
    }

    /**
     * Uninstall the Solution Kit record and the entities installed from the original SKAR (if delete mappings were provided by the SKAR author).
     * @param deleteGuid Solution Kit ID to delete.
     * @return TODO
     */
    @DELETE
    public Response uninstall(final @QueryParam("id") String deleteGuid) {

        // Couldn't use Solution Kit ID in the URL to upgrade (i.e. @Path("{id}") and @PathParam("id")).
        //      ... com.l7tech.external.assertions.gatewaymanagement.tools.WadlTest.test(2)
        //              junit.framework.AssertionFailedError: Invalid doc for param 'id' on request on method with id: 'null' at resource path: {id} ...

        try {
            if (StringUtils.isEmpty(deleteGuid)) {
                // HTTP DELETE, "no content" has no response body so we write the details into the log
                logger.info("Solution Kit ID to uninstall is empty.");
                throw new SolutionKitManagerResourceException(Response.noContent().build());
            }

            // todo: After findBySolutionKitGuid is changed to return a list of installed solution kits, the next line is temporarily changed to use the first returned element.  Please change the next line as needed.
            final SolutionKit solutionKitToUninstall = solutionKitManager.findBySolutionKitGuid(deleteGuid).get(0);
            if (solutionKitToUninstall == null) {
                logger.info("No Solution Kit ID " + deleteGuid + " found for uninstall.");
                throw new SolutionKitManagerResourceException(Response.noContent().build());
            }

            final SolutionKitAdmin solutionKitAdmin = new SolutionKitAdminImpl(licenseManager, solutionKitManager);
            final AsyncAdminMethods.JobId<String> jobId = solutionKitAdmin.uninstall(solutionKitToUninstall.getGoid());
            processJobResult(solutionKitAdmin, jobId);
        } catch (SolutionKitManagerResourceException e) {
            return e.getResponse();
        } catch (SolutionKitException | InterruptedException | FindException |
                AsyncAdminMethods.UnknownJobException | AsyncAdminMethods.JobStillActiveException e) {
            logger.warning(ExceptionUtils.getMessage(e));
            return Response.noContent().build();
        }
        return Response.ok().entity("Request completed successfully." + lineSeparator()).build();
    }

    // set user selection(s)
    private void setUserSelections(@NotNull final SolutionKitsConfig solutionKitsConfig, @Nullable final List<FormDataBodyPart> solutionKitIdInstalls) throws SolutionKitManagerResourceException {
        final Set<SolutionKit> loadedSolutionKits = new TreeSet<>(solutionKitsConfig.getLoadedSolutionKits().keySet());
        if (solutionKitIdInstalls == null || solutionKitIdInstalls.size() == 0) {
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
        //todo:ms Multiple skars modification: entityIdReplaces should be a map keyed by SolutionKit like this Map<SolutionKit, List<FormDataBodyPart>
        if (entityIdReplaces != null) {
            //todo:ms Multiple skars modification: entityIdReplaceMap should be a map keyed by SolutionKit like this Map<SolutionKit, Map<String, String>>
            Map<String, String> entityIdReplaceMap = new HashMap<>(entityIdReplaces.size());
            String entityIdReplaceStr;
            for (FormDataBodyPart entityIdReplace : entityIdReplaces) {
                entityIdReplaceStr = entityIdReplace.getValue();
                if (isNotEmpty(entityIdReplaceStr)) {
                    decodeSplitPut(entityIdReplaceStr, entityIdReplaceMap);
                }
            }
            //todo:ms Multiple skars modification: (A problem here is the same entityIdReplaceMap used for all solution kits.)
            Map<SolutionKit, Map<String, String>> idRemapBySolutionKit = new HashMap<>();
            for (SolutionKit solutionKit: solutionKitsConfig.getSelectedSolutionKits()) {
                idRemapBySolutionKit.put(solutionKit, entityIdReplaceMap);
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
            InterruptedException, SolutionKitException, AsyncAdminMethods.UnknownJobException,
            AsyncAdminMethods.JobStillActiveException, SolutionKitManagerResourceException {

        while( true ) {
            final String status = solutionKitAdmin.getJobStatus( jobId );
            if ( status == null ) {
                throw new SolutionKitException("Unknown jobid: " + jobId);
            } else if ( !status.startsWith( "a" ) ) {
                final AsyncAdminMethods.JobResult<? extends Serializable> jobResult = solutionKitAdmin.getJobResult( jobId );
                if ( jobResult.result != null ) {
                    break; // job done
                } else {
                    throw new SolutionKitException(jobResult.throwableMessage);
                }
            } else {
                Thread.sleep( 5000L );
            }
        }
    }

    private void handleUpgrade(final SolutionKitsConfig solutionKitsConfig, final String upgradeGuid) throws SolutionKitManagerResourceException {
        if (StringUtils.isNotEmpty(upgradeGuid)) {
            try {
                //todo:ms need to re-work here for silently installation for a collection of skars
                final List<SolutionKit> solutionKits = solutionKitManager.findBySolutionKitGuid(upgradeGuid);
                if (solutionKits == null || solutionKits.isEmpty()) {
                    throw new SolutionKitManagerResourceException(status(NOT_FOUND).entity("No Solution Kit ID " + upgradeGuid + " found for upgrade." + lineSeparator()).build());
                }

                // todo: After findBySolutionKitGuid is changed to return a list of installed solution kits,  The next line is temporarily changed to use the first returned element.  Please change the next line as needed.
                List<SolutionKit> upgradeList = new ArrayList<>(1);
                upgradeList.add(solutionKits.get(0));
                solutionKitsConfig.setSolutionKitsToUpgrade(upgradeList);
            } catch (FindException e) {
                throw new SolutionKitManagerResourceException(status(NOT_FOUND).entity("Error finding Solution Kit ID " + upgradeGuid + " for upgrade." + lineSeparator()).build());
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
