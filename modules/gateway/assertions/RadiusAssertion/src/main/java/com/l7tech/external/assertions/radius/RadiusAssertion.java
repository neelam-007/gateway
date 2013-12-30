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

    private static final Set<String> excludedAuthenticators;
    //the following authenticators are excluded from the set due to implementation complexity.
    //TODO: implement support for excluded authenticators
    static{
        excludedAuthenticators = new HashSet<>();
        excludedAuthenticators.add("eap-tls");
        excludedAuthenticators.add("eap-ttls");
        excludedAuthenticators.add("peap");
    }

    @Override
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = defaultMeta();

        meta.put(AssertionMetadata.EXTENSION_INTERFACES_FACTORY, new Functions.Unary<Collection<ExtensionInterfaceBinding>, ApplicationContext>() {
            @Override
            public Collection<ExtensionInterfaceBinding> call(ApplicationContext appContext) {

                ExtensionInterfaceBinding binding = new ExtensionInterfaceBinding<>(RadiusAdmin.class, null, new RadiusAdmin() {
                    @Override
                    public boolean isAttributeNameValid(String name) {
                        return RadiusUtils.isAttributeValid(name);
                    }

                    @Override
                    public boolean isAttributeValid(String name, String value) {
                        try {
                            RadiusUtils.newAttribute(name, value);
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
                        return RadiusClient.getAuthProtocol(authenticator) != null;
                    }
                }
                );
                return Collections.singletonList(binding);
            }
        });

        meta.put(AssertionMetadata.FEATURE_SET_NAME, "(fromClass)");

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
            List<String> list = new ArrayList<>();

            for (Map.Entry<String, Class<?>> entry: authenticators.entrySet()) {
                if(excludedAuthenticators.contains(entry.getKey())) continue;
                list.add(entry.getKey());
            }
            return list.toArray(new String[]{});
        }


    }
}
