package com.l7tech.ems.config.client;

import org.junit.Test;
import org.junit.Assert;
import com.l7tech.config.client.options.OptionSet;
import com.l7tech.config.client.options.OptionGroup;
import com.l7tech.config.client.options.Option;
import com.l7tech.config.client.ConfigurationException;
import com.l7tech.config.client.ConfigurationFactory;

/**
 *
 */
public class ConfigurationFactoryTest {

    @Test
    public void testLoadMissingConfiguration() throws Exception {
        try {
            ConfigurationFactory.newConfiguration(ConfigurationFactoryTest.class, "thereisnothinghere.xml");
            Assert.fail("Expected ConfigurationException");
        } catch ( ConfigurationException ce ) {
            // expected
        }
    }

    @Test
    public void testLoadEmsConfiguration() throws Exception {
        OptionSet options = ConfigurationFactory.newConfiguration(ConfigurationFactoryTest.class, "configTemplates/EsmConfiguration.xml");
        System.out.println( "Loaded option set : " + options.getId() );
        System.out.println( "Loaded option grps: " + options.getOptionGroups().size() );
        System.out.println( "Loaded options    : " + options.getOptions().size() );

        System.out.println();
        System.out.println();
        for ( Option option : options.getOptions() ) {
            System.out.println( "Option: " + option.getName() + (!option.isUpdatable() ? " (create only)" : ""));
        }

        Assert.assertTrue("Group missing", !options.getOptionsForGroup("admin").isEmpty());
        Assert.assertTrue("Group missing", !options.getOptionsForGroup("server").isEmpty());
    }

    @Test
    public void testValidEmsConfiguration() throws Exception {
        validateConfiguration("configTemplates/EsmConfiguration.xml");
    }

    private void validateConfiguration(final String resource) throws Exception {
        OptionSet options = ConfigurationFactory.newConfiguration( ConfigurationFactoryTest.class, resource);

        for ( OptionGroup group : options.getOptionGroups() ) {
            Assert.assertNotNull( "Missing group id", group.getId());
            Assert.assertTrue( "Empty group " + group.getId(), !options.getOptionsForGroup(group.getId()).isEmpty() );
        }

        for ( Option option : options.getOptions() ) {
            Assert.assertNotNull( "Missing option id", option.getId());
            if ( option.getGroup() != null ) {
                Assert.assertNotNull( "Invalid group for option " + option.getId(), options.getOptionGroup(option.getGroup() ));
            }
        }
    }
}