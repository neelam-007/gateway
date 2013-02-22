package com.l7tech.config.client;

import com.l7tech.config.client.beans.ConfigurationBean;
import com.l7tech.config.client.options.Option;
import com.l7tech.config.client.options.OptionGroup;
import com.l7tech.config.client.options.OptionSet;
import com.l7tech.config.client.options.OptionType;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static com.l7tech.config.client.SummaryInteraction.HIDDEN_OPTION_FILTER;
import static org.junit.Assert.*;

public class SummaryInteractionTest {
    private static final String DB_GROUP = "db";
    private static final String DB_HOST = "db-host";
    private SummaryInteraction interaction;
    private OptionSet optionSet;
    private Set<Option> options;
    private Set<OptionGroup> groups;
    private Map<String, ConfigurationBean> configBeans;

    @Before
    public void setup() {
        configBeans = new HashMap<String, ConfigurationBean>();
        optionSet = new OptionSet();
        options = new HashSet<Option>();
        groups = new HashSet<OptionGroup>();
        interaction = new SummaryInteraction(optionSet, configBeans, true, true);
    }

    @Test
    public void getConfigurationSummaryDoesNotSkipNonRequiredGroups() {
        setupDbOptionsAndConfig(false);
        assertEquals("\n  Database Connection\n    Database Host = localhost\n", interaction.getConfigurationSummary());
    }

    /**
     * Password should not be included in the summary or included when considering the validity of the OptionGroup.
     */
    @Test
    public void getConfigurationSummaryIgnoresPassword() {
        setupDbOptionsAndConfig(true);
        assertEquals("\n  Database Connection\n    Database Host = localhost\n", interaction.getConfigurationSummary());
    }

    @Test
    public void isOptionGroupValid() {
        setupDbOptionsAndConfig(false);
        assertTrue(interaction.isOptionGroupValid(DB_GROUP));
    }

    @Test
    public void isOptionGroupValidNoConfigBeanForOption() {
        setupDbOptionsAndConfig(false);
        configBeans.remove(DB_HOST);
        assertFalse(interaction.isOptionGroupValid(DB_GROUP));
    }

    @Test
    public void isOptionGroupValidWithHiddenOptionFilter() {
        setupDbOptionsAndConfig(true);
        assertTrue(interaction.isOptionGroupValid(DB_GROUP, HIDDEN_OPTION_FILTER));
    }

    @Test
    public void hiddenOptionFilter() {
        for (final OptionType type : OptionType.values()) {
            final Option option = createOption(type);
            final boolean isActive = HIDDEN_OPTION_FILTER.isOptionActive(null, option);
            if (type.isHidden()) {
                assertFalse(isActive);
            } else {
                assertTrue(isActive);
            }
        }
    }

    private Option createOption(final OptionType type) {
        return createOption(null, "test", null, null, type);
    }

    private Option createOption(final String id, final String name, final String configValue, final String group, final OptionType type) {
        final Option option = new Option();
        option.setId(id);
        option.setType(type);
        option.setName(name);
        option.setConfigValue(configValue);
        option.setGroup(group);
        return option;
    }

    private OptionGroup createOptionGroup(final String id, final String description, final boolean required) {
        final OptionGroup group = new OptionGroup();
        group.setId(id);
        group.setDescription(description);
        group.setRequired(required);
        return group;
    }

    private ConfigurationBean createConfigBean(final Object value) {
        final ConfigurationBean configBean = new ConfigurationBean();
        configBean.setConfigValue(value);
        return configBean;
    }

    private void setupDbOptionsAndConfig(final boolean includePasswordOption) {
        groups.add(createOptionGroup(DB_GROUP, "Database Connection", false));
        optionSet.setOptionGroups(groups);
        options.add(createOption(DB_HOST, "Database Host", "localhost", DB_GROUP, OptionType.HOSTNAME));
        if (includePasswordOption) {
            options.add(createOption("db-pass", "Database Password", "test", DB_GROUP, OptionType.PASSWORD));
        }
        optionSet.setOptions(options);
        configBeans.put(DB_HOST, createConfigBean("localhost"));
    }
}
