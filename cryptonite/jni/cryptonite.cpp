/*****************************************************************************
 * Author:   Valient Gough <vgough@pobox.com>
 *
 *****************************************************************************
 * Copyright (c) 2003-2004, Valient Gough
 *
 * This library is free software; you can distribute it and/or modify it under
 * the terms of the GNU General Public License (GPL), as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GPL in the file COPYING for more
 * details.
 *
 */


#include <jni.h>

#include <encfs.h>
#include <config.h>

extern "C" {
    JNIEXPORT jint    JNICALL Java_csh_cryptonite_Cryptonite_encfsMount(JNIEnv * env, jobject thiz, jstring srcdir, jstring destdir);
    JNIEXPORT jstring JNICALL Java_csh_cryptonite_Cryptonite_encfsVersion(JNIEnv * env, jobject thiz);
};

JNIEXPORT jint JNICALL
Java_csh_cryptonite_Cryptonite_encfsBrowse(JNIEnv* env, jobject thiz, jstring srcdir, jstring destdir)
{
    // anything that comes from the user should be considered tainted until
    // we've processed it and only allowed through what we support.
    return 0;
}

JNIEXPORT jstring JNICALL
Java_csh_cryptonite_Cryptonite_encfsVersion(JNIEnv* env, jobject thiz)
{
    return env->NewStringUTF(VERSION);
}
