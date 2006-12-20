setlocal

pushd %1
pushd %2

::check for partition_config.cmd. If it's not there, then this partition isn't configured yet so no need to ::uninstall it
if "%2"=="partitiontemplate_" goto end
if "%2"=="default_" goto end
if exist partition_config.cmd goto doremove

:doremove
call "partition_config.cmd"
net stop "%SERVICE_NAME%"
call service.cmd uninstall
popd
popd
goto end

:end