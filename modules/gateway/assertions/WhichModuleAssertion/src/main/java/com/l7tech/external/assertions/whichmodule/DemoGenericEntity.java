package com.l7tech.external.assertions.whichmodule;

import com.l7tech.policy.GenericEntity;

/**
 * A demo entity that can be managed by the GenericEntityManager.
 */
public class DemoGenericEntity extends GenericEntity {
    private String name;
    private int age;
    private boolean playsTrombone;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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
}
