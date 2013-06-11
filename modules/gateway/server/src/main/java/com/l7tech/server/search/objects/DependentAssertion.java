package com.l7tech.server.search.objects;

/**
 * Represents a dependent assertion.
 *
 * @author Victor Kazakov
 */
public class DependentAssertion extends DependentObject {

    private String descriptiveName;

    public DependentAssertion(String name, String descriptiveName) {
        super(name);
        this.descriptiveName = descriptiveName;
    }

    public String getDescriptiveName() {
        return descriptiveName;
    }
}
