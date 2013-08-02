package com.l7tech.objectmodel.imp;

import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.GoidEntity;
import com.l7tech.util.Functions.Unary;
import org.jetbrains.annotations.Nullable;

/**
 * Helper class for locking GoidEntityImp, without forcing public lock()/isLocked() methods to be available
 * in all subclasses of GoidEntityImp.
 */
public class GoidEntityUtil {

    /**
     * Check whether the specified entity is a GoidEntityImp that is locked.
     *
     * @param ge entity to examine.  May be null.
     * @return true iff. this is a GoidEntityImp that is locked.
     */
    public static boolean isLocked(@Nullable GoidEntity ge) {
        if (ge instanceof GoidEntityImp) {
            GoidEntityImp imp = (GoidEntityImp) ge;
            return imp.isLocked();
        }
        return false;
    }

    /**
     * Lock the specified entity, if it is a GoidEntityImp.
     *
     * @param ge the entity to lock.  May be null.
     * @return current lock state of entity after this method.  A false return means the entity could not be locked by this method.
     */
    public static boolean lock(@Nullable GoidEntity ge) {
        if (ge instanceof GoidEntityImp) {
            GoidEntityImp imp = (GoidEntityImp) ge;
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
    public static Unary<Goid,GoidEntity> goid() {
        return new Unary<Goid,GoidEntity>(){
            @Override
            public Goid call( final GoidEntity entity ) {
                return entity == null ?  null : entity.getGoid();
            }
        };
    }

}
