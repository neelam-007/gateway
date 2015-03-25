package com.l7tech.gateway.common.task;

import com.l7tech.gateway.common.admin.Administrative;
import com.l7tech.gateway.common.security.rbac.MethodStereotype;
import com.l7tech.gateway.common.security.rbac.Secured;
import com.l7tech.objectmodel.*;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;

import static org.springframework.transaction.annotation.Propagation.REQUIRED;

/**
 */
@Transactional(propagation=REQUIRED, rollbackFor=Throwable.class)
@Secured(types = EntityType.SCHEDULED_TASK)
@Administrative
public interface ScheduledTaskAdmin {
    /**
     *  Retrieve a scheduled task entity from the database by id
     * @param id: the id of the scheduled task
     * @return a scheduled task entity with the id
     * @throws com.l7tech.objectmodel.FindException : thrown when errors finding the scheduled task entity.
     */
    @Transactional(readOnly = true)
    @Secured(types = EntityType.SCHEDULED_TASK, stereotype = MethodStereotype.FIND_ENTITY)
    ScheduledTask getScheduledTask(Goid id) throws FindException;


    /**
     * Retrieve all scheduled task entities from the database.
     * @return a list of scheduled task entities
     * @throws FindException: thrown when errors finding the scheduled task entities.
     */
    @Transactional(readOnly = true)
    @Secured (types = EntityType.SCHEDULED_TASK, stereotype = MethodStereotype.FIND_ENTITIES)
    Collection<ScheduledTask> getAllScheduledTasks() throws FindException;

    /**
     * Save a scheduled task entity into the database.
     * @param scheduledTask: the scheduled task entity to be saved
     * @return  a long, the saved entity object id.
     * @throws com.l7tech.objectmodel.UpdateException : thrown when errors saving the scheduled task entity.
     */
    @Secured (types = EntityType.SCHEDULED_TASK, stereotype = MethodStereotype.SAVE_OR_UPDATE)
    Goid saveScheduledTask(ScheduledTask scheduledTask) throws UpdateException, SaveException;

    /**
     * Delete a scheduled task entity from the database.
     * @param scheduledTask: the scheduled task entity to be deleted.
     * @throws com.l7tech.objectmodel.DeleteException : thrown when errors deleting the scheduled task entity.
     */
    @Secured (types = EntityType.SCHEDULED_TASK, stereotype = MethodStereotype.DELETE_ENTITY)
    void deleteScheduledTask(ScheduledTask scheduledTask) throws DeleteException;
}
