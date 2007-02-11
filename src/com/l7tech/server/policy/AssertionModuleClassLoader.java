package com.l7tech.server.policy;

import com.l7tech.common.util.ExceptionUtils;

import java.net.URLClassLoader;
import java.net.URL;
import java.util.Set;
import java.util.Collections;
import java.util.HashSet;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import java.io.File;

/**
 * A URLClassloader that keeps track of loaded classes so they can be notified when it is time to unload them.
 */
class AssertionModuleClassLoader extends URLClassLoader {
    protected static final Logger logger = Logger.getLogger(AssertionModuleClassLoader.class.getName());

    /** Classes that have been loaded from this module. */
    private final Set<Class> classes = Collections.synchronizedSet(new HashSet<Class>());
    private final String moduleName;

    public AssertionModuleClassLoader(String moduleName, URL jarUrl, ClassLoader parent) {
        super(new URL[] { jarUrl }, parent);
        this.moduleName = moduleName;
    }

    protected Class<?> findClass(final String name) throws ClassNotFoundException {
        final Class<?> found = super.findClass(name);
        classes.add(found);
        return found;
    }

    /**
     * Notify any interested classes that their module is being unloaded and they should dismantle any
     * datastructures that would keep their instances from being collected
     */
    void onModuleUnloaded() {
        Set<Class> toNotify = new HashSet<Class>(classes);
        for (Class clazz : toNotify) {
            onModuleUnloaded(clazz);
            classes.remove(clazz);
        }
    }

    /**
     * Notify the specified class that its module is being unloaded and it should dismantle any datastructures
     * that would keep its instances from being collected.  Classes are assumed to be interested in such notfication
     * only if they have a public static method "onModuleUnloaded" that takes no arguments and returns void.
     * <p/>
     * Otherwise, they need to register as an application listener and watch for an AssertionModuleUnregisteredEvent
     * that pertains to them.
     *
     * @param clazz the class to notify.  If this is null, or does not include a public onModuleUnloaded static
     *        method, this method takes no action.
     */
    private void onModuleUnloaded(Class clazz) {
        try {
            clazz.getMethod("onModuleUnloaded").invoke(null);
        } catch (NoSuchMethodException e) {
            // Ok, it doesn't care to be notified
        } catch (IllegalAccessException e) {
            logger.log(Level.WARNING, "Module " + moduleName + ": unable to notify class " + clazz.getName() + " of module unload: " + ExceptionUtils.getMessage(e), e);
        } catch (InvocationTargetException e) {
            logger.log(Level.SEVERE, "Module " + moduleName + ": exception while notifying class " + clazz.getName() + " of module unload: " + ExceptionUtils.getMessage(e), e);
        }
    }
}
