package com.l7tech.gateway.common.custom;

import com.l7tech.policy.assertion.ext.Category;
import com.l7tech.policy.assertion.ext.CustomAssertion;
import com.l7tech.policy.assertion.ext.ServiceInvocation;
import com.l7tech.policy.assertion.ext.CustomAssertionUI;

import java.util.HashSet;
import java.util.Set;

/**
 * The class <code>CustomAssertionDescriptor</code> contains the
 * runtime information that represent a custom assertion.
 * <ul>
 * <li> the assertion name
 * <li> the <code>CustomAssertion</code> bean with the properties
 * <li> the corresponding server side <code>ServiceInvocation</code> subclass
 * <li> the optional client side <code>ClientAssertion</code>
 * <li> an optional UI management side <code>CustomAssertionUI</code>
 * <li> the assertion <code>Category</code>
 * </ul>
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 */
public class CustomAssertionDescriptor {
    private final Class assertion;
    private final Class serverAssertion;
    private final String name;
    private final String description;
    private final String paletteNodeName;
    private final String policyNodeName;
    private final String[] uiAllowedPackages;
    private final Set<String> uiAllowedResources;
    private final Category category;
    private final Class uiClass;

    /**
     * Create the new extensibility holder instance with the assertion, server
     * assertion, and the client (Bridge) asseriton class.
     *
     * @param name                       the assertion name
     * @param assertionClass             the assertion class
     * @param uiClass                    the UI class, must be implementation of <code>CustomAssertionUI</code>
     * @param serverAssertionClass       the server side assertion class
     * @param cat                        the assertion category
     * @param optionalDescription        the description (may be null)
     * @param optionalUiAllowedPackages  the allowed packages for use by the SSM (may be null)
     * @param optionalUiAllowedResources the allowed resources for use by the SSM (may be null)
     */
    public CustomAssertionDescriptor(final String name,
                                     final Class assertionClass,
                                     final Class uiClass,
                                     final Class serverAssertionClass,
                                     final Category cat,
                                     final String optionalDescription,
                                     final String optionalUiAllowedPackages,
                                     final String optionalUiAllowedResources,
                                     final String paletteNodeName,
                                     final String policyNodeName) {
        this.name = name;
        this.description = optionalDescription;
        this.assertion = assertionClass;
        this.category = cat;
        this.paletteNodeName = paletteNodeName;
        this.policyNodeName = policyNodeName;

        if (!CustomAssertion.class.isAssignableFrom(assertionClass)) {
            throw new IllegalArgumentException("Assertion " + assertionClass);
        }

        this.serverAssertion = serverAssertionClass;

        if (!ServiceInvocation.class.isAssignableFrom(serverAssertionClass)) {
            throw new IllegalArgumentException("Server assertion " + serverAssertionClass);
        }

        this.uiClass = uiClass;
        if (uiClass != null && !CustomAssertionUI.class.isAssignableFrom(uiClass)) {
            throw new IllegalArgumentException("Editor assertion " + uiClass);
        }

        if (optionalUiAllowedPackages != null) {
            this.uiAllowedPackages = optionalUiAllowedPackages.split(",");
            for (int ix = 0; ix < uiAllowedPackages.length; ix++) {
                this.uiAllowedPackages[ix] = this.uiAllowedPackages[ix].replace('.', '/').trim();
            }
        } else {
            this.uiAllowedPackages = new String[0];
        }

        this.uiAllowedResources = new HashSet<String>();
        if (optionalUiAllowedResources != null) {
            String[] split = optionalUiAllowedResources.split(",");
            for (int ix = 0; ix < split.length; ix++) {
                this.uiAllowedResources.add(split[ix].trim());
            }
        }
    }

    /**
     * @return the custom assertion name
     */
    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getPaletteNodeName() {
        return paletteNodeName;
    }

    public String getPolicyNodeName() {
        return policyNodeName;
    }

    public String[] getUiAllowedPackages() {
        return uiAllowedPackages;
    }

    public Set<String> getUiAllowedResources() {
        return uiAllowedResources;
    }

    /**
     * @return the assertion class
     */
    public Class getAssertion() {
        return assertion;
    }

    /**
     * @return the server assertion class
     */
    public Class getServerAssertion() {
        return serverAssertion;
    }

    /**
     * @return the category for this custom assertion
     * @see Category
     */
    public Category getCategory() {
        return category;
    }

    /**
     * @return the UI class or <b>null</b> if it has not been set
     */
    public Class getUiClass() {
        return uiClass;
    }

    public String toString() {
        return new StringBuffer("[")
          .append("; name='").append(name).append("'")
          .append("; category=").append(category)
          .append("; assertion=").append(safeName(assertion))
          .append("; serverAssertion=").append(safeName(serverAssertion))
          .append("; editorClass=").append(safeName(uiClass))
          .append("]").append(super.toString()).toString();
    }

    private String safeName(Class cl) {
        return cl != null ? cl.getName() : "null";
    }
}