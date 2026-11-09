# Confetti Creepers

Creepers explode into confetti instead of blowing stuff up, call you on the
"Minecraft World" Wi-Fi when they start hissing, and turn nearby players into
Confetti Guy (confetti falls off you as you walk, for 5 minutes).

This file is read automatically whenever Claude Code is opened in this folder.
Everything below is specific to this one mod. The general rules about how to
work with Elduin live in `~/.claude/CLAUDE.md`.

## Facts about this mod

    mod id            confetti_creepers              (underscores — never change this)
    slug              confetti-creepers            (repo name and Modrinth slug)
    package           com.elduin.confetti_creepers
    loader            fabric                (only fabric — see below)
    minecraft         1.21.11, 26.2
    primary version   1.21.11             (the one he plays)
    java              21 for 1.21.x, 25 for 26.x — Gradle picks this per version

The mod id is baked into save files. Once a world has been played with this mod,
**changing the mod id breaks that world.** Rename the display name freely;
never rename the mod id.

## Layout

Multi-version is handled by [Stonecutter](https://plugins.gradle.org/plugin/dev.kikugie.stonecutter):
one source tree, version-conditional comments, many outputs.

    src/main/java/<package>/                the mod
    src/main/resources/                     assets, textures, mixins, lang
    versions/<mcversion>-fabric/build/libs/ built jars land here
    stonecutter.properties.toml             mod id, name, version, dependencies
    settings.gradle.kts                     the Minecraft version list
    .github/workflows/release.yml           builds and publishes on a version tag

There is **no `fabric.mod.json` file** — it is generated at build time from
`stonecutter.properties.toml` by the code in `build-logic/`. Editing mod
metadata means editing the `.toml`, not a json file. Same for `mod.version`:
there is no `mod_version` in `gradle.properties`.

Stonecutter subprojects are named `<mcversion>-fabric`, so the 1.21.11 jar is in
`versions/1.21.11-fabric/build/libs/`. That `-fabric` suffix is easy to forget.

Do **not** add a branch or a repo for a new Minecraft version. Add it to the
list in `settings.gradle.kts`, add a matching `[fabric."<version>"]` block in
`stonecutter.properties.toml`, add it to the matrix in
`.github/workflows/release.yml`, and fix whatever stops compiling.

## Fabric only

This template builds Fabric and nothing else. NeoForge and Forge were removed on
purpose: each extra loader is another full copy of Minecraft to decompile, and
this is an 8 GB machine. Do not add them back.

For the same reason `gradle.properties` sets `org.gradle.parallel=false`.
Leave it off. Turning it on with more than one Minecraft version in the list
will exhaust memory and take the whole machine down.

## Commands

    ./gradlew "Set active project to 1.21.11-fabric"   switch versions first
    ./gradlew "1.21.11-fabric:build"                   build just that version
    ./gradlew build                                    build every version
    ./gradlew runActiveClient                          launch a dev client

Switching rewrites the shared source tree into that version's form. It is **not**
required before building — each version subproject regenerates its own sources,
so the jars are correct either way. Switch to keep the working tree in the
version you're reading, not because the build needs it.

Never hand-edit `.sc_active_version`. Stonecutter records what form the shared
sources are currently in, and editing that file behind its back desyncs the
bookkeeping — you get `cannot find symbol` errors on classes that plainly exist.
Use the task above and nothing else.

## Access wideners

Optional and absent by default. If you need one, create
`src/main/resources/aw/<mcversion>.accesswidener` and Loom picks it up
automatically; without the file the step is skipped entirely. An *empty*
placeholder file does not work — it fails the build on 1.21.11+.

## Conventions for this repo

- Textures are 16x16 unless there's a reason. Keep the pixel-art style consistent
  with the rest of the mod.
- Every new block, item and mob needs an entry in the language file
  (`assets/confetti_creepers/lang/en_us.json`) or it shows up in-game as a raw id, which
  reads to him as "broken".
- Anything a player can tune goes in the config, not hardcoded.
- Keep it dependency-free where possible. If a library is genuinely needed, it
  has to be one that's available for every Minecraft version in the list above.

## Releasing

Handled by the **share-it** skill. Short version: bump `mod.version` in
`stonecutter.properties.toml`, update `CHANGELOG.md` in plain words, push a
`v<version>` tag, and the workflow publishes to Modrinth using the org's
`MODRINTH_TOKEN`.
