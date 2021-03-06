package com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.impl.FirewallRuleTransformer;
import com.l7tech.gateway.api.FirewallRuleMO;
import com.l7tech.gateway.common.transport.firewall.SsgFirewallRule;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.ObjectModelException;
import com.l7tech.server.transport.firewall.SsgFirewallRuleManager;
import com.l7tech.util.SyspropUtil;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import javax.ws.rs.WebApplicationException;
import java.io.File;
import java.util.List;

/**
 * The firewall rule rest resource factory.
 */
@Component
public class FirewallRuleAPIResourceFactory extends EntityManagerAPIResourceFactory<FirewallRuleMO, SsgFirewallRule, EntityHeader> {

    @Inject
    private FirewallRuleTransformer transformer;
    @Inject
    private SsgFirewallRuleManager firewallRuleManager;

    @NotNull
    @Override
    public EntityType getResourceEntityType() {
        return EntityType.FIREWALL_RULE;
    }

    @Override
    protected SsgFirewallRule convertFromMO(FirewallRuleMO resource) throws ResourceFactory.InvalidResourceException {
        return transformer.convertFromMO(resource, null).getEntity();
    }

    @Override
    protected FirewallRuleMO convertToMO(SsgFirewallRule entity) {
        return transformer.convertToMO(entity);
    }

    @Override
    protected SsgFirewallRuleManager getEntityManager() {
        return firewallRuleManager;
    }

    @Override
    protected void beforeUpdateEntity(@NotNull final SsgFirewallRule entity, @NotNull SsgFirewallRule oldEntity) throws ObjectModelException {
        super.beforeUpdateEntity(entity, oldEntity);

        // make sure ordinals starts at 1
        if(entity.getOrdinal() < 1){
            entity.setOrdinal(1);
        }

        // make ordinals sequential and reorder other rules
        final List<SsgFirewallRule> rules = getOrderedFirewallRules();
        final int lastOrdinal = rules.size();
        if (entity.getOrdinal() > lastOrdinal) {
            entity.setOrdinal(lastOrdinal);
        }

        final int currentOrdinal = entity.getOrdinal();
        int ordinal = 1;
        for (SsgFirewallRule rule : rules) {

            if (ordinal == currentOrdinal)   {
                ordinal++;
            }
            if (rule.getOrdinal() != (ordinal) && !rule.getGoid().equals(entity.getGoid())) {
                rule.setOrdinal(ordinal);
                firewallRuleManager.update(rule);
            }

            if(rule.getGoid().equals(entity.getGoid())){
                ordinal --;
            }
            ordinal++;
        }

    }

    @Override
    protected void beforeCreateEntity(final SsgFirewallRule entity) throws ObjectModelException {
        super.beforeCreateEntity(entity);

        // make sure ordinals starts at 1
        if(entity.getOrdinal() < 1){
            entity.setOrdinal(1);
        }

        // make ordinals sequential and reorder other rules
        final List<SsgFirewallRule> rules = getOrderedFirewallRules();
        if(rules.isEmpty()){
            entity.setOrdinal(1);
            return;
        }
        final int lastOrdinal = rules.get(rules.size() - 1).getOrdinal();
        if (entity.getOrdinal() > lastOrdinal) {
            entity.setOrdinal(lastOrdinal + 1);
            return;
        }

        final int currentOrdinal = entity.getOrdinal();
        int ordinal = 1;
        for (SsgFirewallRule rule : rules) {

            if (  ordinal == currentOrdinal)   {
                ordinal++;
            }

            if (rule.getOrdinal() != (ordinal)) {
                rule.setOrdinal(ordinal);
                firewallRuleManager.update(rule);
            }

            ordinal++;
        }
    }


    @Override
    protected void beforeDeleteEntity(SsgFirewallRule entityToDelete) throws ObjectModelException {
        super.beforeDeleteEntity(entityToDelete);

        // make ordinals sequential
        final List<SsgFirewallRule> rules = getOrderedFirewallRules();
        int ordinal = 1;
        for (SsgFirewallRule rule : rules) {
            if (rule != entityToDelete) {
                if (rule.getOrdinal() != (ordinal)) {
                    rule.setOrdinal(ordinal);
                    firewallRuleManager.update(rule);
                }
                ordinal++;
            }
        }
    }

    private List<SsgFirewallRule> getOrderedFirewallRules() throws ObjectModelException {
        return firewallRuleManager.findPagedMatching(0, -1, "ordinal", true, null);
    }

    public boolean isHardware()throws WebApplicationException {
        if(SyspropUtil.getBoolean("com.l7tech.server.performFirewallRuleRestmanApplianceCheck", true)) {
            // TODO is there some cleaner way to do this?
            final File applianceDir = new File("/opt/SecureSpan/Appliance");
            return (applianceDir.exists() && applianceDir.isDirectory());
        } else {
            return true;
        }
    }
}
