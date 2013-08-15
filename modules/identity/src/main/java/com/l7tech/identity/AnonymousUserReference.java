/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.identity;

import com.l7tech.objectmodel.AnonymousEntityReference;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.migration.Migration;
import com.l7tech.objectmodel.migration.PropertyResolver;
import static com.l7tech.objectmodel.migration.MigrationMappingSelection.NONE;

/**
 * @author alex
 */
public class AnonymousUserReference extends AnonymousEntityReference implements User {
    private final UserBean userBean;

    public AnonymousUserReference(String uniqueId, Goid providerOid, String name) {
        super(User.class, uniqueId, name);
        this.userBean = new UserBean(providerOid, null);
        this.userBean.setUniqueIdentifier(uniqueId);
    }

    @Migration(mapName = NONE, mapValue = NONE, export = false, resolver = PropertyResolver.Type.ID_PROVIDER_CONFIG)
    public Goid getProviderId() {
        return userBean.getProviderId();
    }

    public boolean isEquivalentId(Object thatId) {
        return thatId != null && thatId.equals(uniqueId);
    }

    public String getId() {
        return userBean.getId();
    }

    public String getLogin() {
        return userBean.getLogin();
    }

    public String getFirstName() {
        return userBean.getFirstName();
    }

    public String getLastName() {
        return userBean.getLastName();
    }

    public String getEmail() {
        return userBean.getEmail();
    }

    public String getDepartment() {
        return userBean.getDepartment();
    }

    public String getSubjectDn() {
        return userBean.getSubjectDn();
    }

    public UserBean getUserBean() {
        return userBean;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AnonymousUserReference that = (AnonymousUserReference) o;

        if (userBean != null ? !userBean.equals(that.userBean) : that.userBean != null) return false;
        final String thisId = getId();
        final String thatId = that.getId();
        return !(thisId != null ? !thisId.equals(thatId) : thatId != null);
    }

    public int hashCode() {
        int result;
        result = (userBean != null ? userBean.hashCode() : 0);
        final String thisId = getId();
        result = 31 * result + (thisId != null ? thisId.hashCode() : 0);
        return result;
    }
}
