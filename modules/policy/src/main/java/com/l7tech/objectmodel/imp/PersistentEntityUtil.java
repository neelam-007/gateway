package com.l7tech.objectmodel.imp;

import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.PersistentEntity;
import com.l7tech.util.Functions.Unary;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Helper class for locking PersistentEntityImp, without forcing public lock()/isLocked() methods to be available
 * in all subclasses of PersistentEntityImp.
 */
public class PersistentEntityUtil {

    /**
     * Check whether the specified entity is a PersistentEntityImp that is locked.
     *
     * @param ge entity to examine.  May be null.
     * @return true iff. this is a PersistentEntityImp that is locked.
     */
    public static boolean isLocked(@Nullable PersistentEntity ge) {
        if (ge instanceof PersistentEntityImp) {
            PersistentEntityImp imp = (PersistentEntityImp) ge;
            return imp.isLocked();
        }
        return false;
    }

    /**
     * Lock the specified entity, if it is a PersistentEntityImp.
     *
     * @param ge the entity to lock.  May be null.
     * @return current lock state of entity after this method.  A false return means the entity could not be locked by this method.
     */
    public static boolean lock(@Nullable PersistentEntity ge) {
        if (ge instanceof PersistentEntityImp) {
            PersistentEntityImp imp = (PersistentEntityImp) ge;
            imp.lock();
            return true;
        }
        return false;
    }

    /**
     * Check if this entities id should be preserved on save.
     *
     * @param ge The entity to check.
     * @return true if this entities id should be preserved on save.
     */
    public static boolean isPreserveId(@NotNull PersistentEntity ge) {
        if (ge instanceof PersistentEntityImp) {
            PersistentEntityImp imp = (PersistentEntityImp) ge;
            return imp.isPreserveId();
        }
        return false;
    }

    /**
     * Preserve this entities id on save.
     *
     * @param ge The entity who's id to preserve.
     * @return true if the preserve entity id property is properly set on the entity.
     */
    public static boolean preserveId(@NotNull PersistentEntity ge) {
        if (ge instanceof PersistentEntityImp) {
            PersistentEntityImp imp = (PersistentEntityImp) ge;
            imp.preserveId();
            return true;
        }
        return false;
    }

    /**
     * First class function for object identifier access.
     *
     * @return A function to access the identifier of an entity
     */
    public static Unary<Goid,PersistentEntity> goid() {
        return new Unary<Goid,PersistentEntity>(){
            @Override
            public Goid call( final PersistentEntity entity ) {
                return entity == null ?  null : entity.getGoid();
            }
        };
    }

}
