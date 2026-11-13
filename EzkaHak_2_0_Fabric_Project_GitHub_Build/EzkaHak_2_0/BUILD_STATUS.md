# EzkaHak 2.0 — build status

Last checked: 2026-11-13

## Target
- Minecraft: 1.21.11
- Loader: Fabric 0.18.4+
- Fabric API: 0.141.1+1.21.11
- Java: 21+
- Mod id: `ezkahak`
- Version: `2.0.0`

## What is in this source project
- Fabric client entrypoint and mod metadata
- Cyber ClickGUI and module/settings framework
- HUD overlay and HUD editor
- Config profiles
- Notifications and visual options
- AutoTotem, experimental AutoFarm, Crystal Aura and Velocity source modules

## Verification result
- Source archive was extracted and inspected.
- Java 21 is available in the build environment.
- `./build-mod.sh` was run on 2026-11-13 and stopped because the `gradle` executable is not installed.
- No Gradle wrapper or cached Gradle distribution is present in this environment.
- Network/DNS access is unavailable, so Gradle and Minecraft/Fabric dependencies cannot be downloaded here.
- Therefore **no release JAR was produced** and no in-game launch test was performed.
- Existing older development JARs are not verified as compatible with Minecraft 1.21.11 and are not included as a substitute.

## Build on a machine with JDK 21 and Gradle
Run from this directory:

```sh
./build-mod.sh
```

or:

```sh
gradle clean build
```

Expected output after a successful build: `build/libs/ezkahak-2.0.0.jar`.


Дополнительно: добавлен `.github/workflows/build.yml` для удалённой сборки на GitHub Actions (JDK 21 + Gradle 8.14.3). Workflow ещё не запускался; JAR не создан и не проверен. Инструкция: `BUILD_ON_GITHUB.md`.
