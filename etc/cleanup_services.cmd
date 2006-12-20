@echo off
setlocal

call "%~dp0\ssgruntimedefs.cmd"

set PARTITION_BASE=%SSG_HOME%\etc\conf\partitions
for /f "tokens=*" %%i in ('dir /B "%PARTITION_BASE%"') do call remove_service "%PARTITION_BASE%" %%i