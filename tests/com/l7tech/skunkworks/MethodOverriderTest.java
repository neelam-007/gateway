/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.skunkworks;

import com.l7tech.common.util.Locator;

import java.lang.reflect.Proxy;
import java.util.Date;
import java.util.Calendar;

/**
 * Experimenting with using Proxy to override single methods of an interface instance without requiring code changes
 * if the interface has methods added or removed at a later date.
 *
 * @author mike
 */
public class MethodOverriderTest {
    interface Dated {
        Date getDate();
    }

    public static void main(String[] args) {
        final Dated originalDated = new Dated() {
            public Date getDate() {
                return new Date();
            }
        };

        // Override dates on Thursdays
        // TODO implement the overrider
        GetterOverrider overrider = (GetterOverrider)Locator.getDefault().lookup(GetterOverrider.class);


        Dated dated = (Dated)overrider.overrideGetters(originalDated,
                                                       new GetterOverride[] { new GetterOverride("getDate", new Giver() {
            public Object get() throws Throwable {
                Date d = originalDated.getDate();
                Calendar c = Calendar.getInstance();
                c.setTime(d);
                if (c.get(Calendar.DAY_OF_WEEK) == c.get(Calendar.THURSDAY))
                    return null;
                else
                    return d;
            }
        })});

        System.out.println(dated.getDate());
    }
}

interface Giver {
    // TODO support overriding more than just zero-argument "get" methods
    Object get() throws Throwable;
}

final class GetterOverride {
    String methodName;
    Giver giver;
    // TODO first arg should probably be Method not String
    public GetterOverride(String methodName, Giver giver) {
        this.methodName = methodName;
        this.giver = giver;
    }
}

interface GetterOverrider {
    Proxy overrideGetters(Object target, GetterOverride[] overrides);
}
