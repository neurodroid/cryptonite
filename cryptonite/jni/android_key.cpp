#include <openssl/blowfish.h>
#include <openssl/evp.h>

#include <iostream>
#include <sstream>
#include <cerrno>

#define BUF_SIZE 1024 + EVP_MAX_BLOCK_LENGTH


#ifndef STANDALONE
#include <android/log.h>
#define  LOG_TAG    "cryptonite-key-jni"
#define  LOGI(...)  __android_log_print(ANDROID_LOG_INFO,LOG_TAG,__VA_ARGS__)
#define  LOGE(...)  __android_log_print(ANDROID_LOG_ERROR,LOG_TAG,__VA_ARGS__)
#else
#include <cstring>
#define  LOGI(x)  std::cout << x << std::endl;
#define  LOGE(x)  std::cerr << x << std::endl;
#endif

#if 0
#include "android_fake.h"
#else
#include "android_key.h"
#endif

const char* encrypt(unsigned char* inbuff, int inbuff_len, unsigned char* key, unsigned char* iv)
{
    EVP_CIPHER_CTX  ctx;
    EVP_CIPHER_CTX_init(&ctx);
    EVP_EncryptInit(&ctx, EVP_bf_cbc(), key, iv);

    unsigned char* outbuf = (unsigned char *)malloc(sizeof(unsigned char) * BUF_SIZE);
    memset(outbuf, 0, BUF_SIZE);

    int olen=0, tlen=0;
    if (EVP_EncryptUpdate(&ctx, outbuf, &olen, inbuff, inbuff_len) != 1) {
        LOGE("Error in encrypt update");
        return 0;
    }

    if (EVP_EncryptFinal(&ctx, outbuf + olen, &tlen) != 1) {
        std::ostringstream err;
        err << "Error in encrypt final: " << strerror(errno);
        LOGE(err.str().c_str());
        return 0;
    }

    EVP_CIPHER_CTX_cleanup(&ctx);

    return (const char*) outbuf;
}

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
        LOGE("Error in decrypt final");
        return 0;
    }

    EVP_CIPHER_CTX_cleanup(&ctx);

    outbuf[olen+tlen-1] = '\0';
    return (const char*) outbuf;
}

const char* get_full_key() {
    return decrypt(app_full_key_enc, 24, mykey, myiv);
}

const char* get_full_pw() {
    return decrypt(app_full_pw_enc, 24, mykey, myiv);
}

const char* get_folder_key() {
    return decrypt(app_folder_key_enc, 25, mykey, myiv);
}

const char* get_folder_pw() {
    return decrypt(app_folder_pw_enc, 25, mykey, myiv);
}

#ifdef STANDALONE
int main(int argc, char* argv[]) {
    unsigned char secret[16] = {};
    const char* enc = encrypt(secret, 16, mykey, myiv);
    const char* dec = decrypt((unsigned char*)enc, strlen(enc), mykey, myiv);
    std::cout << enc << std::endl;
}
#endif
