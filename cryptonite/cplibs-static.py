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

ndk = "%s/android-ndk-r8" % os.getenv("HOME")
toolchain = "%s/toolchains/arm-linux-androideabi-4.4.3/prebuilt/%s-x86" % (ndk, platform)
openssl_version = "1.0.0i"

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
    cpfile("../openssl/openssl-%s/%s/libssl.a" % (openssl_version, arch), target_dir)
    cpfile("../openssl/openssl-%s/%s/libcrypto.a" % (openssl_version, arch), target_dir)
    cpfile("%s/sources/cxx-stl/gnu-libstdc++/libs/%s/libgnustl_static.a" % (ndk, arch), target_dir)
    cpfile("%s/lib/gcc/arm-linux-androideabi/4.4.3/libgcc.a" % toolchain, target_dir)


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
