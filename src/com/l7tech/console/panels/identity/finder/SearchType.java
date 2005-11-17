package com.l7tech.console.panels.identity.finder;

import com.l7tech.objectmodel.EntityType;

/**
 * Created by IntelliJ IDEA.
 * User: alex
 * Date: 7-Oct-2005
 * Time: 5:53:33 PM
 * To change this template use File | Settings | File Templates.
 */
public class SearchType {
    public static final SearchType USER = new SearchType("Users", new EntityType[]{EntityType.USER});
    public static final SearchType GROUP = new SearchType("Groups", new EntityType[]{EntityType.GROUP});
    public static final SearchType ALL = new SearchType("ALL", new EntityType[]{EntityType.GROUP, EntityType.USER});

    private SearchType(String name, EntityType[] t) {
        this.name = name;
        this.types = t;
    }

    public String getName() {
        return name;
    }

    public EntityType[] getTypes() {
        return types;
    }

    EntityType[] types;
    String name;
}
