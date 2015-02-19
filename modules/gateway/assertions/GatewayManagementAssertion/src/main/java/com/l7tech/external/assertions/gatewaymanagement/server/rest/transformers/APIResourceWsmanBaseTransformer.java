package com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers;

import com.l7tech.external.assertions.gatewaymanagement.server.EntityManagerResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.SecretsEncryptor;
import com.l7tech.gateway.api.ManagedObject;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.PersistentEntity;
import com.l7tech.server.bundling.EntityContainer;
import org.jetbrains.annotations.NotNull;

/**
 * This is a base class that used a wsman factory to transform an MO
 * @param <M> The MO that is being transformed
 * @param <E> The Entity that that MO transforms into
 * @param <F> The wsman resource factory that can transform the MO
 */
public abstract class APIResourceWsmanBaseTransformer<M extends ManagedObject, E extends PersistentEntity, EH extends EntityHeader, F extends EntityManagerResourceFactory<M,E, EH>> implements EntityAPITransformer<M, E> {

    /**
     * The wiseman resource factory
     */
    protected F factory;

    /**
     * Sets the wiseman resource factory. Likely set using injection.
     *
     * @param factory The wiseman resource factory
     */
    protected abstract void setFactory(F factory);

    @Override
    @NotNull
    public String getResourceType(){
        return factory.getType().toString();
    }

    @NotNull
    @Override
    public M convertToMO(@NotNull E e, SecretsEncryptor secretsEncryptor) {
        //need to 'identify' the MO because by default the wsman factories will no set the id and version in the
        // asResource method
        return factory.identify(factory.asResource(e), e);
    }

    @NotNull
    @Override
    public M convertToMO(@NotNull EntityContainer<E> entityContainer, SecretsEncryptor secretsEncryptor) {
        return convertToMO(entityContainer.getEntity(), secretsEncryptor);
    }

    @NotNull
    @Override
    public EntityContainer<E> convertFromMO(@NotNull M m, SecretsEncryptor secretsEncryptor) throws ResourceFactory.InvalidResourceException {
        return convertFromMO(m,true, secretsEncryptor);
    }

    @NotNull
    @Override
    public EntityContainer<E> convertFromMO(@NotNull M m, boolean strict, SecretsEncryptor secretsEncryptor) throws ResourceFactory.InvalidResourceException {

        E entity = factory.fromResourceAsBag(m,strict).getEntity();
        if(entity == null) {
            throw new IllegalStateException("Entity returned from wsman factor should not be null");
        }
        if(m.getId() != null){
            //set the entity id as it is not always set
            entity.setGoid(Goid.parseGoid(m.getId()));
        }
        return new EntityContainer<>(entity);
    }
}
