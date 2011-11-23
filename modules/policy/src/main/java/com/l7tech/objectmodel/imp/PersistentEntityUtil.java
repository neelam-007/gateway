package com.l7tech.objectmodel.imp;

import com.l7tech.objectmodel.PersistentEntity;
import org.jetbrains.annotations.Nullable;

/**
 * Helper class for locking PersistentEntityImp, without forcing public lock()/isLocked() methods to be available
 * in all subclasses of PersistentEntityImp.
 */
public class PersistentEntityUtil {

    /**
     * Check whether the specified entity is a PersistentEntityImp that is locked.
     *
     * @param pe entity to examine.  May be null.
     * @return true iff. this is a PersistentEntityImp that is locked.
     */
    public static boolean isLocked(@Nullable PersistentEntity pe) {
        if (pe instanceof PersistentEntityImp) {
            PersistentEntityImp imp = (PersistentEntityImp) pe;
            return imp.isLocked();
        }
        return false;
    }

    /**
     * Lock the specified entity, if it is a PersistentEntityImp.
     *
     * @param pe the entity to lock.  May be null.
     * @return current lock state of entity after this method.  A false return means the entity could not be locked by this method.
     */
    public static boolean lock(@Nullable PersistentEntity pe) {
        if (pe instanceof PersistentEntityImp) {
            PersistentEntityImp imp = (PersistentEntityImp) pe;
            imp.lock();
            return true;
        }
        return false;
    }
}
