SteamTrade-Java
===============

An unofficial trading library for Valve's Steam Community service.

A heavily modified fork of Top-Cat's Scrapbank.tf project, designed to be less dependent on the project's bots and Steamkit-specific classes.

It also does not rely on any of Valve's public-facing API for inventory loading and supports private backpacks and dynamic loading from inventories, so there is no need for an API key.

To use the API, one must have a valid sessionId and Steam login token, and also know when a trade initiated. The library tries to be as independent as possible (e.g., using long values instead of SteamKit-Java's SteamIDs), but ultimately, using SteamKit-Java or a similar Steam client library would be the best option.
