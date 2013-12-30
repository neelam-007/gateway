package com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.HttpConfigurationResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.RestResourceFactoryUtils;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.WsmanBaseResourceFactory;
import com.l7tech.gateway.api.HttpConfigurationMO;
import com.l7tech.gateway.api.ManagedObjectFactory;
import com.l7tech.gateway.common.resources.HttpConfiguration;
import com.l7tech.util.CollectionUtils;
import com.l7tech.util.Functions;
import com.l7tech.util.Pair;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

/**
 *
 */
@Component
public class HttpConfigurationRestResourceFactory extends WsmanBaseResourceFactory<HttpConfigurationMO, HttpConfigurationResourceFactory> {

    public HttpConfigurationRestResourceFactory() {
        super(
                CollectionUtils.MapBuilder.<String, String>builder()
                        .put("id", "id")
                        .put("host", "host")
                        .put("entityClassName", "entityClassName")
                        .map(),
                CollectionUtils.MapBuilder.<String, Pair<String, Functions.UnaryThrows<?, String, IllegalArgumentException>>>builder()
                        .put("host", new Pair<String, Functions.UnaryThrows<?, String, IllegalArgumentException>>("host", RestResourceFactoryUtils.stringConvert))
                        .put("protocol", new Pair<String, Functions.UnaryThrows<?, String, IllegalArgumentException>>("protocol", new Functions.UnaryThrows<HttpConfiguration.Protocol, String, IllegalArgumentException>() {
                            @Override
                            public HttpConfiguration.Protocol call(String s) throws IllegalArgumentException {
                                if(HttpConfigurationMO.Protocol.ANY.name().equalsIgnoreCase(s))
                                    return null;
                                return HttpConfiguration.Protocol.valueOf(s);
                            }
                        }))
                        .put("ntlmHost", new Pair<String, Functions.UnaryThrows<?, String, IllegalArgumentException>>("ntlmHost", RestResourceFactoryUtils.stringConvert))
                        .put("ntlmDomain", new Pair<String, Functions.UnaryThrows<?, String, IllegalArgumentException>>("ntlmDomain", RestResourceFactoryUtils.stringConvert))
                        .put("securityZone.id", new Pair<String, Functions.UnaryThrows<?, String, IllegalArgumentException>>("securityZone.id", RestResourceFactoryUtils.goidConvert))
                        .map());
    }

    @Override
    @Inject
    public void setFactory(com.l7tech.external.assertions.gatewaymanagement.server.HttpConfigurationResourceFactory factory) {
        super.factory = factory;
    }

    @Override
    public HttpConfigurationMO getResourceTemplate() {
        HttpConfigurationMO httpConfigurationMO = ManagedObjectFactory.createHttpConfiguration();
        httpConfigurationMO.setHost("TemplateHostname");
        httpConfigurationMO.setPort(1234);
        httpConfigurationMO.setPath("TemplatePath");
        return httpConfigurationMO;
    }
}