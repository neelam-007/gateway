package com.l7tech.external.assertions.mqnative.server;

import com.l7tech.external.assertions.mqnative.MqNativeAdmin;
import com.l7tech.policy.assertion.ExtensionInterfaceBinding;
import com.l7tech.server.ServerConfig;
import com.l7tech.server.ServerConfigParams;
import com.l7tech.util.Config;
import org.springframework.context.ApplicationContext;

import java.util.Collection;
import java.util.Collections;

/**
 * Sever-side glue for the MQ Native Admin implementation,
 * which is exposed via a simple admin extension interface.
 *
 * @author ghuang
 */
public class MqNativeAdminServerSupport {
    private static MqNativeAdminServerSupport instance;

    private Config config;

    public static synchronized MqNativeAdminServerSupport getInstance(final ApplicationContext context) {
        if (instance == null) {
            MqNativeAdminServerSupport support = new MqNativeAdminServerSupport();
            support.init(context);
            instance = support;
        }
        return instance;
    }

    public void init(ApplicationContext context) {
        config = context.getBean("serverConfig", ServerConfig.class);
    }

    public Collection<ExtensionInterfaceBinding> getExtensionInterfaceBindings() {
        ExtensionInterfaceBinding binding = new ExtensionInterfaceBinding<MqNativeAdmin>(MqNativeAdmin.class, null, new MqNativeAdmin() {
            @Override
            public long getDefaultMqMessageMaxBytes() {
                return config.getLongProperty(ServerConfigParams.PARAM_IO_MQ_MESSAGE_MAX_BYTES, 2621440L);  // ioMqMessageMaxBytes.default = 2621440
            }
        });

        return Collections.singletonList(binding);
    }
}