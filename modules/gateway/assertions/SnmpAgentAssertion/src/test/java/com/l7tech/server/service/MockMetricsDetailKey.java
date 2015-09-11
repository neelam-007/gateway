package com.l7tech.server.service;

import com.l7tech.gateway.common.mapping.MessageContextMapping;
import com.l7tech.identity.User;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: rseminoff
 * Date: 5/15/12
 * Time: 2:44 PM
 * To change this template use File | Settings | File Templates.
 */
public class MockMetricsDetailKey extends ServiceMetrics.MetricsDetailKey {
    MockMetricsDetailKey(String operation, User user, List<MessageContextMapping> mappings) {
        super(operation, user, mappings);
        System.out.println("*** CALL *** MockMetricsDetailKey: constructor(String, User, List)");
    }
}
