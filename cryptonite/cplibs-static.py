#! /usr/bin/python

import glob
import shutil
import sys
import subprocess
import os

def cpfile(src, target):
    sys.stdout.write("Copying %s to %s\n" % (src, target))
    shutil.copy(src, target)

archs = ["armeabi",]

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
