--
-- View to report on API usage for custom bins.
-- bin_start_time is inclusive, bin_end_time is exclusive
--
drop view if exists api_usage_view;

create view api_usage_view as
select
sm.objectid as metric_id,
sm.uuid as uuid,
sm.published_service_oid as api_id,
p.name as api_name,
p.routing_uri as api_uri,
sm.resolution as resolution,
sm.period_start as bin_start_time,
sm.end_time as bin_end_time,
sm.front_min as front_min,
sm.front_max as front_max,
sm.front_sum as front_sum,
sm.back_min as back_min,
sm.back_max as back_max,
sm.back_sum as back_sum,

sm.attempted as hits_total,
sm.completed as hits_success,
sm.attempted - sm.completed as hits_total_errors,
sm.attempted - sm.authorized as hits_policy_errors,
sm.authorized - sm.completed as hits_routing_errors

from service_metrics sm, published_service p where sm.published_service_oid = p.objectid;

--
-- View to report on API, API key and method usage.
-- bin_start_time is inclusive, bin_end_time is exclusive
--
drop view if exists api_key_or_method_usage_view;

create view api_key_or_method_usage_view as
select
sm.objectid as metric_id,
sm.uuid as uuid,
sm.published_service_oid as api_id,
p.name as api_name,
p.routing_uri as api_uri,
sm.resolution as resolution,
sm.period_start as bin_start_time,
sm.end_time as bin_end_time,

smd.front_min as front_min,
smd.front_max as front_max,
smd.front_sum as front_sum,
smd.back_min as back_min,
smd.back_max as back_max,
smd.back_sum as back_sum,

smd.attempted as hits_total,
smd.completed as hits_success,
smd.attempted - smd.completed as hits_total_errors,
smd.attempted - smd.authorized as hits_policy_errors,
smd.authorized - smd.completed as hits_routing_errors,

case
	when mcmk.mapping1_key = 'API_KEY'  then mcmv.mapping1_value
	when mcmk.mapping2_key = 'API_KEY'  then mcmv.mapping2_value
	when mcmk.mapping3_key = 'API_KEY'  then mcmv.mapping3_value
	when mcmk.mapping4_key = 'API_KEY'  then mcmv.mapping4_value
	when mcmk.mapping5_key = 'API_KEY'  then mcmv.mapping5_value
end as api_key,

case
	when mcmk.mapping1_key = 'API_METHOD'  then mcmv.mapping1_value
	when mcmk.mapping2_key = 'API_METHOD'  then mcmv.mapping2_value
	when mcmk.mapping3_key = 'API_METHOD'  then mcmv.mapping3_value
	when mcmk.mapping4_key = 'API_METHOD'  then mcmv.mapping4_value
	when mcmk.mapping5_key = 'API_METHOD'  then mcmv.mapping5_value
end as api_method

from published_service p, service_metrics sm, service_metrics_details smd, message_context_mapping_values mcmv, message_context_mapping_keys mcmk
where
sm.published_service_oid = p.objectid and
sm.objectid = smd.service_metrics_oid and

smd.mapping_values_oid = mcmv.objectid and
mcmv.mapping_keys_oid = mcmk.objectid  and
(
	( mcmk.mapping1_key = 'API_KEY'  )  or ( mcmk.mapping2_key = 'API_KEY'  )  or ( mcmk.mapping3_key = 'API_KEY'  )  or ( mcmk.mapping4_key = 'API_KEY'  )  or ( mcmk.mapping5_key = 'API_KEY'  )
) and
(
	( mcmk.mapping1_key = 'API_METHOD'  )  or ( mcmk.mapping2_key = 'API_METHOD'  )  or ( mcmk.mapping3_key = 'API_METHOD'  )  or ( mcmk.mapping4_key = 'API_METHOD'  )  or ( mcmk.mapping5_key = 'API_METHOD'  )
);
