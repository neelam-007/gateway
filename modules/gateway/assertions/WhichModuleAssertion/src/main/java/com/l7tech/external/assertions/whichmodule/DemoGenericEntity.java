package com.l7tech.external.assertions.whichmodule;

import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.GenericEntity;
import com.l7tech.search.Dependency;

/**
 * A demo entity that can be managed by the GenericEntityManager.
 */
public class DemoGenericEntity extends GenericEntity {
    private int age;
    private boolean playsTrombone;
    private Goid serviceId;

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

    @Dependency(methodReturnType = Dependency.MethodReturnType.GOID, type = Dependency.DependencyType.SERVICE)
    public Goid getServiceId() {
        return serviceId;
    }

    public void setServiceId(Goid serviceId) {
        this.serviceId = serviceId;
    }
}
