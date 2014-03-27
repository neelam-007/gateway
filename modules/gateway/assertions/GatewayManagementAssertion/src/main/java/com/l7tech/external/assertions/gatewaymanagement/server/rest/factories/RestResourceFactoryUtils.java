package com.l7tech.external.assertions.gatewaymanagement.server.rest.factories;

import com.l7tech.external.assertions.gatewaymanagement.server.rest.exceptions.InvalidArgumentException;
import com.l7tech.util.Either;
import com.l7tech.util.Eithers;
import com.l7tech.util.Functions;
import com.l7tech.util.Option;
import org.jetbrains.annotations.NotNull;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import java.util.Map;
import java.util.Set;

/**
 * This contains utilities used by rest resource factories.
 */
public class RestResourceFactoryUtils {
    /**
     * This is used to validate annotated beans
     */
    private static final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    /**
     * This will validate the entity using annotations that if has declared on it fields and methods.
     *
     * @param obj The object to validate
     * @throws InvalidArgumentException This is thrown if the entity is invalid.
     */
    public static <O> void validate(@NotNull final O obj, @NotNull final Map<String, String> propertyNamesMap) {
        //TODO: consider changing the way the error messages are generated. Should the propertyNamesMap be passed in here?
        //validate the entity
        final Set<ConstraintViolation<O>> violations = validator.validate(obj);
        if (!violations.isEmpty()) {
            //the entity is invalid. Create a nice exception message.
            final StringBuilder validationReport = new StringBuilder("Invalid Value: ");
            boolean first = true;
            for (final ConstraintViolation<O> violation : violations) {
                if (!first) validationReport.append('\n');
                first = false;
                String propertyName = violation.getPropertyPath().toString();
                validationReport.append(propertyNamesMap.containsKey(propertyName) ? propertyNamesMap.get(propertyName) : propertyName);
                validationReport.append(" - ");
                validationReport.append(violation.getMessage());
            }
            throw new InvalidArgumentException(validationReport.toString());
        }
    }

    /**
     * This is a helper function used to run the given Nullary function within a transaction. This will create the
     * transaction using the manager and properly handle any errors.
     *
     * @param transactionManager     The transaction manager to use to run the transaction with.
     * @param readonly               True to make the transaction readonly false otherwise
     * @param transactionalOperation The transactional operation to perform
     * @param <R>                    The return type of the transactional operation
     * @return The return from the transactional operation.
     */
    public static <R> R transactional(@NotNull final PlatformTransactionManager transactionManager, final boolean readonly, @NotNull final Functions.Nullary<R> transactionalOperation) {
        return RestResourceFactoryUtils.transactional(transactionManager, readonly, new Functions.NullaryThrows<R, RuntimeException>() {
            @Override
            public R call() throws RuntimeException {
                return transactionalOperation.call();
            }
        });
    }

    /**
     * This is a helper function used to run the given Nullary function within a transaction. This will create the
     * transaction using the manager and properly handle any errors.
     *
     * @param transactionManager     The transaction manager to use to run the transaction with.
     * @param readonly               True to make the transaction readonly false otherwise
     * @param transactionalOperation The transactional operation to perform
     * @param <T>                    The exception that can be thrown by the transactional operation.
     * @throws T This gets thrown if the transactional operation throws an exception
     */
    public static <T extends Throwable> void transactional(@NotNull final PlatformTransactionManager transactionManager, final boolean readonly, @NotNull final Functions.NullaryVoidThrows<T> transactionalOperation) throws T {
        RestResourceFactoryUtils.transactional(transactionManager, readonly, new Functions.NullaryThrows<Void, T>() {
            @Override
            public Void call() throws T {
                transactionalOperation.call();
                return null;
            }
        });
    }

    /**
     * This is a helper function used to run the given Nullary function within a transaction. This will create the
     * transaction using the manager and properly handle any errors.
     *
     * @param transactionManager     The transaction manager to use to run the transaction with.
     * @param readonly               True to make the transaction readonly false otherwise
     * @param transactionalOperation The transactional operation to perform
     * @param <T>                    The exception that can be thrown by the transactional operation.
     * @param <R>                    The return type of the transactional operation
     * @return The return from the transactional operation.
     * @throws T This gets thrown if the transactional operation throws an exception
     */
    public static <R, T extends Throwable> R transactional(@NotNull final PlatformTransactionManager transactionManager, final boolean readonly, @NotNull final Functions.NullaryThrows<R, T> transactionalOperation) throws T {
        //create a new transaction template
        final TransactionTemplate tt = new TransactionTemplate(transactionManager);
        //set the readonly property
        tt.setReadOnly(readonly);
        //execute and then extract the transactional operation.
        //The return is wrapped in an Optional because Either does not support null values but a null return can be valid
        return Eithers.extract(tt.execute(new TransactionCallback<Either<T, Option<R>>>() {
            @Override
            public Either<T, Option<R>> doInTransaction(final TransactionStatus transactionStatus) {
                try {
                    //call and wrap the transactional operation return
                    return Either.right(Option.optional(transactionalOperation.call()));
                } catch (final Throwable t) {
                    //an exception was thrown so rollback
                    transactionStatus.setRollbackOnly();
                    try {
                        // This return the exception as the left if it is of the correct type
                        //noinspection unchecked
                        return Either.left((T) t);
                    } catch (final ClassCastException e) {
                        //The exception is not cast-able to T. It may be a runtime exception.
                        //throw the a runtime exception if this is a runtime exception.
                        if (RuntimeException.class.isAssignableFrom(t.getClass())) {
                            //noinspection ConstantConditions
                            throw (RuntimeException) t;
                        } else {
                            //I don't think this is ever reachable because of compiler type checking, but just in case...
                            throw new IllegalStateException("Unexpected exception thrown form function. Exception thrown: " + t.getClass(), t);
                        }
                    }
                }
            }
        })).toNull();
    }
}
