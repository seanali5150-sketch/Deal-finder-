# Deal Finder (Android)

Catches price errors and glitch-level deals on **computers, laptops, iPads/tablets and phones** at major stores, and buzzes your phone as soon as one shows up.

## Install

1. On your phone, open `https://github.com/<you>/<repo>/releases/latest/download/DealFinder.apk`.
2. Allow "Install unknown apps" for your browser when Android asks, then install.
3. Open Deal Finder, allow notifications, tap **Live watch** and allow it to ignore battery optimization (otherwise Android pauses checks when the screen is off).

Every push to `main` builds a new APK that installs over the old one.

## Where the deals come from

| Source | Key | What it catches |
|---|---|---|
| Best Buy Products API | free, [developer.bestbuy.com](https://developer.bestbuy.com) | Computers & Tablets and Cell Phones on sale by your minimum %, checked straight from Best Buy |
| Amazon via Keepa | paid (~$20/mo), [keepa.com/#!api](https://keepa.com/#!api) | Amazon items far below their own 90-day average |
| eBay Browse API | free, [developer.ebay.com](https://developer.ebay.com) | New buy-it-now laptops, MacBooks, iPads, phones well under their listed original price |
| Slickdeals + Reddit RSS | none | Price errors people post from any store (Walmart, Target, Costco, Newegg, Dell, Apple, B&H…) |

Official APIs and public feeds only, so nothing gets blocked and no store rules are broken.

## How "way cheaper than normal" is decided

Each product's normal price is, in order of preference: the app's own 90-day median of prices it has seen, the store's 90-day average (Keepa), or the store's regular price. A regular price far above what the app has actually seen is ignored, which filters out fake "was $1,999" stickers. Anything whose title literally says "price error" or "glitch" is always flagged.

Defaults (change in Settings): show at 40% off, alert at 60% off, ignore items normally under $80, Live watch every 3 minutes. Without Live watch, Android checks about every 15 minutes.

## Build locally

Needs JDK 17 and the Android SDK: `./gradlew assembleRelease`.

The signing key in `app/sideload.keystore` is checked in on purpose so updates install over each other. It is only for sideloading; don't use it for a Play Store release.
