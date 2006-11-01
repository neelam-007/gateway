/**
 * LAYER 7 TECHNOLOGIES, INC<br/>
 *
 * User: flascell<br/>
 * Date: Jul 14, 2005<br/>
 */
package com.l7tech.common.xml.schema;

import com.l7tech.objectmodel.imp.NamedEntityImp;

/**
 * A row in the communityschema table. These xml schemas are meant to define additional
 * elements that are potentially common to more than one services such as envelope schemas
 * and schemas for stuff that end up in soap headers.
 *
 * @author flascelles@layer7-tech.com
 */
public class SchemaEntry extends NamedEntityImp {

    /** @return the schema text, which may be empty or null. */
    public String getSchema() {
        // Protect legacy code now that this field is nullable
        return schema != null ? schema : "";
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
