# Fran Launcher

An ultralight Android TV launcher: a clock over a sky that follows the time of day, your apps, and nothing else. No data collection, no ads, no accounts, no internet permission.

Made by [Pakvothe](https://github.com/Pakvothe).

[Español](README.es.md) · **[Download the APK](https://github.com/Pakvothe/fran-launcher-tv/releases/latest)** (141 KB, Android TV 10+)

## Why

The stock Android TV launcher takes over 100 MB, pulls remote recommendations and banners, and stutters while you navigate. Fran Launcher does less on purpose: everything is drawn locally, from disk, in a single native View.

Measured on an Admiral 4K (Android TV 10, 1080p UI) against the stock launcher, with every feature on:

| | Fran Launcher | Stock |
|---|---|---|
| APK size | **141 KB** | 12.7 MB |
| Private RAM after 10 min | **11 MB** | 54-62 MB |
| Janky frames while navigating | **0%** (p99 9 ms) | 5.9% (p99 30 ms) |
| Cold start | **285 ms** | 625-712 ms |
| Back to Home (own cost) | **3-8 ms** | n/a |
| Idle CPU (ambient mode) | **0.03%** no seconds · 0.56% with seconds | ~0.01% |

With the stock launcher disabled, available RAM two minutes after a clean boot went from 804 MB to 938 MB.

## Features

- **Sky of the hour**: a gradient and two soft light orbs that follow local time (night, dawn, day, dusk), crossfading between phases.
- **Big clock** with tabular digits, optional seconds (number, line or off), date, 12/24 h.
- **Favorites row** with banners, animated focus, label under focus, and focus restored to the last app used.
- **All apps** side panel: wheel-picker style list, letter index, and a long-press menu (move, remove/add to home, info, uninstall).
- **Inputs**: HDMI and AV with signal state (only if the TV has inputs).
- **Now playing**: what's on, with artwork and play/pause. Optional; needs notification access, used only to read the media session.
- **Ambient mode** after a few idle seconds, with OLED protection (the clock shifts every minute) and night dimming.
- **Theme**: light, dark, or automatic by time of day.
- **Languages**: Spanish, English and Portuguese, with its own picker in Settings.
- **Accessibility**: virtual nodes for TalkBack.

## How it's built

- **Plain Java**, `minSdk 29`, no Kotlin, no AndroidX, zero third-party dependencies.
- **One Activity, one View, a tree of `RenderNode`s**: no XML layouts, no `RecyclerView`, no `Fragment`. Focus and animations are just properties on already-recorded nodes.
- **Own animation engine** on top of `Choreographer`, with re-targeting and no per-frame allocations.
- **Zero idle work**: no polling; orbs and clock only request frames when something actually changed.
- **Boots from a binary snapshot**: the app list, config, and banner/icon atlases are read raw (`FileChannel.map`), no image decoding. The `PackageManager` query runs after the first frame, on a single I/O thread.
- **JVM-testable logic**: the focus state machine, snapshot format, sky phases and OLED sequence live in the `core` module with no Android APIs (41 tests).

```
launcher/
  app/       Android app (HomeActivity, Stage, scene layers, data, media, TV)
  core/      Plain Java logic + JUnit tests
  scripts/   bench.sh: reproducible on-device benchmark
```

## Install and set as Home

Grab `fran-launcher.apk` from [Releases](https://github.com/Pakvothe/fran-launcher-tv/releases/latest) (or build it) and install it over adb:

```sh
adb connect <TV IP>:5555
adb install -r fran-launcher.apk
adb shell cmd package compile -m speed -f pakvothe.franlauncher.tv
adb shell cmd package set-home-activity pakvothe.franlauncher.tv/.Home
```

On Google TV and many Android TV devices, the Home button still goes to the stock launcher because its intent filter has system priority. In that case, disable it (reversible):

```sh
adb shell pm disable-user --user 0 com.google.android.tvlauncher   # disable
adb shell pm enable com.google.android.tvlauncher                  # undo
```

A system update may re-enable the original launcher.

For "Now playing" on TVs without a notification access screen:

```sh
adb shell cmd notification allow_listener pakvothe.franlauncher.tv/pakvothe.franlauncher.tv.media.MediaAccessService
```

## Build

Requires JDK 17 and the Android SDK (platform 36).

```sh
cd launcher
./gradlew :core:test                                  # JVM tests
./gradlew :app:assembleRelease                        # APK without the HOME filter
./gradlew :app:assembleRelease -PhomeEnabled=true     # APK that can be the launcher
```

The HOME filter lives in an `activity-alias` that's off by default, so you can try the app next to the stock launcher without the TV showing a chooser when you press Home. Without a signing config, release builds are signed with the debug key.

## Benchmark

```sh
export SERIAL=<TV IP>:5555
launcher/scripts/bench.sh -l "label"            # full, ~4 min
launcher/scripts/bench.sh --quick -l "label"    # quick, ~1:45
launcher/scripts/bench.sh -l stock -p com.google.android.tvlauncher -a .MainActivity --no-compile
```

Measures cold start, onCreate and process-to-first-frame, back-to-Home, smoothness on the row and the list, detailed memory, CPU and idle heap. Results go to `./bench-results` (override with `DEVICE_DIR`); exits with code 2 if any timing regresses more than 10% against the previous run or memory/APK exceed budget. The APK under test must be built with `-PbenchExtras=true` (it accepts `fl.*` launch extras only then). It sends key events over adb: don't use the TV while it runs.

## Privacy

No internet, location or storage permissions, and no `QUERY_ALL_PACKAGES`. The only permission is `REQUEST_DELETE_PACKAGES`, to uninstall apps from the menu. Config never leaves the device (`allowBackup="false"`). Notification access is optional and the service is empty: it only enables seeing what's playing.

## License

[MIT](LICENSE).

## Credits

[Outfit](https://github.com/Outfitio/Outfit-Fonts) font, under the SIL Open Font License 1.1 (full text in Settings > Licenses).
