Instructions to build & install package:
1. Linux side
	a. prepare a standard linux tarball, output file is ssg-bNNNN.tar.gz (please note that it should be a tarball from an obfuscated build, and "NNNN" is the build number)
        b. prepare a tarball of the entire solaris_build dir:
   		cd UneasyRooster/etc/
   		tar -czvf solaris_build.tar.gz solaris_build
	c. copy both tarballs to the solaris build system (if you can't scp at a build machine login session then scp at a solaris package machine login session)
		cd UneasyRooster/build/
		scp ssg-bNNNN.tar.gz <user>@<solaris_build_machine>:
		cd UneasyRooster/etc/
		scp solaris_build.tar.gz <user>@<solaris_build_machine>:

(eg. where <user> is "build" and <solaris_build_machine> is "buildsolaris" for Layer7 in-house build platform)

2. Solaris side (eg. user "build" on "buildsolaris" machine for Layer7 in-house build platform)

* Must have gnu tar: package name is SUNWgtar on the dvd. Sun Tar mangles the filenames and the resulting file will not run

* Create a working directory "working":
	cd ~
	mkdir working
	*   On solaris this makes /export/home/username/working
  
* Unpack the solaris build kit:
	cd working
	gunzip ../solaris_build.tar.gz
        tar -xf ../solaris_build.tar
        mv ~/ssg-bNNNN.tar.gz . ( Where NNNN is the build number)

eg: 
[jay@jaysol working]$ ls -l
total 245170
drwxr-xr-x   6 jay      other        512 May 31 16:25 solaris_build
-rw-r--r--   1 jay      other  125448735 May 29 17:44 ssg-b3404.tar.gz

* run the build script, supplying build number: 
	cd solaris_build
	./build.sh 4.0m1 

3. Build script 
	1. makes the overlay tarball
	2. unpacks the main tarball 
	3. unpacks the overlay tarball, replacing the startup scripts.
	4. Then build runs and creates a package:
		 solaris_build/L7TECHssg.pkg
	5. copy this file to repository
	
Theory of operation:

The script extracts the ssg tarball into pkgroot, moves some files around then creates listings for use with pkgmk.

This will produce a Sun package file in solaris_build/L7TECHssg.pkg, this file is suitable for using with pkgadd.

You can install the package using 'pkgadd -d L7TECHssg.pkg', (the .pkg extension is not standard and is just used for developer's clarity)


Instruction to remove installed L7TECHssg package: pkgrm L7TECHssg

Bugs:
-> depends on Java 1.5, not 1.6. This may or may not be a problem
