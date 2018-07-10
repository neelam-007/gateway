@echo off

if "%1" == "/initSvcConf" goto initSvcConf

jre\bin\java -Dfile.encoding=UTF-8  -Dsun.net.inetaddr.ttl=10 -Dnetworkaddress.cache.ttl=10 -Xms96M -Xmx96M -Xss256k -classpath Client.jar com.l7tech.proxy.cli.Main
goto end

:initSvcConf
cd %systemroot%\System32
mkdir Config
cd Config
mkdir SystemProfile
cd SystemProfile
mkdir .l7tech
echo Y| cacls .l7tech /S:"D:PAI(A;OICI;FA;;;BA)(A;OICI;FA;;;SY)"
goto end

:end
