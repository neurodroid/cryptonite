#! /usr/bin/python

import glob
import shutil
import sys
import subprocess
import os

if 'linux' in sys.platform:
    platform = 'linux'
else:
    platform = 'darwin'

ndk = "%s/android-ndk-r7" % os.getenv("HOME")
toolchain = "%s/toolchains/arm-linux-androideabi-4.4.3/prebuilt/%s-x86" % (ndk, platform)

def cpfile(src, target):
    sys.stdout.write("Copying %s to %s\n" % (src, target))
    shutil.copy(src, target)

archs = ["armeabi","armeabi-v7a"]

for arch in archs:
    try:
        os.makedirs("./obj/local/%s" % arch)
    except os.error:
        pass

    cpfile("../boost/boost_1_46_1/android/lib/libboost_filesystem.a", "./obj/local/%s/" % arch)
    cpfile("../boost/boost_1_46_1/android/lib/libboost_serialization.a", "./obj/local/%s/" % arch)
    cpfile("../boost/boost_1_46_1/android/lib/libboost_system.a", "./obj/local/%s/" % arch)
    cpfile("../fuse/fuse-android/obj/local/%s/libfuse.a" % arch, "./obj/local/%s/" % arch)
    cpfile("../rlog/rlog-1.4/%s/lib/librlog.a" % arch, "./obj/local/%s/" % arch)
    cpfile("../encfs/encfs-1.7.4/%s/lib/libencfs.a" % arch, "./obj/local/%s/" % arch)
    cpfile("%s/sources/cxx-stl/gnu-libstdc++/libs/%s/libgnustl_static.a" % (ndk, arch), "./obj/local/%s/" % arch)
    cpfile("%s/lib/gcc/arm-linux-androideabi/4.4.3/libgcc.a" % toolchain, "./obj/local/%s/" % arch)
    cpfile("%s/sources/cxx-stl/gnu-libstdc++/libs/%s/libgnustl_static.a" % (ndk, arch), "./obj/local/%s/" % arch)
