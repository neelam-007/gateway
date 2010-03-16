package com.l7tech.gateway.common.schema;

import com.l7tech.objectmodel.DeleteException;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.gateway.common.security.rbac.Secured;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.gateway.common.security.rbac.MethodStereotype;
import com.l7tech.gateway.common.admin.Administrative;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.Collection;

import static org.springframework.transaction.annotation.Propagation.SUPPORTS;

/**
 * An admin interface that allows indirect interaction with the SchemaEntryManager through
 * the SSM
 *
 * @author flascelles@layer7-tech.com
 */
@Transactional(propagation=Propagation.REQUIRED, rollbackFor=Throwable.class)
@Secured(types= EntityType.SCHEMA_ENTRY)
@Administrative
public interface SchemaAdmin {
    @Secured(stereotype=MethodStereotype.FIND_ENTITIES)
    Collection<SchemaEntry> findAllSchemas() throws FindException;

    @Secured(stereotype=MethodStereotype.DELETE_ENTITY)
    void deleteSchemaEntry(SchemaEntry existingSchema) throws DeleteException;

    @Secured(stereotype=MethodStereotype.SAVE_OR_UPDATE)
    long saveSchemaEntry(SchemaEntry entry) throws SaveException, UpdateException;

    //todo this does not need to be a collection for a unique property
    @Secured(stereotype=MethodStereotype.FIND_ENTITIES)
    Collection<SchemaEntry> findByName(String schemaName) throws FindException;

    @Secured(stereotype=MethodStereotype.FIND_ENTITIES)
    Collection<SchemaEntry> findByTNS(String tns) throws FindException;

    /**
     * Get a schema document from a URL.
     * <p/>
     * URL may be http or https with or without client auth
     *
     * @param url the url that the gateway will use to resolve the schema document. this may contain
     *            userinfo type credentials
     * @return the contents resolved by this url
     * @throws java.io.IOException            thrown on I/O error accessing the url
     * @throws java.net.MalformedURLException thrown on malformed url
     */
    @Transactional(propagation = SUPPORTS)
    String resolveSchemaTarget(String url) throws IOException;

}
