package com.l7tech.portal.metrics;

public abstract class BaseMapping {
    private long id;
    private String digested;
    private Long createTime;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getDigested() {
        return digested;
    }

    public void setDigested(String digested) {
        this.digested = digested;
    }

    public Long getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Long createTime) {
        this.createTime = createTime;
    }
}
