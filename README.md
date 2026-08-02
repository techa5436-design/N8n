# AGENT GAME 1 — Android 3D Battle-Royale-style Shooter (Kotlin + jMonkeyEngine)

A native Android game (Kotlin) built on the **jMonkeyEngine 3.5.2** real 3D engine — **not** a
web wrapper. Everything (menus, lobby, gameplay) runs as a real Android app in `app/src`.

All 3D geometry (characters, weapons, vehicles, zombies, houses, trees, grass, terrain, sky) is
**generated procedurally in code** — so the game runs with **zero downloaded model/texture files**.
Your exact 3D models (glTF/OBJ/FBX) can be swapped in later; see "Using your own 3D models" below.

---

## What's implemented (feature → where in code)

| Free Fire-style feature | Status | Where |
|---|---|---|
| Splash screen (cinematic bg + game logo) | ✅ | `SplashActivity.kt` |
| Starting lobby with a **3D** character (rotating on pedestal) | ✅ | `LobbyActivity.kt` + `LobbyApp.kt` |
| Multi-sections: CHARACTERS / WEAPONS / VEHICLES / DRESS | ✅ | `CharactersActivity.kt`, `WeaponsActivity.kt`, `VehiclesActivity.kt`, `DressActivity.kt` |
| PLAY → Mode select → START | ✅ | `ModeSelectActivity.kt` → `GameActivity.kt` |
| **Infinite Zombie** mode (never ends unless zombies kill you) | ✅ | `GameApp.kt` |
| Unlimited ammo for the player | ✅ | `GameApp.kt` (auto-fire, never depletes) |
| Kill counter shown at the top | ✅ | `Hud.kt` (killText, top-centre) |
| Very many zombies (horde, ramps up over time) | ✅ | `ZombieManager.kt` |
| Detailed ~1 km × 1 km map: terrain, city, houses, trees, grass, roads | ✅ | `WorldBuilder.kt` |
| Third-person character + on-screen touch controls (joystick + auto-aim fire) | ✅ | `Player.kt`, `TouchControl.kt` |

### Gameplay notes
- **Controls (landscape):** left half of the screen = movement joystick; right half = drag to aim
  (rotate camera) and it **auto-fires** at the crosshair. This matches "FIRE — AUTO SHOOT AT CROSSHAIR".
- **Mode rules:** Infinite Zombie never ends — the horde grows and speeds up the longer you survive.
  Your run only ends when zombies kill you. Unlimited ammo, so survival is the only challenge.
- **Loadout:** whatever you equip in CHARACTERS/WEAPONS/VEHICLES/DRESS changes the 3D character
  model, the gun model, fire-rate/damage/spread, and movement speed in the match.

---

## Project structure (Android project at the repo root)
```
├─ settings.gradle
├─ build.gradle
├─ gradle.properties
├─ gradle/wrapper/gradle-wrapper.properties
├─ .github/workflows/build-apk.yml   ← builds the APK on GitHub Actions
└─ app/
   ├─ build.gradle                 (jMonkeyEngine 3.5.2-stable deps)
   └─ src/main/
      ├─ AndroidManifest.xml
      ├─ java/com/agentgame/one/
      │  ├─ SplashActivity.kt          Free-Fire-style splash → Lobby
      │  ├─ LobbyActivity.kt           AndroidHarness + overlaid menu buttons
      │  ├─ LobbyApp.kt                3D lobby scene (character on pedestal)
      │  ├─ ModeSelectActivity.kt      mode list + START
      │  ├─ Characters/Weapons/Vehicles/DressActivity.kt
      │  ├─ SectionHelper.kt           shared item list UI
      │  ├─ GameActivity.kt            AndroidHarness for the match
      │  ├─ GameApp.kt                 Infinite Zombie mode (SimpleApplication)
      │  ├─ WorldBuilder.kt            ~1 km terrain + city + houses + trees + grass + roads
      │  ├─ CharacterBuilder.kt        procedural articulated humanoid
      │  ├─ WeaponBuilder.kt           procedural gun (per weapon type)
      │  ├─ Player.kt                  third-person movement + camera
      │  ├─ TouchControl.kt            joystick + aim + auto-fire
      │  ├─ Zombie.kt / ZombieManager.kt   horde AI + shooting + kill count
      │  ├─ Hud.kt                     kills / health / ammo / timer / crosshair / death
      │  ├─ Procedural.kt              materials + procedural textures + primitives
      │  └─ GameConfig.kt              loadout catalogue + persistence
      └─ res/ (layouts-free programmatic UI + generated drawables)
```

---

## How to build & run

### Option A — GitHub Actions (no local setup, recommended)
The repo includes a workflow that builds the APK automatically on GitHub's servers:
1. Push this project to a GitHub repository.
2. Open the repo's **Actions** tab → **Build APK** workflow runs on every push. You can also
   trigger it manually via **Run workflow**.
3. When it finishes, open the run, scroll to the **artifacts** section and download
   **`agent-game-1-debug-apk`** → inside is `app-debug.apk`.
4. Install that APK on an Android 6.0+ device.

> Note: the GitHub token must have **Contents read/write** so Actions can run on the repo.
> The workflow generates the Gradle wrapper at build time, so no wrapper JAR is committed.

### Option B — Android Studio
1. Install [Android Studio](https://developer.android.com/studio) (Arctic Fox or newer) with
   **JDK 11+** and the **Android SDK (API 34)**.
2. **File ▸ Open** the repository **root** (where `settings.gradle` lives). Gradle will download
   the Android Gradle Plugin, Kotlin, and jMonkeyEngine 3.5.2 from Google Maven / Maven Central
   (internet required on the build machine).
3. Connect a phone (Android 6.0+) with USB debugging on, or start an emulator.
4. Click **Run ▶**. The app installs and launches at the splash screen.

---

## Using your own 3D models (swap placeholders for your glTF/OBJ/FBX)
jME3 loads glTF/OBJ out of the box via `jme3-plugins`. To use your models:
1. Put files under `app/src/main/assets/Models/...`.
2. Load them, e.g.:
   ```kotlin
   val model = assetManager.loadModel("Models/MyCharacter.gltf")
   ```
3. Replace the geometry built in `CharacterBuilder.build(...)` / `WeaponBuilder.build(...)` with
   the loaded model (keep the same root/limb names so walk animation still works), or replace the
   block-building calls in `WorldBuilder.kt` with `loadModel(...)`.

---

## Controls & tuning knobs
- Zombie difficulty ramp: `ZombieManager.update(...)` (`activeCap`, `spawnInterval`).
- Player speed: `Player.kt` `baseSpeed` × `VEHICLE_SPEED_MULT`.
- Weapon balance: `GameConfig.kt` `WEAPON_STATS`.
- World layout / seed: `WorldBuilder` constructor `seed`.

## Roadmap (natural next steps)
- Add your exact character/zombie/weapon/vehicle models (links welcome).
- Rideable vehicles in-match, grenades, medkits, loot crates.
- More maps (night, snow) and modes (Battle Royale, Team Deathmatch) UI shells are already in place.
