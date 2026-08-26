# OrionMD Wallpapers

Standalone Android wallpaper-picker app. Full-screen live preview, a
"Set wallpaper" checkmark to confirm, a scrollable thumbnail strip
(12 bundled wallpapers + pick-from-gallery), optional bottom-center
custom text (Roboto, `#CCFFFFFF`, slight shadow, 12-40 size slider),
an adjustable 0-200 export-DPI slider, and a Home/Lock/Both target
choice — exported fitted to 600x1024.

**This project already builds clean** — verified locally with
`./gradlew assembleDebug` before packaging.

## Repo layout

```
app/src/main/java/com/mdmac/wallpaperpicker/   MainActivity, adapter, export util
app/src/main/res/layout/                        activity_main, dialogs, thumbnail item
app/src/main/res/drawable/                      icons + scrims
app/src/main/res/drawable-nodpi/                the 12 bundled wallpapers
app/src/main/res/values/                        strings, colors, themes
.github/workflows/build.yml                     CI: builds a debug APK on every push
gradlew / gradle/wrapper/                        Gradle wrapper (already included — no
                                                  network/version step needed in CI)
```

## Uploading from your Android device (no Termux, no git CLI)

This is a **brand-new, empty repo**, so you're pushing the whole
project for the first time — a bit more up-front work than adding a
few files, but still no command line needed.

1. **Extract the zip** with your Files app (tap it → Extract). You'll
   get an `OrionMD_wallpaperpicker` folder matching the layout above.

2. **Open the repo** in Chrome:
   `github.com/mdmac1983/OrionMD_wallpaperpicker`

3. **Try the fast path first.** On the empty repo's front page, tap
   **"uploading an existing file"**, then **"choose your files"**. If
   your file manager lets you select the *whole* extracted
   `OrionMD_wallpaperpicker` folder (rather than only individual
   files), do that — GitHub preserves the folder structure and this
   uploads everything in one commit. Skip to step 5 if this works.

4. **Guaranteed fallback (folder by folder), if step 3 only grabs
   loose files:** upload the files that belong in each folder,
   creating each folder as you go — GitHub creates a folder
   automatically the first time you upload a file into it. Do these
   in order:
   - Root: `settings.gradle.kts`, `build.gradle.kts`,
     `gradle.properties`, `gradlew`, `gradlew.bat`, `.gitignore`,
     `README.md`
   - `gradle/wrapper/`: `gradle-wrapper.jar`, `gradle-wrapper.properties`
   - `.github/workflows/`: `build.yml`
   - `app/`: `build.gradle.kts`
   - `app/src/main/`: `AndroidManifest.xml`
   - `app/src/main/java/com/mdmac/wallpaperpicker/`: the 4 `.kt` files
   - `app/src/main/res/layout/`: the 4 `.xml` layout files
   - `app/src/main/res/drawable/`: the 6 icon/scrim `.xml` files
   - `app/src/main/res/drawable-nodpi/`: all 12 wallpaper `.jpg` files
   - `app/src/main/res/values/`: `colors.xml`, `strings.xml`, `themes.xml`

   For each: navigate into that path in the GitHub file browser (type
   the folder name and tap "Create new folder" if it doesn't exist
   yet, which GitHub offers right in the upload screen), tap
   **Add file → Upload files**, select just the files listed for that
   folder, add a commit message, **Commit changes**.

5. Once everything's pushed, check the **Actions** tab — the included
   workflow builds a debug APK automatically and attaches it as a
   downloadable artifact on each run.

## Wiring notes

- `MainActivity` already calls `promptCustomTextThenApply()` from the
  checkmark button — nothing else to connect.
- Package/applicationId is `com.mdmac.wallpaperpicker`, separate from
  the Organizer/OrionMD Launcher app, so both can be installed side
  by side.
- `minSdk 26`, `targetSdk 29` (matches your Android 10 tablet),
  `compileSdk 34`.
