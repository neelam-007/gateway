package com.l7tech.gateway.api;

import java.util.List;

/**
 * This is a reference builder. It assists in building references.
 */
public class ItemsListBuilder<I> extends ReferenceBuilder<List<Item<I>>,ItemsList<I>> {

    public ItemsListBuilder(ItemsList<I> reference) {
        super(reference);
    }

    public ItemsListBuilder(String title, String type) {
        super(title, type, new ItemsList<I>());
    }

    public ItemsListBuilder(String title, String id, String type) {
        super(title, type, id, new ItemsList<I>());
    }
}
