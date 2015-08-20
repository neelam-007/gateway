package com.l7tech.console.util;

import com.l7tech.console.panels.CancelableOperationDialog;
import com.l7tech.gateway.common.AsyncAdminMethods;
import com.l7tech.gateway.common.AsyncAdminMethods.JobId;
import com.l7tech.gateway.common.AsyncAdminMethods.JobResult;
import com.l7tech.gui.util.Utilities;
import com.l7tech.util.Either;
import com.l7tech.util.Eithers;
import com.l7tech.util.Pair;

import javax.swing.*;
import java.awt.*;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.Callable;

import static com.l7tech.util.Either.left;
import static com.l7tech.util.Either.right;

/**
 * GUI utilities related to administration.
 */
public class AdminGuiUtils {

    /**
     * Get the result of an asynchronous administrative method.
     *
     * <p>A cancel dialog will be shown for the operation after a short
     * delay.  Clicking the cancel dialog will cancel the related job</p>
     *
     * @param admin The async API
     * @param parent The parent window for any dialogs
     * @param taskTitle The title for the long running task
     * @param taskInfo The info for the long running task
     * @param jobId The identifier for the administrative job
     * @param <R> The result type
     * @return Either the result or an error string (never null)
     */
    public static <R extends Serializable> Either<String,R> doAsyncAdmin( final AsyncAdminMethods admin,
                                                                          final Window parent,
                                                                          final String taskTitle,
                                                                          final String taskInfo,
                                                                          final JobId<R> jobId) throws InterruptedException, InvocationTargetException {
        return doAsyncAdmin(admin,parent,taskTitle,taskInfo,jobId,true);
    }

    public static <R extends Serializable> Either<String,R> doAsyncAdmin( final AsyncAdminMethods admin,
                                                                          final Window parent,
                                                                          final String taskTitle,
                                                                          final String taskInfo,
                                                                          final JobId<R> jobId,
                                                                          final boolean showCancelButton ) throws InterruptedException, InvocationTargetException {

        final CancelableOperationDialog cancelDialog = showProgressBarAndCancelDialog(parent, taskTitle, taskInfo, showCancelButton);

        final Callable<Either<String,R>> callable = new Callable<Either<String,R>>() {
            @Override
            public Either<String,R> call() throws Exception {
                Thread.sleep( 350L );

                while( true ) {
                    final String status = admin.getJobStatus( jobId );
                    if ( status == null ) {
                        return left( "Unknown job" );
                    } else if ( !status.startsWith( "a" ) ) {
                        final JobResult<R> jobResult = admin.getJobResult( jobId );
                        if ( jobResult.result != null ) {
                            return right( jobResult.result );
                        } else {
                            return left( jobResult.throwableMessage );
                        }
                    } else {
                        Thread.sleep( 5000L );
                    }
                }

            }
        };


        try{
            return Utilities.doWithDelayedCancelDialog(callable, cancelDialog, 500L);
        }catch (InterruptedException e){
            admin.cancelJob( jobId, false );
            throw e;
        }
    }

    /**
     * Get the result of an asynchronous administrative method.  This method is very similar to "doAsyncAdmin" above,
     * except it was written with the express purpose of being able to identify handled exceptions for user friendly
     * error messages (that are displayed to the user for info) versus unhandled exceptions.
     *
     * <p>A cancel dialog will be shown for the operation after a short
     * delay.  Clicking the cancel dialog will cancel the related job</p>
     *
     * @param admin The async API
     * @param parent The parent window for any dialogs
     * @param taskTitle The title for the long running task
     * @param taskInfo The info for the long running task
     * @param jobId The identifier for the administrative job
     * @param <R> The result type
     * @return Either the result or an error string (never null)
     * @throws Throwable Used for throwing handled and unhandled exceptions
     */
    public static <R extends Serializable> R doAsyncAdminWithException( final AsyncAdminMethods admin,
                                                                        final Window parent,
                                                                        final String taskTitle,
                                                                        final String taskInfo,
                                                                        final JobId<R> jobId,
                                                                        final boolean showCancelButton) throws Throwable {

        final CancelableOperationDialog cancelDialog = showProgressBarAndCancelDialog(parent, taskTitle, taskInfo, showCancelButton);
        final Callable<Either<Throwable,R>> callable = new Callable<Either<Throwable, R>>() {
            @Override
            public Either<Throwable, R> call() throws Exception {
                Thread.sleep( 350L );

                while( true ) {
                    final String status = admin.getJobStatus( jobId );
                    if ( status == null ) {
                        return Either.<Throwable, R>left(new Exception("Unknown job"));
                    } else if ( !status.startsWith( "a" ) ) {
                        final JobResult<R> jobResult = admin.getJobResult( jobId );
                        if ( jobResult.result != null ) {
                            return right( jobResult.result );
                        } else {
                            try {
                                final Class cls = this.getClass().getClassLoader().loadClass(jobResult.throwableClassname);
                                if (!Throwable.class.isAssignableFrom(cls)) {
                                    return Either.<Throwable, R>left(new Exception(jobResult.throwableClassname + " is not a Throwable class :" + jobResult.throwableMessage));
                                }
                                Constructor constructor = cls.getDeclaredConstructor(String.class);
                                return Either.left((Throwable) constructor.newInstance(jobResult.throwableMessage));
                            } catch (ClassNotFoundException e) {
                                throw e;
                            }
                        }
                    } else {
                        Thread.sleep( 5000L );
                    }
                }

            }
        };


        try{
            final Either<Throwable,R> res = Utilities.doWithDelayedCancelDialog(callable, cancelDialog, 500L);
            if (res.isRight()) {
                return res.right();
            }
            throw res.left();
        }catch (InterruptedException e){
            admin.cancelJob( jobId, false );
            throw e;
        }
    }

    /**
     * private method to show a progress bar on the Solution Kit Install Wizard while the job is running on the server
     *
     * @param parent The parent window for any dialogs
     * @param taskTitle The title for the long running task
     * @param taskInfo The info for the long running task
     * @param showCancelButton Switch for whether to show the cancel button
     * @return The Cancel Dialog
     */
    private static CancelableOperationDialog showProgressBarAndCancelDialog( final Window parent,
                                                                             final String taskTitle,
                                                                             final String taskInfo,
                                                                             final boolean showCancelButton) {
        final JProgressBar progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);
        final CancelableOperationDialog cancelDialog =
                new CancelableOperationDialog(parent, taskTitle, taskInfo, progressBar,showCancelButton);
        cancelDialog.pack();
        cancelDialog.setModal(true);
        Utilities.centerOnParentWindow( cancelDialog );
        return cancelDialog;
    }
}
