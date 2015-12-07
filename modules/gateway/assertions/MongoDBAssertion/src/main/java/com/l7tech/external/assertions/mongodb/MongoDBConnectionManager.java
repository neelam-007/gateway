package com.l7tech.external.assertions.mongodb;

import com.l7tech.common.io.SingleCertX509KeyManager;
import com.l7tech.external.assertions.mongodb.entity.MongoDBConnectionEntity;
import com.l7tech.external.assertions.mongodb.entity.MongoDBConnectionEntityAdminImpl;
import com.l7tech.gateway.common.security.keystore.SsgKeyEntry;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.security.prov.JceProvider;
import com.l7tech.server.DefaultKey;
import com.l7tech.server.security.keystore.SsgKeyStoreManager;
import com.l7tech.server.security.password.SecurePasswordManager;
import com.mongodb.*;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.X509TrustManager;
import java.net.UnknownHostException;
import java.security.*;
import java.text.ParseException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created with IntelliJ IDEA.
 * User: joe
 * Date: 6/3/13
 * Time: 4:29 PM
 * To change this template use File | Settings | File Templates.
 */
public final class MongoDBConnectionManager {
    private static final Logger logger = Logger.getLogger(MongoDBConnectionManager.class.getName());

    private static Map<Goid, MongoDBConnection> connectionMap = new HashMap<Goid, MongoDBConnection>();
    private SecurePasswordManager securePasswordManager = null;
    private static MongoDBConnectionManager instance = null;
    private X509TrustManager trustManager = null;
    private SsgKeyStoreManager keyStoreManager = null;
    private SecureRandom secureRandom = null;
    private DefaultKey defaultKey = null;

    public static synchronized void createMongoDBConnectionManager(SecurePasswordManager securePasswordManager, SsgKeyStoreManager keyStoreManager, X509TrustManager trustManager,
                                                                   SecureRandom secureRandom, DefaultKey defaultKey) {

        if (instance == null) {
            instance = new MongoDBConnectionManager(securePasswordManager, keyStoreManager, trustManager, secureRandom, defaultKey);
        }
    }

    public static MongoDBConnectionManager getInstance() {
        return instance;
    }

    //For testing only
    static void setInstance(MongoDBConnectionManager instance) {
        MongoDBConnectionManager.instance = instance;
    }


    private MongoDBConnectionManager(SecurePasswordManager securePasswordManager, SsgKeyStoreManager keyStoreManager, X509TrustManager trustManager,
                                     SecureRandom secureRandom, DefaultKey defaultKey) {
        this.defaultKey = defaultKey;
        this.securePasswordManager = securePasswordManager;
        this.keyStoreManager = keyStoreManager;
        this.trustManager = trustManager;
        this.secureRandom = secureRandom;
    }

    public void loadMongoDBConnections()  {

        List<MongoDBConnectionEntity> mongoDBConnections = null;

        try {
            mongoDBConnections = new ArrayList<MongoDBConnectionEntity>();
            Collection<MongoDBConnectionEntity> mongoDBConnectionEntities = MongoDBConnectionEntityAdminImpl.getInstance(null).findByType();
            if (mongoDBConnectionEntities != null) {
                mongoDBConnections.addAll(mongoDBConnectionEntities);
            }
        } catch (FindException e) {
            logger.warning("Unable to load MongoDB Connection " + e.getMessage());
        }

        for (MongoDBConnectionEntity mongoDBConnectionEntity : mongoDBConnections) {
            try {
                MongoDBConnection mongoDBConnection = createConnection(mongoDBConnectionEntity);
                connectionMap.put(mongoDBConnectionEntity.getGoid(), mongoDBConnection);
            } catch (Exception e) {
                logger.log(Level.INFO, "Unable to close mongo connection", e);

            }
        }
    }

    public MongoDBConnection getConnection(Goid connectionGoid) {
        return connectionMap.get(connectionGoid);
    }

    public void closeConnections() {

            for (MongoDBConnection mongoDBConnection : connectionMap.values()){
                try {
                    mongoDBConnection.getMongoClient().close();
                } catch (Exception e) {
                    logger.log(Level.INFO, "Unable to close mongo connection", e);
                }
            }

    }

    public void addConnection(MongoDBConnectionEntity mongoDBConnectionEntity) {

            MongoDBConnection mongoDBConnection = createConnection(mongoDBConnectionEntity);
            connectionMap.put(mongoDBConnectionEntity.getGoid(), mongoDBConnection);
    }

    public void removeConnection(MongoDBConnectionEntity mongoDBConnectionEntity) {
        MongoDBConnection mongoDBConnection = connectionMap.get(mongoDBConnectionEntity.getGoid());

        if (mongoDBConnection.getMongoClient() != null) {
            mongoDBConnection.getMongoClient().close();
        }
        connectionMap.remove(mongoDBConnectionEntity.getGoid());
    }

    public void removeConnection(Goid goid) {

        boolean entityFound = false;
        MongoDBConnectionEntity entity = null;
        Iterator<Goid> iter = connectionMap.keySet().iterator();

        while (iter.hasNext() && !entityFound) {
            entity = connectionMap.get(iter.next()).getMongoDBConnectionEntity();

            if (entity.getGoid().compareTo(goid) == 0)
                entityFound = true;
        }

        if (entityFound)
            removeConnection(entity);
    }

    public void updateConnection(MongoDBConnectionEntity mongoDBConnectionEntity) {
        removeConnection(mongoDBConnectionEntity);
        addConnection(mongoDBConnectionEntity);
    }

    private MongoDBConnection createConnection(MongoDBConnectionEntity mongoDBConnectionEntity) {

        MongoClient mongoClient = null;
        MongoDBEncryption authMethod;

        if (null == mongoDBConnectionEntity.getUri() || mongoDBConnectionEntity.getUri().isEmpty() || null == mongoDBConnectionEntity.getPort() || mongoDBConnectionEntity.getPort().isEmpty() || null == mongoDBConnectionEntity.getDatabaseName() || mongoDBConnectionEntity.getDatabaseName().isEmpty()){
            return new MongoDBConnection(mongoDBConnectionEntity, mongoClient);
        }

        //if no authentication type has been set, set it to default
        if (null == mongoDBConnectionEntity.getAuthType()) {
            authMethod = MongoDBEncryption.NO_ENCRYPTION;
            mongoDBConnectionEntity.setAuthType(MongoDBEncryption.NO_ENCRYPTION.name());
        } else {
            authMethod = MongoDBEncryption.valueOf(mongoDBConnectionEntity.getAuthType());
        }

        try {

            //Get list of all servers
            List<ServerAddress> serverList = getServerList(mongoDBConnectionEntity);

            MongoCredential credential;
            MongoClientOptions.Builder optionBuilder = MongoClientOptions.builder();

            //Basic Authentication with username/password with no encryption
            if (authMethod.equals(MongoDBEncryption.NO_ENCRYPTION) && useAuthentication(mongoDBConnectionEntity)) {
                credential = MongoCredential.createMongoCRCredential(mongoDBConnectionEntity.getUsername(), mongoDBConnectionEntity.getDatabaseName(), getPassword(mongoDBConnectionEntity));
                setMongoDBOptions(optionBuilder, mongoDBConnectionEntity);
                mongoClient = new MongoClient(serverList, Arrays.asList(credential), optionBuilder.build());

                //If SSL or X509 Authentication is being used
            } else if (authMethod.equals((MongoDBEncryption.SSL)) || authMethod.equals(MongoDBEncryption.X509_Auth)) {

                //add SSLSocketFactory to mongoClientOptions
                createSSLSocketFactory(optionBuilder, mongoDBConnectionEntity);
                //Set additional options
                setMongoDBOptions(optionBuilder, mongoDBConnectionEntity);
                MongoClientOptions mongoClientOptions = optionBuilder.build();

                //Using SSL with authentication
                if (authMethod.equals((MongoDBEncryption.SSL)) && useAuthentication(mongoDBConnectionEntity)) {
                    credential = MongoCredential.createMongoCRCredential(mongoDBConnectionEntity.getUsername(), mongoDBConnectionEntity.getDatabaseName(), getPassword(mongoDBConnectionEntity));
                    mongoClient = new MongoClient(serverList, Arrays.asList(credential), mongoClientOptions);

                    //Using X509 with username
                } else if (authMethod.equals(MongoDBEncryption.X509_Auth) && usernameProvided(mongoDBConnectionEntity)) {
                    credential = MongoCredential.createMongoX509Credential(mongoDBConnectionEntity.getUsername());
                    mongoClient = new MongoClient(serverList, Arrays.asList(credential), mongoClientOptions);
                } else {
                    mongoClient = new MongoClient(serverList, mongoClientOptions);
                }
            } else {
                setMongoDBOptions(optionBuilder, mongoDBConnectionEntity);
                mongoClient = new MongoClient(serverList, optionBuilder.build());
            }
        } catch (NoSuchAlgorithmException | UnrecoverableKeyException | UnknownHostException | FindException | ParseException | KeyStoreException | KeyManagementException e ) {
            logger.log(Level.WARNING, "Unable to create a MongoDB Connection", e);
        }
        return new MongoDBConnection(mongoDBConnectionEntity, mongoClient);
    }

    private boolean useAuthentication(MongoDBConnectionEntity entity) {
        return (entity.getUsername() != null && !entity.getUsername().isEmpty()) ||
                (entity.getPassword() != null && !entity.getPassword().isEmpty()) ||
                (!Goid.isDefault(entity.getStoredPasswordGoid()));
    }

    private boolean usernameProvided(MongoDBConnectionEntity entity) {
        return (entity.getUsername() != null && !entity.getUsername().isEmpty());
    }

    private char[] getPassword(MongoDBConnectionEntity entity) throws FindException, ParseException {

        if (entity.getPassword() != null && !entity.getPassword().isEmpty()) {
            return entity.getPassword().toCharArray();
        } else if (!Goid.isDefault(entity.getStoredPasswordGoid())) {
            return securePasswordManager.decryptPassword(securePasswordManager.findByPrimaryKey(entity.getStoredPasswordGoid()).getEncodedPassword());
        } else {
            return new char[]{};
        }
    }

    private void createSSLSocketFactory(MongoClientOptions.Builder mongoClientOptions, MongoDBConnectionEntity mongoDBConnectionEntity) throws  FindException, KeyStoreException, UnrecoverableKeyException, NoSuchAlgorithmException, KeyManagementException {

        KeyManager[] keyManagers;
        if (mongoDBConnectionEntity.isUsesNoKey()) {
            keyManagers = null;
        } else if (mongoDBConnectionEntity.isUsesDefaultKeyStore()) {
            keyManagers = defaultKey.getSslKeyManagers();
        } else {
            SsgKeyEntry keyEntry = keyStoreManager.lookupKeyByKeyAlias(mongoDBConnectionEntity.getKeyAlias(), mongoDBConnectionEntity.getNonDefaultKeystoreId());
            keyManagers = new KeyManager[]{new SingleCertX509KeyManager(keyEntry.getCertificateChain(), keyEntry.getPrivate())};
        }

        X509TrustManager[] trustManagers = new X509TrustManager[]{trustManager};

        //create the SSLContext for the provided protocol
        Provider provider = JceProvider.getInstance().getProviderFor("SSLContext.TLSv1");
        SSLContext sslContext = SSLContext.getInstance("TLSv1", provider);

        //initialize the SSLContext with an array of keymanagers, trustmanager, and random number generator
        sslContext.init(keyManagers, trustManagers, secureRandom);
        SSLSocketFactory sslFactory = sslContext.getSocketFactory();

        mongoClientOptions.socketFactory(sslFactory);

    }

    private void setMongoDBOptions(MongoClientOptions.Builder mongoClientOptions, MongoDBConnectionEntity mongoDBConnectionEntity) {
        String readReferenceValue = null;
        try {
            readReferenceValue = mongoDBConnectionEntity.getReadPreference();
            if (readReferenceValue != null) {
                mongoClientOptions.readPreference(ReadPreference.valueOf(readReferenceValue));
            } else {
                logger.log(Level.WARNING, "Invalid Read Preference Mode. Setting Read Preference Mode to primary.");
                mongoClientOptions.readPreference(ReadPreference.primary());
                mongoDBConnectionEntity.setReadPreference(ReadPreference.primary().getName());
            }

        } catch (IllegalArgumentException e) {
            logger.log(Level.WARNING, "Invalid Read Preference Mode: " + readReferenceValue + " . Setting Read Preference Mode to primary.");
            mongoClientOptions.readPreference(ReadPreference.primary());
        }
    }

    private List<ServerAddress> getServerList(MongoDBConnectionEntity mongoDBConnectionEntity) throws UnknownHostException{
        // The URI can be in the form of: ip1:port1,ip2:port2,ip3:port3
        // The ports are optional. If not provided (e.g. ip1,ip2:port2,ip3),
        // the separate port field will be used as port for the respective ip.
        List<ServerAddress> serverList = new ArrayList<>();
        for (String uri : mongoDBConnectionEntity.getUri().split(",")) {
            int colonIndex = uri.indexOf(':');
            int port;
            String host;
            if (colonIndex > -1) {
                port = Integer.parseInt(uri.substring(colonIndex + 1));
                host = uri.substring(0, colonIndex).trim();
            } else {
                port = Integer.parseInt(mongoDBConnectionEntity.getPort());
                host = uri.trim();
            }
            serverList.add(new ServerAddress(host, port));
        }

        return serverList;
    }
}