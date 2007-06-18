#!/bin/sh
rm -rf ssg
mkdir -p ssg/bin
cp startup/* ssg/bin
tar -cvf solaris_ssg_bin.tar ssg/bin/* ssg/migration/cfg/*
gzip solaris_ssg_bin.tar
rm -rf ssg
