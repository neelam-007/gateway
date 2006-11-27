:: -----------------------------------------------------------------------------
:: LAYER 7 TECHNOLOGIES
:: November 2006
:: Launches the SSG Flasher Utility
:: -----------------------------------------------------------------------------
@echo off

setlocal

pushd "%~dp0\.."
set SSG_ROOT="%CD%"
set JAVA_HOME=%SSG_ROOT%\jdk
popd

echo "using %SSG_ROOT% as the SSG_ROOT"
%JAVA_HOME%\bin\java -Dcom.l7tech.server.home=%SSG_ROOT% -jar SSGFlasher.jar %*
