SteamTrade-Java (json-reference branch)
=======================================

Where things go to butts and everything breaks!


About
-----

A heavily modified fork of [Top-Cat's Scrapbank.tf project](https://github.com/Top-Cat/ScrapBank.tf/), designed to be less dependent on the project's bots and Steamkit-specific classes.

It does not rely on any of Valve's public-facing API for inventory loading, so there is no need for an API key.

**This branch of the library is being used to rewrite everything to use the reference** ```org.json.*``` **library.  The familiarity of the library will assist in bringing the desired stackable item and currency support.**

**Very little should change on the frontend.**

Support for them should hopefully be added soon, ~~Valve time~~.


Prerequisites, Dependencies and How-To
--------------------------------------

To use the library, one must have a valid Steam sessionId and Steam login token, and also know when a trade is initiated. The library tries to be as independent as possible (e.g., using long values instead of SteamKit-Java's SteamIDs), but ultimately, using SteamKit-Java or a similar Steam client library would be the current best option.

A small snippet of the library in example use is available as the SampleTrade project.

This is a NetBeans project and is dependent on only the reference JSON library.

Just a Note
-----------

This library, while fairly featured and fleshed out for normal use, is still undergoing changes in structure, shedding off old stuff and rearranging and streamlining others; be sure to keep an eye on the methods and what various changes there may be.
