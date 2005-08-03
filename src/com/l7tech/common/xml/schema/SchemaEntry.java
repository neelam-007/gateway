/**
 * LAYER 7 TECHNOLOGIES, INC<br/>
 *
 * User: flascell<br/>
 * Date: Jul 14, 2005<br/>
 */
package com.l7tech.common.xml.schema;

import java.io.Serializable;

/**
 * A row in the communityschema table. These xml schemas are meant to define additional
 * elements that are potentially common to more than one services such as envelope schemas
 * and schemas for stuff that end up in soap headers.
 *
 * @author flascelles@layer7-tech.com
 */
public class SchemaEntry implements Serializable {
    public long getOid() {
        return oid;
    }

    public void setOid(long oid) {
        this.oid = oid;
    }

    public String getSchema() {
        return schema;
    }

    public void setSchema(String schema) {
        this.schema = schema;
    }

    public String getTns() {
        return tns;
    }

    public void setTns(String tns) {
        this.tns = tns;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String toString() {
        return "SchemaEntry oid:" + oid + " schema: " + schema;
    }

    private long oid = -1;
    private String schema;
    private String tns;
    private String name;
}
