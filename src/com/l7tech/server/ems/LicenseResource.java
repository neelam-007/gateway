package com.l7tech.server.ems;

import org.restlet.resource.*;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.Context;
import org.xml.sax.SAXException;
import org.w3c.dom.Document;
import com.l7tech.server.GatewayLicenseManager;
import com.l7tech.common.License;
import com.l7tech.common.InvalidLicenseException;
import com.l7tech.common.io.ByteLimitInputStream;
import com.l7tech.common.util.XmlUtil;
import com.l7tech.common.util.ResourceUtils;
import com.l7tech.objectmodel.UpdateException;

import java.io.IOException;

/**
 * Represents the current Gateway license.
 */
public class LicenseResource extends Resource {
    public LicenseResource(Context context, Request request, Response response) {
        super(context, request, response);
        getVariants().add(new Variant(MediaType.APPLICATION_XML));
        setModifiable(true);
    }

    @Override
    public Representation represent(Variant variant) throws ResourceException {
        try {
            final License license = getLicenseManager().getCurrentLicense();
            if (license == null)
                throw new ResourceException(404, "License", "License not found", getRequest().getResourceRef().toString());
            return new DomRepresentation(MediaType.APPLICATION_XML, XmlUtil.stringToDocument(license.asXml()));
        } catch (InvalidLicenseException e) {
            throw new ResourceException(e);
        } catch (SAXException e) {
            throw new ResourceException(e);
        }
    }

    public void acceptRepresentation(Representation entity) throws ResourceException {
        if (!MediaType.APPLICATION_XML.isCompatible(entity.getMediaType()))
            throw new ResourceException(Status.CLIENT_ERROR_UNSUPPORTED_MEDIA_TYPE);

        ByteLimitInputStream is = null;
        try {
            is = new ByteLimitInputStream(entity.getStream());
            Document doc = XmlUtil.parse(is);
            getLicenseManager().installNewLicense(XmlUtil.nodeToString(doc));
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

    private GatewayLicenseManager getLicenseManager() {
        return (GatewayLicenseManager)Ems.getAppContext(this).getBean("licenseManager", GatewayLicenseManager.class);
    }
}
