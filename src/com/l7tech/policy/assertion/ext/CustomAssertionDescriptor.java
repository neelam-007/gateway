package com.l7tech.policy.assertion.ext;


import com.l7tech.proxy.policy.assertion.ClientAssertion;

/**
 * The class <code>CustomAssertionDescriptor</code> contains the
 * elements that represent an custom assertion.
 * <ul>
 * <li> the <code>Assertion</code> bean with the proerties
 * <li> the corresponding server side <code>ServerAssertion</code>
 * <li> the corresponding client side <code>ClientAssertion</code>
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

    /**
     * Create the new extensibility holder instance with the assertion, server
     * assertion, and the client (agent) asseriton class.
     *
     * @param name the assertion name
     * @param a    the assertion class
     * @param ca   the agent assertion class
     * @param sa   the server side assertion class
     */
    public CustomAssertionDescriptor(String name, Class a, Class ca, Class sa, Category cat) {
        this.name = name;
        this.assertion = a;
        this.category = cat;
        if (!CustomAssertion.class.isAssignableFrom(a)) {
            throw new IllegalArgumentException("assertion " + a);
        }

        this.clientAssertion = ca;
        if (!ClientAssertion.class.isAssignableFrom(ca)) {
            throw new IllegalArgumentException("client assertion " + ca);
        }
        this.serverAssertion = sa;

        if (!ServiceInvocation.class.isAssignableFrom(sa)) {
            throw new IllegalArgumentException("server assertion " + sa);
        }
    }

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
     * @return the client assertion class
     */
    public Class getClientAssertion() {
        return clientAssertion;
    }

    /**
     * @return the category for this custom assertion
     */
    public Category getCategory() {
        return category;
    }

    /**
     * @return the server assertion class
     */
    public Class getServerAssertion() {
        return serverAssertion;
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