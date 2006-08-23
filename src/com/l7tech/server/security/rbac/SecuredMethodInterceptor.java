/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.server.security.rbac;

import com.l7tech.common.security.rbac.EntityType;
import com.l7tech.common.security.rbac.*;
import static com.l7tech.common.security.rbac.OperationType.*;
import com.l7tech.common.util.JaasUtils;
import com.l7tech.identity.User;
import com.l7tech.objectmodel.*;
import com.l7tech.server.EntityFinder;
import com.l7tech.server.event.EntityInvalidationEvent;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.commons.collections.iterators.ArrayIterator;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author alex
 */
public class SecuredMethodInterceptor implements MethodInterceptor, ApplicationListener {
    private static final Logger logger = Logger.getLogger(SecuredMethodInterceptor.class.getName());
    private final RoleManager roleManager;
    private final EntityFinder entityFinder;
    private static final String DEFAULT_ID = Long.toString(PersistentEntity.DEFAULT_OID);

    public SecuredMethodInterceptor(RoleManager roleManager, EntityFinder entityFinder) {
        this.roleManager = roleManager;
        this.entityFinder = entityFinder;
    }

    private List<Object> filter(Iterator iter, User user, CheckInfo check) throws FindException {
        List<Object> newlist = new ArrayList<Object>();
        while (iter.hasNext()) {
            Object element = iter.next();
            Entity testEntity;
            if (element instanceof Entity) {
                testEntity = (Entity)element;
            } else if (element instanceof EntityHeader) {
                EntityHeader header = (EntityHeader) element;
                if (header.getType() == com.l7tech.objectmodel.EntityType.MAXED_OUT_SEARCH_RESULT) {
                    // This one can get by without a permission check
                    testEntity = null;
                } else {
                    testEntity = (Entity)entityFinder.find(header);
                }
            } else {
                throw new IllegalArgumentException("Element of collection was neither Entity nor EntityHeader");
            }

            if (testEntity == null || roleManager.isPermittedForEntity(user, testEntity, check.operation, check.otherOperationName)) {
                newlist.add(element);
            } else {
                if (logger.isLoggable(Level.FINEST)) {
                    logger.log(Level.FINEST, 
                            "Omitting {0} #{1} from return value of {2}",
                            new Object[] {
                                testEntity.getClass().getSimpleName(),
                                testEntity.getId(),
                                check.methodName}
                            );
                }
            }
        }
        return newlist;
    }

    private static void collectAnnotations(Class clazz, List<Secured> annotations) {
        //noinspection unchecked
        Secured secured = (Secured) clazz.getAnnotation(Secured.class);
        if (secured != null) annotations.add(secured);
        for (Class intf : clazz.getInterfaces()) {
            collectAnnotations(intf, annotations);
        }
    }

    public Object invoke(MethodInvocation methodInvocation) throws Throwable {
        Object target = methodInvocation.getThis();
        Object[] args = methodInvocation.getArguments();
        Method method = methodInvocation.getMethod();

        String mname = target.getClass().getName() + "." + method.getName();
        logger.log(Level.FINE, "Intercepted invocation of {0}", mname);

        List<Secured> annotations = new ArrayList<Secured>();
        CheckInfo check;
        {
            Secured methodSecured = method.getAnnotation(Secured.class);
            if (methodSecured != null) annotations.add(methodSecured);

            collectAnnotations(target.getClass(), annotations);
            EntityType[] checkTypes = null;
            OperationType checkOperation = null;
            MethodStereotype checkStereotype = null;
            String checkOtherOperationName = null;
            int checkRelevantArg = -1;
            for (Secured secured : annotations) {
                if (secured.types() != null && checkTypes == null || (checkTypes.length == 1 && checkTypes[0] == EntityType.ANY)) {
                    checkTypes = secured.types();
                }

                if (secured.operation() != null && checkOperation == null)
                    checkOperation = secured.operation();

                if (secured.stereotype() != null && checkStereotype == null)
                    checkStereotype = secured.stereotype();

                if (secured.relevantArg() >= 0 && checkRelevantArg < 0)
                    checkRelevantArg = secured.relevantArg();

                if (secured.otherOperationName() != null && checkOtherOperationName == null)
                    checkOtherOperationName = secured.otherOperationName();
            }

            if (checkOperation == null && checkStereotype == null) throw new IllegalStateException("Security declaration for " + mname + " does not specify either an operation or a stereotype");
            if (checkTypes == null) throw new IllegalStateException("Security declaration for " + mname + " does not specify an entity type");

            check = new CheckInfo(mname, checkTypes, checkOperation, checkStereotype, checkRelevantArg, checkOtherOperationName);
        }

        User user = JaasUtils.getCurrentUser();
        if (user == null) throw new IllegalStateException("Secured method " + mname + " invoked with Subject containing no User principal");

        switch (check.operation) {
            case CREATE:
            case UPDATE:
                checkEntityBefore(check, args, UPDATE);
                break;
            case READ:
                checkEntityAfter(check);
                break;
            case DELETE:
                if (getEntityArg(check, args) != null) {
                    checkEntityBefore(check, args, DELETE);
                } else {
                    checkEntityFromId(check, args, DELETE);
                }
                break;
            case OTHER:
                if (check.otherOperationName == null || check.otherOperationName.length() == 0)
                    throw new IllegalStateException("Security declaration for method " +
                            mname + " specifies " + check.operation +
                            ", but does not specify otherOperationName");

                if (getEntityArg(check, args) != null) {
                    check.before = CheckBefore.ENTITY;
                } else {
                    getIdArgOrThrow(check, args);
                    check.before = CheckBefore.ID;
                }

                check.after = CheckAfter.NONE;
                break;
            default:
                switch (check.stereotype) {
                    case SAVE_OR_UPDATE:
                        Entity entity = getEntityArg(check, args);
                        if (entity != null) {
                            String id = entity.getId();
                            checkEntityBefore(check, args, id == null || DEFAULT_ID.equals(id) ? CREATE : UPDATE);
                            break;
                        } else {
                            // TODO this is incredibly ugly
                            check.before = CheckBefore.NONE;
                            check.after = CheckAfter.NONE;
                            for (EntityType type : check.types) {
                                if (!roleManager.isPermittedForAllEntities(user, type, UPDATE)) {
                                    throw new PermissionDeniedException(UPDATE, type);
                                }
                                if (!roleManager.isPermittedForAllEntities(user, type, CREATE)) {
                                    throw new PermissionDeniedException(CREATE, type);
                                }
                            }
                            return methodInvocation.proceed();
                        }
                    case FIND_ENTITIES:
                        check.before = CheckBefore.NONE;
                        check.operation = READ;
                        if (Collection.class.isAssignableFrom(method.getReturnType()) ||
                                method.getReturnType().isArray()) {
                            check.after = CheckAfter.COLLECTION;
                        } else {
                            // Unsupported return value type; must be able to read all
                            check.before = CheckBefore.ALL;
                        }
                        break;
                    case FIND_HEADERS:
                        check.before = CheckBefore.NONE;
                        check.operation = READ;
                        check.after = CheckAfter.COLLECTION;
                        break;
                    case FIND_BY_PRIMARY_KEY:
                        // Check after; need to read entity before evaluating attribute predicates
                        checkEntityAfter(check);
                        break;
                    case FIND_ENTITY_BY_ATTRIBUTE:
                        // Check after, not enough info to resolve entity
                        checkEntityAfter(check);
                        break;
                    case DELETE_ENTITY:
                        // Check before to prevent illegal deletion
                        checkEntityBefore(check, args, DELETE);
                        break;
                    case DELETE_BY_ID:
                        // Check before to prevent illegal deletion
                        checkEntityFromId(check, args, DELETE);
                        break;
                    case DELETE_IDENTITY_BY_ID:
                        // Check before to prevent illegal deletion
                        checkIdentityFromId(args, check, DELETE);
                        break;
                    case GET_IDENTITY_PROPERTY_BY_ID:
                        checkIdentityFromId(args, check, READ);
                        break;
                    case SET_PROPERTY_BY_ID:
                        checkEntityFromId(check, args, READ);
                        break;
                    case GET_PROPERTY_OF_ENTITY:
                        checkEntityBefore(check, args, READ);
                        break;
                    case SET_PROPERTY_OF_ENTITY:
                        checkEntityBefore(check, args, UPDATE);
                        break;
                    default:
                        throw new UnsupportedOperationException("Security declaration for method " + mname + " specifies unsupported stereotype " + check.stereotype.name());
                }
        }

        if (check.before == null) throw new NullPointerException("check.before");
        if (check.after == null) throw new NullPointerException("check.after");
        if (check.operation == null || check.operation == OperationType.NONE)
            throw new NullPointerException("check.operation");

        switch(check.before) {
            case ENTITY:
                if (check.entity == null) throw new NullPointerException("check.entity");
                if (!roleManager.isPermittedForEntity(user, check.entity, check.operation, null)) {
                    throw new PermissionDeniedException(check.operation, check.entity);
                }
                break;
            case ID:
                if (check.id == null) throw new NullPointerException("check.id");
                if (check.types.length > 1) throw new IllegalStateException("Security declaration for method " + mname + " needs to check ID, but multiple EntityTypes specified");
                Entity entity = entityFinder.find(check.types[0].getEntityClass(), check.id);
                if (!roleManager.isPermittedForEntity(user, entity, check.operation, check.otherOperationName)) {
                    throw new PermissionDeniedException(check.operation, entity);
                }
                break;
            case ALL:
                for (EntityType checkType : check.types) {
                    if (!roleManager.isPermittedForAllEntities(user, checkType, check.operation)) {
                        throw new PermissionDeniedException(check.operation, checkType);
                    }
                }
                break;
        }

        Object rv = methodInvocation.proceed();

        if (rv == null) return null;

        switch(check.after) {
            case COLLECTION:
                if (rv instanceof EntityHeader[]) {
                    EntityHeader[] array = (EntityHeader[]) rv;
                    List<Object> headers = filter(new ArrayIterator(array), user, check);
                    Object[] a0 = (Object[]) Array.newInstance(array.getClass().getComponentType(), 0);
                    return headers.toArray(a0);
                } else if (rv instanceof Entity[]) {
                    Entity[] array = (Entity[]) rv;
                    List<Object> entities = filter(new ArrayIterator(array), user, check);
                    Object[] a0 = (Object[]) Array.newInstance(array.getClass().getComponentType(), 0);
                    return entities.toArray(a0);
                } else if (rv instanceof Collection) {
                    // TODO check generic type?
                    Collection coll = (Collection) rv;
                    return filter(coll.iterator(), user, check);
                } else {
                    throw new IllegalStateException("Return value of " + mname + " was not Entity[] or Collection<Entity>");
                }
            case ENTITY:
                if (!(rv instanceof Entity)) {
                    throw new IllegalStateException("Return value of " + mname + " was not an Entity");
                }

                Entity entity = (Entity)rv;

                if (!roleManager.isPermittedForEntity(user, entity, check.operation, null))
                    throw new PermissionDeniedException(check.operation, entity);
                return rv;
            case NONE:
                logger.log(Level.FINE, "Permitted {0} on {1} for {2}",
                        new Object[]{check.operation.name(),
                                getEntityName(check.entity),
                                mname});
                return rv;
            default:
                String ename = getEntityName(check.entity);
                Level level = (check.entity == null) ? Level.INFO : Level.FINE;
                logger.log(level, "Permitted {0} on {1} for {2}", 
                        new Object[]{check.operation.name(), ename, mname});
                return rv;
        }
    }

    private void checkEntityAfter(CheckInfo check) {
        check.before = CheckBefore.NONE;
        check.operation = READ;
        check.after = CheckAfter.ENTITY;
    }

    private void checkIdentityFromId(Object[] args, CheckInfo check, OperationType operation) throws FindException {
        if (args[0] instanceof Long && args[1] instanceof String) {
            IdentityHeader header = new IdentityHeader((Long)args[0], (String)args[1], check.types[0].getOldEntityType(), null, null);
            Entity ent = (Entity) entityFinder.find(header);
            check.before = CheckBefore.ENTITY;
            check.operation = operation;
            check.entity = ent;
            check.after = CheckAfter.NONE;
        } else {
            throwNoId(check);
        }
    }

    private void checkEntityFromId(CheckInfo check, Object[] args, OperationType operation) throws FindException {
        if (check.types.length != 1)
            throw new IllegalStateException("Security declaration for method " + check.methodName +
                    " specifies " + operation.getName() + ", but has multiple types");

        Serializable id = getIdArg(check, args);
        if (id == null) throwNoId(check);

        check.before = CheckBefore.ENTITY;
        check.operation = operation;
        check.entity = entityFinder.find(check.types[0].getEntityClass(), id);
        check.after = CheckAfter.NONE;
    }

    private String getEntityName(Entity entity) {
        if (entity instanceof NamedEntity) {
            return ((NamedEntity)entity).getName();
        } else if (entity != null) {
            return entity.getClass().getSimpleName() + " #" + entity.getId();
        } else {
            return "<unknown>";
        }
    }

    private void checkIdBefore(CheckInfo check, Object[] args, OperationType operation) {
        getIdArgOrThrow(check, args);
        logger.log(Level.FINER, "Will check ID before invocation");
        check.before = CheckBefore.ENTITY;
        check.operation = operation;
        check.after = CheckAfter.NONE;
    }

    private void checkEntityBefore(CheckInfo check, Object[] args, OperationType operation) {
        getEntityArgOrThrow(check, args);
        logger.log(Level.FINER, "Will check Entity before invocation");
        check.before = CheckBefore.ENTITY;
        check.operation = operation;
        check.after = CheckAfter.NONE;
    }

    private Entity getEntityArgOrThrow(CheckInfo info, Object[] args) {
        Entity ent = getEntityArg(info, args);
        if (ent != null) return ent;
        throw new IllegalStateException("Security declaration for method " +
                info.methodName + " specifies " + info.stereotype +
                ", but can't find Entity argument");

    }

    private Entity getEntityArg(CheckInfo info, Object[] args) {
        Object arg = getSingleOrRelevantArg(args, info.relevantArg);
        if (arg == null) return null;

        if (arg instanceof Entity) {
            info.entity = (Entity)arg;
            return info.entity;
        }

        return null;
    }

    private Serializable getIdArgOrThrow(CheckInfo info, Object[] args) {
        Serializable arg = getIdArg(info, args);
        if (arg == null) throwNoId(info);
        return arg;
    }

    private void throwNoId(CheckInfo info) {
        throw new IllegalStateException("Security declaration for method " +
                info.methodName + " specifies " + info.stereotype +
                ", but can't find appropriate argument(s)");
    }

    private Serializable getIdArg(CheckInfo info, Object[] args) {
        Object arg = getSingleOrRelevantArg(args, info.relevantArg);
        if (arg == null) return null;

        if (arg instanceof Long || arg instanceof String) {
            info.id = arg.toString();
            return arg.toString();
        }

        return null;
    }

    private Object getSingleOrRelevantArg(Object[] args, int relevantArg) {
        if (args.length == 1) {
            return args[0];
        } else if (args.length > 1 && relevantArg >= 0 && relevantArg < args.length) {
            return args[relevantArg];
        } else {
            return null;
        }
    }

    public void onApplicationEvent(ApplicationEvent event) {
        if (event instanceof EntityInvalidationEvent) {
            EntityInvalidationEvent eie = (EntityInvalidationEvent) event;
            if (Role.class.isAssignableFrom(eie.getEntityClass())) {
                for (long oid : eie.getEntityIds()) {
                    try {
                        // Refresh cached roles, if they still exist
                        roleManager.getCachedEntity(oid, 0);
                    } catch (Exception e) {
                        logger.log(Level.WARNING, "Couldn't refresh cached Role", e);
                    }
                }
            }
        }
    }
}
