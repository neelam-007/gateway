package com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.IdentityProviderResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.RestResourceFactoryUtils;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.WsmanBaseResourceFactory;
import com.l7tech.gateway.api.IdentityProviderMO;
import com.l7tech.gateway.api.ManagedObjectFactory;
import com.l7tech.gateway.api.Mapping;
import com.l7tech.identity.IdentityProviderType;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.util.CollectionUtils;
import com.l7tech.util.Functions;
import com.l7tech.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

/**
 *
 */
@Component
public class IdentityProviderAPIResourceFactory extends WsmanBaseResourceFactory<IdentityProviderMO, IdentityProviderResourceFactory> {

    public IdentityProviderAPIResourceFactory() {
        super(
                CollectionUtils.MapBuilder.<String, String>builder()
                        .put("id", "id")
                        .put("name", "name")
                        .put("type", "typeVal")
                        .map(),
                CollectionUtils.MapBuilder.<String, Pair<String, Functions.UnaryThrows<?, String, IllegalArgumentException>>>builder()
                        .put("name", new Pair<String, Functions.UnaryThrows<?, String, IllegalArgumentException>>("name", RestResourceFactoryUtils.stringConvert))
                        .put("type", new Pair<String, Functions.UnaryThrows<?, String, IllegalArgumentException>>("typeVal", new Functions.UnaryThrows<Integer, String, IllegalArgumentException>() {
                            @Override
                            public Integer call(String s) throws IllegalArgumentException {
                                if(s.equalsIgnoreCase("LDAP")){
                                    return IdentityProviderType.LDAP.toVal();
                                }
                                else if(s.equalsIgnoreCase( "Internal")){
                                    return IdentityProviderType.INTERNAL.toVal();
                                }
                                else if(s.equalsIgnoreCase( "Federated")){
                                    return IdentityProviderType.FEDERATED.toVal();
                                }
                                else if(s.equalsIgnoreCase( "Simple LDAP")){
                                    return IdentityProviderType.BIND_ONLY_LDAP.toVal();
                                }
                                    // TODO POLICY_BACKED
                                throw new IllegalArgumentException("Invalid parameter for identity provider type:" + s );
                            }
                        }))
                        .put("securityZone.id", new Pair<String, Functions.UnaryThrows<?, String, IllegalArgumentException>>("securityZone.id", RestResourceFactoryUtils.goidConvert))
                        .map());
    }

    @NotNull
    @Override
    public EntityType getEntityType(){
        return EntityType.ID_PROVIDER_CONFIG;
    }

    @Override
    @Inject
    public void setFactory(IdentityProviderResourceFactory factory) {
        super.factory = factory;
    }

    @Override
    public IdentityProviderMO getResourceTemplate() {
        IdentityProviderMO identityProviderMO = ManagedObjectFactory.createIdentityProvider();
        identityProviderMO.setName("TemplateIdentityProvider");
        identityProviderMO.setIdentityProviderType(IdentityProviderMO.IdentityProviderType.LDAP);
        IdentityProviderMO.LdapIdentityProviderDetail providerDetail = identityProviderMO.getLdapIdentityProviderDetail();
        providerDetail.setBindDn("dn=template");
        providerDetail.setBindPassword("templatePassword");
        providerDetail.setServerUrls(CollectionUtils.list("ldap://template:389"));
        identityProviderMO.setProperties(CollectionUtils.MapBuilder.<String, Object>builder().put("IdentityProviderProperty", "PropertyValue").map());
        return identityProviderMO;
    }

    @Override
    public Mapping buildMapping(@NotNull IdentityProviderMO resource, @Nullable Mapping.Action defaultAction, @Nullable String defaultMapBy) {
        //The default mapping action for identity providers is to always map.
        Mapping mapping = super.buildMapping(resource, Mapping.Action.NewOrExisting, "id");
        mapping.setProperties(CollectionUtils.MapBuilder.<String, Object>builder().put("FailOnNew", true).map());
        return mapping;
    }
}
