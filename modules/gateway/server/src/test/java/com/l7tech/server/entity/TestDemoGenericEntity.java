package com.l7tech.server.entity;

import com.l7tech.policy.GenericEntity;

/**
 *
 */
public class TestDemoGenericEntity extends GenericEntity {
    private int age;
    private boolean playsTrombone;

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
}