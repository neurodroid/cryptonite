# Copyright (C) 2012 Seth Huang<seth.hg@gmail.com>
#
LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_CFLAGS	:= -D_FILE_OFFSET_BITS=64 -DFUSE_USE_VERSION=26 -D__MULTI_THREAD
LOCAL_MODULE    := libfuse
LOCAL_SRC_FILES := cuse_lowlevel.c fuse.c fuse_kern_chan.c fuse_loop.c fuse_loop_mt.c fuse_lowlevel.c fuse_mt.c fuse_opt.c fuse_session.c fuse_signals.c helper.c mount.c mount_util.c ulockmgr.c
LOCAL_C_INCLUDES := jni/include

include $(BUILD_STATIC_LIBRARY)
#include $(BUILD_SHARED_LIBRARY)

include $(CLEAR_VARS)

LOCAL_CFLAGS	:= -D_FILE_OFFSET_BITS=64
LOCAL_C_INCLUDES := jni/include
LOCAL_MODULE    := fusexmp
LOCAL_SRC_FILES := fusexmp.c
LOCAL_STATIC_LIBRARIES := libfuse
include $(BUILD_EXECUTABLE)
