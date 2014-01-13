package com.l7tech.gateway.api;

import java.util.ArrayList;

/**
 * This is a reference builder. It assists in building references.
 */
public abstract class ReferenceBuilder<I, R extends Reference<I>> {
    protected final R reference;

    public ReferenceBuilder(R reference) {
        this.reference = reference;
    }

    public ReferenceBuilder(String title, String type, R reference) {
        this(reference);
        reference.setName(title);
        reference.setType(type);
    }

    public ReferenceBuilder(String title, String id, String type, R reference) {
        this(title, type, reference);
        reference.setId(id);
    }

    public R build() {
        return reference;
    }

    public ReferenceBuilder<I, R> setContent(I content) {
        reference.setContent(content);
        return this;
    }

    /**
     * This is a Reference builder to make it easier to add links to a reference
     *
     * @param link The link to add to the reference
     * @return The reference with the newly added link
     */
    public ReferenceBuilder<I, R> addLink(Link link) {
        if (reference.getLinks() == null) {
            reference.setLinks(new ArrayList<Link>());
        }
        reference.getLinks().add(link);
        return this;
    }
}
