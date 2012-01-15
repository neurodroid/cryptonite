/* This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 * 
 * encfsctl.cpp:
 *****************************************************************************
 * Author:   Valient Gough <vgough@pobox.com>
 * Edited for Android NDK: Christoph Schmidt-Hieber <christsc_at_gmx.de>
 *
 *****************************************************************************
 * Copyright (c) 2004, Valient Gough
 *
 */


#include <jni.h>
#include <android/log.h>

#include <boost/shared_ptr.hpp>
#include <boost/scoped_array.hpp>
#include <string>
#include <iostream>
#include <sstream>

#include <encfs.h>
#include <FileNode.h>
#include <DirNode.h>
#include <config.h>

#define  LOG_TAG    "cryptonite-jni"
#define  LOGI(...)  __android_log_print(ANDROID_LOG_INFO,LOG_TAG,__VA_ARGS__)
#define  LOGE(...)  __android_log_print(ANDROID_LOG_ERROR,LOG_TAG,__VA_ARGS__)

static bool checkDir( std::string &rootDir )
{
    if( !isDirectory( rootDir.c_str() ))
    {
        std::ostringstream err;
        err << "directory " << rootDir.c_str() << " does not exist.\n";
        LOGE(err.str().c_str());
	return false;
    }
    if(rootDir[ rootDir.length()-1 ] != '/')
	rootDir.append("/");
    return true;
}

/* Passing a copy to rootDir is on purpose here */
static int isValidEncFS(std::string rootDir) {
    if( !checkDir( rootDir ))
	return EXIT_FAILURE;

    boost::shared_ptr<EncFSConfig> config(new EncFSConfig);
    ConfigType type = readConfig( rootDir, config );

    std::ostringstream info;
    // show information stored in config..
    switch(type)
    {
    case Config_None:
	// xgroup(diag)
	LOGI("Unable to load or parse config file");
	return EXIT_FAILURE;
    case Config_Prehistoric:
	// xgroup(diag)
	LOGI("A really old EncFS filesystem was found.\n"
             "It is not supported in this EncFS build.");
	return EXIT_FAILURE;
    case Config_V3:
	// xgroup(diag)
        info << "Version 3 configuration; created by " << config->creator.c_str();
        LOGI(info.str().c_str());
	break;
    case Config_V4:
	// xgroup(diag)
        info << "Version 4 configuration; created by " << config->creator.c_str();
        LOGI(info.str().c_str());
	break;
    case Config_V5:
	// xgroup(diag)
        info << "Version 5 configuration; created by " << config->creator.c_str()
             << " (revision " << config->subVersion << ")";
        LOGI(info.str().c_str());
	break;
    case Config_V6:
	// xgroup(diag)
        info << "Version 6 configuration; created by " << config->creator.c_str()
             << " (revision " << config->subVersion << ")";
        LOGI(info.str().c_str());
	break;
    }

    showFSInfo( config );

    return EXIT_SUCCESS;
}

// apply an operation to every block in the file
template<typename T>
static int processContents( const shared_ptr<EncFS_Root> &rootInfo, 
                            const char *path, T &op )
{
    int errCode = 0;
    shared_ptr<FileNode> node = rootInfo->root->openNode( path, "encfsctl",
	    O_RDONLY, &errCode );

    if(!node)
    {
        // try opening directly, so a cipher-path can be passed in
        node = rootInfo->root->directLookup( path );
        if(node)
        {
            errCode = node->open( O_RDONLY );
            if(errCode < 0)
                node.reset();
        }
    }

    if(!node)
    {
        std::ostringstream err;
        err << "unable to open " << path << "\n";
        LOGE(err.str().c_str());
	return errCode;
    } else
    {
	unsigned char buf[512];
	int blocks = (node->getSize() + sizeof(buf)-1) / sizeof(buf);
	// read all the data in blocks
	for(int i=0; i<blocks; ++i)
	{
	    int bytes = node->read(i*sizeof(buf), buf, sizeof(buf));
	    int res = op(buf, bytes);
	    if(res < 0)
		return res;
	}
    }
    return 0;
} 

static bool endsWith(const std::string &str, char ch)
{
    if(str.empty())
	return false;
    else
	return str[str.length()-1] == ch;
}

class WriteOutput
{
    int _fd;
public:
    WriteOutput(int fd) { _fd = fd; }
    ~WriteOutput() { close(_fd); }

    int operator()(const void *buf, int count)
    {
	return (int)write(_fd, buf, count);
    }
};

static int copyLink(const struct stat &stBuf, 
                    const boost::shared_ptr<EncFS_Root> &rootInfo,
                    const std::string &cpath, const std::string &destName )
{
    boost::scoped_array<char> buf(new char[stBuf.st_size+1]);
    int res = ::readlink( cpath.c_str(), buf.get(), stBuf.st_size );
    if(res == -1)
    {
        std::ostringstream err;
        err << "unable to readlink of " << cpath;
        LOGE(err.str().c_str());
        return EXIT_FAILURE;
    }

    buf[res] = '\0';
    std::string decodedLink = rootInfo->root->plainPath(buf.get());

    res = ::symlink( decodedLink.c_str(), destName.c_str() );
    if(res == -1)
    {
        std::ostringstream err;
        err << "unable to create symlink for " << cpath 
            << " to " << decodedLink << "\n";
        LOGE(err.str().c_str());
    }

    return EXIT_SUCCESS;
}

static int copyContents(const boost::shared_ptr<EncFS_Root> &rootInfo, 
                        const char* encfsName, const char* targetName,
                        bool fake)
{
    boost::shared_ptr<FileNode> node = 
	rootInfo->root->lookupNode( encfsName, "encfsctl" );

    if(!node)
    {
        std::ostringstream err;
        err << "unable to open " << encfsName;
        LOGE(err.str().c_str());
        return EXIT_FAILURE;
    } else
    {
        struct stat st;

        if(node->getAttr(&st) != 0)
            return EXIT_FAILURE;

        if((st.st_mode & S_IFLNK) == S_IFLNK)
        {
            std::string d = rootInfo->root->cipherPath(encfsName);
            char linkContents[PATH_MAX+2];

            if(readlink (d.c_str(), linkContents, PATH_MAX + 1) <= 0)
            {
                std::ostringstream err;
                err << "unable to read link " << encfsName;
                LOGE(err.str().c_str());
                return EXIT_FAILURE;
            }
            symlink(rootInfo->root->plainPath(linkContents).c_str(), 
		    targetName);
        } else
        {
            int outfd = creat(targetName, st.st_mode);
            std::ostringstream out;
            out << "Creating " << targetName;
	    LOGI(out.str().c_str());
	    WriteOutput output(outfd);
            if (!fake)
                processContents( rootInfo, encfsName, output );
        }
    }
    return EXIT_SUCCESS;
}

static int traverseDirs(const boost::shared_ptr<EncFS_Root> &rootInfo, 
                        std::string volumeDir, std::string destDir, bool fake)
{
    if(!endsWith(volumeDir, '/'))
	volumeDir.append("/");
    if(!endsWith(destDir, '/'))
	destDir.append("/");

    // Lookup directory node so we can create a destination directory
    // with the same permissions
    {
        struct stat st;
        shared_ptr<FileNode> dirNode = 
            rootInfo->root->lookupNode( volumeDir.c_str(), "encfsctl" );
        if(dirNode->getAttr(&st))
            return EXIT_FAILURE;

        mkdir(destDir.c_str(), st.st_mode);
    }
    // show files in directory
    DirTraverse dt = rootInfo->root->openDir(volumeDir.c_str());
    if(dt.valid())
    {
        for(std::string name = dt.nextPlaintextName(); !name.empty(); 
            name = dt.nextPlaintextName())
        {
            // Recurse to subdirectories
            if(name != "." && name != "..")
            {
                std::string plainPath = volumeDir + name;
                std::string cpath = rootInfo->root->cipherPath(plainPath.c_str());
                std::string destName = destDir + name;

                int r = EXIT_SUCCESS;
                struct stat stBuf;
                if( !lstat( cpath.c_str(), &stBuf ))
                {
                    if( S_ISDIR( stBuf.st_mode ) )
                    {
                        traverseDirs(rootInfo, (plainPath + '/').c_str(), 
                                     destName + '/', fake);
                    } else if( S_ISLNK( stBuf.st_mode ))
                    {
                        r = copyLink( stBuf, rootInfo, cpath, destName );
                    } else
                    {
                         r = copyContents(rootInfo, plainPath.c_str(), 
                                          destName.c_str(), fake);
                    }
                } else
                {
                    r = EXIT_FAILURE;
                }
                if(r != EXIT_SUCCESS)
                    return r;
            }
        }
    }
    return EXIT_SUCCESS;
}


bool myIsDirectory( const char *fileName )
{
    struct stat buf;
    if( !lstat( fileName, &buf ))
    {
	return S_ISDIR( buf.st_mode );
    } else
    {
	return false;
    }
}

static RootPtr initRootInfo(const std::string& rootDir, const std::string& password)
{
    RootPtr result;
    boost::shared_ptr<EncFS_Opts> opts( new EncFS_Opts() );
    opts->createIfNotFound = false;
    opts->checkKey = false;
    opts->password.assign(password);
    opts->rootDir.assign(rootDir);
    if(checkDir( opts->rootDir ))
        result = initFS( NULL, opts );

    if(!result) {
        std::ostringstream err;
        err << "Unable to initialize encrypted filesystem - check path.";
        LOGE(err.str().c_str());
    }

    return result;
}

#ifdef __cplusplus
extern "C" {
#endif
    JNIEXPORT jint    JNICALL Java_csh_cryptonite_Cryptonite_jniFailure(JNIEnv * env, jobject thiz);
    JNIEXPORT jint    JNICALL Java_csh_cryptonite_Cryptonite_jniSuccess(JNIEnv * env, jobject thiz);
    JNIEXPORT jint    JNICALL Java_csh_cryptonite_Cryptonite_jniIsValidEncFS(JNIEnv * env, jobject thiz, jstring srcdir);
    JNIEXPORT jint    JNICALL Java_csh_cryptonite_Cryptonite_jniBrowse(JNIEnv * env, jobject thiz, jstring srcdir, jstring destdir, jstring password);
    JNIEXPORT jstring JNICALL Java_csh_cryptonite_Cryptonite_jniVersion(JNIEnv * env, jobject thiz);
#ifdef __cplusplus
};
#endif

JNIEXPORT jint JNICALL
Java_csh_cryptonite_Cryptonite_jniFailure(JNIEnv * env, jobject thiz)
{
    return (jint)EXIT_FAILURE;
}

JNIEXPORT jint JNICALL Java_csh_cryptonite_Cryptonite_jniSuccess(JNIEnv * env, jobject thiz)
{
    return (jint)EXIT_SUCCESS;
}

JNIEXPORT jint JNICALL Java_csh_cryptonite_Cryptonite_jniIsValidEncFS(JNIEnv * env, jobject thiz, jstring srcdir)
{
    const char* c_srcdir = env->GetStringUTFChars(srcdir, 0);
    jint res = (jint)isValidEncFS(c_srcdir);
    env->ReleaseStringUTFChars(srcdir, c_srcdir);

    return res;
}

JNIEXPORT jint JNICALL
Java_csh_cryptonite_Cryptonite_jniBrowse(JNIEnv* env, jobject thiz, jstring srcdir, jstring destdir, jstring password)
{
    const char* c_srcdir = env->GetStringUTFChars(srcdir, 0);
    const char* c_password = env->GetStringUTFChars(password, 0);
    int res = EXIT_FAILURE;

    RootPtr rootInfo = initRootInfo(std::string(c_srcdir), std::string(c_password));

    env->ReleaseStringUTFChars(srcdir, c_srcdir);

    /* clear password copy */
    int pw_len = (int)env->GetStringLength(password);
    memset((char*)c_password, 0, pw_len);
    env->ReleaseStringUTFChars(password, c_password);

    if(!rootInfo)
        return EXIT_FAILURE;

    const char* c_destdir = env->GetStringUTFChars(destdir, 0);
    std::string std_destdir(c_destdir);
    // if the dir doesn't exist, then create it (with user permission)
    if(!checkDir(std_destdir) && !userAllowMkdir(c_destdir, 0700))
	return EXIT_FAILURE;
    std::ostringstream out;

    res = traverseDirs(rootInfo, "/", c_destdir, true);
    
    env->ReleaseStringUTFChars(destdir, c_destdir);

    return res;
}

JNIEXPORT jstring JNICALL
Java_csh_cryptonite_Cryptonite_jniVersion(JNIEnv* env, jobject thiz)
{
    return env->NewStringUTF(VERSION);
}
