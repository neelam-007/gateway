/**
* Copyright (C) 2008, Layer 7 Technologies Inc.
* User: darmstrong
* Date: Nov 18, 2008
* Time: 11:29:34 AM
*/
package com.l7tech.server.ems.standardreports;

public class ReportTotalBean{
    private String name;
    private Long value;

    public ReportTotalBean(String name, Long value) {
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public Long getValue() {
        return value;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setValue(Long value) {
        this.value = value;
    }
}