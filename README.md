# Emotecraft Compat

A client-side compatibility mod for NeoForge 1.21.1, inspired by
[catman0221/emotecraft-fix-animate-texture](https://github.com/catman0221/emotecraft-fix-animate-texture).
It keeps Emotecraft animations compatible with Entity Model Features, Player
Animator, BendyLib, and 3D Skin Layers while leaving unrelated render paths
untouched.

## Requirements

- Minecraft 1.21.1
- NeoForge 21.1.249 or newer in the 21.1 line
- Emotecraft

This mod is client-side. EMF, Player Animator, BendyLib, and 3D Skin Layers are
optional; each integration activates only when its required stack is present.

## Tested Compatibility

The following versions have been tested successfully in-game on Minecraft
1.21.1 with NeoForge 21.1.249 and Emotecraft 2.4.12:

- Entity Model Features 3.3.5
- 3D Skin Layers 1.11.2

These are confirmed working versions, not declarations that every other
version is incompatible. EMF and 3D Skin Layers remain optional dependencies.

## Build with Docker

No local Java or Gradle installation is required.

```sh
docker compose run --rm build
```

The built mod is written to `build/libs/`.

To launch the NeoForge development client from the same container:

```sh
docker compose run --rm build gradle --no-daemon runClient
```

## Attribution

- Catman0221: original Forge 1.20.1 mod
- atleugim: upstream Fabric port
- Tassis: NeoForge 1.21.1 implementation and expanded compatibility work

This is an independent implementation inspired by the projects above; no
upstream source code is included.

## License

Emotecraft Compat is available under the [MIT License](LICENSE).
