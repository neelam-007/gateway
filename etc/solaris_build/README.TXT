myke@MacPwn:~/Desktop/L7$ du -ah build
4.0K    build/build.sh				build script
4.0K    build/ipf.conf				Firewall (not in ssg tarball)
4.0K    build/pkgbuild/checkinstall		Verifies OS version and arch
4.0K    build/pkgbuild/copyright		Copyright notice
4.0K    build/pkgbuild/depend			Pre-reqs
4.0K    build/pkgbuild/pkginfo			Package Metadata
4.0K    build/pkgbuild/preinstall		Creates users
?	build/pkgbuild/postinstall		Runs partition migrate and overwrites /etc/issue
 20K    build/pkgbuild
  0B    build/pkgroot				Where the package is massaged
  0B    build/README.TXT			This file... likely > 0 bytes now
4.0K    build/gateway_DOTprofile		gateway user's .profile  (not in ssg tarball)
 16K    build/tune_solaris_tcp_stack.sh		TCP tuning  (not in ssg tarball)
 48K    build
myke@MacPwn:~/Desktop/L7$ 

Instructions for use:
1. Linux side
	a. prepare a standard linux tarball, output file is ssg-b????.tar.gz
        b. prepare a tarball of the entire solaris_build dir:
   		cd UneasyRooster/etc/
   		tar -czvf solaris_build.tar.gz
	c. copy both tarballs to the solaris build system
		scp ssgb????.tar.gz user@solaris:
		scp solaris_build.tar.gz user@solaris:

2. Solaris side

* Must have gnu tar: package name is SUNWgtar on the dvd. Sun Tar mangles the filenames and the resulting file will not run

* Create a working directory "working":
	cd ~
	mkdir working
	*   On solaris this makes /export/home/username/working
  
	cd working

* Unpack the solaris build kit:
	gunzip ../solaris_build.tar.gz
        tar -xf ../solaris_build.tar
        cd solaris_build
        cp ~/ssg-b????.tar.gz ssg.tar.gz

* run the build script
	./build.sh
	

3. Build script 
	1. makes the overlay tarball
	2. unpacks the main tarball 
	3. unpacks the overlay tarball, replacing the startup scripts.
	4. Then build runs and creates a package:
		 solaris_build/pkgbuild/L7TECHssg.pkg
	5. copy this file to repository
	

Theory of operation:

The script extracts the ssg tarball into pkgroot, moves some files around then creates listings for use with pkgmk.

This will produce a Sun package file in solaris_build/pkgbuild/L7TECHssg.pkg, this file is suitable for using with pkgadd.

You can install the package using 'pkgadd -d L7TECHssg.pkg', (the .pkg extension is not standard and is just used for developer's clarity)

Bugs:
-> checkinstall script does not work properly (at all)
-> depends on Java 1.5, not 1.6. This may or may not be a problem

