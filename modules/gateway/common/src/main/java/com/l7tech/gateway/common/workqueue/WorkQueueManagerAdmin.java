package com.l7tech.gateway.common.workqueue;

import com.l7tech.gateway.common.AsyncAdminMethods;
import com.l7tech.gateway.common.admin.Administrative;
import com.l7tech.gateway.common.security.rbac.MethodStereotype;
import com.l7tech.gateway.common.security.rbac.Secured;
import com.l7tech.objectmodel.*;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.springframework.transaction.annotation.Propagation.REQUIRED;

@Transactional(propagation=REQUIRED, rollbackFor=Throwable.class)
@Secured(types = EntityType.WORK_QUEUE)
@Administrative
public interface WorkQueueManagerAdmin extends AsyncAdminMethods {
    /**
     * Retrieve a Work Queue entity from the database by using a Work Queue name.
     *
     * @param workQueueName: the name of a Work Queue
     * @return a Work Queue entity with the name, "workQueueName".
     * @throws FindException: thrown when errors finding the Work Queue entity.
     */
    @Transactional(readOnly=true)
    @Secured(types=EntityType.WORK_QUEUE, stereotype= MethodStereotype.FIND_ENTITY)
    WorkQueue getWorkQueue(String workQueueName) throws FindException;

    /**
     * Retrieve a Work Queue entity from the database by using a Work Queue name.
     *
     * @param id: the id of a Work Queue
     * @return a Work Queue entity with the name, "workQueueName".
     * @throws FindException: thrown when errors finding the Work Queue entity.
     */
    @Transactional(readOnly=true)
    @Secured(types=EntityType.WORK_QUEUE, stereotype= MethodStereotype.FIND_ENTITY)
    WorkQueue getWorkQueue(Goid id) throws FindException;

    /**
     * Retrieve all Work Queue entities from the database.
     *
     * @return a list of Work Queue entities
     * @throws FindException: thrown when errors finding Work Queue entities.
     */
    @Transactional(readOnly=true)
    @Secured(types=EntityType.WORK_QUEUE, stereotype= MethodStereotype.FIND_ENTITIES)
    List<WorkQueue> getAllWorkQueues() throws FindException;

    /**
     * Get the names of all Work Queue entities.
     *
     * @return a list of the names of all Work Queue entities.
     * @throws FindException: thrown when errors finding Work Queue entities.
     */
    @Transactional(readOnly=true)
    @Secured(stereotype = MethodStereotype.UNCHECKED_WIDE_OPEN)
    List<String> getAllWorkQueueNames() throws FindException;

    /**
     * Save a Work Queue entity into the database.
     *
     * @param workQueue: the Work Queue entity to be saved.
     * @return a long, the saved entity object id.
     * @throws UpdateException error updating the work queue entity or its executor.
     * @throws SaveException error saving the work queue
     * @throws FindException error finding the original work queue
     */
    @Secured(types=EntityType.WORK_QUEUE, stereotype= MethodStereotype.SAVE_OR_UPDATE)
    Goid saveWorkQueue(WorkQueue workQueue) throws UpdateException, SaveException, FindException;

    /**
     * Delete a Work Queue entity from the database.
     *
     * @param workQueue: the Work Queue entity to be deleted.
     * @throws DeleteException: thrown when errors deleting the Work Queue entity.
     */
    @Secured(types=EntityType.WORK_QUEUE, stereotype= MethodStereotype.DELETE_ENTITY)
    void deleteWorkQueue(WorkQueue workQueue) throws DeleteException;
}
