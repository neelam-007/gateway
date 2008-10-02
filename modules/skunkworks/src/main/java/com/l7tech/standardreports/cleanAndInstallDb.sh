#!/bin/bash

#delete all current records
mysql -u root ssg -e "delete from service_metrics"
mysql -u root ssg -e "delete from service_metrics_details"
mysql -u root ssg -e "delete from message_context_mapping_values"
mysql -u root ssg -e "delete from message_context_mapping_keys"

mysql -u root ssg < MappingRecords.sql

for i in *_*.sql
do
   mysql -u root ssg < $i
done
