#!/bin/sh
rm -rf ssg
mkdir -p ssg/bin
cp startup/* ssg/bin
tar -czvf solaris_ssg_bin.tar.gz ssg/bin/*
rm -rf ssg
