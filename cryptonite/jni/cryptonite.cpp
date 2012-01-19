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
#include <boost/filesystem.hpp>

#include <string>
#include <set>
#include <iostream>
#include <sstream>
#include <cerrno>
#include <unistd.h>

#include <sys/types.h>
#include <sys/stat.h>
#include <dirent.h>

#include <encfs.h>
#include <FileNode.h>
#include <DirNode.h>
#include <config.h>

#define  LOG_TAG    "cryptonite-jni"
#define  LOGI(...)  __android_log_print(ANDROID_LOG_INFO,LOG_TAG,__VA_ARGS__)
#define  LOGE(...)  __android_log_print(ANDROID_LOG_ERROR,LOG_TAG,__VA_ARGS__)

namespace fs = boost::filesystem;

enum writeMode {
    WRITE,
    TOUCH,
    EXPORT
};
    
/* Ugly hack for now: Keep a global rootPtr for repeated access
   to the same decoded EncFS
   TODO: return a rootPtr equivalent to Java instead
*/
RootPtr gRootInfo;

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
static int processContents( const shared_ptr<EncFS_Root> &lRootInfo, 
                            const char *path, T &op )
{
    int errCode = 0;
    shared_ptr<FileNode> node = lRootInfo->root->openNode( path, "encfsctl",
	    O_RDONLY, &errCode );

    if(!node)
    {
        // try opening directly, so a cipher-path can be passed in
        node = lRootInfo->root->directLookup( path );
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
                    const boost::shared_ptr<EncFS_Root> &lRootInfo,
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
    std::string decodedLink = lRootInfo->root->plainPath(buf.get());

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

static int copyContents(const boost::shared_ptr<EncFS_Root> &lRootInfo, 
                        const char* encfsName, const char* targetName,
                        bool fake)
{
    boost::shared_ptr<FileNode> node = 
	lRootInfo->root->lookupNode( encfsName, "encfsctl" );

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
            std::string d = lRootInfo->root->cipherPath(encfsName);
            char linkContents[PATH_MAX+2];

            if(readlink (d.c_str(), linkContents, PATH_MAX + 1) <= 0)
            {
                std::ostringstream err;
                err << "unable to read link " << encfsName;
                LOGE(err.str().c_str());
                return EXIT_FAILURE;
            }
            symlink(lRootInfo->root->plainPath(linkContents).c_str(), 
		    targetName);
        } else
        {
            int outfd = creat(targetName, st.st_mode);

            if (outfd == -1) {
                if (errno == EACCES /* sic! */ || errno == EROFS || errno == ENOSPC) {
                    std::ostringstream out;
                    out << "Not creating " << targetName << ": "
                        << strerror(errno);
                    LOGE(out.str().c_str());
                    return EXIT_FAILURE;
                }
            }

            if (!fake) {
                WriteOutput output(outfd);
                std::ostringstream out;
                out << "Writing to " << encfsName;
                LOGE(out.str().c_str());
                processContents( lRootInfo, encfsName, output );
            }
        }
    }
    return EXIT_SUCCESS;
}

static int traverseDirs(const boost::shared_ptr<EncFS_Root> &lRootInfo, 
                        std::string volumeDir, std::string destDir,
                        const std::set<std::string>& toWrite )
{
    bool fake = toWrite.empty();
    
    if(!endsWith(volumeDir, '/'))
	volumeDir.append("/");
    if(!endsWith(destDir, '/'))
	destDir.append("/");

    std::string test_string = volumeDir;
    if (endsWith(test_string, '/') &&
        test_string.length() != 1) {
        test_string = test_string.substr(0, test_string.length()-1);
    }
    /* Abort if we're in export mode and this directory
     * doesn't need to be written
     */
    if (!toWrite.empty() &&
        toWrite.find(test_string) == toWrite.end()) {
        return EXIT_SUCCESS;
    }

    // Lookup directory node so we can create a destination directory
    // with the same permissions
    {
        struct stat st;
        shared_ptr<FileNode> dirNode = 
            lRootInfo->root->lookupNode( volumeDir.c_str(), "encfsctl" );
        if(dirNode->getAttr(&st))
            return EXIT_FAILURE;

        // In fake mode, we always create rw:
        mode_t srcmode = st.st_mode;
        if (fake)
            srcmode = S_IRWXU;
        if (mkdir(destDir.c_str(), srcmode) == -1) {
            if (errno == EACCES /* sic! */ || errno == EROFS || errno == ENOSPC) {
                std::ostringstream out;
                out << "Not creating " << destDir << ": "
                    << strerror(errno);
                LOGE(out.str().c_str());
                return EXIT_FAILURE;
            }
        }
    }
    // show files in directory
    DirTraverse dt = lRootInfo->root->openDir(volumeDir.c_str());
    if(dt.valid())
    {
        for(std::string name = dt.nextPlaintextName(); !name.empty(); 
            name = dt.nextPlaintextName())
        {
            bool skip = !toWrite.empty() && toWrite.find(volumeDir+name) == toWrite.end();

            // Recurse to subdirectories
            if(name != "." && name != ".." && !skip)
            {
                std::string plainPath = volumeDir + name;
                std::string cpath = lRootInfo->root->cipherPath(plainPath.c_str());
                std::string destName = destDir + name;

                std::ostringstream out;
                out << "Decoding " << cpath << " to " << plainPath << " in " << destName;
                LOGI(out.str().c_str());
                
                int r = EXIT_SUCCESS;
                struct stat stBuf;
                if( !lstat( cpath.c_str(), &stBuf ))
                {
                    if( S_ISDIR( stBuf.st_mode ) )
                    {
                        r = traverseDirs(lRootInfo, (plainPath + '/').c_str(), 
                                         destName + '/', toWrite);
                    } else if( S_ISLNK( stBuf.st_mode ))
                    {
                        r = copyLink( stBuf, lRootInfo, cpath, destName );
                    } else
                    {
                        r = copyContents(lRootInfo, plainPath.c_str(), 
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

static int exportFiles(const boost::shared_ptr<EncFS_Root> &lRootInfo, 
                       std::string volumeDir, std::string destDir,
                       const std::set<std::string>& decodedNames) {
    return traverseDirs(lRootInfo, volumeDir, destDir, decodedNames);
}

static RootPtr initRootInfo(const std::string& rootDir, const std::string& password)
{
    RootPtr result;
    boost::shared_ptr<EncFS_Opts> opts( new EncFS_Opts() );
    opts->createIfNotFound = false;
    opts->checkKey = true;
    opts->password.assign(password);
    opts->rootDir.assign(rootDir);
    if(checkDir( opts->rootDir )) {
        LOGI((std::string("Initialising file system with root ") + rootDir).c_str());
        result = initFS( NULL, opts );
    }

    // clear buffer
    opts->password.assign(opts->password.length(), '\0');
    
    if(!result) {
        LOGE("Unable to initialize encrypted filesystem - check path.");
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
    JNIEXPORT jint    JNICALL Java_csh_cryptonite_Cryptonite_jniExport(JNIEnv * env, jobject thiz,
                                                                       jobjectArray exportpaths, jstring exportroot,
                                                                       jstring destdir);
    JNIEXPORT jstring JNICALL Java_csh_cryptonite_Cryptonite_jniVersion(JNIEnv * env, jobject thiz);
#ifdef __cplusplus
};
#endif


class jniStringManager {
  public:
    jniStringManager()
    {
        env = NULL;
        pjs = NULL;
        pc = NULL;
    }

    jniStringManager(JNIEnv* penv, jstring src) :
        env(penv), pjs(src)
    {
        pc = env->GetStringUTFChars(pjs, 0);
        str_repr = std::string(pc);
    }

    ~jniStringManager() {
        release();
    }

    void init(JNIEnv* penv, jstring src) {
        env = penv;
        pjs = src;
        pc = penv->GetStringUTFChars(pjs, 0);
        str_repr = std::string(pc);
    }

    void release() {
        if (pjs != NULL) {
            str_repr.assign(str_repr.length(), '\0');

            int len = (int)env->GetStringLength(pjs);
            memset((char*)pc, 0, len);
            
            env->ReleaseStringUTFChars(pjs, pc);
            pjs = NULL;
        }
    }

    const char* c_str() {
        return pc;
    }
    
    const std::string& str() const {
        return str_repr;
    }
    
    std::string& str() {
        return str_repr;
    }
    
  private:

    JNIEnv* env;
    jstring pjs;
    const char* pc;
    std::string str_repr;

};

void recTree(std::string currentPath, std::set<std::string>& fullList, const std::string& exportroot) {
    if (fs::exists(currentPath)) {

        fs::path bRoot(exportroot);
        fs::path bPath(currentPath);
        std::string stripstr = bPath.string().substr(bRoot.string().length());
        LOGI((std::string("recTree: ") + stripstr).c_str());
        if (fs::is_directory(bPath.string())) {

            fullList.insert(stripstr);

            /* boost::filesystem will throw an exception
             * if the max number of open files is reached
             */
#if 0
            fs::directory_iterator end_iter;
            for (fs::directory_iterator dir_iter(bPath);
                 dir_iter != end_iter;
                 ++dir_iter) {
                recTree(dir_iter->string(), fullList, exportroot);
            }
#else
            DIR* d;
            std::string dir_str = bPath.string();
            LOGI((std::string("dir_str: ") + dir_str).c_str());
            if (dir_str.at(dir_str.length() - 1) != '/') dir_str += "/";
            if ((d = opendir(dir_str.c_str())) == NULL) return;

            struct dirent* de;

            while ((de = readdir(d)) != NULL) {
                std::string path = dir_str;

                if (std::string(de->d_name) != "." && std::string(de->d_name) != "..") {
                    path += std::string(de->d_name);
                    LOGI((std::string("path: ") + path).c_str());
                    recTree(path, fullList, exportroot);
                }
            }
            closedir(d);
#endif
        } else {
            fullList.insert(stripstr);
        }
    }
}

std::set<std::string> fullTree(const std::set<std::string>& pathList,
                               const std::string& exportroot) {

    std::set<std::string> fullList;
    fullList.insert("/");
    
    std::set<std::string>::const_iterator it = pathList.begin();
    for (; it != pathList.end(); ++it) {
        recTree(*it, fullList, exportroot);
    }

    return fullList;
}

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
    int pw_len = (int)env->GetStringLength(password);
    if (pw_len  == 0) {
        return EXIT_FAILURE;
    }
    
    jniStringManager msrcdir(env, srcdir);
    jniStringManager mpassword(env, password);
    int res = EXIT_FAILURE;

    if ((jint)isValidEncFS(msrcdir.str()) != EXIT_SUCCESS) {
        LOGE("EncFS root directory is not valid");
        return EXIT_FAILURE;
    }
    gRootInfo = initRootInfo(msrcdir.str(), mpassword.str());

    /* clear password copy */
    mpassword.release();

    if(!gRootInfo) {
        LOGE("Error initialising root volume");
        return EXIT_FAILURE;
    }

    if (!gRootInfo->volumeKey) {
        LOGI("Wrong password");
        return EXIT_FAILURE;
    }
    
    jniStringManager mdestdir(env, destdir);

    // if the dir doesn't exist, then create it (with user permission)
    if(!checkDir(mdestdir.str()) && !userAllowMkdir(mdestdir.c_str(), 0700))
	return EXIT_FAILURE;

    std::set<std::string> empty;
    res = traverseDirs(gRootInfo, "/", mdestdir.str(), empty);
    
    return res;
}

JNIEXPORT jint JNICALL Java_csh_cryptonite_Cryptonite_jniExport(JNIEnv * env, jobject thiz, jobjectArray exportpaths,
                                                                jstring exportroot, jstring destdir)
{
    int res = EXIT_FAILURE;
    
    // TODO: put these checks back to the beginning
    if(!gRootInfo) {
        LOGE("No EncFS root info");
        return EXIT_FAILURE;
    }

    if (!gRootInfo->volumeKey) {
        LOGI("No EncFS volume key");
        return EXIT_FAILURE;
    }

    int npaths = env->GetArrayLength(exportpaths);
    if (npaths==0)
        return res;
    
    std::ostringstream info;
    info << "Received " << npaths << " paths";
    LOGI(info.str().c_str());

    jniStringManager mexportroot = jniStringManager(env, exportroot);
    
    jclass stringClass = env->FindClass("java/lang/String");
     
    std::set<std::string> std_exportpaths;
    for (int nstr = 0; nstr < npaths; ++nstr) {
        jobject obj = env->GetObjectArrayElement(exportpaths, nstr);
        if (env->IsInstanceOf(obj, stringClass)) {
            std_exportpaths.insert(jniStringManager(env, (jstring)obj).str());
        }
    }

    std::set<std::string> allPaths(fullTree(std_exportpaths, mexportroot.str()));

    info.str("");
    info << "Full size of tree is " << allPaths.size();
    LOGI(info.str().c_str());

    
    std::set<std::string>::const_iterator it = allPaths.begin();
    for (; it != allPaths.end(); ++it) {
        LOGI((*it).c_str());
    }

    jniStringManager mdestdir(env, destdir);

    // if the dir doesn't exist, then create it (with user permission)
    if(!checkDir(mdestdir.str()) && !userAllowMkdir(mdestdir.c_str(), 0700))
	return EXIT_FAILURE;

    LOGI((std::string("Exporting to ") + mdestdir.str()).c_str());
    res = (int)exportFiles(gRootInfo, "/", mdestdir.str(), allPaths);

    return res;
}

JNIEXPORT jstring JNICALL
Java_csh_cryptonite_Cryptonite_jniVersion(JNIEnv* env, jobject thiz)
{
    return env->NewStringUTF(VERSION);
}
