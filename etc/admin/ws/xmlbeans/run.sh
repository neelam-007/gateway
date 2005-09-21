#!/bin/bash

GENSRC=../../../gen
SCHEMA=../../schema
/opt/java/xmlbeans-2.0.0/bin/scomp -src $GENSRC -srconly -javasource 1.4 \
	$SCHEMA/PublishedService.xsd \
	$SCHEMA/ServiceUsage.xsd \
	$SCHEMA/InternalUser.xsd \
	$SCHEMA/FederatedUser.xsd \
	adminws.xsdconfig
