package com.l7tech.portal.metrics;

public abstract class BaseMetric {
    private int attempted;
    private int authorized;
    private int completed;
    private Integer backMin;
    private Integer backMax;
    private int backSum;
    private Integer frontMin;
    private Integer frontMax;
    private int frontSum;

    public int getAttempted() {
        return attempted;
    }

    public void setAttempted(int attempted) {
        this.attempted = attempted;
    }

    public int getAuthorized() {
        return authorized;
    }

    public void setAuthorized(int authorized) {
        this.authorized = authorized;
    }

    public int getCompleted() {
        return completed;
    }

    public void setCompleted(int completed) {
        this.completed = completed;
    }

    public Integer getBackMin() {
        return backMin;
    }

    public void setBackMin(Integer backMin) {
        this.backMin = backMin;
    }

    public Integer getBackMax() {
        return backMax;
    }

    public void setBackMax(Integer backMax) {
        this.backMax = backMax;
    }

    public int getBackSum() {
        return backSum;
    }

    public void setBackSum(int backSum) {
        this.backSum = backSum;
    }

    public Integer getFrontMin() {
        return frontMin;
    }

    public void setFrontMin(Integer frontMin) {
        this.frontMin = frontMin;
    }

    public Integer getFrontMax() {
        return frontMax;
    }

    public void setFrontMax(Integer frontMax) {
        this.frontMax = frontMax;
    }

    public int getFrontSum() {
        return frontSum;
    }

    public void setFrontSum(int frontSum) {
        this.frontSum = frontSum;
    }
}
