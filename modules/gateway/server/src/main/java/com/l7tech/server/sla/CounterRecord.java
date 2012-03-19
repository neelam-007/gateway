package com.l7tech.server.sla;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Proxy;

import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Column;
import java.io.Serializable;

/**
 * This class is meant for hibernate serialization purposes only.
 *
 * @author flascelles@layer7-tech.com
 */
@Entity
@Proxy(lazy=false)
@Table(name="counters")
public class CounterRecord implements Serializable {
    public long counterId = -1L;
    public String counterName;

    @Id
    @GenericGenerator( name="generator", strategy = "layer7-generator" )
    @GeneratedValue( generator = "generator")
    public long getCounterId() {
        return counterId;
    }

    public void setCounterId(long counterId) {
        this.counterId = counterId;
    }

    @Column(name="countername", nullable=false, length=255)
    public String getCounterName() {
        return counterName;
    }

    public void setCounterName(String counterName) {
        this.counterName = counterName;
    }
}