# MyWorld
3D graphics platform for building Android games.  

All of the games written by Gallant Realm are based on this homegrown 3D graphics engine.  It's not the best graphics engine by any means, but it gets the job done and was fun to build.

You'll find two different rendering implementations for pre-OpenSL 2.0 and post-OpenSL 2.0.  These days only post-OpenSL 2.0 is likely needed, but I'm keeping the old implementation around for a while.

You'll also find pieces of a client/server implementation which is likely not working.  This predates my Android programming and was a pure-Java windows implementation which allowed a server world to be mirrored by clients.  I might get this working again so I've left the code in place.

Note that this project is still eclipse-based.  I haven't had much luck in converting to AndroidStudio, so am continuing to use eclipse (with the AndMore plugin).

## Acknowledgments

MyWorld contains modified versions of the following open source:

- _android-toolbox_ by Victor Reiser.  The current original is at https://github.com/Knickedi/android-toolbox.  MIT License. 

- _aFileChooser_ by Paul Burke.  The original is at https://github.com/iPaulPro/aFileChooser.  Apache License.

- _Base64_ by Rober Harder and modified by Google.  http://iharder.sourceforge.net/base64.  Apache 2.0 license.

- Sample code for billing from Google.  Apache 2.0 license.

I've left the original package names should you want to compare and update.  I highly appreciate the efforts of these developers and their generous donation to the open source community.
