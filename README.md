# GTORenders

Shared rendering library for the [GregTech Odyssey](https://github.com/GregTech-Odyssey) mods (Minecraft 1.20.1, Forge, Java 21).

GregTech Odyssey 系列模组的公共渲染库。

## Contents

All effects are client-only and drawn in one pass after block entities (`RenderProFrame`). Motes are computed on the GPU: their parameters are written once
when they are born, and a transform-feedback shader turns every live mote into a camera-facing quad each frame, drawn through the vanilla eyes or particle
shader so shader packs keep working.

| Renderer | API | Draws |
|---|---|---|
| Ring | `RenderProFrame.ring(...)` with `ParticleRing`, `RingShape`, `RingStyle` | particle rings and multi-strand helices (voxels, glow sprites, ribbons) |
| Mote particle | `MoteParticles.spawn / slide` | drop-in replacement for simple mod particles: drag, gravity, lifetime, size curve, fade, spin and sprite animation; a mod's particle provider calls it with its own sprite and returns no particle |
| Link | `LinkEffects.flow / pulse / beam` with `FlowStyle`, `PulseStyle`, `BeamStyle` | motes between two points: a continuous stream, a single pulse with a fading trail, or a short-lived beam |
| Burst | `BurstEffects.burst` with `BurstStyle` | motes spreading out from, or hovering around, a point |
| Emitter | `ParticleEmitters.burst / emitter` | vanilla particles, one-shot or for a number of ticks |
| Body effect | `BodyEffects.register / draw` with `PixelSheet` | pixel-glow sheets worn on player body parts (wings, auras), with an optional per-draw tint for dimming: placed with the model part's real pose while the player renders and drawn after particles (every entity batch flushed) from static geometry |
| Sprite box | `SpriteQuads.box / face` | axis-aligned sprite faces in pixel units, tiled on the 16-pixel grid (liquid surfaces, panels) |

`MoteLook` picks how a mote looks: the GTORenders pixel glow with a bright core (`MoteLook.GLOW`), or a mod's own particle sprite from the particle atlas
(`MoteLook.sprite / animated / spinning` with `MoteSprite.of(spriteSet)`), so mods keep their original look.

Shader packs (Iris/Oculus): custom draws go through `QuadOutput`, which binds attributes by name (including the `iris_` names). With a pack active, GTO pixel-glow motes are drawn through the vanilla particle shader and body effects are written into the entity buffers, so packs such as Complementary and Photon keep them visible and glowing.

Shared pieces: `ShaderPacks.inUse()` (whether an Iris/Oculus shader pack is active, found by reflection; use it to swap custom core shaders, which shader packs do not support, for vanilla render types), `EffectClock` (fade-in / fade-out per owner), `EffectPalette` (shades of one base color), `EffectSource` (register with
`RenderProFrame.addSource` to submit continuous effects every frame). Motes are culled by distance, paused with the game and cleared when the level
changes or resources reload.

## Usage

```groovy
repositories {
    maven { url = 'https://gtodyssey.com/maven/releases' }
}

dependencies {
    modImplementation("com.gto:gtorenders-forge-1.20.1:<version>")
}
```

Mod ID: `gtorenders`. Declare it as a required dependency in your `mods.toml`.

## Building

```bash
./gradlew publishToMavenLocal
```

## License

[PolyForm Noncommercial License 1.0.0](LICENSE): noncommercial use only.
