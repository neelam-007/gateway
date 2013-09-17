package com.l7tech.server.entity;

import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.GenericEntity;

import java.util.Hashtable;

/**
 *
 */
public class TestDemoGenericEntity extends GenericEntity {
    private int age;
    private boolean playsTrombone;
    private Hashtable<String,String> hashtable = new Hashtable<>();
    private Goid testGoid;

    {
        hashtable.put("defaultEntry", "blah");
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public boolean isPlaysTrombone() {
        return playsTrombone;
    }

    public void setPlaysTrombone(boolean playsTrombone) {
        this.playsTrombone = playsTrombone;
    }

    public Hashtable<String,String> getHashtable() {
        return hashtable;
    }

    public void setHashtable(Hashtable<String,String> hashtable) {
        this.hashtable = hashtable;
    }

    public Goid getTestGoid() {
        return testGoid;
    }

    public void setTestGoid(Goid testGoid) {
        this.testGoid = testGoid;
    }
}