/**
 * LAYER 7 TECHNOLOGIES, INC<br/>
 *
 * User: flascell<br/>
 * Date: Jul 14, 2005<br/>
 */
package com.l7tech.gateway.common.schema;

import com.l7tech.objectmodel.imp.NamedEntityImp;
import com.l7tech.util.Charsets;
import com.l7tech.util.HexUtils;
import org.hibernate.annotations.Proxy;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * A row in the communityschema table. These xml schemas are meant to define additional
 * elements that are potentially common to more than one services such as envelope schemas
 * and schemas for stuff that end up in soap headers.
 * <p/>
 * All imports from a schema, which are successfully imported, are also added to the community_schemas table
 * (includes are not)
 * <p/>
 * The name is unqiue, however this is enforced by the name_hash property
 *
 * @author flascelles@layer7-tech.com
 * @author darmstrong
 */
@Entity
@Proxy(lazy = false)
@Table(name = "community_schemas")
@XmlRootElement
public class SchemaEntry extends NamedEntityImp {
    public final static int MAX_NAME_LENGTH = 4096;

    @Size(min = 1, max = MAX_NAME_LENGTH)
    @Transient
    @Override
    public String getName() {
        return super.getName();
    }

    /**
     * Set the name of the schema. This is known as a 'system id' in the SSM, and can often be a URL value, from where
     * an imported schema originated
     *  
     * @param name required. Cannot be null, but can be the emtpy String
     */
    @Override
    public void setName(String name) {
        if(name == null) throw new NullPointerException("name cannot be null");
        
        super.setName(name);
        String newHash = createNameHash(name);
        setNameHash(newHash);
    }

    /**
     * @return the schema text, which may be empty or null.
     */
    @NotNull
    @Size(min = 0, max = 5242880)
    @Column(name = "schema_xml", length = Integer.MAX_VALUE)
    @Lob
    public String getSchema() {
        // Protect legacy code now that this field is nullable
        return schema != null ? schema : "";
    }

    public void setSchema(String schema) {
        this.schema = schema;
    }

    @Size(min = 0, max = 128)
    @Column(name = "tns", length = 128)
    public String getTns() {
        return tns;
    }

    public void setTns(String tns) {
        if(tns == null){
            this.tns = "";
        }else{
            this.tns = tns;
        }
    }

    @Column(name = "system")
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

    public static String createNameHash(String name) {
        return HexUtils.encodeBase64(HexUtils.getSha512Digest(name.getBytes(Charsets.UTF8)), true);
    }

    // - PACKAGE
    
    @NotNull
    @Size(max = 128)
    @Column(name = "name_hash", length = 128, nullable = false)
    String getNameHash() {
        return nameHash;
    }

    void setNameHash(String nameHash) {
        String calculatedHash = createNameHash(getName());
        //being defensive, validate the hash being set
        //On upgrade nameHash equals getName, in which case allow the value to be set
        //this property is package private, so no callers should set it directly, if it is set directly to a value
        //which matches the name, then the schema will never be found
        if (!calculatedHash.equals(nameHash) && !nameHash.equals(getName()))
            throw new IllegalArgumentException("Hash value must match the generated hash of name property");

        this.nameHash = nameHash;
    }

    // - PRIVATE

    private String schema;
    private String tns;
    private boolean system;
    private String nameHash;
}