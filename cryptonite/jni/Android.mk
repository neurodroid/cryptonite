# Copyright (C) 2009 The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
# Have to repeat libgcc.a and libgnustl_static.a so that they come after
# the other static libraries.

LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE    := libcryptonite
LOCAL_SRC_FILES := cryptonite.cpp android_key.cpp
ENCFS_VERSION   := 1.7.5

ifeq ($(ENCFS_VERSION), svn)
    ENCFS_PATH     := encfs-svn
    ENCFS_INCLUDES := ../protobuf/protobuf-2.4.1/$(TARGET_ARCH_ABI)/include \
                      ../tinyxml/tinyxml
    ENCFS_CPPFLAGS := -DENCFS_SVN -DTIXML_USE_STL
    ENCFS_LDLIBS   := ./obj/local/$(TARGET_ARCH_ABI)/libprotobuf.a \
                      ./obj/local/$(TARGET_ARCH_ABI)/libtinyxml.a
else
    ENCFS_PATH  := encfs-$(ENCFS_VERSION)/encfs-$(ENCFS_VERSION)
    ENCFS_INCLUDES := ../boost/boost_1_46_1
    ENCFS_CPPFLAGS := -DBOOST_FILESYSTEM_VERSION=2
    ENCFS_LDLIBS   := ./obj/local/armeabi/libboost_serialization.a \
                      ./obj/local/armeabi/libboost_filesystem.a \
                      ./obj/local/armeabi/libboost_system.a
endif

LOCAL_C_INCLUDES := \
    ../encfs-1.7.5/encfs-1.7.5 \
    ../encfs-1.7.5/encfs-1.7.5/encfs \
    ../encfs-1.7.5/encfs-1.7.5/intl \
    ../fuse29/jni/include \
    ../rlog/rlog-1.4/$(TARGET_ARCH_ABI)/include \
    ../openssl/openssl-1.0.0n/include \
    $(ENCFS_INCLUDES)

LOCAL_CPPFLAGS := \
    -D_FILE_OFFSET_BITS=64 \
    -DRLOG_COMPONENT="encfs" \
    -DFUSE_USE_VERSION=30 \
    -D__STDC_FORMAT_MACROS \
    -D__MULTI_THREAD \
    -DOPENSSL_NO_ENGINE \
    -DHAVE_EVP_AES \
    -DHAVE_EVP_BF \
    -fexceptions \
    -frtti \
    $(ENCFS_CPPFLAGS)

LOCAL_LDLIBS := \
    ./obj/local/$(TARGET_ARCH_ABI)/libencfs.a \
    ./obj/local/$(TARGET_ARCH_ABI)/libfuse.a \
    ./obj/local/$(TARGET_ARCH_ABI)/librlog.a \
    $(ENCFS_LDLIBS) \
    ./obj/local/$(TARGET_ARCH_ABI)/libstdc++.a \
    ./obj/local/$(TARGET_ARCH_ABI)/libgcc.a \
    ./obj/local/$(TARGET_ARCH_ABI)/libcrypto.a \
    ./obj/local/$(TARGET_ARCH_ABI)/libssl.a \
    -llog -ldl

LOCAL_ALLOW_UNDEFINED_SYMBOLS := true

include $(BUILD_SHARED_LIBRARY)
