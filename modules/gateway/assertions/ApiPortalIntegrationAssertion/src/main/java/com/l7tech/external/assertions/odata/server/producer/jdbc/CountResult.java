package com.l7tech.external.assertions.odata.server.producer.jdbc;

import org.odata4j.producer.CountResponse;

public class CountResult implements CountResponse {

    private long count = 0;

    public CountResult(long count) {
        this.count = count;
    }

    public void setCount(long count) {
        this.count = count;
    }

    @Override
    public long getCount() {
        return count;
    }
}
