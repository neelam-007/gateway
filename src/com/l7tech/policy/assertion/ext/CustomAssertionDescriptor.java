package com.l7tech.policy.assertion.ext;


import com.l7tech.proxy.policy.assertion.ClientAssertion;

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
 * <li> the optional <code>SecurityManager</code> that the assertion runs under
 * </ul>
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 */
public class CustomAssertionDescriptor {
    private final Class assertion;
    private final Class serverAssertion;
    private final Class clientAssertion;
    private final String name;
    private final Category category;
    private final SecurityManager securityManager;
    private final Class uiClass;

    /**
     * Create the new extensibility holder instance with the assertion, server
     * assertion, and the client (Bridge) asseriton class.
     *
     * @param name                 the assertion name
     * @param assertionClass       the assertion class
     * @param clientAssertionClass the client (Bridge for example) assertion class
     * @param uiClass              the UI class, must be implementation of <code>CustomAssertionUI</code>
     * @param serverAssertionClass the server side assertion class
     */
    public CustomAssertionDescriptor(String name, Class assertionClass, Class clientAssertionClass,
                                     Class uiClass, Class serverAssertionClass, Category cat, SecurityManager securityManager) {
        this.name = name;
        this.assertion = assertionClass;
        this.category = cat;
        this.securityManager = securityManager;
        if (!CustomAssertion.class.isAssignableFrom(assertionClass)) {
            throw new IllegalArgumentException("Assertion " + assertionClass);
        }

        this.clientAssertion = clientAssertionClass;
        if (clientAssertionClass != null && !ClientAssertion.class.isAssignableFrom(clientAssertionClass)) {
            throw new IllegalArgumentException("Client assertion " + clientAssertionClass);
        }
        this.serverAssertion = serverAssertionClass;

        if (!ServiceInvocation.class.isAssignableFrom(serverAssertionClass)) {
            throw new IllegalArgumentException("Server assertion " + serverAssertionClass);
        }

        this.uiClass = uiClass;
        if (uiClass != null && !CustomAssertionUI.class.isAssignableFrom(uiClass)) {
            throw new IllegalArgumentException("Editor assertion " + uiClass);
        }
    }

    /**
     * @return the custom assertion name
     */
    public String getName() {
        return name;
    }

    /**
     * @return the assertion class
     */
    public Class getAssertion() {
        return assertion;
    }

    /**
     * @return the client assertion class or <b>null</b> if none specified
     */
    public Class getClientAssertion() {
        return clientAssertion;
    }

    /**
     * @return the server assertion class
     */
    public Class getServerAssertion() {
        return serverAssertion;
    }

    /**
     * @return the <code>SecurityManager</code> or <b>null</b> if none specified
     */
    public SecurityManager getSecurityManager() {
        return securityManager;
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
          .append("category=").append(category)
          .append("assertion=").append(safeName(assertion))
          .append("; serverAssertion=").append(safeName(serverAssertion))
          .append("; clientAssertion=").append(safeName(clientAssertion))
          .append("; editorClass=").append(safeName(uiClass))
          .append("]").append(super.toString()).toString();
    }

    private String safeName(Class cl) {
        return cl != null ? cl.getName() : "null";
    }
}