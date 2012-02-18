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

    target_dir = "./obj/local/%s/" % arch

    cpfile("../boost/boost_1_46_1/android/lib/libboost_filesystem.a", target_dir)
    cpfile("../boost/boost_1_46_1/android/lib/libboost_serialization.a", target_dir)
    cpfile("../boost/boost_1_46_1/android/lib/libboost_system.a", target_dir)
    cpfile("../fuse/fuse-android/obj/local/%s/libfuse.a" % arch, target_dir)
    cpfile("../rlog/rlog-1.4/%s/lib/librlog.a" % arch, target_dir)
    cpfile("../encfs/encfs-1.7.4/%s/lib/libencfs.a" % arch, target_dir)
    cpfile("%s/sources/cxx-stl/gnu-libstdc++/libs/%s/libgnustl_static.a" % (ndk, arch), target_dir)
    cpfile("%s/lib/gcc/arm-linux-androideabi/4.4.3/libgcc.a" % toolchain, target_dir)
