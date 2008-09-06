/**
 * LAYER 7 TECHNOLOGIES, INC<br/>
 *
 * User: flascell<br/>
 * Date: Jul 14, 2005<br/>
 */
package com.l7tech.gateway.common.schema;

import com.l7tech.objectmodel.imp.NamedEntityImp;

import javax.xml.bind.annotation.XmlRootElement;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Column;
import javax.persistence.Lob;

/**
 * A row in the communityschema table. These xml schemas are meant to define additional
 * elements that are potentially common to more than one services such as envelope schemas
 * and schemas for stuff that end up in soap headers.
 *
 * @author flascelles@layer7-tech.com
 */
@Entity
@Table(name="community_schemas")
@XmlRootElement
public class SchemaEntry extends NamedEntityImp {

    /** @return the schema text, which may be empty or null. */
    @Column(name="schema_xml", length=Integer.MAX_VALUE)
    @Lob
    public String getSchema() {
        // Protect legacy code now that this field is nullable
        return schema != null ? schema : "";
    }

    public void setSchema(String schema) {
        this.schema = schema;
    }

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

    private String schema;
    private String tns;
    private boolean system;
}
