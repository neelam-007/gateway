package com.l7tech.server.ems;

import com.l7tech.common.InvalidLicenseException;
import com.l7tech.common.util.ResourceUtils;
import com.l7tech.common.util.SyspropUtil;
import com.l7tech.common.util.XmlUtil;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.server.GatewayLicenseManager;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.disk.DiskFileItem;
import org.restlet.Context;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.ext.fileupload.RestletFileUpload;
import org.restlet.resource.Representation;
import org.restlet.resource.Resource;
import org.restlet.resource.ResourceException;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * Write-only resource used to receive initial setup information from Setup.html.
 */
public class SetupResource extends Resource {
    private static final int MAX_LICENSE_FILE_UPLOAD_BYTES = SyspropUtil.getInteger("com.l7tech.server.upload.licenseFile.maxBytes", 1024 * 100);

    public SetupResource(Context context, Request request, Response response) {
        super(context, request, response);
        setModifiable(true);
    }

    public void acceptRepresentation(Representation entity) throws ResourceException {
        if (!MediaType.MULTIPART_FORM_DATA.isCompatible(entity.getMediaType()))
            throw new ResourceException(Status.CLIENT_ERROR_UNSUPPORTED_MEDIA_TYPE);


        InputStream is = null;
        try {
            // TODO replace some or all of this commons fieluplod code as it is quite bad
            FileItemFactory fic = new FileItemFactory() {
                public FileItem createItem(String fieldName, String contentType, boolean isFormField, String fileName) {
                    return new DiskFileItem(fieldName, contentType, isFormField, fileName, MAX_LICENSE_FILE_UPLOAD_BYTES, null);
                }
            };
            List<FileItem> fileItems = new RestletFileUpload(fic).parseRepresentation(entity);
            for (FileItem fileItem : fileItems) {
                if ("licenseFile".equals(fileItem.getFieldName())) {
                    is = fileItem.getInputStream();
                    break;
                }
            }
            if (is == null)
                throw new ResourceException(Status.CLIENT_ERROR_UNPROCESSABLE_ENTITY, "No licenseFile item was present in request");

            Document doc = XmlUtil.parse(is);
            getLicenseManager().installNewLicense(XmlUtil.nodeToString(doc));

            // TODO create administrator user out of user fields

            getResponse().setLocationRef("/ems/Gateways.html");
            getResponse().setStatus(Status.REDIRECTION_SEE_OTHER);
        } catch (IOException e) {
            throw new ResourceException(e);
        } catch (SAXException e) {
            throw new ResourceException(Status.CLIENT_ERROR_UNPROCESSABLE_ENTITY, e);
        } catch (InvalidLicenseException e) {
            throw new ResourceException(Status.CLIENT_ERROR_UNPROCESSABLE_ENTITY, e);
        } catch (UpdateException e) {
            throw new ResourceException(e);
        } catch (Exception e) {
            throw new ResourceException(e);
        } finally {
            ResourceUtils.closeQuietly(is);
        }
    }

    private GatewayLicenseManager getLicenseManager() {
        return (GatewayLicenseManager)Ems.getAppContext(this).getBean("licenseManager", GatewayLicenseManager.class);
    }
}
