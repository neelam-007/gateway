package com.l7tech.external.assertions.mongodb;

import com.l7tech.external.assertions.mongodb.entity.MongoDBConnectionEntity;
import com.mongodb.MongoClient;

/**
 * Created with IntelliJ IDEA.
 * User: joe
 * Date: 6/14/13
 * Time: 2:24 PM
 * To change this template use File | Settings | File Templates.
 */
public class MongoDBConnection {
    final private MongoDBConnectionEntity mongoDBConnectionEntity;
    final private MongoClient mongoClient;

    public MongoDBConnection(final MongoDBConnectionEntity mongoDBConnectionEntity, final MongoClient mongoClient){
        this.mongoDBConnectionEntity = mongoDBConnectionEntity;
        this.mongoClient = mongoClient;
    }

    public MongoClient getMongoClient() {
        return mongoClient;
    }

    public MongoDBConnectionEntity getMongoDBConnectionEntity() {
        return mongoDBConnectionEntity;
    }
}
