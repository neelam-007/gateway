package com.l7tech.console.tree.policy;

import com.l7tech.policy.assertion.Assertion;

import java.util.*;
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
    protected Assertion assertion;

    /**
     * the subclasses must implement the
     * @param assertion
     */
    public AssertionDescription(Assertion assertion) {
        this.assertion = assertion;
    }

    /**
     * @return the long description for the assertion
     */
    public String getLongDescription() {
        Class assertionClass = assertion.getClass();
        String key = assertionClass.getName()+ ".long";
        return MessageFormat.format(getMessageBundle().getString(key), parameters());
    }

    /**
     * @return the short description for the assertion
     */
    public String getShortDescription() {
        Class assertionClass = assertion.getClass();
        String key = assertionClass.getName()+ ".short";
        return MessageFormat.format(getMessageBundle().getString(key), parameters());
    }

    /**
     * The subclasses implement this method. The method returns
     * the assertion parameters to be used in messages.

     * @return the <CODE>Object[]</CODE> array of assertion
     * parameters
     */
    protected abstract Object[] parameters();


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
    private static final
    String DESCRIPTIONS_BUNDLE = "com/l7tech/console/tree/policy/assertions";
}





