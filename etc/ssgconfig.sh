#!/bin/bash

pushd ..
SSG_ROOT=`pwd`
popd

echo "using ${SSG_ROOT} as the SSG_ROOT\n"
java -Dcom.l7tech.server.home=${SSG_ROOT} -jar ConfigWizard.jar
