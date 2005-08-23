:: Copyright (C) 2005 Layer 7 Technologies Inc.
::
:: $Id$
::
:: Compatibility: Windows XP, Windows Server 2003.

:: Startup/Shutdown script for the SecureSpan Gateway server.

@echo off

setlocal

if ""%1"" == ""start""   goto doStart
if ""%1"" == ""stop""    goto doStop

echo Usage: ssg command
echo Commands:
echo   start        Start the SecureSpan Gateway server.
echo   stop         Stop the SecureSpan Gateway server.
goto end

:doStart
call "%~dp0\ssgruntimedefs.cmd"
set ssgRunning=0
pushd "%SSG_HOME%"
for /F "tokens=*" %%i in ('"bin\process.exe --quiet --fields=exeFull"') do if "%%i"=="%JAVA_HOME%\bin\java.exe" set ssgRunning=1
popd
if %ssgRunning%==1 goto dontStart
echo Starting SSG...
pushd "%TOMCAT_HOME%\bin"
call startup.bat
popd
goto end

:dontStart
echo SSG Already Running
goto end

:doStop
call "%~dp0\ssgruntimedefs.cmd"
set ssgRunning=0
pushd "%SSG_HOME%"
for /F "tokens=*" %%i in ('"bin\process.exe --quiet --fields=exeFull"') do if "%%i"=="%JAVA_HOME%\bin\java.exe" set ssgRunning=1
popd
if %ssgRunning%==0 goto dontStop
echo Stopping SSG...
pushd "%TOMCAT_HOME%\bin"
call shutdown.bat
:: If that doesn't stop in 10 seconds, force a kill.
"%SSG_HOME%\bin\killproc.exe" --wait=10000 --ignoreCase "%JAVA_HOME%\bin\java.exe"
popd
goto end

:dontStop
echo SSG Not Running
goto end

:end
