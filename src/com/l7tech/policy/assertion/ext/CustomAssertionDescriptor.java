package com.l7tech.policy.assertion.ext;


import com.l7tech.proxy.policy.assertion.ClientAssertion;

/**
 * The class <code>CustomAssertionDescriptor</code> contains the
 * runtime information that represent a custom assertion.
 * <ul>
 * <li> the assertion name
 * <li> the <code>CustomAssertion</code> bean with the properties
 * <li> the corresponding server side <code>ServiceInvocation</code> subclass
 * <li> the optional client side <code>ClientAssertion</code>; must be
 * an existing assertion
 * <li> the assertion <code>Category</code>
 * <li> the optional <code>SecurityManager</code> that the assertion runs under
 * </ul>
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 */
public class CustomAssertionDescriptor {
    private Class assertion;
    private Class serverAssertion;
    private Class clientAssertion;
    private String name;
    private Category category;
    private SecurityManager securityManager;

    /**
     * Create the new extensibility holder instance with the assertion, server
     * assertion, and the client (Bridge) asseriton class.
     *
     * @param name the assertion name
     * @param a    the assertion class
     * @param ca   the Bridge assertion class
     * @param sa   the server side assertion class
     */
    public CustomAssertionDescriptor(String name, Class a, Class ca, Class sa, Category cat, SecurityManager sm) {
        this.name = name;
        this.assertion = a;
        this.category = cat;
        this.securityManager = sm;
        if (!CustomAssertion.class.isAssignableFrom(a)) {
            throw new IllegalArgumentException("assertion " + a);
        }

        this.clientAssertion = ca;
        if (ca != null && !ClientAssertion.class.isAssignableFrom(ca)) {
            throw new IllegalArgumentException("client assertion " + ca);
        }
        this.serverAssertion = sa;

        if (!ServiceInvocation.class.isAssignableFrom(sa)) {
            throw new IllegalArgumentException("server assertion " + sa);
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

    public String toString() {
        return new StringBuffer("[")
          .append("; name='").append(name).append("'")
          .append("category=").append(category)
          .append("assertion=").append(safeName(assertion))
          .append("; serverAssertion=").append(safeName(serverAssertion))
          .append("; clientAssertion=").append(safeName(clientAssertion))
          .append("]").append(super.toString()).toString();
    }

    private String safeName(Class cl) {
        return cl != null ? cl.getName() : "null";
    }
}