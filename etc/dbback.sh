#!/bin/sh
mysqldump -u gateway -player7 ssg client_cert cluster_info hibernate_unique_key identity_provider internal_group internal_user internal_user_group jms_connection jms_endpoint object_identity published_service service_resolution service_usage urlcache > backup
mysqldump -u gateway -player7 -d ssg ssg_logs >> backup
gzip -9 backup
dt=`date +"%F"`
mv backup.gz ssg-db-backup-$dt.sql.gz

