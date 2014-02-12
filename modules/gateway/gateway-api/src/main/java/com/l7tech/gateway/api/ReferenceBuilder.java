package com.l7tech.gateway.api;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;

/**
 * This is a reference builder. It assists in building references.
 */
public abstract class ReferenceBuilder<I, R extends Reference<I>> {
    protected final R reference;

    public ReferenceBuilder(@NotNull R reference) {
        this.reference = reference;
    }

    public ReferenceBuilder(String title, String type, @NotNull R reference) {
        this(reference);
        reference.setName(title);
        reference.setType(type);
    }

    public ReferenceBuilder(String title, String id, String type, @NotNull R reference) {
        this(title, type, reference);
        reference.setId(id);
    }

    @NotNull
    public R build() {
        return reference;
    }

    public ReferenceBuilder<I, R> setContent(I content) {
        reference.setContent(content);
        return this;
    }

    /**
     * This will add a link to the reference. This does not check for duplicate links
     *
     * @param link The link to add to the reference
     * @return The reference with the newly added link
     */
    public ReferenceBuilder<I, R> addLink(@NotNull Link link) {
        if (reference.getLinks() == null) {
            reference.setLinks(new ArrayList<Link>());
        }
        reference.getLinks().add(link);
        return this;
    }

    /**
     * This will add the given links to the reference. This does not check for duplicate links
     *
     * @param links The links to add to the reference
     * @return The reference with the newly added links
     */
    public ReferenceBuilder<I, R> addLinks(@NotNull Collection<Link> links) {
        if (reference.getLinks() == null) {
            reference.setLinks(new ArrayList<Link>());
        }
        reference.getLinks().addAll(links);
        return this;
    }
}
