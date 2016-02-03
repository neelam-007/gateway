package com.l7tech.external.assertions.apiportalintegration.server;

import com.l7tech.external.assertions.apiportalintegration.ProcessIncrementAssertion;
import com.l7tech.external.assertions.apiportalintegration.server.apikey.manager.ApiKey;
import com.l7tech.external.assertions.apiportalintegration.server.apikey.manager.ApiKeyManager;
import com.l7tech.external.assertions.apiportalintegration.server.resource.*;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.objectmodel.ObjectModelException;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.server.cluster.ClusterPropertyManager;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import com.l7tech.util.ExceptionUtils;
import org.apache.commons.collections.CollectionUtils;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonToken;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.springframework.context.ApplicationContext;
import org.springframework.dao.DataAccessException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Server side implementation of the ProcessIncrementAssertion.
 *
 * @author chean22, 1/19/2016
 */
public class ServerProcessIncrementAssertion extends AbstractServerAssertion<ProcessIncrementAssertion> {
    private static final Logger LOGGER = Logger.getLogger(ServerProcessIncrementAssertion.class.getName());

    private static final String ENTITY_TYPE_APPLICATION = "APPLICATION";
    private static final String APP_INCREMENT_START_PROP = "portal.application.increment.start";
    private static final String ERROR_MSG = "Database transaction failed";

    private final String[] variablesUsed;
    private final PlatformTransactionManager transactionManager;
    private final ApiKeyResourceTransformer appTransformer = ApiKeyResourceTransformer.getInstance();
    private PortalGenericEntityManager<ApiKey> portalGenericEntityManager;
    private ClusterPropertyManager clusterPropertyManager;

    public ServerProcessIncrementAssertion(ProcessIncrementAssertion assertion, ApplicationContext context) throws PolicyAssertionException, JAXBException {
        super(assertion);
        this.variablesUsed = assertion.getVariablesUsed();
        transactionManager = context.getBean("transactionManager", PlatformTransactionManager.class);
        setPortalGenericEntityManager(ApiKeyManager.getInstance(context));
        clusterPropertyManager = context.getBean("clusterPropertyManager", ClusterPropertyManager.class);
    }

    // for testing
    void setPortalGenericEntityManager(PortalGenericEntityManager<ApiKey> portalGeenricEntityManager) {
        this.portalGenericEntityManager = portalGeenricEntityManager;
    }

    @Override
    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        try {
            Map<String, Object> vars = context.getVariableMap(this.variablesUsed, getAudit());

            Object entityType = vars.get(assertion.getVariablePrefix() + "." + ProcessIncrementAssertion.SUFFIX_TYPE);
            Object jsonPayload = vars.get(assertion.getVariablePrefix() + "." + ProcessIncrementAssertion.SUFFIX_JSON);

            // validate inputs
            if (entityType == null) {
                throw new PolicyAssertionException(assertion, "Assertion must supply an entity type");
            }
            if (!entityType.equals(ENTITY_TYPE_APPLICATION)) {
                throw new PolicyAssertionException(assertion, "Not supported entity type: " + entityType);
            }

            if (jsonPayload == null || !(jsonPayload instanceof String) || ((String) jsonPayload).isEmpty()) {
                throw new PolicyAssertionException(assertion, "Assertion must supply a JSON string payload");
            }

            JsonFactory jsonFactory = new JsonFactory();
            JsonParser jsonParser = jsonFactory.createJsonParser((String) jsonPayload);
            ObjectMapper mapper = new ObjectMapper();
            JsonToken current = jsonParser.nextToken();
            if (current != JsonToken.START_OBJECT) {
                throw new IOException("Invalid JSON input");
            }
            final ApplicationJson applicationJson = mapper.readValue(jsonParser, ApplicationJson.class);
            final String incrementStartStr = clusterPropertyManager.getProperty(APP_INCREMENT_START_PROP);
            if (incrementStartStr == null) {
                throw new IOException(APP_INCREMENT_START_PROP + " cluster property not found");
            }
            final long incrementStart = Long.parseLong(incrementStartStr);

            // apply changes to db

            List<Map<String, String>> results = (List<Map<String, String>>) applyChanges(applicationJson);

            // save result
            context.setVariable(assertion.getVariablePrefix() + "." + ProcessIncrementAssertion.SUFFIX_POSTBACK, buildJsonPostBack(incrementStart, applicationJson, results));

        } catch (Exception ex) {
            final String errorMsg = "Error Applying Increment";
            logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO,
                    new String[]{errorMsg + ": " + ExceptionUtils.getMessage(ex)}, ExceptionUtils.getDebugException(ex));
            return AssertionStatus.FAILED;
        }
        return AssertionStatus.NONE;
    }

    Object applyChanges(final ApplicationJson applicationJson) throws IOException {
        List<String> deletedAppList = applicationJson.getDeletedIds();
        List<ApplicationEntity> appListFromJson = applicationJson.getNewOrUpdatedEntities();
        Map<String, ApiKey> entitiesMap = new HashMap<>();

        for (final ApplicationEntity applicationEntity : appListFromJson) {
            ApiKey apiKey = appTransformer.resourceToEntity(applicationEntity);
            entitiesMap.put(apiKey.getName(), apiKey);
        }
        final Map<String, ApiKey> newOrUpdatedEntities = Collections.unmodifiableMap(entitiesMap);
        final List<String> deletedAppIds = Collections.unmodifiableList(deletedAppList);

        // update generic entities
        try {
            final TransactionTemplate tt = new TransactionTemplate(transactionManager);
            return tt.execute(new TransactionCallback() {
                @Override
                public Object doInTransaction(final TransactionStatus transactionStatus) {
                    try {
                        final List<ApiKey> all = portalGenericEntityManager.findAll();
                        final Set<String> existingNames = new HashSet<>();
                        final Map<String, ApiKey> existingEntities = new HashMap<>();
                        for (final ApiKey existingEntity : all) {
                            existingEntities.put(existingEntity.getApplicationId(), existingEntity);
                            existingNames.add(existingEntity.getName());
                        }
                        // insert
                        final Collection<String> toAdd = CollectionUtils.subtract(newOrUpdatedEntities.keySet(), existingNames);
                        for (final String add : toAdd) {
                            portalGenericEntityManager.add(newOrUpdatedEntities.get(add));
                        }
                        // update
                        final Collection<String> toUpdate = CollectionUtils.intersection(newOrUpdatedEntities.keySet(), existingNames);
                        for (final String update : toUpdate) {
                            portalGenericEntityManager.update(newOrUpdatedEntities.get(update));
                        }
                        // delete
                        for (final String id : deletedAppIds) {
                            if (existingEntities.get(id) != null) {
                                LOGGER.log(Level.FINE, "Deleting portal application: " + existingEntities.get(id).getName());
                                portalGenericEntityManager.delete(existingEntities.get(id).getName());
                            }
                        }
                        // set end time in cluster property
                        clusterPropertyManager.putProperty(APP_INCREMENT_START_PROP, String.valueOf(applicationJson.getIncrementStart()));

                        return null;
                    } catch (ObjectModelException e) {
                        transactionStatus.setRollbackOnly();

                        LOGGER.log(Level.WARNING, "Database error, rolling back transaction.");

                        // return error IDs, one txn so all IDs or nothing
                        List<Map<String, String>> results = new ArrayList<>();
                        for (final ApiKey api : newOrUpdatedEntities.values()) {
                            Map<String, String> error = new HashMap();
                            error.put("id", api.getApplicationId());
                            error.put("msg", ERROR_MSG);
                            results.add(error);
                        }
                        for (final String id : deletedAppIds) {
                            Map<String, String> error = new HashMap();
                            error.put("id", id);
                            error.put("msg", ERROR_MSG);
                            results.add(error);
                        }
                        return results;
                    }
                }
            });
        } catch (TransactionException | DataAccessException e) {
            throw new IOException(ExceptionUtils.getMessage(e), e);
        }
    }

    String buildJsonPostBack(final long incrementStart, final ApplicationJson applicationJson, final List<Map<String, String>> results) throws IOException {
        PortalSyncPostbackJson postback = new PortalSyncPostbackJson();
        // currently updates are done in one db txn, therefore either "ok" or "error" status, "partial" isn't used
        if (results == null) {
            postback.setIncrementStatus("ok");
        } else {
            postback.setIncrementStatus("error");
        }
        postback.setIncrementStart(incrementStart);
        postback.setIncrementEnd(applicationJson.getIncrementStart());
        postback.setEntityType(applicationJson.getEntityType());
        postback.setEntityErrors(null);
        if (results != null && !results.isEmpty()) {
            postback.setErrorMessage(ERROR_MSG);
        }

        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonSerialize.Inclusion.NON_NULL);
        String jsonString;
        try {
            jsonString = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(postback);
        } catch (IOException ioe) {
            throw new IOException("Unable to write json string: " + ExceptionUtils.getMessage(ioe), ioe);
        }
        return jsonString;
    }
}
