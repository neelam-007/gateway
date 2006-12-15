:: Copyright (C) 2006 Layer 7 Technologies Inc.
::
:: Edits the SSG Log Properties file when installing CA Unicenter WSDM Observer.
::
:: Compatibility: Windows XP, Windows Server 2003.

@echo off

setlocal

set PROP_PATH=%~1

if "%PROP_PATH%"=="" (
    echo usage: %0 ssg_log_properties_path
    exit /b 1
)

if not exist "%PROP_PATH%" (
    echo !! File not found: %PROP_PATH%
    exit /b 1
)

set LOG_LEVEL_FOUND=0
for /F "tokens=* usebackq" %%i in ("%PROP_PATH%") do if "%%i"=="LOCAL_REQUEST_LOG.level=OFF" set LOG_LEVEL_FOUND=1
if %LOG_LEVEL_FOUND%==0 (
    echo.>> "%PROP_PATH%"
    echo # Suppresses harmless empty SEVERE logs from CA Unicenter WSDM ODK.>> "%PROP_PATH%"
    echo LOCAL_REQUEST_LOG.level=OFF>> "%PROP_PATH%"
    echo.>> "%PROP_PATH%"
)
