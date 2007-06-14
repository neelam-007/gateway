#!/bin/sh
rm -rf ssg
mkdir -p ssg/bin
cp startup/* ssg/bin
mkdir ssg/migration/cfg
cp ../grandmaster_flash.solaris ssg/migration/cfg/grandmaster_flash
tar -cvf solaris_ssg_bin.tar ssg/bin/* ssg/migration/cfg/*
gzip solaris_ssg_bin.tar
rm -rf ssg
