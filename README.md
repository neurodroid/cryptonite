# Cryptonite

## Cryptonite is not actively maintained any longer

## Introduction

Cryptonite brings [EncFS](http://www.arg0.net/encfs) and [TrueCrypt](http://www.truecrypt.org/) to Android. You can browse, export and open EncFS-encrypted directories and files on your Dropbox and on your phone. On rooted phones that support [FUSE](http://fuse.sourceforge.net/) (e.g. [CyanogenMod](http://www.cyanogenmod.com/)) you can also mount EncFS and TrueCrypt volumes. TrueCrypt is only available as a command-line version at this time.

[I haven't had any backdoor requests yet](http://www.theguardian.com/technology/2013/sep/09/nsa-sabotage-dead-mans-switch). [Watch closely for the removal of the previous sentence](http://www.librarian.net/technicality.html).

## Get Cryptonite

Cryptonite is no longer available from Google Play. Bleeding-edge alpha versions are sometimes available [here](https://github.com/neurodroid/cryptonite/releases). Report bugs to the [issue tracker](https://github.com/neurodroid/cryptonite/issues).

## More information

What's new? See [changelog](https://code.google.com/p/cryptonite/wiki/ChangeLog).

Read up on important security considerations [here](https://code.google.com/p/cryptonite/wiki/SecurityConsiderations).

How can I access encrypted Dropbox volumes when I'm offline? Why can't I decrypt and encrypt on the fly? See the [FAQ](https://code.google.com/p/cryptonite/wiki/FrequentlyAskedQuestions).

Want to build your own apk? See [build instructions](https://github.com/neurodroid/cryptonite/wiki/Build-Instructions).

Want to see Cryptonite on F-Droid? [Vote here](http://f-droid.org/forums/topic/cryptonite/).

## Encrypt your Dropbox with EncFS
### GNU/Linux
This is simple and straightforward. The default packages should work just fine. There's a nice tutorial on [Webupd8](http://www.webupd8.org/2011/06/encrypt-your-private-dropbox-data-with.html).

### OS X
[MacPorts](http://www.macports.org) provides EncFS and works just fine on all OS Xen that I've tested (10.6, 10.7 and 10.8). The MacPorts installation takes some time, but it's definitely worth it. Once installed, simply do:
```bash
sudo port install encfs
```

If you use homebrew:

```
brew cask install osxfuse
brew install homebrew/fuse/encfs
```

To make your decrypted folder look a bit prettier, use
```bash
encfs -ovolname=Dropbox -ovolicon=${ICONFOLDER}/dropbox.icns ${ENCRYPTEDDIR} ${DECRYPTEDDIR}
```
Let me know if you need a hi-res icons file.

### Windows
[encfs4win](http://members.ferrara.linux.it/freddy77/encfs.html) works fine for me.

### Windows & OS X

You can also use the cross-platform frontend [encfsmp](http://encfsmp.sourceforge.net/), if you don't want to use command-line tools.
