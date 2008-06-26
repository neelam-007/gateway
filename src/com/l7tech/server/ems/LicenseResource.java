package com.l7tech.server.ems;

import com.l7tech.common.InvalidLicenseException;
import com.l7tech.common.License;
import com.l7tech.common.io.ByteLimitInputStream;
import com.l7tech.common.util.ResourceUtils;
import com.l7tech.common.util.XmlUtil;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.server.GatewayLicenseManager;
import org.restlet.data.MediaType;
import org.restlet.data.Status;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.resource.*;
import org.restlet.Context;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import java.io.IOException;

/**
 * Provides REST access to view or change the current Gateway license XML.
 */
public class LicenseResource extends ErrorReportingResource {
    private final GatewayLicenseManager licenseManager;

    public LicenseResource(GatewayLicenseManager licenseManager) {
        this.licenseManager = licenseManager;
    }

    @Override
    public void init(Context context, Request request, Response response) {
        super.init(context, request, response);
        getVariants().add(new Variant(MediaType.APPLICATION_XML));
        setModifiable(true);
    }

    @Override
    public Representation represent(Variant variant) throws ResourceException {
        try {
            final License license = licenseManager.getCurrentLicense();
            if (license == null)
                throw new ResourceException(404, "License", "License not found", getRequest().getResourceRef().toString());
            return new DomRepresentation(MediaType.APPLICATION_XML, XmlUtil.stringToDocument(license.asXml()));
        } catch (InvalidLicenseException e) {
            throw new ResourceException(e);
        } catch (SAXException e) {
            throw new ResourceException(e);
        }
    }

    @Override
    public void acceptRepresentation(Representation entity) throws ResourceException {
        if (!MediaType.APPLICATION_XML.isCompatible(entity.getMediaType()))
            throw new ResourceException(Status.CLIENT_ERROR_UNSUPPORTED_MEDIA_TYPE);

        ByteLimitInputStream is = null;
        try {
            is = new ByteLimitInputStream(entity.getStream());
            Document doc = XmlUtil.parse(is);
            licenseManager.installNewLicense(XmlUtil.nodeToString(doc));
            getResponse().setStatus(Status.SUCCESS_NO_CONTENT);
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
}
