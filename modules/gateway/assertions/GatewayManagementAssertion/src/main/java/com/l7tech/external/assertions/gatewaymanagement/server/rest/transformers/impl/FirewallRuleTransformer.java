package com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.SecretsEncryptor;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.EntityAPITransformer;
import com.l7tech.gateway.api.*;
import com.l7tech.gateway.common.transport.firewall.SsgFirewallRule;
import com.l7tech.objectmodel.*;
import com.l7tech.server.bundling.EntityContainer;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class FirewallRuleTransformer implements EntityAPITransformer<FirewallRuleMO, SsgFirewallRule> {


    @NotNull
    @Override
    public String getResourceType() {
        return EntityType.FIREWALL_RULE.toString();
    }

    @NotNull
    @Override
    public FirewallRuleMO convertToMO(@NotNull EntityContainer<SsgFirewallRule> userEntityContainer,  SecretsEncryptor secretsEncryptor) {
        return convertToMO(userEntityContainer.getEntity(), secretsEncryptor);
    }

    @NotNull
    public FirewallRuleMO convertToMO(@NotNull SsgFirewallRule ssgFirewallRule) {
        return convertToMO(ssgFirewallRule, null);
    }

    @NotNull
    @Override
    public FirewallRuleMO convertToMO(@NotNull SsgFirewallRule firewallRule,  SecretsEncryptor secretsEncryptor) {
        FirewallRuleMO firewallRuleMO = ManagedObjectFactory.createFirewallRuleMO();
        firewallRuleMO.setId(firewallRule.getId());
        firewallRuleMO.setVersion(firewallRule.getVersion());
        firewallRuleMO.setName(firewallRule.getName());
        firewallRuleMO.setEnabled(firewallRule.isEnabled());
        firewallRuleMO.setOrdinal(firewallRule.getOrdinal());

        Map<String,String> properties = new HashMap<String,String>();
        for (String propertyName : firewallRule.getPropertyNames()) {
            properties.put(propertyName, firewallRule.getProperty(propertyName));
        }
        firewallRuleMO.setProperties(properties);

        return firewallRuleMO;
    }

    @NotNull
    @Override
    public EntityContainer<SsgFirewallRule> convertFromMO(@NotNull FirewallRuleMO firewallRuleMO, SecretsEncryptor secretsEncryptor) throws ResourceFactory.InvalidResourceException {
        return convertFromMO(firewallRuleMO,true, secretsEncryptor);
    }

    @NotNull
    @Override
    public EntityContainer<SsgFirewallRule> convertFromMO(@NotNull FirewallRuleMO firewallRuleMO, boolean strict, SecretsEncryptor secretsEncryptor) throws ResourceFactory.InvalidResourceException {
        SsgFirewallRule firewallRule = new SsgFirewallRule();
        firewallRule.setId(firewallRuleMO.getId());
        if(firewallRuleMO.getVersion()!=null) {
            firewallRule.setVersion(firewallRuleMO.getVersion());
        }
        firewallRule.setName(firewallRuleMO.getName());
        firewallRule.setOrdinal(firewallRuleMO.getOrdinal());
        firewallRule.setEnabled(firewallRuleMO.isEnabled());

        Map<String, String> props = firewallRuleMO.getProperties();
        if(props!=null){
            for (Map.Entry<String, String> entry : props.entrySet()) {
                firewallRule.putProperty(entry.getKey(), entry.getValue());
            }
        }

        return new EntityContainer<SsgFirewallRule>(firewallRule);
    }

    @NotNull
    @Override
    public Item<FirewallRuleMO> convertToItem(@NotNull FirewallRuleMO m) {
        return new ItemBuilder<FirewallRuleMO>(m.getName(), m.getId(), EntityType.FIREWALL_RULE.name())
                .setContent(m)
                .build();
    }
}
