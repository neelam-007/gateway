package com.l7tech.objectmodel.imp;

import com.l7tech.objectmodel.NamedEntity;

public abstract class NamedEntityImp extends EntityImp implements NamedEntity {
    public String getName() { return _name; }
    public void setName( String name ) { _name = name; }

    public int getVersion() { return _version; }
    public void setVersion(int version) { _version = version; }

    public long getLoadTime() {
        return _loadTime;
    }

    public void setLoadTime(long loadTime) {
        _loadTime = loadTime;
    }

    protected String _name;
    protected int _version;

    protected transient long _loadTime;
}
