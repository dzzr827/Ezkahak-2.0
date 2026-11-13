# EzkaHak 2.0 — Minecraft 1.21.11 Fabric

Cyber-style client mod source project.

## Included
- Dark-blue Cyber ClickGUI with module categories, search, settings and keybind editing.
- Module framework and persistent settings.
- HUD overlay and draggable HUD editor.
- Named config profiles with create, duplicate, rename, delete, reset, and properties-based persistence.
- Notifications and visual accent options.
- AutoTotem (uses the player's normal handler and does not open the inventory screen).
- AutoFarm experimental Home 1 → mining → Home 2 → chest deposit → Home 1 flow.
- Crystal Aura and Velocity modules.

## Build
Requires JDK 21 and Gradle compatible with Fabric Loom 1.14. From this directory run:

```bash
gradle build
```

The project currently does not include the Gradle Wrapper. A successful build and in-game launch have **not** been verified in this environment because Gradle is not installed here. Do not treat any prior development JAR as a verified 1.21.11 release.

## Controls
- Right Shift: open ClickGUI
- F6: open HUD editor

## Important notes
- AutoFarm is experimental and depends on server support for `/home 1`, `/home 2`, chest access, and ordinary block-breaking rules. It does not bypass server protections.
- Crystal Aura and other combat modules may be restricted or prohibited by multiplayer server rules. Use only where permitted.
- AutoTotem waits while a non-player container is open and does not open an inventory screen itself.


## Получить настоящий JAR

См. [BUILD_ON_GITHUB.md](BUILD_ON_GITHUB.md): GitHub Actions собирает мод на JDK 21 и Gradle 8.14.3 и публикует JAR как artifact.
