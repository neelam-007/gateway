package com.l7tech.server.tomcat;

import org.apache.catalina.Loader;
import org.apache.catalina.Container;

import java.beans.PropertyChangeSupport;
import java.beans.PropertyChangeListener;

/** A Loader that delegates to the specified ClassLoader and contains no repositories. */
public class ClassLoaderLoader implements Loader {
    private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);
    private final ClassLoader classLoader;
    private Container container;
    private boolean delegate;
    private String info = getClass().getName() + "/1.0";
    private boolean reloadable;

    public ClassLoaderLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    public void backgroundProcess() {
    }

    public ClassLoader getClassLoader() {
        return classLoader;
    }

    public Container getContainer() {
        return container;
    }

    public void setContainer(Container container) {
        if (container != this.container) pcs.firePropertyChange("container", this.container, container);
        this.container = container;
    }

    public boolean getDelegate() {
        return delegate;
    }

    public void setDelegate(boolean delegate) {
        if (delegate != this.delegate) pcs.firePropertyChange("delegate", this.delegate, delegate);
        this.delegate = delegate;
    }

    public String getInfo() {
        return info;
    }

    public boolean getReloadable() {
        return reloadable;
    }

    public void setReloadable(boolean reloadable) {
        if (reloadable != this.reloadable) pcs.firePropertyChange("reloadable", this.reloadable, reloadable);
        this.reloadable = reloadable;
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        pcs.addPropertyChangeListener(listener);
    }

    public void addRepository(String repository) {
        // Ignored
    }

    public String[] findRepositories() {
        // None
        return new String[0];
    }

    public boolean modified() {
        return false;
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        pcs.removePropertyChangeListener(listener);
    }
}
