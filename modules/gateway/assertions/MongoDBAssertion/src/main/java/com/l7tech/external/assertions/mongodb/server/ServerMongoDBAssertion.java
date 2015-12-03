package com.l7tech.external.assertions.mongodb.server;

import com.l7tech.external.assertions.mongodb.MongoDBAssertion;
import com.l7tech.external.assertions.mongodb.MongoDBConnection;
import com.l7tech.external.assertions.mongodb.MongoDBConnectionManager;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import com.l7tech.server.policy.variable.ExpandVariables;
import com.mongodb.*;
import com.mongodb.util.JSON;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Server side implementation of the MongoDBAssertion.
 *
 * @see com.l7tech.external.assertions.mongodb.MongoDBAssertion
 */
public class ServerMongoDBAssertion extends AbstractServerAssertion<MongoDBAssertion> {
    private final String[] variablesUsed;
    private static final Logger logger = Logger.getLogger(ServerMongoDBAssertion.class.getName());


    public ServerMongoDBAssertion(final MongoDBAssertion assertion) throws PolicyAssertionException {
        super(assertion);

        this.variablesUsed = assertion.getVariablesUsed();

        if (assertion.getConnectionGoid() == null) {
            throw new PolicyAssertionException(assertion, "Assertion must supply a connection name");
        }
    }

    public AssertionStatus checkRequest(final PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        if (context == null) throw new IllegalStateException("Policy Enforcement Context cannot be null.");

        MongoDBConnection mongoDBConnection = MongoDBConnectionManager.getInstance().getConnection(assertion.getConnectionGoid());

        if ((mongoDBConnection == null) || (mongoDBConnection.getMongoClient() == null)) {
            getAudit().logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, "Error retrieving MongoDB Connection, MongoClient is null");
            return AssertionStatus.FAILED;
        }

        DB db = mongoDBConnection.getMongoClient().getDB(mongoDBConnection.getMongoDBConnectionEntity().getDatabaseName());
        DBCollection collection = db.getCollection(assertion.getCollectionName());

        //Resolve context variable references within query document
        Map<String, Object> vars = context.getVariableMap(Syntax.getReferencedNames(assertion.getQueryDocument()), getAudit());
        String queryDocument = ExpandVariables.process(assertion.getQueryDocument(), vars, getAudit(), true);
        if (null == queryDocument || queryDocument.isEmpty()) {
            getAudit().logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, "Error Query Document cannot be empty");
            return AssertionStatus.FAILED;
        }

        //Resolve context variable references within projection document
        Map<String, Object> projectionVars = context.getVariableMap(Syntax.getReferencedNames(assertion.getProjectionDocument()), getAudit());
        String projectionDocument = ExpandVariables.process(assertion.getProjectionDocument(), projectionVars, getAudit(), true);

        //Resolve context variable references within update document
        Map<String, Object> updateVars = context.getVariableMap(Syntax.getReferencedNames(assertion.getUpdateDocument()), getAudit());
        String updateDocument = ExpandVariables.process(assertion.getUpdateDocument(), updateVars, getAudit(), true);

        DBObject dbQueryObject;
        DBObject dbProjectionObject;
        DBObject dbUpdateObject;
        WriteConcern writeConcern = WriteConcern.valueOf(assertion.getWriteConcern());
        WriteResult result;
        DBCursor resultCursor;
        try {
            switch (assertion.getOperation()) {
                case "FIND":
                    // Performance improvement can be made by providing checkbox whether query will just return the number of matches.
                    // If checked, then do not create the hashmap store the results to return.
                    dbQueryObject = (DBObject) JSON.parse(queryDocument);
                    dbProjectionObject = (DBObject) JSON.parse(projectionDocument);
                    resultCursor = collection.find(dbQueryObject, dbProjectionObject);
                    int numFound = resultCursor.count();

                    StringBuilder resultString = new StringBuilder();
                    //Get resultCursor into map
                    Map<String, List<Object>> resultMap = new HashMap<String, List<Object>>();

                    while (resultCursor.hasNext()) {
                        DBObject dbObject = resultCursor.next();
                        resultString.append(dbObject.toString());
                        for (String key : dbObject.keySet()) {
                            List<Object> col = resultMap.get(key);

                            if (col == null) {
                                col = new ArrayList<Object>();
                                resultMap.put(key, col);
                            }

                            Object o = dbObject.get(key);
                            col.add(o);
                        }
                        resultString.append("\n");
                    }

                    //Get results into context variable
                    String prefix = assertion.getPrefix();
                    context.setVariable(prefix + ".queryresult.count", numFound);

                    int row = 0;
                    for (String key : resultMap.keySet()) {
                        if (resultMap.get(key) != null) {
                            row = resultMap.get(key).size();
                            context.setVariable(prefix + "." + key, resultMap.get(key).toArray());
                        }
                    }

                    context.setVariable(prefix + ".output", resultString.toString());

                    if (row < 1 && assertion.isFailIfNoResults()) {
                        getAudit().logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, "Failing assertion due to no result returned.");
                        return AssertionStatus.FAILED;
                    }
                    break;
                case "INSERT":
                    dbQueryObject = (DBObject) JSON.parse(queryDocument);
                    result = collection.insert(dbQueryObject, writeConcern);
                    context.setVariable(assertion.getPrefix() + ".queryresult.count", result.getN());

                    // Need to use the new error handling API when using new MongoDB 3.0 drivers.
                    if (result.getError() != null) {
                        getAudit().logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, "Failed to INSERT document into the database. " + result.getError());
                        return AssertionStatus.FAILED;
                    }
                    break;
                case "UPDATE":
                    dbQueryObject = (DBObject) JSON.parse(queryDocument);
                    dbUpdateObject = (DBObject) JSON.parse(updateDocument);
                    if (null == updateDocument || updateDocument.isEmpty()) {
                        getAudit().logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, "Error with MongoDB UPDATE operation. Update Document cannot be empty");
                        return AssertionStatus.FAILED;
                    }
                    result = collection.update(dbQueryObject, dbUpdateObject, assertion.isEnableUpsert(), assertion.isEnableMulti(), writeConcern);

                    context.setVariable(assertion.getPrefix() + ".queryresult.count", result.getN());


                    // Need to use the new error handling API when using new MongoDB 3.0 drivers.
                    if ( result.getError() != null ) {
                        getAudit().logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, "Failed to UPDATE document(s) in the database. " + result.getError());
                        return AssertionStatus.FAILED;
                    }

                    if (assertion.isFailIfNoResults() && result.getN() <= 0) {
                        return AssertionStatus.FAILED;
                    }

                    break;
                case "DELETE":
                    dbQueryObject = (DBObject) JSON.parse(queryDocument);
                    result = collection.remove(dbQueryObject);

                    context.setVariable(assertion.getPrefix() + ".queryresult.count", result.getN());

                    // Need to use the new error handling API when using new MongoDB 3.0 drivers.
                    if (result.getError() != null) {
                        getAudit().logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, "Failed to DELETE document(s) in the database. " + result.getError());
                        return AssertionStatus.FAILED;
                    }

                    if (assertion.isFailIfNoResults() && result.getN() <= 0) {
                        return AssertionStatus.FAILED;
                    }
                    break;
                default:
                    logger.warning("Unhandled/invalid assertion operation.");
            }

        } catch (Exception e) { //Will use general Exception since it can throw run-time exception and the MongoException and Exception will be handled the same.
            getAudit().logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, "Error performing MongoDB operation:" + assertion.getOperation() + ". The connection properties and error message are:" + e.getMessage());
            return AssertionStatus.FAILED;
        }


        return AssertionStatus.NONE;
    }

    /*
     * Called reflectively by module class loader when module is unloaded, to ask us to clean up any globals
     * that would otherwise keep our instances from getting collected.
     *
     * DELETEME if not required.
     */
    public static void onModuleUnloaded() {
        // This assertion doesn't have anything to do in response to this, but it implements this anyway
        // since it will be used as an example by future modular assertion authors
    }

}
