/**
 * LAYER 7 TECHNOLOGIES, INC<br/>
 *
 * User: flascell<br/>
 * Date: Jul 14, 2005<br/>
 */
package com.l7tech.gateway.common.schema;

import com.l7tech.objectmodel.imp.NamedEntityImp;

import javax.persistence.Transient;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Column;
import javax.persistence.Lob;

import org.hibernate.annotations.Proxy;

/**
 * A row in the communityschema table. These xml schemas are meant to define additional
 * elements that are potentially common to more than one services such as envelope schemas
 * and schemas for stuff that end up in soap headers.
 *
 * @author flascelles@layer7-tech.com
 */
@Entity
@Proxy(lazy=false)
@Table(name="community_schemas")
@XmlRootElement
public class SchemaEntry extends NamedEntityImp {

    @Size(max=128)
    @Transient
    @Override
    public String getName() {
        return super.getName();
    }

    /** @return the schema text, which may be empty or null. */
    @NotNull
    @Size(min=0,max=5242880)
    @Column(name="schema_xml", length=Integer.MAX_VALUE)
    @Lob
    public String getSchema() {
        // Protect legacy code now that this field is nullable
        return schema != null ? schema : "";
    }

    public void setSchema(String schema) {
        this.schema = schema;
    }

    @Size(min=0,max=128)
    @Column(name="tns", length=128)
    public String getTns() {
        return tns;
    }

    public void setTns(String tns) {
        this.tns = tns;
    }

    @Column(name="system")
    public boolean isSystem() {
        return system;
    }

    public void setSystem(boolean system) {
        this.system = system;
    }

    public String toString() {
        return "SchemaEntry oid:" + getOid() + " schema: " + schema;
    }

    public SchemaEntry asCopy() {
        SchemaEntry copy = new SchemaEntry();
        copy.setName(getName());
        copy.setTns(getTns());
        copy.setSchema(getSchema());

        return copy;
    }

    private String schema;
    private String tns;
    private boolean system;
}
