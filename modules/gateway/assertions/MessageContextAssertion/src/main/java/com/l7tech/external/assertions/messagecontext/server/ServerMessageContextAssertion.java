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
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

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
    private ApplicationContext applicationContext;

    public ServerMessageContextAssertion(MessageContextAssertion assertion, ApplicationContext context) throws PolicyAssertionException {
        super(assertion);

        this.assertion = assertion;
        this.auditor = context != null ? new Auditor(this, context, logger) : new LogOnlyAuditor(logger);
        this.variablesUsed = assertion.getVariablesUsed();
        for (String var : variablesUsed) System.out.println("In ServerMCA constructor, c.v. = " + var);
        applicationContext = context;
    }

    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        List<MessageContextMapping> mappings = new ArrayList<MessageContextMapping>(5);
        mappings.addAll(Arrays.asList(assertion.getMappings()));
        updateMappingsInPolicyEnforcementContext(context, mappings);

        return AssertionStatus.NONE;
    }

    private void updateMappingsInPolicyEnforcementContext(PolicyEnforcementContext context, List<MessageContextMapping> newMappings) {
        if (newMappings != null && !newMappings.isEmpty()) {
            removeOverriddenMappings(newMappings);
        }

        for (MessageContextMapping mapping: newMappings) {
            processMappingValue(mapping, context);
        }

        List<MessageContextMapping> prevMappings = context.getMappings();
        if (prevMappings == null || prevMappings.isEmpty()) {
            context.setMappings(newMappings);
            return;
        }

        for (MessageContextMapping newMapping: newMappings) {
            boolean foundDuplicates = false;
            for (MessageContextMapping prevMapping: prevMappings) {
                if (newMapping.hasEqualTypeAndKeyDifferentValue(prevMapping)) {
                    foundDuplicates = true;
                    prevMappings.remove(prevMapping);
                    System.out.println("### mapping overridden - key = " + prevMapping.getKey());
                    auditor.logAndAudit(AssertionMessages.MCM_MAPPING_OVERRIDDEN, prevMapping.getKey());
                    break;
                }
            }
            if (! foundDuplicates) {
                if (prevMappings.size() >= 5) {
                    MessageContextMapping droppedMapping = prevMappings.remove(0);
                    System.out.println("### mapping dropped - key = " + droppedMapping.getKey());
                    auditor.logAndAudit(AssertionMessages.MCM_TOO_MANY_MAPPINGS, droppedMapping.getKey());
                }
            }
            prevMappings.add(newMapping);
        }
        
        context.setMappings(prevMappings);
    }

    private void removeOverriddenMappings(List<MessageContextMapping> mappings) {
        for (int i = mappings.size() - 1; i >= 0; i--) {
            for (int j = i - 1; j >= 0; j--) {
                if (mappings.get(i).hasEqualTypeAndKeyDifferentValue(mappings.get(j))) {
                    MessageContextMapping overriddenMapping = mappings.remove(j);
                    auditor.logAndAudit(AssertionMessages.MCM_MAPPING_OVERRIDDEN, overriddenMapping.getKey());
                    j--;
                    i--;
                }
            }
        }
    }

    private void processMappingValue(MessageContextMapping checkedMapping, PolicyEnforcementContext context) {
        String value = checkedMapping.getValue();
        if (value == null || value.trim().equals("")) return;

        // Check if the varaibles are set.
        value = ExpandVariables.process(value, context.getVariableMap(variablesUsed, auditor), auditor);

        // Check if the value is extreme long.
        if (value.length() > 255) {
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
