package com.l7tech.external.assertions.whichmodule;

import com.l7tech.policy.assertion.*;
import com.l7tech.policy.variable.DataType;
import com.l7tech.policy.variable.VariableMetadata;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.module.ModularAssertionModule;
import com.l7tech.server.policy.ServerAssertionRegistry;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.util.Set;

/**
 * Set a variable with XML-encoded information about all loaded modules.
 */
public class DumpModulesAssertion extends Assertion implements SetsVariables {
    private static final String OUTVAR = "modules";

    @Override
    public VariableMetadata[] getVariablesSet() {
        return new VariableMetadata[] {
                new VariableMetadata(OUTVAR, false, true, null, false, DataType.STRING),
        };
    }

    @Override
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = defaultMeta();
        meta.put(AssertionMetadata.DESCRIPTION, "Gets info about all currently loaded modules");
        meta.put(AssertionMetadata.PALETTE_NODE_ICON, "com/l7tech/console/resources/ServerRegistry.gif");
        meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[] { "policyLogic" });
        meta.put(AssertionMetadata.SERVER_ASSERTION_CLASSNAME, getClass().getName() + "$ServerImpl");
        meta.put(AssertionMetadata.PROPERTIES_EDITOR_CLASSNAME, getClass().getName() + "$PropDialog");
        meta.put(AssertionMetadata.POLICY_VALIDATOR_CLASSNAME, getClass().getName() + "$Validator");
        return meta;
    }

    public static class ServerImpl extends AbstractServerAssertion<DumpModulesAssertion> {
        private final ServerAssertionRegistry assertionRegistry;
        private final String[] simpleProps = new String[] {
                AssertionMetadata.SHORT_NAME,
                AssertionMetadata.LONG_NAME,
                AssertionMetadata.DESCRIPTION,
                AssertionMetadata.PALETTE_NODE_NAME,
                AssertionMetadata.USED_BY_CLIENT,
        };

        public ServerImpl(DumpModulesAssertion assertion, ApplicationContext context) {
            super(assertion);
            this.assertionRegistry = context.getBean("assertionRegistry", ServerAssertionRegistry.class);
        }

        @Override
        public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {

            Set<ModularAssertionModule> mods = assertionRegistry.getLoadedModules();
            StringBuffer sb = new StringBuffer();
            for (ModularAssertionModule mod : mods) {

                sb.append("<module>\n");
                sb.append("    <name>").append(mod.getName()).append("</name>\n");
                sb.append("    <digest>").append(mod.getDigest()).append("</digest>\n");
                for (Assertion proto : mod.getAssertionPrototypes()) {
                    final AssertionMetadata meta = proto.meta();
                    sb.append("    <assertion classname=\"").append(proto.getClass().getName()).append("\">\n");
                    for (String prop : simpleProps) {
                        sb.append("        <").append(prop).append(">").append(meta.get(prop)).append("</").append(prop).append(">\n");
                    }
                    sb.append("    </assertion>\n");
                }
                sb.append("</module>\n");
            }
            context.setVariable(OUTVAR, sb.toString());

            return AssertionStatus.NONE;
        }
    }
}

