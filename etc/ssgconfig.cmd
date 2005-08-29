:: Copyright (C) 2005 Layer 7 Technologies Inc.
::
:: Compatibility: Windows XP, Windows Server 2003.

:: Launches the SecureSpan Gateway configuration tool.

@echo off

setlocal

pushd "%~dp0\.."
set SSG_ROOT="%CD%"
set JAVA_HOME=%SSG_ROOT%\jdk
popd

echo "using %SSG_ROOT% as the SSG_ROOT\n"
%JAVA_HOME%\bin\java -Dcom.l7tech.server.home=%SSG_ROOT% -jar ConfigWizard.jar

