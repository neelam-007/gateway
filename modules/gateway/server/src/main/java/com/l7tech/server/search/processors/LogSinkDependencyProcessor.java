package com.l7tech.server.search.processors;

import com.l7tech.gateway.common.log.GatewayDiagnosticContextKeys;
import com.l7tech.gateway.common.log.SinkConfiguration;
import com.l7tech.objectmodel.Entity;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.IdentityHeader;
import com.l7tech.objectmodel.SsgKeyHeader;
import com.l7tech.server.EntityCrud;
import com.l7tech.server.search.exceptions.CannotReplaceDependenciesException;
import com.l7tech.server.search.exceptions.CannotRetrieveDependenciesException;
import com.l7tech.server.search.objects.Dependency;
import com.l7tech.util.Functions;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import org.jetbrains.annotations.NotNull;

/**
 * This is used to find SinkConfiguration's dependencies
 *
 */
public class LogSinkDependencyProcessor extends DefaultDependencyProcessor<SinkConfiguration> implements DependencyProcessor<SinkConfiguration> {

    @Inject
    private EntityCrud entityCrud;

    /**
     * Find the dependencies of a log sink.
     *
     * @param sinkConfiguration The sink configuration to find dependencies for
     * @param finder The finder that is performing the current dependency search
     * @return The list of dependencies that this folder has.
     * @throws FindException
     */
    @NotNull
    @Override
    public List<Dependency> findDependencies(@NotNull final SinkConfiguration sinkConfiguration, @NotNull final DependencyFinder finder) throws FindException, CannotRetrieveDependenciesException {
        final List<Dependency> dependencies = super.findDependencies(sinkConfiguration,finder);

        Functions.BinaryThrows<EntityHeader,String,EntityType, CannotRetrieveDependenciesException>  entityHeaderResolver = new Functions.BinaryThrows<EntityHeader, String, EntityType, CannotRetrieveDependenciesException>() {
            @Override
            public EntityHeader call(String id, EntityType entityType) throws CannotRetrieveDependenciesException  {
                return new EntityHeader(Goid.parseGoid(id), entityType, null, null);
            }
        };

        dependencies.addAll(getDependencies(sinkConfiguration, finder, GatewayDiagnosticContextKeys.SERVICE_ID, EntityType.SERVICE, entityHeaderResolver));
        dependencies.addAll(getDependencies(sinkConfiguration, finder, GatewayDiagnosticContextKeys.LISTEN_PORT_ID, EntityType.SSG_CONNECTOR, entityHeaderResolver));
        dependencies.addAll(getDependencies(sinkConfiguration, finder, GatewayDiagnosticContextKeys.EMAIL_LISTENER_ID, EntityType.EMAIL_LISTENER, entityHeaderResolver));
        dependencies.addAll(getDependencies(sinkConfiguration, finder, GatewayDiagnosticContextKeys.JMS_LISTENER_ID, EntityType.JMS_ENDPOINT, entityHeaderResolver));
        dependencies.addAll(getDependencies(sinkConfiguration, finder, GatewayDiagnosticContextKeys.POLICY_ID, EntityType.POLICY, entityHeaderResolver));
        dependencies.addAll(getDependencies(sinkConfiguration, finder, GatewayDiagnosticContextKeys.FOLDER_ID, EntityType.FOLDER, entityHeaderResolver));
        dependencies.addAll(getDependencies(sinkConfiguration, finder, GatewayDiagnosticContextKeys.USER_ID, EntityType.USER, new Functions.BinaryThrows<EntityHeader, String, EntityType, CannotRetrieveDependenciesException>() {
            @Override
            public EntityHeader call(String s, EntityType entityType) throws CannotRetrieveDependenciesException {
                String[] split = s.split(":");
                if(split.length==2) {
                    return new IdentityHeader(Goid.parseGoid(split[0]), new EntityHeader(split[1], entityType, null, null));
                }
                throw new CannotRetrieveDependenciesException(SinkConfiguration.class, "Malformed User ID filter value: " + s);
            }
        }));

        if( sinkConfiguration.getProperty(SinkConfiguration.PROP_SYSLOG_SSL_KEYSTORE_ID)!=null &&
                sinkConfiguration.getProperty(SinkConfiguration.PROP_SYSLOG_SSL_KEY_ALIAS)!=null){
            Entity entity = entityCrud.find(new SsgKeyHeader(null, Goid.parseGoid(sinkConfiguration.getProperty(SinkConfiguration.PROP_SYSLOG_SSL_KEYSTORE_ID)), sinkConfiguration.getProperty(SinkConfiguration.PROP_SYSLOG_SSL_KEY_ALIAS), ""));
            final Dependency dependency = finder.getDependency(DependencyFinder.FindResults.create(entity,null));
            if (dependency != null) {
                dependencies.add(dependency);
            }
        }


        return dependencies;
    }

    private List<Dependency> getDependencies(@NotNull final SinkConfiguration sinkConfiguration, @NotNull final DependencyFinder finder, String key, EntityType entityType, Functions.BinaryThrows<EntityHeader,String,EntityType, CannotRetrieveDependenciesException> getEntityHeader) throws FindException, CannotRetrieveDependenciesException{
        List<Dependency> dependencies = new ArrayList<>();
        if(sinkConfiguration.getFilters().containsKey(key)){
            for(String id: sinkConfiguration.getFilters().get(key)){
                Entity entity = entityCrud.find(getEntityHeader.call(id,entityType));
                final Dependency dependency = finder.getDependency(DependencyFinder.FindResults.create(entity,null));
                if (dependency != null) {
                    dependencies.add(dependency);
                }
            }
        }
        return dependencies;
    }

    @Override
    public void replaceDependencies(@NotNull SinkConfiguration sinkConfiguration, @NotNull Map<EntityHeader, EntityHeader> replacementMap, @NotNull DependencyFinder finder, boolean replaceAssertionsDependencies) throws CannotReplaceDependenciesException {
        super.replaceDependencies(sinkConfiguration, replacementMap, finder, replaceAssertionsDependencies);

        Functions.TernaryThrows<String, String, EntityType, Map<EntityHeader, EntityHeader>, CannotReplaceDependenciesException> defaultGetReplacementId = new Functions.TernaryThrows<String, String, EntityType, Map<EntityHeader, EntityHeader>, CannotReplaceDependenciesException>(){
            @Override
            public String call(String oldId, EntityType entityType, Map<EntityHeader, EntityHeader> replacementMap) throws CannotReplaceDependenciesException {
                EntityHeader searchHeader = new EntityHeader(oldId, entityType,null,null);
                if (replacementMap.containsKey(searchHeader)) {
                    return replacementMap.get(searchHeader).getStrId();
                } else {
                    return oldId;
                }
            }
        } ;

        Map<String, List<String>> filters = new HashMap<>(sinkConfiguration.getFilters());
        replaceDependencies(filters, replacementMap, GatewayDiagnosticContextKeys.SERVICE_ID, EntityType.SERVICE, defaultGetReplacementId);
        replaceDependencies(filters, replacementMap, GatewayDiagnosticContextKeys.LISTEN_PORT_ID, EntityType.SSG_CONNECTOR, defaultGetReplacementId);
        replaceDependencies(filters, replacementMap, GatewayDiagnosticContextKeys.EMAIL_LISTENER_ID, EntityType.EMAIL_LISTENER, defaultGetReplacementId);
        replaceDependencies(filters, replacementMap, GatewayDiagnosticContextKeys.JMS_LISTENER_ID, EntityType.JMS_ENDPOINT, defaultGetReplacementId);
        replaceDependencies(filters, replacementMap, GatewayDiagnosticContextKeys.POLICY_ID, EntityType.POLICY, defaultGetReplacementId);
        replaceDependencies(filters, replacementMap, GatewayDiagnosticContextKeys.FOLDER_ID, EntityType.FOLDER, defaultGetReplacementId);
        replaceDependencies(filters, replacementMap, GatewayDiagnosticContextKeys.USER_ID, EntityType.USER, new Functions.TernaryThrows<String, String, EntityType, Map<EntityHeader, EntityHeader>, CannotReplaceDependenciesException>(){
            @Override
            public String call(String oldId, EntityType entityType, Map<EntityHeader, EntityHeader> replacementMap) throws CannotReplaceDependenciesException {
                String[] split = oldId.split(":");
                if(split.length == 2) {
                    EntityHeader searchHeader = new IdentityHeader(Goid.parseGoid(split[0]), new EntityHeader(split[1], entityType, null, null));
                    if (replacementMap.containsKey(searchHeader)) {
                        IdentityHeader replacementHeader = (IdentityHeader)replacementMap.get(searchHeader);
                        return replacementHeader.getProviderGoid() + ":" + replacementHeader.getStrId();
                    } else {
                        return oldId;
                    }
                } else{
                    throw new CannotReplaceDependenciesException(SinkConfiguration.class, "Malformed User ID filter value:" + oldId);
                }
            }
        });
        sinkConfiguration.setFilters(filters);


        // ssg key
        if(sinkConfiguration.getProperty(SinkConfiguration.PROP_SYSLOG_SSL_KEYSTORE_ID)!= null &&
                sinkConfiguration.getProperty(SinkConfiguration.PROP_SYSLOG_SSL_KEY_ALIAS)!= null){
            final String oldKeyId =   sinkConfiguration.getProperty(SinkConfiguration.PROP_SYSLOG_SSL_KEYSTORE_ID) + ":" + sinkConfiguration.getProperty(SinkConfiguration.PROP_SYSLOG_SSL_KEY_ALIAS);
            EntityHeader searchHeader = new EntityHeader(oldKeyId, EntityType.SSG_KEY_ENTRY,null,null);
            if(replacementMap.containsKey(searchHeader)){
                SsgKeyHeader newKeyHeader = (SsgKeyHeader)replacementMap.get(searchHeader);
                sinkConfiguration.setProperty(SinkConfiguration.PROP_SYSLOG_SSL_KEYSTORE_ID, newKeyHeader.getKeystoreId().toString());
                sinkConfiguration.setProperty(SinkConfiguration.PROP_SYSLOG_SSL_KEY_ALIAS, newKeyHeader.getAlias());
            }
        }

    }

    private void replaceDependencies(Map<String, List<String>> filters, @NotNull Map<EntityHeader, EntityHeader> replacementMap, String filterKey, EntityType entityType, Functions.TernaryThrows<String, String, EntityType, Map<EntityHeader, EntityHeader>, CannotReplaceDependenciesException> getReplacementId) throws CannotReplaceDependenciesException {
        if(filters.containsKey(filterKey)){
            List<String> oldIds = filters.get(filterKey);
            List<String> newIds = new ArrayList<>();
            for(String oldId: oldIds) {
                newIds.add(getReplacementId.call(oldId, entityType, replacementMap));
            }
            filters.put(filterKey, newIds);
        }
    }
}
