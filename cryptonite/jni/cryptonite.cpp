// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.

// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.

// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.

// Copyright (c) 2012, Christoph Schmidt-Hieber

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
    return 0;
}

JNIEXPORT jstring JNICALL
Java_csh_cryptonite_Cryptonite_encfsVersion(JNIEnv* env, jobject thiz)
{
    return env->NewStringUTF(VERSION);
}
