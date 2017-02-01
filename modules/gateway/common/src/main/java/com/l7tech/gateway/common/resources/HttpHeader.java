package com.l7tech.gateway.common.resources;

import com.l7tech.objectmodel.imp.PersistentEntityImp;
import org.hibernate.annotations.Proxy;

import javax.persistence.*;

/**
 * A rule that determines if an http header or parameter should be forwarded, and if so,
 * which fullValue should it have.
 * <p/>
 * <p/>
 * <br/><br/>
 * CA Technologies, INC<br/>
 * User: Ekta<br/>
 * Date: June 18, 2016<br/>
 */
@Entity
@Proxy(lazy=false)
@Table(name="http_header")
public class HttpHeader extends PersistentEntityImp implements com.l7tech.common.http.HttpHeader {

    public HttpHeader() {
    }

    public HttpHeader(String name, String fullValue) {
        this.name = name;
        this.fullValue = fullValue;
    }

    @Column(name="name")
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Column(name="fullValue")
    public String getFullValue() {
        return fullValue;
    }

    public void setFullValue(String value) {
        this.fullValue = value;
    }

    @ManyToOne()
    @JoinColumn(name = "http_configuration_goid", nullable=false)
    public HttpConfiguration getHttpConfiguration() {
        return httpConfiguration;
    }

    public void setHttpConfiguration(HttpConfiguration httpConfiguration) {
        this.httpConfiguration = httpConfiguration;
    }

    @SuppressWarnings("RedundantIfStatement")
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        HttpHeader that = (HttpHeader) o;

        if (name != null ? !name.equals(that.name) : that.name != null) return false;
        if (fullValue != null ? !fullValue.equals(that.fullValue) : that.fullValue != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (fullValue != null ? fullValue.hashCode() : 0);
        return result;
    }

    private String name;
    private String fullValue;
    private HttpConfiguration httpConfiguration;
}
