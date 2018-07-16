package com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory.InvalidResourceException;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.SecretsEncryptor;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.EntityAPITransformer;
import com.l7tech.gateway.api.Item;
import com.l7tech.gateway.api.ItemBuilder;
import com.l7tech.gateway.api.LogSinkFilter;
import com.l7tech.gateway.api.LogSinkMO;
import com.l7tech.gateway.api.ManagedObjectFactory;
import com.l7tech.gateway.common.log.SinkConfiguration;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.server.bundling.EntityContainer;
import com.l7tech.util.InetAddressUtil;
import com.l7tech.util.Pair;
import com.l7tech.util.TextUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.l7tech.util.ValidationUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import static com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory.InvalidResourceException.ExceptionType.INVALID_VALUES;
import static com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory.InvalidResourceException.ExceptionType.MISSING_VALUES;

@Component
public class LogSinkTransformer extends EntityManagerAPITransformer<LogSinkMO, SinkConfiguration> implements EntityAPITransformer<LogSinkMO, SinkConfiguration> {

    @NotNull
    @Override
    public String getResourceType() {
        return EntityType.LOG_SINK.toString();
    }

    @NotNull
    @Override
    public LogSinkMO convertToMO(@NotNull EntityContainer<SinkConfiguration> userEntityContainer,  SecretsEncryptor secretsEncryptor) {
        return convertToMO(userEntityContainer.getEntity(), secretsEncryptor);
    }


    @NotNull
    public LogSinkMO convertToMO(@NotNull SinkConfiguration sinkConfiguration) {
        return convertToMO(sinkConfiguration, null);
    }

    @NotNull
    @Override
    public LogSinkMO convertToMO(@NotNull SinkConfiguration sinkConfiguration,  SecretsEncryptor secretsEncryptor) {
        LogSinkMO logSinkMO = ManagedObjectFactory.createLogSinkMO();
        logSinkMO.setId(sinkConfiguration.getId());
        logSinkMO.setVersion(sinkConfiguration.getVersion());
        logSinkMO.setName(sinkConfiguration.getName());
        logSinkMO.setDescription(sinkConfiguration.getDescription());
        logSinkMO.setType(LogSinkMO.SinkType.valueOf(sinkConfiguration.getType().toString()));
        logSinkMO.setEnabled(sinkConfiguration.isEnabled());
        logSinkMO.setSeverity(LogSinkMO.SeverityThreshold.valueOf(sinkConfiguration.getSeverity().toString()));
        logSinkMO.setCategories(convertToMO(sinkConfiguration.getCategories()));
        logSinkMO.setSyslogHosts(sinkConfiguration.syslogHostList());
        logSinkMO.setFilters(convertToMO(sinkConfiguration.getFilters()));

        Map<String,String> properties = new HashMap<>();
        for (String propertyName : sinkConfiguration.getPropertyNames()) {
            properties.put(propertyName, sinkConfiguration.getProperty(propertyName));
        }
        logSinkMO.setProperties(properties);

        doSecurityZoneToMO(logSinkMO, sinkConfiguration);

        return logSinkMO;
    }

    private List<LogSinkMO.Category> convertToMO(String categoryStr) {
        List<LogSinkMO.Category> categoryList = new ArrayList<>();
        String[] categories = categoryStr.split(",");
        for( final String category : categories) {
            categoryList.add( LogSinkMO.Category.valueOf(category) );
        }
        return categoryList;
    }

    private List<LogSinkFilter> convertToMO(Map<String, List<String>> filters) {
        List<LogSinkFilter> sinkFilters = new ArrayList<>();
        for(String filterKey : filters.keySet()){
            LogSinkFilter filter = ManagedObjectFactory.createLogSinkFilter();
            filter.setType(filterKey);
            filter.setValues(filters.get(filterKey));
            sinkFilters.add(filter);
        }
        return sinkFilters;
    }

    @NotNull
    @Override
    public EntityContainer<SinkConfiguration> convertFromMO(@NotNull LogSinkMO LogSinkMO, SecretsEncryptor secretsEncryptor)
            throws InvalidResourceException {
        return convertFromMO(LogSinkMO, true, secretsEncryptor);
    }

    @NotNull
    @Override
    public EntityContainer<SinkConfiguration> convertFromMO(@NotNull LogSinkMO logSinkMO, boolean strict, SecretsEncryptor secretsEncryptor)
            throws InvalidResourceException {
        SinkConfiguration sinkConfiguration = new SinkConfiguration();
        sinkConfiguration.setId(logSinkMO.getId());
        if (logSinkMO.getVersion() != null) {
            sinkConfiguration.setVersion(logSinkMO.getVersion());
        }
        sinkConfiguration.setName(logSinkMO.getName());
        sinkConfiguration.setEnabled(logSinkMO.isEnabled());
        sinkConfiguration.setDescription(logSinkMO.getDescription());
        sinkConfiguration.setType(SinkConfiguration.SinkType.valueOf(logSinkMO.getType().toString()));
        sinkConfiguration.setCategories(TextUtils.join(",", logSinkMO.getCategories()).toString());
        sinkConfiguration.setSeverity(SinkConfiguration.SeverityThreshold.valueOf(logSinkMO.getSeverity().toString()));
        sinkConfiguration.setFilters(convertFromMO(logSinkMO.getFilters()));

        List<String> syslogHosts = logSinkMO.getSyslogHosts();

        if (null != syslogHosts) {
            // if Syslog sink, must have hosts
            if (sinkConfiguration.getType() == SinkConfiguration.SinkType.SYSLOG && syslogHosts.isEmpty()) {
                throw new InvalidResourceException(MISSING_VALUES, "Syslog hosts must be specified for Syslog Log Sink");
            }

            for (String host : syslogHosts) {
                validateSyslogHost(host);
                sinkConfiguration.addSyslogHostEntry(host);
            }
        }

        Map<String, String> props = logSinkMO.getProperties();
        if (props != null) {
            for (Map.Entry<String, String> entry : props.entrySet()) {
                sinkConfiguration.setProperty(entry.getKey(), entry.getValue());
            }
        }

        doSecurityZoneFromMO(logSinkMO, sinkConfiguration, strict);

        return new EntityContainer<>(sinkConfiguration);
    }

    private void validateSyslogHost(String host) throws InvalidResourceException {
        final Pair<String,String> hostAndPort = InetAddressUtil.getHostAndPort(host, null);

        if (!ValidationUtils.isValidDomain(InetAddressUtil.stripIpv6Brackets(hostAndPort.left)) ||
                !ValidationUtils.isValidInteger(hostAndPort.right,false,1,0xFFFF)) {
            throw new InvalidResourceException(INVALID_VALUES, "Invalid Syslog host: " + host);
        }
    }

    private Map<String, List<String>> convertFromMO(List<LogSinkFilter> filters) {
        Map<String, List<String>> sinkFilters = new HashMap<>();
        for(LogSinkFilter filter: filters){
            // todo validate values?
            sinkFilters.put(filter.getType(),filter.getValues());
        }
        return sinkFilters;
    }

    @NotNull
    @Override
    public Item<LogSinkMO> convertToItem(@NotNull LogSinkMO m) {
        return new ItemBuilder<LogSinkMO>(m.getName(), m.getId(), EntityType.LOG_SINK.name())
                .setContent(m)
                .build();
    }
}
