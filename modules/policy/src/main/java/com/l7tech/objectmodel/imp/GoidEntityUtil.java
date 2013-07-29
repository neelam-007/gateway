package com.l7tech.objectmodel.imp;

import com.l7tech.objectmodel.Goid;
import com.l7tech.util.Functions.Unary;
import org.jetbrains.annotations.Nullable;

/**
 * Helper class for locking GoidEntityImp, without forcing public lock()/isLocked() methods to be available
 * in all subclasses of GoidEntityImp.
 */
public class GoidEntityUtil {

    /**
     * Check whether the specified entity is a PersistentEntityImp that is locked.
     *
     * @param pe entity to examine.  May be null.
     * @return true iff. this is a GoidEntityImp that is locked.
     */
    public static boolean isLocked(@Nullable GoidEntityImp pe) {
        if (pe instanceof GoidEntityImp) {
            GoidEntityImp imp = (GoidEntityImp) pe;
            return imp.isLocked();
        }
        return false;
    }

    /**
     * Lock the specified entity, if it is a GoidEntityImp.
     *
     * @param pe the entity to lock.  May be null.
     * @return current lock state of entity after this method.  A false return means the entity could not be locked by this method.
     */
    public static boolean lock(@Nullable GoidEntityImp pe) {
        if (pe instanceof GoidEntityImp) {
            GoidEntityImp imp = (GoidEntityImp) pe;
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
    public static Unary<Goid,GoidEntityImp> goid() {
        return new Unary<Goid,GoidEntityImp>(){
            @Override
            public Goid call( final GoidEntityImp entity ) {
                return entity == null ?  null : entity.getGoid();
            }
        };
    }

}
