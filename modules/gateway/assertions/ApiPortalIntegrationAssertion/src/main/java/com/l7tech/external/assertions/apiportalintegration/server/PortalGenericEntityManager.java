package com.l7tech.external.assertions.apiportalintegration.server;

import com.l7tech.objectmodel.DeleteException;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.objectmodel.UpdateException;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Manager which can perform CRUD operations on an AbstractPortalGenericEntity.
 *
 * @param <T> the type of AbstractPortalGenericEntity that can be managed.
 */
public interface PortalGenericEntityManager<T extends AbstractPortalGenericEntity> {
    T add(@NotNull final T genericEntity) throws SaveException;

    T update(@NotNull final T genericEntity) throws FindException, UpdateException;

    void delete(@NotNull final String name) throws FindException, DeleteException;

    T find(@NotNull final String name) throws FindException;

    List<T> findAll() throws FindException;
}
