:: Copyright (C) 2005 Layer 7 Technologies Inc.
::
:: Compatibility: Windows 2000, Windows XP, Windows Server 2003.

:: Install/Uninstall the SecureSpan Gateway Windows service.

@echo off

setlocal

:: Compute the SSG home folder absolute path (without trailing backslash!).
pushd %~dp0\..
set SSG_HOME=%CD%
popd

:: Location of our private copy of Tomcat.
set TOMCAT_HOME=%SSG_HOME%\tomcat

:: Service name cannot have spaces, but display name and description can.
set SERVICE_NAME=SecureSpanGateway

:: The service installer/uninstaller/starter/stopper application.
set PRUNSRV=%TOMCAT_HOME%\bin\tomcat5.exe
if exist "%PRUNSRV%" goto okExe
echo ERROR: The file "%PRUNSRV%" was not found.
goto end
:okExe

if ""%1"" == ""install""   goto doInstall
if ""%1"" == ""uninstall"" goto doUninstall

echo Usage: service command
echo Commands:
echo   install     Install the SecureSpan Gateway Windows service.
echo   uninstall   Uninstall the service from the System.
goto end


:doInstall
:: Environment variables prefixed with PR_ are read by tomcat5.exe.
set PR_DISPLAYNAME=SecureSpan Gateway
set PR_DESCRIPTION=Layer 7 Technologies SecureSpan Gateway
set PR_INSTALL=%PRUNSRV%
set PR_STARTUP=auto
set PR_LOGPATH=%SSG_HOME%\logs
set PR_LOGPREFIX=ssg_service.log
:: Log level can be error, info, warn or debug.
set PR_LOGLEVEL=debug
set PR_STDOUTPUT=%SSG_HOME%\logs\ssg_service_stdout.log
set PR_STDERROR=%SSG_HOME%\logs\ssg_service_stderr.log
"%PRUNSRV%" //IS//%SERVICE_NAME% ^
    --StartMode exe --StartImage "%SSG_HOME%\bin\ssg.cmd" --StartParams start ^
    --StopMode  exe --StopImage  "%SSG_HOME%\bin\ssg.cmd" --StopParams  stop
if not errorlevel 1 goto installed
echo ERROR: Failed to install "%SERVICE_NAME%" service.
goto end
:installed
echo Installed service "%SERVICE_NAME%".
goto end


:doUninstall
"%PRUNSRV%" //DS//%SERVICE_NAME%
echo Uninstalled service "%SERVICE_NAME%".
goto end


:end
