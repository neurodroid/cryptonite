#include <android/log.h>

#include <openssl/blowfish.h>
#include <openssl/evp.h>

#include <iostream>
#include <sstream>
#include <cerrno>

#define BUF_SIZE 1024 + EVP_MAX_BLOCK_LENGTH

#define  LOG_TAG    "cryptonite-key-jni"
#define  LOGI(...)  __android_log_print(ANDROID_LOG_INFO,LOG_TAG,__VA_ARGS__)
#define  LOGE(...)  __android_log_print(ANDROID_LOG_ERROR,LOG_TAG,__VA_ARGS__)

#if 0
#include "android_fake.h"
#else
#include "android_key.h"
#endif

const char* decrypt(unsigned char* inbuff, int inbuff_len, unsigned char* key, unsigned char* iv)
{
    EVP_CIPHER_CTX  ctx;
    EVP_CIPHER_CTX_init(&ctx);
    EVP_DecryptInit(&ctx, EVP_bf_cbc(), key, iv);

    unsigned char* outbuf = (unsigned char *)malloc(sizeof(unsigned char) * BUF_SIZE);
    memset(outbuf, 0, BUF_SIZE);

    int olen=0, tlen=0;
    if (EVP_DecryptUpdate(&ctx, outbuf, &olen, inbuff, inbuff_len) != 1) {
        LOGE("Error in decrypt update");
        return 0;
    }

    if (EVP_DecryptFinal(&ctx, outbuf + olen, &tlen) != 1) {
        std::ostringstream err;
        err << "Error in decrypt final: " << strerror(errno);
        LOGE(err.str().c_str());
        return 0;
    }

    EVP_CIPHER_CTX_cleanup(&ctx);

    outbuf[olen+tlen-1] = '\0';
    return (const char*) outbuf;
}

const char* get_key() {
    return decrypt(app_key_enc, 24, mykey, myiv);
}

const char* get_pw() {
    return decrypt(app_pw_enc, 24, mykey, myiv);
}
