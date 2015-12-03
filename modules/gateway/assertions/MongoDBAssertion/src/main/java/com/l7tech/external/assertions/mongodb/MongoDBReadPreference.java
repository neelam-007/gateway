package com.l7tech.external.assertions.mongodb;


public enum MongoDBReadPreference {
    Primary("Primary"),
    PrimaryPreferred("Primary Preferred"),
    Secondary("Secondary"),
    SecondaryPreferred("Secondary Preferred"),
    Nearest("Nearest");

    private String value;

    private MongoDBReadPreference(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }


}

 