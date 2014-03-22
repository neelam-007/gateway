package com.l7tech.policy;

import com.l7tech.objectmodel.imp.NamedEntityImp;
import com.l7tech.security.rbac.RbacAttribute;
import org.hibernate.annotations.Proxy;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;


@XmlRootElement(name = "CustomKeyValueStore")
@XmlType(propOrder = {"value"})
@Entity
@Proxy(lazy=false)
@Inheritance(strategy= InheritanceType.SINGLE_TABLE)
@Table(name="custom_key_value_store")
public class CustomKeyValueStore extends NamedEntityImp {

    private byte[] value;

    @NotNull
    @Column(name="value", nullable=false, length=Integer.MAX_VALUE)
    @Lob
    public final byte[] getValue() {
        return value;
    }

    public final void setValue(byte[] value) {
        this.value = value;
    }

    @RbacAttribute
    @Size(max = 128)
    @Override
    @Transient
    public String getName() {
        return super.getName();
    }
}