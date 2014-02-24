SteamTrade-Java
===============

An unofficial trading library for Valve's Steam Community service.


About
-----

A heavily modified fork of [Top-Cat's Scrapbank.tf project](https://github.com/Top-Cat/ScrapBank.tf/), designed to be less dependent on the project's bots and Steamkit-specific classes.

It does not rely on any of Valve's public-facing API for inventory loading and supports private backpacks and dynamic loading from inventories, so there is no need for an API key.

Currently unsupported are stackable items and currencies.  The library won't bug out on them; they just won't be responded to.  Support for them should hopefully be added in the future.


Prerequisites, Dependencies and How-To
--------------------------------------

To use the library, one must have a valid Steam sessionId and Steam login token, and also know when a trade is initiated. The library tries to be as independent as possible (e.g., using long values instead of SteamKit-Java's SteamIDs), but ultimately, using SteamKit-Java or a similar Steam client library would be the current best option.

A small snippet of the library in example use is available as the SampleTrade project.

This is a Netbeans project and is dependent on only the JSON Simple library.  In the future, this may be changed to use the reference ```org.json.JSONObject``` library, if only because I am comfortable in it and prefer its unambiguousness.

Just a Note
-----------

This library, while fairly featured and fleshed out for normal use, is still undergoing changes in structure, shedding off old stuff and rearranging and streamlining others; be sure to keep an eye on the methods and what various changes there may be.
