package com.l7tech.server.service;

import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.gateway.common.service.PublishedServiceAlias;
import com.l7tech.gateway.common.service.ServiceHeader;

/**
 * @author darmstrong
 */
public interface ServiceAliasManager extends AliasManager<PublishedServiceAlias, PublishedService, ServiceHeader> { }
