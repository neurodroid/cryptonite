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

toolchain = "%s/android-toolchain" % os.getenv("HOME")
openssl_version = "1.0.0o"
encfs_version = "1.7.5"

def cpfile(src, target):
    sys.stdout.write("Copying %s to %s\n" % (src, target))
    shutil.copy(src, target)

archs = ["armeabi","armeabi-v7a"]

if encfs_version != "svn":
    encfs_dir = "encfs-%s/encfs-%s" % (encfs_version, encfs_version)
else:
    encfs_dir = "encfs-svn"

for arch in archs:
    try:
        os.makedirs("./obj/local/%s" % arch)
    except os.error:
        pass

    target_dir = "./obj/local/%s/" % arch

    if encfs_version != "svn":
        cpfile("../boost/boost_1_46_1/android/lib/libboost_filesystem.a", target_dir)
        cpfile("../boost/boost_1_46_1/android/lib/libboost_serialization.a", target_dir)
        cpfile("../boost/boost_1_46_1/android/lib/libboost_system.a", target_dir)
    else:
        cpfile("../protobuf/protobuf-2.4.1/%s/lib/libprotobuf.a" % arch, target_dir)
        cpfile("../tinyxml/tinyxml/%s/libtinyxml.a" % arch, target_dir)
    cpfile("../fuse28/obj/local/%s/libfuse.a" % arch, target_dir)
    cpfile("../rlog/rlog-1.4/%s/lib/librlog.a" % arch, target_dir)
    cpfile("../%s/%s/lib/libencfs.a" % (encfs_dir, arch), target_dir)
    cpfile("../openssl/openssl-%s/%s/libssl.a" % (openssl_version, arch), target_dir)
    cpfile("../openssl/openssl-%s/%s/libcrypto.a" % (openssl_version, arch), target_dir)
    if arch=="armeabi":
        arch_subdir = ""
    elif arch == "armeabi-v7a":
        arch_subdir = "armv7-a/"
    cpfile("%s/arm-linux-androideabi/lib/%slibstdc++.a" % (toolchain, arch_subdir), target_dir)
    cpfile("%s/lib/gcc/arm-linux-androideabi/4.8/%slibgcc.a" % (toolchain, arch_subdir), target_dir)

arch = "armeabi"

try:
    os.makedirs("./assets/%s" % arch)
except os.error:
    pass

# Split into 1M chunks for Android <= 2.2:

# truecrypt
p = subprocess.Popen("/usr/bin/split -b 1m truecrypt truecrypt.split", 
                     cwd="../tc/truecrypt-7.1a-source/Main", 
                     shell=True)
p.wait()

splitfiles = glob.glob("../tc/truecrypt-7.1a-source/Main/truecrypt.split*")
print(splitfiles)
for splitfile in splitfiles:
    cpfile(splitfile, "./assets/%s/" % arch)
