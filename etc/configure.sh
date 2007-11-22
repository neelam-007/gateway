#!/bin/bash

#we're in /SSG_ROOT/bin right now
cd `dirname $0`
pushd .. > /dev/null
SSG_ROOT=`pwd`
popd > /dev/null

. ${SSG_ROOT}/bin/ssg-utilities

ensure_JDK

CONFWIZARD="${SSG_ROOT}/configwizard/ssgconfig.sh"

doLogout() {
    exit
}

isValid="n"
while [ 1 ]
do
    clear
    echo "Welcome to the SecureSpan Gateway"
    echo
    echo "What would you like to do?"
        echo "1) Configure the SecureSpan Gateway application on this appliance"
        echo "2) Change the Master Passphrase"
        echo "3) Exit"
        echo -n "Please make a selection: "
        read choice

        case $choice in
                1) 	clear;
                    (${CONFWIZARD});
                    echo "Press enter to continue";
                    read;
                    clear;;
                2)  clear;
                    (${CONFWIZARD} -changeMasterPassphrase);
                    echo "Press enter to continue";
                    read;
                    clear;;
                3)  doLogout;;
                *)  echo "That is not a valid selection";
                    echo "Press enter to continue";
                    read;;
        esac
done