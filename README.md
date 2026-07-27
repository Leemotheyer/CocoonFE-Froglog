# Cocoon

> **CocoonFE-Froglog:** One Cocoon app with a **Froglog Pod** (account + sync) and automatic playtime upload from the session tracker. See [FROGLOG.md](FROGLOG.md). Upstream: [inssekt/CocoonFE](https://github.com/inssekt/CocoonFE).

<div align="center">

![Banner](banner.png)

**A Modern Emulation Frontend for Android**

[![Platform](https://img.shields.io/badge/platform-Android-green.svg)](https://www.android.com/)
![Discord](https://img.shields.io/discord/1445504354563002429)


*An emulation frontend inspired by the 3DS UI built for single and dual screen Android devices*

[Features](#features) • [Installation](#installation) • [Setup](#setup)

</div>

---

## Features

### Multi-Platform Game Library
- **Universal Emulator Support**: Works with any Android emulator (RetroArch, Dolphin, PPSSPP, and more)
- **100+ Platform Support**: Nintendo (NES, SNES, N64, GameCube, Wii, Switch), PlayStation (1-3), Sega (Genesis, Saturn, Dreamcast), Arcade, and more
- **Smart Organization**: Automatically categorizes games by platform with intelligent folder detection

### Beautiful UI & Visuals
- **Rich Artwork**: Automatically fetches icons, logos, and hero images from SteamGridDB or ScreenScraper
- **Smooth Animations**: Fluid transitions and responsive interactions
- **Adaptive game grid with zoom feature**: just like the 3DS!

### Automatic Metadata Scraping
- **Hash-Based ROM Identification**: Uses CRC32, MD5, and SHA1 for accurate game matching
- **ScreenScraper.fr Integration**: 
  - Comprehensive game metadata (title, description, release date, genres)
  - Developer and publisher information
  - User ratings and classifications
  - Multiple regions support
  - Works with guest API (no account required) or authenticated for faster access
- **SteamGridDB Artwork**: High-quality game artwork in multiple styles
- **Batch Processing**: Scrape entire libraries with progress tracking

### Advanced Display Features

![Banner](screen_swap.gif)

- **External Display Support**: Display game information and launch games to an external display
- **Display Management**: 
  - Automatic external display detection
  - Seamless game launching to secondary displays
  - External display overlay with game info
- **Live screen-swapping**: Seamlessly switch between screen roles with a button

### Powerful Configuration
- **Per-Platform Settings**: 
  - Custom emulator app/player selection
  - Multiple ROM folder paths per platform
- **Appearance Customization**:
  - Customize library layout
  - Icon image size adjustment

---

## Installation
[<img src="https://raw.githubusercontent.com/ImranR98/Obtainium/refs/heads/main/assets/graphics/badge_obtainium.png"
     alt="Get it on Obtainium"
     height="55"
     style="border: 13px solid transparent;">](https://apps.obtainium.imranr.dev/redirect?r=obtainium://app/%7B%22id%22%3A%22rip.moth.cocoonshell%22%2C%22url%22%3A%22https%3A%2F%2Fgithub.com%2Finssekt%2FCocoonFE%22%2C%22author%22%3A%22inssekt%22%2C%22name%22%3A%22CocoonFE%22%2C%22preferredApkIndex%22%3A0%2C%22additionalSettings%22%3A%22%7B%5C%22includePrereleases%5C%22%3Atrue%2C%5C%22fallbackToOlderReleases%5C%22%3Atrue%2C%5C%22filterReleaseTitlesByRegEx%5C%22%3A%5C%22%5C%22%2C%5C%22filterReleaseNotesByRegEx%5C%22%3A%5C%22%5C%22%2C%5C%22verifyLatestTag%5C%22%3Afalse%2C%5C%22sortMethodChoice%5C%22%3A%5C%22date%5C%22%2C%5C%22useLatestAssetDateAsReleaseDate%5C%22%3Afalse%2C%5C%22releaseTitleAsVersion%5C%22%3Afalse%2C%5C%22trackOnly%5C%22%3Afalse%2C%5C%22versionExtractionRegEx%5C%22%3A%5C%22%5C%22%2C%5C%22matchGroupToUse%5C%22%3A%5C%22%5C%22%2C%5C%22versionDetection%5C%22%3Atrue%2C%5C%22releaseDateAsVersion%5C%22%3Afalse%2C%5C%22useVersionCodeAsOSVersion%5C%22%3Afalse%2C%5C%22apkFilterRegEx%5C%22%3A%5C%22%5C%22%2C%5C%22invertAPKFilter%5C%22%3Afalse%2C%5C%22autoApkFilterByArch%5C%22%3Atrue%2C%5C%22appName%5C%22%3A%5C%22%5C%22%2C%5C%22appAuthor%5C%22%3A%5C%22%5C%22%2C%5C%22shizukuPretendToBeGooglePlay%5C%22%3Afalse%2C%5C%22allowInsecure%5C%22%3Afalse%2C%5C%22exemptFromBackgroundUpdates%5C%22%3Afalse%2C%5C%22skipUpdateNotifications%5C%22%3Afalse%2C%5C%22about%5C%22%3A%5C%22%5C%22%2C%5C%22refreshBeforeDownload%5C%22%3Afalse%2C%5C%22includeZips%5C%22%3Afalse%2C%5C%22zippedApkFilterRegEx%5C%22%3A%5C%22%5C%22%7D%22%2C%22overrideSource%22%3Anull%7D)

### Download
1. Download the latest APK from [Releases](https://github.com/inssekt/CocoonFE/releases)
2. Enable "Install from Unknown Sources" in Android settings
3. Install the APK

---

## 🎯 Setup

### First-Time Setup Wizard

---

## Setups (Optional but recommended):
   - **SteamGridDB**: Get artwork for your games
     - Visit [steamgriddb.com/profile/preferences/api](https://www.steamgriddb.com/profile/preferences/api)
     - Create an API key
   - **ScreenScraper.fr**: Get game metadata and/or artwork
     - Register at [screenscraper.fr](https://www.screenscraper.fr/)
     - Optional - works with guest API without login

2. **Organization Mode**:
   - **Smart Folders** (Recommended): Cocoon auto-detects platforms and creates smart folders from which you can create homescreen shortcuts
   - **Manual Mode**: Cocoon puts all detected ROMs onto your homescreen to be organised yourself (Smart Folders can still be manually created)

3. **ROM Folders**:
   - Browse to your ROM root folder
   - Cocoon will scan for games automatically with optional subfolder scanning

---


The complete platform list is available in [index.json](platforms/index.json).

---


## Support

For bug reports and feature requests, please use [GitHub Discussions](https://github.com/inssekt/CocoonFE/discussions). For general help and community chat, [join our Discord](https://discord.gg/cocoon).

---

## Acknowledgments

- **ScreenScraper.fr**: Game metadata and ROM information
- **SteamGridDB**: High-quality game artwork
- **Daijisho**: A great curated collection of platforms & players that we use as a base
