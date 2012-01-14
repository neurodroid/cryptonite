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

#include <boost/shared_ptr.hpp>
#include <boost/scoped_array.hpp>
#include <string>
#include <iostream>

#include <encfs.h>
// #include <FileUtils.h>
// #include <Cipher.h>

// #include <Context.h>
#include <FileNode.h>
#include <DirNode.h>
#include <config.h>

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
        std::cerr << "unable to open " << path << "\n";
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
        std::cerr << "unable to readlink of " << cpath << "\n";
        return EXIT_FAILURE;
    }

    buf[res] = '\0';
    std::string decodedLink = rootInfo->root->plainPath(buf.get());

    res = ::symlink( decodedLink.c_str(), destName.c_str() );
    if(res == -1)
    {
        std::cerr << "unable to create symlink for " << cpath 
            << " to " << decodedLink << "\n";
    }

    return EXIT_SUCCESS;
}

static int copyContents(const boost::shared_ptr<EncFS_Root> &rootInfo, 
                        const char* encfsName, const char* targetName)
{
    boost::shared_ptr<FileNode> node = 
	rootInfo->root->lookupNode( encfsName, "encfsctl" );

    if(!node)
    {
        std::cerr << "unable to open " << encfsName << "\n";
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
                std::cerr << "unable to read link " << encfsName << "\n";
                return EXIT_FAILURE;
            }
            symlink(rootInfo->root->plainPath(linkContents).c_str(), 
		    targetName);
        } else
        {
            int outfd = creat(targetName, st.st_mode);
	    
	    WriteOutput output(outfd);
	    processContents( rootInfo, encfsName, output );
        }
    }
    return EXIT_SUCCESS;
}

static int traverseDirs(const boost::shared_ptr<EncFS_Root> &rootInfo, 
                        std::string volumeDir, std::string destDir)
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
                                     destName + '/');
                    } else if( S_ISLNK( stBuf.st_mode ))
                    {
                        r = copyLink( stBuf, rootInfo, cpath, destName );
                    } else
                    {
                         r = copyContents(rootInfo, plainPath.c_str(), 
                                          destName.c_str());
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

static bool checkDir( std::string &rootDir )
{
    if( !isDirectory( rootDir.c_str() ))
    {
        std::cerr << "directory " << rootDir.c_str() << " does not exist.\n";
	return false;
    }
    if(rootDir[ rootDir.length()-1 ] != '/')
	rootDir.append("/");
    return true;
}

static RootPtr initRootInfo(const char* crootDir)
{
    std::string rootDir(crootDir);
    RootPtr result;
    if(checkDir( rootDir ))
    {
        boost::shared_ptr<EncFS_Opts> opts( new EncFS_Opts() );
	opts->rootDir = rootDir;
	opts->createIfNotFound = false;
	opts->checkKey = false;
	result = initFS( NULL, opts );
    }
    if(!result)
        std::cerr << "Unable to initialize encrypted filesystem - check path.\n";
    return result;
}

#ifdef __cplusplus
extern "C" {
#endif
    JNIEXPORT jint    JNICALL Java_csh_cryptonite_Cryptonite_encfsMount(JNIEnv * env, jobject thiz, jstring srcdir, jstring destdir);
    JNIEXPORT jstring JNICALL Java_csh_cryptonite_Cryptonite_encfsVersion(JNIEnv * env, jobject thiz);
#ifdef __cplusplus
};
#endif

JNIEXPORT jint JNICALL
Java_csh_cryptonite_Cryptonite_encfsBrowse(JNIEnv* env, jobject thiz, jstring srcdir, jstring destdir)
{
    const char* c_srcdir = env->GetStringUTFChars(srcdir, 0);
    int res = EXIT_FAILURE;

    RootPtr rootInfo = initRootInfo(c_srcdir);
    env->ReleaseStringUTFChars(srcdir, c_srcdir);

    if(!rootInfo)
        return EXIT_FAILURE;

    const char* c_destdir = env->GetStringUTFChars(destdir, 0);
    std::string std_destdir(c_destdir);
    // if the dir doesn't exist, then create it (with user permission)
    if(!checkDir(std_destdir) && !userAllowMkdir(c_destdir, 0700))
	return EXIT_FAILURE;
    res = traverseDirs(rootInfo, "/", c_destdir);
    
    env->ReleaseStringUTFChars(destdir, c_destdir);

    return res;
}

JNIEXPORT jstring JNICALL
Java_csh_cryptonite_Cryptonite_encfsVersion(JNIEnv* env, jobject thiz)
{
    return env->NewStringUTF(VERSION);
}
