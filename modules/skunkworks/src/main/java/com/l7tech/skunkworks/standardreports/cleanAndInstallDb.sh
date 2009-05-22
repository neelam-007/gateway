#!/bin/bash

#delete all current records
#echo "Deleting metrics"
#mysql -u root ssg -e "delete from service_metrics_details"
#mysql -u root ssg -e "delete from service_metrics"
#echo "Deleting mappings"
#mysql -u root ssg -e "delete from message_context_mapping_values"
#mysql -u root ssg -e "delete from message_context_mapping_keys"

echo "Loading Mappings"
mysql -u root ssg < MappingRecords.sql

echo "Loading metrics"

for i in *_*.sql
do
   mysql -u root ssg < $i
done

