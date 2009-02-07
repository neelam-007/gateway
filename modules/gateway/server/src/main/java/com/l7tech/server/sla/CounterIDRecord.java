/**
 * LAYER 7 TECHNOLOGIES, INC<br/>
 *
 * User: flascell<br/>
 * Date: Apr 4, 2005<br/>
 */
package com.l7tech.server.sla;

import org.hibernate.annotations.GenericGenerator;
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
public class CounterIDRecord implements Serializable {
    public static final String UNIDENTIFIED_USER = "*";
    public long counterId = -1;
    public String userId = UNIDENTIFIED_USER;
    public long providerId = -1;
    public String counterName;

    @Id
    @GeneratedValue(strategy= GenerationType.SEQUENCE, generator="generator")
    @GenericGenerator( name="generator", strategy = "hilo" )
    public long getCounterId() {
        return counterId;
    }

    public void setCounterId(long counterId) {
        this.counterId = counterId;
    }

    @Column(name="userid", nullable=false, length=128)
    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        if (userId == null) this.userId = UNIDENTIFIED_USER;
        else this.userId = userId;
    }

    @Column(name="providerid", nullable=false)
    public long getProviderId() {
        return providerId;
    }

    public void setProviderId(long providerId) {
        this.providerId = providerId;
    }

    @Column(name="countername", nullable=false, length=128)
    public String getCounterName() {
        return counterName;
    }

    public void setCounterName(String counterName) {
        this.counterName = counterName;
    }
}
