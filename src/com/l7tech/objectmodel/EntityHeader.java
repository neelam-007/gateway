/*
 * Layer 7 Technologies, inc.
 * User: flascelles
 * Date: May 9, 2003
 *
 * $Id$
 */

package com.l7tech.objectmodel;

import com.l7tech.objectmodel.imp.NamedEntityImp;

/**
 * Header objects are used to refer to objects in find methods
 * of all managers
 * @version $Revision$
 * @author flascelles
 */
public class EntityHeader extends NamedEntityImp {

    public EntityHeader(long oid, EntityType type, String name, String description) {
        setOid(oid);
        setType(type);
        setName(name);
        setDescription(description);
    }

    public EntityHeader() {
        type = EntityType.UNDEFINED;
        description = "";
    }

    public EntityType getType() {
        return type;
    }

    public void setType(EntityType type) {
        this.type = type;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String toString() {
        return "EntityHeader. Name=" + getName() + ", oid=" + getOid() + ", description=" + description;
    }

    // ************************************************
    // PRIVATES
    // ************************************************
    private EntityType type;
    private String description;
}
