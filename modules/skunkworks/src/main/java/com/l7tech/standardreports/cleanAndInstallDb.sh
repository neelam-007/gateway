#!/bin/bash

#delete all current records
mysql -u root ssg -e "delete from service_metrics_details"
mysql -u root ssg -e "delete from service_metrics"
mysql -u root ssg -e "delete from message_context_mapping_values"
mysql -u root ssg -e "delete from message_context_mapping_keys"

mysql -u root ssg -e "INSERT INTO published_service (objectid, version , name, routing_uri) VALUES (229376,1,'Warehouse','w1')"
mysql -u root ssg -e "INSERT INTO published_service (objectid, version , name, routing_uri) VALUES (229378,1,'Warehouse','w2')"
mysql -u root ssg -e "INSERT INTO published_service (objectid, version , name, routing_uri) VALUES (229380,1,'Warehouse','w3')"
mysql -u root ssg -e "INSERT INTO published_service (objectid, version , name, routing_uri) VALUES (229382,1,'Warehouse','w4')"
mysql -u root ssg -e "INSERT INTO published_service (objectid, version , name, routing_uri) VALUES (229384,1,'Warehouse','w5')"

mysql -u root ssg < MappingRecords.sql

for i in *_*.sql
do
   mysql -u root ssg < $i
done

