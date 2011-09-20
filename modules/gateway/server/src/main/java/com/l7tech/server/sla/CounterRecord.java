package com.l7tech.server.sla;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.Proxy;

import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
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
    public long objectId = -1L;
    public String counterName;

    @Id
    @Column(name="objectid", nullable=false, updatable=false)
    @GeneratedValue(strategy= GenerationType.SEQUENCE, generator="generator")
    @GenericGenerator( name="generator", strategy = "seqhilo", parameters = @Parameter(name="max_lo", value="32767") )
    public long getObjectId() {
        return objectId;
    }

    public void setObjectId(long objectId) {
        this.objectId = objectId;
    }

    @Column(name="countername", nullable=false, length=255)
    public String getCounterName() {
        return counterName;
    }

    public void setCounterName(String counterName) {
        this.counterName = counterName;
    }
}