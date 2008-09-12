package com.l7tech.external.assertions.messagecontext.server;

import com.l7tech.server.audit.Auditor;
import com.l7tech.server.audit.LogOnlyAuditor;
import com.l7tech.external.assertions.messagecontext.MessageContextAssertion;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.gateway.common.mapping.MessageContextMapping;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import com.l7tech.server.policy.variable.ExpandVariables;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.message.Message;
import com.l7tech.message.TcpKnob;
import com.l7tech.identity.User;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.List;
import java.util.ArrayList;

/**
 * Server side implementation of the MessageContextAssertion.
 *
 * @see com.l7tech.external.assertions.messagecontext.MessageContextAssertion
 */
public class ServerMessageContextAssertion extends AbstractServerAssertion<MessageContextAssertion> {
    private static final Logger logger = Logger.getLogger(ServerMessageContextAssertion.class.getName());

    private final MessageContextAssertion assertion;
    private final Auditor auditor;
    private final String[] variablesUsed;

    public ServerMessageContextAssertion(MessageContextAssertion assertion, ApplicationContext context) throws PolicyAssertionException {
        super(assertion);

        this.assertion = assertion;
        this.auditor = context != null ? new Auditor(this, context, logger) : new LogOnlyAuditor(logger);
        this.variablesUsed = assertion.getVariablesUsed();
    }

    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        List<MessageContextMapping> mappings = new ArrayList<MessageContextMapping>(5);
        for (MessageContextMapping mapping: assertion.getMappings()) {
            mappings.add(mapping.asCopy());
        }

        processMappings(context, mappings);

        return AssertionStatus.NONE;
    }

    /**
     * Process the mappings in this MessageContextAssertion (MCA) by validating these mappings and giving warning audits.
     * @param context: the PolicyEnforcementContext
     * @param newMappings: the mappings to be processed.
     */
    private void processMappings(PolicyEnforcementContext context, List<MessageContextMapping> newMappings) {
        // Step 1: evaluate the values of these mappings in the current MCA.
        for (MessageContextMapping mapping: newMappings) {
            processMappingValues(mapping, context);
        }

        // Step 2: remove those overidden mappings in the current MCA.
        if (newMappings != null && !newMappings.isEmpty()) {
            removeOverriddenMappings(newMappings);
        }

        // Step 3: check if there exists multiple MessageContextAssertions, which would cause TOO-MANY or OVERRIDDEN problems.
        // Case 1: this assertion is the first MessageContextAssertion in the policy.
        List<MessageContextMapping> prevMappings = context.getMappings();
        if (prevMappings == null || prevMappings.isEmpty()) {
            context.setMappings(newMappings);
            return;
        }

        // Case 2: there exists some other MessageContextAssertions before this asssertion, so need to check if there
        // exist overridden mappings and check if there are more than five distinct mappings.
        for (MessageContextMapping newMapping: newMappings) {
            boolean foundDuplicates = false;
            for (MessageContextMapping prevMapping: prevMappings) {
                if (newMapping.hasEqualTypeAndKeyExcludingValue(prevMapping)) {
                    foundDuplicates = true;
                    prevMappings.remove(prevMapping);
                    auditor.logAndAudit(AssertionMessages.MCM_MAPPING_OVERRIDDEN, prevMapping.getKey());
                    break;
                }
            }
            if (! foundDuplicates) {
                if (prevMappings.size() >= 5) {
                    MessageContextMapping droppedMapping = prevMappings.remove(0);
                    auditor.logAndAudit(AssertionMessages.MCM_TOO_MANY_MAPPINGS, droppedMapping.getKey());
                }
            }
            prevMappings.add(newMapping);
        }

        // Step 4: update the mappings in the PolicyEnforcementContext.
        context.setMappings(prevMappings);
    }

    /**
     * Remove duplicate mappings in the assertion.
     * @param mappings
     */
    private void removeOverriddenMappings(List<MessageContextMapping> mappings) {
        for (int i = mappings.size() - 1; i >= 0; i--) {
            for (int j = i - 1; j >= 0; j--) {
                if (mappings.get(i).hasEqualTypeAndKeyExcludingValue(mappings.get(j))) {
                    MessageContextMapping overriddenMapping = mappings.remove(j);
                    auditor.logAndAudit(AssertionMessages.MCM_MAPPING_OVERRIDDEN, overriddenMapping.getKey());
                    i--;
                }
            }
        }
    }

    /**
     * Evaluate the mapping value for each mapping.
     * @param checkedMapping
     * @param context
     */
    private void processMappingValues(MessageContextMapping checkedMapping, PolicyEnforcementContext context) {
        String value = checkedMapping.getValue();
        if (value == null || value.trim().equals("")) return;

        String mappingType = checkedMapping.getMappingType();
        if (mappingType.equals(MessageContextMapping.MappingType.IP_ADDRESS.getName())) {
            Message request = context.getRequest();
            TcpKnob reqTcp = (TcpKnob)request.getKnob(TcpKnob.class);
            value = (reqTcp != null)? reqTcp.getRemoteAddress() : null;
        } else if (mappingType.equals(MessageContextMapping.MappingType.AUTH_USER.getName())) {
            User user = context.getLastAuthenticatedUser();
            value = (user != null)? user.getName() : null;
        } else {
            // Check if the varaibles are set.
            value = ExpandVariables.process(value, context.getVariableMap(variablesUsed, auditor), auditor);
        }

        // Check if the value is extreme long.
        if (value != null && value.length() > 255) {
            value = value.substring(0, 255);
            auditor.logAndAudit(AssertionMessages.MCM_TOO_LONG_VALUE, checkedMapping.getKey());
        }

        checkedMapping.setValue(value);
    }

    /*
     * Called reflectively by module class loader when module is unloaded, to ask us to clean up any globals
     * that would otherwise keep our instances from getting collected.
     */
    public static void onModuleUnloaded() {
        // This assertion doesn't have anything to do in response to this, but it implements this anyway
        // since it will be used as an example by future modular assertion authors
        logger.log(Level.INFO, "ServerMessageContextAssertion is preparing itself to be unloaded");
    }
}
