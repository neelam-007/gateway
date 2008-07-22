/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.security.prov.luna;

import com.l7tech.util.ExceptionUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Safely check if Luna is available and logged into a partition, without failing at class load time if the
 * Luna classes are not present in the classpath.
 */
public class LunaProber {
    private static final String LUNA_TOKEN_MANAGER_CLASS = "com.chrysalisits.crypto.LunaTokenManager";
    private static final String LUNA_KEY_CLASS = "com.chrysalisits.crypto.LunaKey";
    private static final String INCOMPAT = "Luna classes appear to be incompatible with the version we expect: ";

    private LunaProber() {}

    /**
     * Safely check if a connection to a Luna partition is available, without dying at class load time if
     * the Luna jars are not installed.
     *
     * @return true iff. a logged in Luna partition was created
     * @throws ClassNotFoundException if the Luna classes are not in the current classpath
     * @throws ClassNotFoundException if the Luna class version is not compatible with this code
     */
    public static boolean isPartitionLoggedIn() throws ClassNotFoundException {
        Class ltmClass = Class.forName(LUNA_TOKEN_MANAGER_CLASS);
        if (ltmClass == null) throw new ClassNotFoundException("null class: " + LUNA_TOKEN_MANAGER_CLASS); // can't happen
        try {
            Method ltmGetInstance = ltmClass.getMethod("getInstance", new Class[0]);
            Method isLoggedIn = ltmClass.getMethod("isLoggedIn", new Class[0]);
            Object ltm = ltmGetInstance.invoke(null, new Object[0]);
            Object result = isLoggedIn.invoke(ltm, new Object[0]);
            if (result == null)
                throw new ClassNotFoundException("Luna classes appear to be incompatible with the version we expect: LunaTokenManager.isLoggedIn() returned null");
            if (result instanceof Boolean) {
                Boolean aBoolean = (Boolean)result;
                return aBoolean.booleanValue();
            }
            throw new ClassNotFoundException("Luna classes appear to be incompatible with the version we expect: LunaTokenManager.isLoggedIn() returned instance of :" + result.getClass());
        } catch (NoSuchMethodException e) {
            throw new ClassNotFoundException(INCOMPAT + ExceptionUtils.getMessage(e), e);
        } catch (IllegalAccessException e) {
            throw new ClassNotFoundException(INCOMPAT + ExceptionUtils.getMessage(e), e);
        } catch (InvocationTargetException e) {
            throw new ClassNotFoundException(INCOMPAT + ExceptionUtils.getMessage(e), e);
        }
    }

    /**
     * Using the Luna API directly, locate a key handle using its alias.
     *
     * @param alias the alias to search for.  Must be non-null and non-empty.
     * @return the handle for a matching key object, or null if no matching key was found.
     * @throws ClassNotFoundException if the Luna classes are not in the current classpath
     * @throws ClassNotFoundException if the Luna class version is not compatible with this code
     */
    static Integer locateKeyHandleByAlias(String alias) throws ClassNotFoundException {
        return locateKeyHandleByAlias(alias, false);
    }

    /**
     * Using the Luna API, find and destroy the key with the specified alias.
     * If this method returns, the key was found and was
     * destroyed.
     *
     * @param alias the alias to search for.  Must not be null or empty.
     * @throws KeyNotFoundException   if no key matching this alias was found
     * @throws ClassNotFoundException if the Luna classes are not in the current classpath
     * @throws ClassNotFoundException if the Luna class version is not compatible with this code
     */
    static void destroyKeyByAlias(String alias) throws KeyNotFoundException, ClassNotFoundException {
        Integer got = locateKeyHandleByAlias(alias, true);
        if (got == null)
            throw new KeyNotFoundException("No key with the alias " + alias + " was found in the current security partition");
    }

    static class KeyNotFoundException extends Exception {
        public KeyNotFoundException() {}
        public KeyNotFoundException(String message) { super(message); }
        public KeyNotFoundException(String message, Throwable cause) { super(message, cause); }
        public KeyNotFoundException(Throwable cause) { super(cause); }
    }

    /**
     * If the Luna API is available, use the Luna API directly to locate a key handle using its alias.
     *
     * @param alias the alias to look for.  Must be non-null and non-empty.
     * @param destroyKey if true, the sought-after key will be destroyed if it is found.
     *                   In this case, the returned handle
     *                   serves strictly to indicate whether the sought-after handle did exist the instant
     *                   this method was called.
     * @return the object handle for this key within the current partition, or null if the key was not found.
     * @throws ClassNotFoundException if the Luna classes are not in the current classpath
     * @throws ClassNotFoundException if the Luna class version is not compatible with this code
     */
    private static Integer locateKeyHandleByAlias(String alias, boolean destroyKey) throws ClassNotFoundException {
        Class lunaKeyClass = Class.forName(LUNA_KEY_CLASS);
        if (lunaKeyClass == null) throw new ClassNotFoundException("null class: " + LUNA_KEY_CLASS); // can't happen
        try {
            Method locateKeyByAlias = lunaKeyClass.getMethod("LocateKeyByAlias", new Class[] { String.class });
            Method getKeyHandle = lunaKeyClass.getMethod("GetKeyHandle", new Class[0]);
            Method destroyKeyM = lunaKeyClass.getMethod("DestroyKey", new Class[0]);
            Object foundKeyObj = locateKeyByAlias.invoke(null, new Object[] { alias });

            // Did we find a key with this handle?
            if (foundKeyObj == null)
                return null; // nope, no key with that handle was found

            Object foundKeyHandle = getKeyHandle.invoke(foundKeyObj, new Object[0]);
            if (foundKeyHandle == null)
                throw new ClassNotFoundException(INCOMPAT + "LunaKey.GetKeyHandle() returned null");
            if (!(foundKeyHandle instanceof Integer))
                throw new ClassNotFoundException(INCOMPAT + "LunaKey.GetKeyHandle() returned instance of " + foundKeyHandle.getClass());
            Integer integerKeyHandle = (Integer)foundKeyHandle;

            // Are we supposed to destroy the key after we find it?
            if (destroyKey)
                destroyKeyM.invoke(foundKeyObj, new Object[0]);

            return integerKeyHandle;
        } catch (NoSuchMethodException e) {
            throw new ClassNotFoundException(INCOMPAT + ExceptionUtils.getMessage(e), e);
        } catch (IllegalAccessException e) {
            throw new ClassNotFoundException(INCOMPAT + ExceptionUtils.getMessage(e), e);
        } catch (InvocationTargetException e) {
            throw new ClassNotFoundException(INCOMPAT + ExceptionUtils.getMessage(e), e);
        }
    }
}
