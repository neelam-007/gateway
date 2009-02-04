package com.l7tech.server.audit;

/**
 *
 */
public class AuditContextUtils {

    //- PUBLIC

    /**
     * Is the current record a system generated record?
     *
     * @return true if system
     */
    public static boolean isSystem() {
        return systemThreadLocal.get();
    }

    /**
     * Sets whether the current record is a system generated record.
     *
     * @param system true if system.
     */
    public static void setSystem(boolean system) {
        systemThreadLocal.set( system );
    }

    //- PRIVATE

    private static final ThreadLocal<Boolean> systemThreadLocal = new ThreadLocal<Boolean>(){
        @Override
        protected Boolean initialValue() {
            return false;
        }
    };
}
