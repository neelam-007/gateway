package com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.PrivateKeyResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.WsmanBaseResourceFactory;
import com.l7tech.gateway.api.PrivateKeyCreationContext;
import com.l7tech.gateway.api.PrivateKeyMO;
import com.l7tech.gateway.api.impl.*;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.util.CollectionUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.List;

/**
 *
 */
@Component
public class PrivateKeyAPIResourceFactory extends WsmanBaseResourceFactory<PrivateKeyMO, PrivateKeyResourceFactory> {

    public PrivateKeyAPIResourceFactory() {}

    @Override
    @Inject
    public void setFactory(PrivateKeyResourceFactory factory) {
        super.factory = factory;
    }

    public PrivateKeyMO createPrivateKey(PrivateKeyCreationContext resource, String id) throws ResourceFactory.InvalidResourceSelectors, ResourceFactory.InvalidResourceException {
        return factory.createPrivateKey(CollectionUtils.<String, String>mapBuilder().put("id", id).map(),resource);
    }

    public PrivateKeyExportResult exportResource(String id, String password, String exportAlias) throws ResourceFactory.ResourceNotFoundException {
        PrivateKeyExportContext context = new PrivateKeyExportContext();
        context.setPassword(password);
        context.setAlias(exportAlias);
        return factory.exportPrivateKey(CollectionUtils.<String, String>mapBuilder().put("id", id).map(), context);
    }

    public PrivateKeyMO importResource(String importId, String alias, byte[] pkcs12Data, String password ) throws ResourceFactory.ResourceNotFoundException, ResourceFactory.InvalidResourceException {
        PrivateKeyImportContext context = new PrivateKeyImportContext();
        context.setAlias(alias);
        context.setPkcs12Data(pkcs12Data);
        context.setPassword(password);
        return factory.importPrivateKey(CollectionUtils.<String, String>mapBuilder().put("id", importId).map(), context);
    }

    public PrivateKeyMO setSpecialPurpose(String id, List<String> purpose) throws ResourceFactory.ResourceNotFoundException, ResourceFactory.InvalidResourceException {
        PrivateKeySpecialPurposeContext ctx = new PrivateKeySpecialPurposeContext();
        ctx.setSpecialPurposes(purpose);
        ctx.setId(id);
        return factory.setSpecialPurposes(CollectionUtils.<String, String>mapBuilder().put("id", id).map(),ctx);
    }

    public PrivateKeyGenerateCsrResult generateCSR(@NotNull String id, @Nullable String dn, @Nullable String signatureHash) throws ResourceFactory.ResourceNotFoundException, ResourceFactory.InvalidResourceException {
        PrivateKeyGenerateCsrContext ctx = new PrivateKeyGenerateCsrContext();
        ctx.setDn(dn);
        if (signatureHash != null) {
            ctx.setProperties(CollectionUtils.MapBuilder.<String, Object>builder().put(PrivateKeyGenerateCsrContext.PROP_SIGNATURE_HASH, signatureHash).map());
        }
        return factory.generateCSR(CollectionUtils.<String, String>mapBuilder().put("id", id).map(), ctx);
    }

    @NotNull
    @Override
    public String getResourceType() {
        return EntityType.SSG_KEY_ENTRY.toString();
    }
}
