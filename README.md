KeepScore ![targets Android 16](https://img.shields.io/badge/Android-16%20(API%2036)-3DDC84.svg)
=========

Author
--------
Nolan Lawson wrote KeepScore and all of its original Android code. Upstream
repository: <https://github.com/nolanlawson/KeepScore>.

This repository is a fork of that work, maintained at
<https://github.com/carlo-colombo/KeepScore> (see [Fork](#fork) below).

License
---------
[WTFPL][1] (Do What The Fuck You Want To Public License, Version 2), although
attribution would be nice. Copyright (C) Nolan Lawson and contributors; see
[NOTICE](NOTICE) for the attribution that ships with the app.

Overview
----------

![Screenshot][2]

KeepScore is a free and open-source score-keeping app for Android. 
Pull requests are always welcome.  I'll try to incorporate your changes into the app 
on the Google Play Store as soon as I can.

Download
----------

KeepScore may be downloaded from [the Google Play Store][4].  

You can also find a direct APK download link from [the KeepScore page on my web site][3].

More details about KeepScore can also be found in [my blog posts][10].

Fork
------

This repository is a fork of Nolan Lawson's KeepScore, maintained at
<https://github.com/carlo-colombo/KeepScore>. The original author, the original
code and the WTFPL license are unchanged; the fork modernizes the app for
current Android:

- `minSdk 34`, `compileSdk`/`targetSdk` 36 (Android 16)
- AndroidX views plus Compose New Game, Settings, Organize Players and the
  responsive scoring screen (portrait column; landscape two-column grid for up
  to eight players, larger games remain a usable scrolling column)
- WorkManager for the daily automatic backup, app-specific storage plus a
  FileProvider for sharing backups and spreadsheets
- a CI/release pipeline that publishes a tagged APK for every push to `main`

Downloadable APKs and their checksums are attached to the
[releases](https://github.com/carlo-colombo/KeepScore/releases). The Google Play
link above points at the original app.

The Android module lives in `app/` (upstream kept it in `KeepScore/`), so a path
such as `KeepScore/res/values-eo/strings.xml` from the translation section below
maps to `app/src/main/res/values-eo/strings.xml` here.

Build it yourself
------------------

KeepScore builds with the committed Gradle wrapper (Gradle 9.8.1, AGP 9.4.1,
Kotlin 2.4.21); dependency versions come from the version catalog in
`gradle/libs.versions.toml`. The app is a single `:app` module that compiles
against Android 16 (API 36) with `minSdk 34`, using AndroidX AppCompat Views and
Jetpack Compose.

Requirements: JDK 21 and an Android SDK with platform 36 (and matching
build-tools) installed. Create `local.properties` (gitignored) pointing at your
SDK:

```
sdk.dir=/path/to/Android/Sdk
```

Then, from the repository root:

```
./gradlew :app:assembleDebug            # build a debug APK
./gradlew :app:testDebugUnitTest        # run the unit tests (JUnit4 + Robolectric)
./gradlew :app:lintDebug                # run lint
./gradlew :app:assembleRelease          # build the minified, shrunk release APK
```

The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk` and the
release APK to `app/build/outputs/apk/release/app-release-unsigned.apk`.

### CI and releases

`.github/workflows/pr.yml` builds, tests and lints every pull request
(`:app:assembleDebug :app:testDebugUnitTest :app:lintDebug`) and uploads the
debug APK along with the test and lint reports.

`.github/workflows/build.yml` runs on every push to `main` and publishes a
tagged release: it assembles the release APK, validates it with
`:app:testDebugUnitTest :app:lintRelease`, then pushes the version tag, signs
the APK and creates a GitHub release carrying the APK, its SHA-256 checksum,
`LICENSE` and `NOTICE`. Signing uses the `SIGNING_KEY`, `ALIAS` and
`KEY_STORE_PASSWORD` secrets; when they are absent the release still publishes,
signed with a throwaway key.

Add a translation
-------------------

KeepScore is currently localized into English, French, Japanese, Hebrew, Italian, and German.  For the benefit of
board gamers around the world, though, I'm
always happy to get new translations!

### French and Japanese

I manage these myself.  I'm not a native speaker, though, so if you have corrections to make, please fork these files:

- [French](https://github.com/nolanlawson/KeepScore/blob/master/KeepScore/res/values-fr/strings.xml)
- [Japanese](https://github.com/nolanlawson/KeepScore/blob/master/KeepScore/res/values-ja/strings.xml)

### Italian, Hebrew, and German

These translations are only needed when new feature are added.  Thanks to those who helped with the 1.3.0 translations!

### Adding a new language

New languages are always welcome!  Based on the language statistics, it would be nice to have 
Dutch, Korean, Russian, Portoguese, and Spanish.

All you need to do is add some additional files, fork the project on GitHub, and send me a pull request. 
And thanks to GitHub [file creation][7], you can do all of this through the web interface!

For instance,
if your language is Esperanto (ISO code 'eo'), you would need to create the following files:

```
KeepScore/res/values-eo/strings.xml           # Main translations
KeepScore/res/values-eo/dimensions.xml        # Modifies width of the time period overlay (e.g. "Yesterday", 
                                              # "The Past Week") for languages with long words, e.g. French
KeepScore/res/raw-eo/version_and_credits.xml  # Page shown in "About KeepScore"
documentation/description-eo.txt              # Play Store description
documentation/donate/description-eo.txt       # Play Store description (Donate version)
```

(Again, please read the GitHub page on [file creation][7] to understand how to do this.)

I maintain the English, French, and Japanese translations myself.
So you can look to these translations as an example:

<table border='0'>
<tr>
<td align='center'><b>English</b></td>
<td align='center'><b>French</b></td>
<td align='center'><b>Japanese</b></td>
</tr>
<tr>
<td align='center'><a href='https://github.com/nolanlawson/KeepScore/blob/master/KeepScore/res/values/strings.xml'>strings.xml</a></td>
<td align='center'><a href='https://github.com/nolanlawson/KeepScore/blob/master/KeepScore/res/values-fr/strings.xml'>strings.xml</a></td>
<td align='center'><a href='https://github.com/nolanlawson/KeepScore/blob/master/KeepScore/res/values-ja/strings.xml'>strings.xml</a></td>
</tr>
<tr>
<td align='center'><a href='https://github.com/nolanlawson/KeepScore/blob/master/KeepScore/res/values/dimensions.xml#L57'>dimensions.xml</a></td>
<td align='center'><a href='https://github.com/nolanlawson/KeepScore/blob/master/KeepScore/res/values-fr/dimensions.xml'>dimensions.xml</a></td>
<td align='center'><a href='https://github.com/nolanlawson/KeepScore/blob/master/KeepScore/res/values-ja/dimensions.xml'>dimensions.xml</a></td>
</tr>
<tr>
<td align='center'><a href='https://github.com/nolanlawson/KeepScore/blob/master/KeepScore/res/raw/version_and_credits.htm'>version_and_credits.htm</a></td>
<td align='center'><a href='https://github.com/nolanlawson/KeepScore/blob/master/KeepScore/res/raw-fr/version_and_credits.htm'>version_and_credits.htm</a></td>
<td align='center'><a href='https://github.com/nolanlawson/KeepScore/blob/master/KeepScore/res/raw-ja/version_and_credits.htm'>version_and_credits.htm</a></td>
</tr>
<tr>
<td align='center'><a href='https://github.com/nolanlawson/KeepScore/blob/master/documentation/description-en.txt'>description-en.txt</a></td>
<td align='center'><a href='https://github.com/nolanlawson/KeepScore/blob/master/documentation/description-fr.txt'>description-fr.txt</a></td>
<td align='center'><a href='https://github.com/nolanlawson/KeepScore/blob/master/documentation/description-ja.txt'>description-ja.txt</a></td>
</tr>
<tr>
<td align='center'><a href='https://github.com/nolanlawson/KeepScore/blob/master/documentation/donate/description-en.txt'>donate/description-en.txt</a></td>
<td align='center'><a href='https://github.com/nolanlawson/KeepScore/blob/master/documentation/donate/description-fr.txt'>donate/description-fr.txt</a></td>
<td align='center'><a href='https://github.com/nolanlawson/KeepScore/blob/master/documentation/donate/description-ja.txt'>donate/description-ja.txt</a></td>
</tr>
</table>

Note that not all strings in ```strings.xml``` need to be translated.  If you look at [the English strings.xml file][6],
you'll see that I've helpfully marked constant strings with the prefix ```CONSTANT_```.  Constants do not need
to be translated.

A helpful tool for finding missing translations is the [Android Localization Helper][9].  

To ignore constants while using it, you can
run:

```/path/to/localization/helper/bin/alh.sh /path/to/keepscore/ | grep -v CONSTANT```

Please also tell me what name and email address you would like me to use in the "About" section, so I 
can give you proper credit.  Or just fork and modify the file ```KeepScore/res/raw/translations.htm```
yourself.  Thanks!

[1]: http://sam.zoy.org/wtfpl/
[2]: http://nolanwlawson.files.wordpress.com/2013/01/device-2013-01-06-141649.png?w=450
[3]: http://nolanlawson.com/apps/#keepscore
[4]: https://play.google.com/store/apps/details?id=com.nolanlawson.keepscore
[5]: http://actionbarsherlock.com/
[6]: https://github.com/nolanlawson/KeepScore/blob/master/KeepScore/res/values/strings.xml
[7]: https://github.com/blog/1327-creating-files-on-github
[8]: http://maven.apache.org/
[9]: https://github.com/4e6/android-localization-helper
[10]: http://nolanlawson.com/tag/keepscore/
[11]: https://code.google.com/p/android-color-picker/
