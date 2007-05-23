myke@MacPwn:~/Desktop/L7$ du -ah build
4.0K    build/build.sh				build script
4.0K    build/ipf.conf				Firewall (not in ssg tarball)
4.0K    build/pkgbuild/checkinstall		Verifies OS version and arch
4.0K    build/pkgbuild/copyright		Copyright notice
4.0K    build/pkgbuild/depend			Pre-reqs
4.0K    build/pkgbuild/pkginfo			Package Metadata
4.0K    build/pkgbuild/preinstall		Creates users
 20K    build/pkgbuild
  0B    build/pkgroot				Where the package is massaged
  0B    build/README.TXT			This file... likely > 0 bytes now
4.0K    build/gateway_DOTprofile		gateway user's .profile  (not in ssg tarball)
 16K    build/tune_solaris_tcp_stack.sh		TCP tuning  (not in ssg tarball)
 48K    build
myke@MacPwn:~/Desktop/L7$ 

Instructions for use:
* Make overlay tarball: 

In this dir, sh make_overlay_tarball.sh

This produces solaris_ssg_bin.tar.gz in this dir, containing replacements for the linux startup scripts. 

* Make ssg tarball from standard build.

* Place a ssg.tar.gz and the above created solaris_ssb_bin.tar.gz  in the build/ directory
* cd build; ./build.sh
Build script unpacks the main tarball and then the other tarball, replacing the startup scripts.
Then build runs and creates a package

This creates a 200 megabyte pkg. This can be gzipped down to the same approximate size as the rpm. Solaris packages are uncompressed, for some reason.


Basically, the script extracts the ssg tarball into pkgroot, moves some files around then creates listings for use with pkgmk.
This will produce a Sun package file in build/pkgbuild/L7TECHssg.pkg, this file is suitable for using with pkgadd.
You can install the package using 'pkgadd -d L7TECHssg.pkg', (the .pkg extension is not standard and is just used for developer's clairity)

Bugs:
-> checkinstall script does not work properly (at all)
-> depends on Java 1.5, not 1.6. This may or may not be a problem
-> Needs a proper copyright notice
-> This filename contains spaces: "ssg/tomcat/webapps/ROOT/ssg/webadmin/help/securespan manager help system.log"
-> When untarring ssg.tar.bz, one sees many messages like: tar: ././@LongLink: typeflag 'L' not recognized, converting to regular file

