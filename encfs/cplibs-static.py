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
        os.makedirs("../cryptonite/assets/%s" % arch)
    except os.error:
        pass

    # Split into 1M chunks for Android <= 2.2:

    # encfs
    p = subprocess.Popen("/usr/bin/split -b 1m encfs encfs.split", 
                         cwd="./encfs-1.7.4/%s/bin" % arch, 
                         shell=True)
    p.wait()

    splitfiles = glob.glob("./encfs-1.7.4/%s/bin/encfs.split*" % arch)
    print splitfiles
    for splitfile in splitfiles:
        cpfile(splitfile, "../cryptonite/assets/%s/" % arch)

    # encfsctl
    # p = subprocess.Popen("/usr/bin/split -b 1m encfsctl encfsctl.split", 
    #                      cwd="./encfs-1.7.4/%s/bin" % arch, 
    #                      shell=True)
    # p.wait()

    # splitfiles = glob.glob("./encfs-1.7.4/%s/bin/encfsctl.split*" % arch)
    # print splitfiles
    # for splitfile in splitfiles:
    #     cpfile(splitfile, "../cryptonite/assets/%s/" % arch)
