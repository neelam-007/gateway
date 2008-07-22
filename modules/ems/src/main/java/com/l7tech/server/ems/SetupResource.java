package com.l7tech.server.ems;

import com.l7tech.util.SyspropUtil;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItem;
import org.restlet.Context;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.ext.fileupload.RestletFileUpload;
import org.restlet.resource.Representation;
import org.restlet.resource.ResourceException;

import java.util.List;

/**
 * Write-only resource used to receive initial setup information from Setup.html.
 * Accepts multipart/form-data with three fields:
 * <pre>
 *      licenseFile         type=file           xml license file
 *      username            type=text           username of new admin user
 *      password            type=password       password of new admin user
 *      password2           type=password       password of new admin user (must match)
 * </pre>
 *
 * This resource fails all requests immediately unless all of the following is true:
 * <ul>
 * <li>No valid license is currently installed.</li>
 * <li>No internal users currently exist.</li>
 * </ul> 
 */
public class SetupResource extends ErrorReportingResource {
    private static final int MAX_LICENSE_FILE_UPLOAD_BYTES = SyspropUtil.getInteger("com.l7tech.ems.licenseFile.maxBytes", 1024 * 500);

    private final SetupManager setupManager;

    public SetupResource(SetupManager setupManager) {
        this.setupManager = setupManager;
    }

    @Override
    public void init(Context context, Request request, Response response) {
        super.init(context, request, response);
        setModifiable(true);
    }

    @Override
    public void acceptRepresentation(Representation entity) throws ResourceException {
        if (!MediaType.MULTIPART_FORM_DATA.isCompatible(entity.getMediaType()))
            throw new ResourceException(Status.CLIENT_ERROR_UNSUPPORTED_MEDIA_TYPE);

        String username = null;
        String password = null;
        String password2 = null;
        String licenseXml = null;
        try {
            if (setupManager.isSetupPerformed())
                throw new ResourceException(Status.CLIENT_ERROR_LOCKED);

            // TODO replace some or all of this commons fileupload code as it is quite Bad
            FileItemFactory fic = new FileItemFactory() {
                public FileItem createItem(String fieldName, String contentType, boolean isFormField, String fileName) {
                    return new DiskFileItem(fieldName, contentType, isFormField, fileName, MAX_LICENSE_FILE_UPLOAD_BYTES, null);
                }
            };
            List<FileItem> fileItems = new RestletFileUpload(fic).parseRepresentation(entity);
            for (FileItem fileItem : fileItems) {
                if ("licenseFile".equals(fileItem.getFieldName())) {
                    licenseXml = fileItem.getString();
                } else if ("username".equals(fileItem.getFieldName())) {
                    username = fileItem.getString();
                } else if ("password".equals(fileItem.getFieldName())) {
                    password = fileItem.getString();
                } else if ("password2".equals(fileItem.getFieldName())) {
                    password2 = fileItem.getString();
                }
            }
            if (licenseXml == null)
                throw new ResourceException(Status.CLIENT_ERROR_UNPROCESSABLE_ENTITY, "No licenseFile item was present in request");
            if (username == null || password == null || password2 == null)
                throw new ResourceException(Status.CLIENT_ERROR_UNPROCESSABLE_ENTITY, "username, password, and password2 are all required");
            if (!password.equals(password2))
                throw new ResourceException(Status.CLIENT_ERROR_UNPROCESSABLE_ENTITY, "The two passwords do not match");

            setupManager.performInitialSetup(licenseXml, username, password);

            getResponse().setLocationRef("/ems/Gateways.html");
            getResponse().setStatus(Status.REDIRECTION_SEE_OTHER);
        } catch (SetupException e) {
            throw new ResourceException(Status.CLIENT_ERROR_UNPROCESSABLE_ENTITY, e);
        } catch (FileUploadException e) {
            throw new ResourceException(Status.SERVER_ERROR_INSUFFICIENT_STORAGE, e);
        }
    }
}
