package com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.CertificateResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.RestResourceFactoryUtils;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.WsmanBaseResourceFactory;
import com.l7tech.gateway.api.CertificateData;
import com.l7tech.gateway.api.ManagedObjectFactory;
import com.l7tech.gateway.api.TrustedCertificateMO;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.util.CollectionUtils;
import com.l7tech.util.Functions;
import com.l7tech.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

/**
 *
 */
@Component
public class CertificateAPIResourceFactory extends WsmanBaseResourceFactory<TrustedCertificateMO, CertificateResourceFactory> {

    public CertificateAPIResourceFactory() {
        super(
                CollectionUtils.MapBuilder.<String, String>builder()
                        .put("id", "id")
                        .put("name", "name").map(),
                CollectionUtils.MapBuilder.<String, Pair<String, Functions.UnaryThrows<?, String, IllegalArgumentException>>>builder()
                        .put("name", new Pair<String, Functions.UnaryThrows<?, String, IllegalArgumentException>>("name", RestResourceFactoryUtils.stringConvert))
                        .put("securityZone.id", new Pair<String, Functions.UnaryThrows<?, String, IllegalArgumentException>>("securityZone.id", RestResourceFactoryUtils.goidConvert))
                        .map());
    }

    @NotNull
    @Override
    public String getResourceType(){
        return EntityType.TRUSTED_CERT.toString();
    }

    @Override
    @Inject
    public void setFactory(com.l7tech.external.assertions.gatewaymanagement.server.CertificateResourceFactory factory) {
        super.factory = factory;
    }

    @Override
    public TrustedCertificateMO getResourceTemplate() {
        TrustedCertificateMO trustedCertMO = ManagedObjectFactory.createTrustedCertificate();
        trustedCertMO.setName("TemplateTrustedCert");
        CertificateData certData = ManagedObjectFactory.createCertificateData();
        certData.setSubjectName("dn=me");
        trustedCertMO.setCertificateData(certData);
        trustedCertMO.setProperties(CollectionUtils.MapBuilder.<String, Object>builder().put("CertificateProperty", "PropertyValue").map());
        return trustedCertMO;
    }
}
