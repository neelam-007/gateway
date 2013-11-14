package com.l7tech.external.assertions.radius;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.policy.assertion.DefaultAssertionMetadata;
import com.l7tech.policy.assertion.ExtensionInterfaceBinding;
import com.l7tech.util.Functions;
import net.jradius.client.RadiusClient;
import net.jradius.exception.RadiusException;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.util.*;

/**
 * A hidden assertion that registers the Radius Admin extension interface and GUI action.
 */
public class RadiusAssertion extends Assertion {

    public static String REASON_CODE = "reasonCode";

    @Override
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = defaultMeta();

        meta.put(AssertionMetadata.EXTENSION_INTERFACES_FACTORY, new Functions.Unary<Collection<ExtensionInterfaceBinding>, ApplicationContext>() {
            @Override
            public Collection<ExtensionInterfaceBinding> call(ApplicationContext appContext) {

                ExtensionInterfaceBinding binding = new ExtensionInterfaceBinding<RadiusAdmin>(RadiusAdmin.class, null, new RadiusAdmin() {
                    @Override
                    public boolean isAttributeValid(String name, String value) {
                        try {
                            RadiusUtils.getAttribute(name, value);
                            return true;
                        } catch (RadiusException e) {
                            return false;
                        }
                    }

                    @Override
                    public String[] getAuthenticators() {
                        try {
                            return (new RadiusClientWrapper()).getAuthenticators();
                        } catch (IOException e) {
                            return new String[0];
                        }
                    }

                    @Override
                    public boolean isAuthenticatorSupport(String authenticator) {
                        if (RadiusClient.getAuthProtocol(authenticator) != null) {
                            return true;
                        } else {
                            return false;
                        }
                    }
                }
                );
                return Collections.singletonList(binding);
            }
        });

        return meta;
    }

    private static class RadiusClientWrapper extends RadiusClient {

        public RadiusClientWrapper() throws IOException {
            super();
        }

        /**
         * Retrieve the registered Authenticators.
         *
         * @return a list of registered authenticators
         */
        public String[] getAuthenticators() {
            String[] list = new String[authenticators.size()];

            int i = 0;
            for (Map.Entry<String, Class<?>> entry: authenticators.entrySet()) {
                list[i] = entry.getKey();
                i++;
            }
            return list;
        }
    }
}
