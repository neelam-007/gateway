package com.l7tech.external.assertions.mongodb.entity;


import com.l7tech.external.assertions.mongodb.MongoDBEncryption;
import com.l7tech.external.assertions.mongodb.MongoDBReadPreference;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.migration.Migration;
import com.l7tech.objectmodel.migration.MigrationMappingSelection;
import com.l7tech.objectmodel.migration.PropertyResolver;
import com.l7tech.policy.GenericEntity;
import com.l7tech.policy.assertion.OptionalPrivateKeyable;
import com.l7tech.search.Dependency;

/**
 * Created with IntelliJ IDEA.
 * User: joe
 * Date: 1/25/13
 * Time: 5:50 AM
 * To change this template use File | Settings | File Templates.
 */

public class MongoDBConnectionEntity extends GenericEntity implements OptionalPrivateKeyable {

    private String name;
    private String uri;
    private String port;
    private String databaseName;
    private String username;
    private String password;
    private Goid storedPasswordGoid;

    private String authType = MongoDBEncryption.NO_ENCRYPTION.name();

    protected boolean usesDefaultKeyStore = true;
    protected boolean usesNoKey = true;
    protected String keyAlias;
    protected Goid nonDefaultKeystoreId;




    private String readPreference = MongoDBReadPreference.Primary.name();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getPort() {
        return port;
    }

    public void setPort(String port) {
        this.port = port;
    }

    public String getAuthType() {
        return authType;
    }

    public String getDatabaseName() {
        return databaseName;
    }

    public void setDatabaseName(String databaseName) {
        this.databaseName = databaseName;
    }

    public String getPassword() {
        return password;
    }

    public String getReadPreference() {
        return readPreference;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }


    public void setAuthType(String authType) {
        this.authType = authType;
    }

    public void setReadPreference(String readPreference) {
        this.readPreference = readPreference;
    }

    @Dependency(methodReturnType = Dependency.MethodReturnType.GOID, type = Dependency.DependencyType.SECURE_PASSWORD)
    public Goid getStoredPasswordGoid() {
        return storedPasswordGoid;
    }

    public void setStoredPasswordGoid(Goid storedPasswordGoid) {
        this.storedPasswordGoid = storedPasswordGoid;
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public boolean isUsesNoKeyAllowed() {
        return true;
    }

    @Override
    public boolean isUsesNoKey() {
        return usesNoKey;
    }

    @Override
    public void setUsesNoKey(boolean usesNoKey) {
        this.usesNoKey = usesNoKey;
    }

    @Override
    public boolean isUsesDefaultKeyStore() {
        return usesDefaultKeyStore;
    }

    @Override
    public void setUsesDefaultKeyStore(boolean usesDefault) {
        this.usesDefaultKeyStore = usesDefault;
    }

    @Override
    @Migration(mapName = MigrationMappingSelection.REQUIRED, export = false, resolver = PropertyResolver.Type.SSGKEY)
    public Goid getNonDefaultKeystoreId() {
        return nonDefaultKeystoreId;
    }

    @Override
    public void setNonDefaultKeystoreId(Goid nonDefaultId) {
        this.nonDefaultKeystoreId = nonDefaultId;
    }

    @Override
    public String getKeyAlias() {
        return keyAlias;
    }

    @Override
    public void setKeyAlias(String keyAlias) {
        this.keyAlias = keyAlias;
    }
}
