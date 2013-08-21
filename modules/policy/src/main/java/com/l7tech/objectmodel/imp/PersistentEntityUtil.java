package com.l7tech.objectmodel.imp;

import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.PersistentEntity;
import com.l7tech.util.Functions.Unary;
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
