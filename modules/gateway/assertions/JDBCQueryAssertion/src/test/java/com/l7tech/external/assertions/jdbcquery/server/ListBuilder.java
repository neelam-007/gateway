package com.l7tech.external.assertions.jdbcquery.server;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by drace01 on 3/24/2016.
 */
class ListBuilder<T> {

    private List<T> list;

    @SuppressWarnings("unchecked")
    ListBuilder() {
        list = new ArrayList<>();
    }

    ListBuilder<T> add(final T element) {
        list.add(element);
        return this;
    }

    List<T> build() {
        return list;
    }
}
