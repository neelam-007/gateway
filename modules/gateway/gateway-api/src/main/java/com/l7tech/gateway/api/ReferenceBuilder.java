package com.l7tech.gateway.api;

import java.util.ArrayList;

/**
 * This is a reference builder. It assists in building references.
 */
public class ReferenceBuilder {
    private final Reference reference;

    public ReferenceBuilder(Reference reference) {
        this.reference = reference;
    }

    public ReferenceBuilder(String title, String id, String type) {
        reference = new Reference();
        reference.setTitle(title);
        reference.setId(id);
        reference.setType(type);
    }

    public Reference build() {
        return reference;
    }

    public ReferenceBuilder setContent(Object content) {
        reference.setResource(content);
        return this;
    }

    /**
     * This is a Reference builder to make it easier to add links to a reference
     *
     * @param link The link to add to the reference
     * @return The reference with the newly added link
     */
    public ReferenceBuilder addLink(Link link) {
        if (reference.getLinks() == null) {
            reference.setLinks(new ArrayList<Link>());
        }
        reference.getLinks().add(link);
        return this;
    }
}
