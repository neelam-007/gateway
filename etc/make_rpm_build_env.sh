#!/bin/sh
mkdir $HOME/rpm
mkdir $HOME/rpm/SOURCES
mkdir $HOME/rpm/SPECS
mkdir $HOME/rpm/BUILD
mkdir $HOME/rpm/SRPMS
mkdir $HOME/rpm/RPMS
mkdir $HOME/rpm/RPMS/i386
echo "%_topdir    $HOME/rpm" >> $HOME/.rpmmacros
