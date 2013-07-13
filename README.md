
Description
===========

DroidUPnP is an upnp control point application for android.
DroidUPnP discover your home UPnP device, content provider,
connected television or any device controllable by UPnP
and allow you to control those.
It also allow you to share your local content to other UPnP capable device.

It is licensed under the **GPLv3**.

BUILD
=====

Application can be build using maven.

To generate the apk :

	mvn install

To install on your android device via adb :

	mvn android:deploy

Dependencies
============

The main dependence of DroidUPnP is **Cling** which is the an
upnp stack made in java and with an android support layer.
Cling in licensed under the **LGPLv2**.
Source code is available at [4thline.org](http://4thline.org/projects/cling/)

DroidUPnP use also **NanoHttpd**. NanoHttpd is under a modify **BSD license**.
Source code is available on [GitHub](https://github.com/NanoHttpd/nanohttpd).

Copying
=======

Copyright (C) 2013, Aurélien Chabot <aurelien@chabot.fr>

Licensed under **GPLv3**

See [COPYING](https://github.com/trishika/DroidUPnP/blob/master/COPYING).
