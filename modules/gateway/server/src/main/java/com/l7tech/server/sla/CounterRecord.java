package com.l7tech.server.sla;

import com.l7tech.objectmodel.imp.PersistentEntityImp;
import org.hibernate.annotations.Proxy;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * This class is meant for hibernate serialization purposes only.
 *
 * @author flascelles@layer7-tech.com
 */
@Entity
@Proxy(lazy=false)
@Table(name="counters")
public class CounterRecord extends PersistentEntityImp {
    public String counterName;

    @Column(name="countername", nullable=false, length=255)
    public String getCounterName() {
        return counterName;
    }

    public void setCounterName(String counterName) {
        this.counterName = counterName;
    }
}