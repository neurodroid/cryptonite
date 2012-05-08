#! /bin/bash

WXVER=2.8.12
BUILDDIR=wxWidgets-${WXVER}/bld

rm -rf ${BUILDDIR}
mkdir -p ${BUILDDIR}
cd ${BUILDDIR}
../../config-arm-wx.sh
make -j4
make install
