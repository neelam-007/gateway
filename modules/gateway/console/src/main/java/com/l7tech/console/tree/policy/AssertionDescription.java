package com.l7tech.console.tree.policy;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionMetadata;

import java.util.*;
import java.util.logging.Logger;
import java.text.MessageFormat;

/**
 * The class is an 'description' for <CODE>Assertion</CODE>
 * instances. It provides short and long description for an
 * assertion.
 * <p>
 * The description is stored in resource bundle (properties).
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version $Revision$
 */
public abstract class AssertionDescription {
    private static final String NODESCRIPTION_FOUND_MSG = "<b><font color=\"red\">No description found for assertion: </font></b>";

    /**
     * the subclasses must implement the
     * @param assertion
     */
    public AssertionDescription(Assertion assertion) {
        this.assertion = assertion;
    }

    /**
     * @return long description from the resource file, or null if not found.
     */
    private String getLongDescriptionFromResource() {
        Class assertionClass = assertion.getClass();
        String key = assertionClass.getName()+ ".long";
        String longDesc = null;
        try {
            longDesc = getMessageBundle().getString(key);
        } catch (MissingResourceException e) {
            // fallthrough
        }
        return longDesc;
    }

    /**
     * @return the short description for the assertion
     */
    public String getShortDescription() {
        String description = null;

        Class assertionClass = assertion.getClass();
        String key = assertionClass.getName()+ ".short";
        try {
            description = MessageFormat.format(getMessageBundle().getString(key), parameters());
        }
        catch(MissingResourceException mre) {
            description = null;
        }
        if (description == null || description.length() < 1) description = (String)assertion.meta().get(AssertionMetadata.SHORT_NAME);
        if (description == null) description = "DESCRIPTION NOT SET";

        return description;
    }

    /**
     * The subclasses implement this method. The method returns
     * the assertion parameters to be used in messages.

     * @return the <CODE>Object[]</CODE> array of assertion
     * parameters
     */
    protected abstract Object[] parameters();

    public String getDescriptionText() {
        Class assertionClass = assertion.getClass();
        String key = assertionClass.getName() + ".description";
        String desc = null;
        try {
            desc = getMessageBundle().getString(key);
        } catch (MissingResourceException mrex) {
            try {
                if (desc == null) {
                    // Ignore AssertionMetadata LONG_NAME here because DESCRIPTION's
                    // default MetadataFinder already takes it into account
                    desc = getLongDescriptionFromResource();
                }
            } catch(MissingResourceException mrexagain) {
                desc = null;
            }
        }
        if (desc == null) desc = (String)assertion.meta().get(AssertionMetadata.DESCRIPTION);

        return desc != null ? MessageFormat.format(desc, parameters()) : NODESCRIPTION_FOUND_MSG + assertionClass.getName();
    }


    /** singleton holder for ResurceBundle instance class. */
    private static class DescriptionsBundle {
        static final
        ResourceBundle bundle =
          ResourceBundle.getBundle(DESCRIPTIONS_BUNDLE);
    }

    /**
     * @return the messages resource bundle
     * @exception MissingResourceException
     *                   if the ResourceBundle cannot be created
     *                   (caused by missing resource file, permissions
     *                   etc)
     */
    ResourceBundle getMessageBundle()
      throws MissingResourceException {
        if (DescriptionsBundle.bundle != null) {
            return DescriptionsBundle.bundle;
        }

        throw new
          MissingResourceException(DESCRIPTIONS_BUNDLE,
            ResourceBundle.class.getName(), "bundle");
    }

    /** the log messages bundle */
    private static final String DESCRIPTIONS_BUNDLE = "com/l7tech/console/tree/policy/assertions";
    private static final Logger logger = Logger.getLogger(AssertionDescription.class.getName());

    protected Assertion assertion;
}



