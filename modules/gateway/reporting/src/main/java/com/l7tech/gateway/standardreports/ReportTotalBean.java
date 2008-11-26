/**
* Copyright (C) 2008, Layer 7 Technologies Inc.
* User: darmstrong
* Date: Nov 18, 2008
* Time: 11:29:34 AM
*/
package com.l7tech.gateway.standardreports;

public class ReportTotalBean implements Comparable{
    private String name;
    private Long value;
    private Integer index;

    public ReportTotalBean(String name, Long value) {
        if(name == null || name.equals("")){
            throw new IllegalArgumentException("name must be non null and non empty");
        }
        if(name.lastIndexOf(" ") == -1){
            throw new IllegalArgumentException("name must be a value followed by a space followed by an integer, like 'Group 1'.");
        }

        String beanIndex = name.substring(name.lastIndexOf(" ")+1, name.length());
        try{
            index = Integer.valueOf(beanIndex);
        }catch (NumberFormatException nfe){
            throw new IllegalArgumentException("name must be a value followed by a space followed by a number, like 'Group 1'");
        }

        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public Long getValue() {
        return value;
    }

    public Integer getIndex() {
        return index;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setValue(Long value) {
        this.value = value;
    }


    public int compareTo(Object o) {
        ReportTotalBean rtb = (ReportTotalBean) o;
        return index.compareTo(rtb.getIndex());
    }
}