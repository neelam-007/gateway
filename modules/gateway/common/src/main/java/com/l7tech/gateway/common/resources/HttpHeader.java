package com.l7tech.gateway.common.resources;

import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.imp.PersistentEntityImp;
import org.hibernate.annotations.Proxy;

import javax.persistence.*;

/**
 * A rule that determines if an http header or parameter should be forwarded, and if so,
 * which value should it have.
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
public class HttpHeader extends PersistentEntityImp {

    public HttpHeader() {
    }

    public HttpHeader(String name, String value) {
        this.name = name;
        this.value = value;
    }

    @Column(name="name")
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Column(name="value")
    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        if (value == null) {
            value = "";
        }
        this.value = value;
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
        if (!super.equals(o)) return false;

        HttpHeader that = (HttpHeader) o;

        if (getGoid() != null ? !getGoid().equals(that.getGoid()) : that.getGoid() != null) return false;
        if (name != null ? !name.equals(that.name) : that.name != null) return false;
        if (value != null ? !value.equals(that.value) : that.value != null) return false;
        if ((httpConfiguration == null) != (that.httpConfiguration == null)) return false;
        if (httpConfiguration != null && !Goid.equals(httpConfiguration.getGoid(), that.httpConfiguration.getGoid())) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (value != null ? value.hashCode() : 0);
        result = 31 * result + (httpConfiguration != null && httpConfiguration.getGoid() != null ? httpConfiguration.getGoid().hashCode() : 0);
        return result;
    }

    @Override
    @Version
    @Column(name="version")
    public int getVersion() {
        return super.getVersion();
    }

    private String name;
    private String value;
    private HttpConfiguration httpConfiguration;
}
