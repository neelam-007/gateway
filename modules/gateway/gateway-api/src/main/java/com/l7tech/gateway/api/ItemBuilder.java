package com.l7tech.gateway.api;

/**
 * This is a reference builder. It assists in building references.
 */
public class ItemBuilder<I> extends ReferenceBuilder<I,Item<I>> {

    public ItemBuilder(Item<I> reference) {
        super(reference);
    }

    public ItemBuilder(String title, String type) {
        super(title, type, new Item<I>());
    }

    public ItemBuilder(String title, String id, String type) {
        super(title, id, type, new Item<I>());
    }
}
