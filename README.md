# Emotecraft Compat

A compatibility mod for NeoForge 1.21.1, inspired by
[catman0221/emotecraft-fix-animate-texture](https://github.com/catman0221/emotecraft-fix-animate-texture).
It keeps Emotecraft animations compatible with Entity Model Features, Player
Animator, BendyLib, and 3D Skin Layers while leaving unrelated render paths
untouched. When installed on a server, it also repairs Emotecraft 2.4.12's
server-emote definition synchronization.

## Requirements

- Minecraft 1.21.1
- NeoForge 21.1.1 or newer in the 21.1 line
- Emotecraft

Client and server installations are independent. Installing the mod only on a
client enables the rendering integrations; installing it on a server with
Emotecraft enables the synchronization repair. EMF, Player Animator, BendyLib,
and 3D Skin Layers are optional, and each integration activates only when its
required stack is present.

The server synchronization repair is deliberately limited to Emotecraft
2.4.12, whose published NeoForge build contains the affected synthetic sender.
Later Emotecraft versions are left untouched so an upstream fix is not
overridden.

## Tested Compatibility

The following versions have been tested successfully in-game on Minecraft
1.21.1 with NeoForge 21.1.249 and Emotecraft 2.4.12. The mod's declared
minimum is NeoForge 21.1.1, which is also covered by automated build and unit
tests:

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

## Development Notes

This project was developed with the assistance of AI tools, including code
generation, Gradle project setup, and test case creation.

The implementation was guided, reviewed, adjusted, and manually tested by the
author to address real compatibility issues.

AI-assisted code may still have limitations, so bug reports and feedback are
welcome.

## Attribution

- Catman0221: original Forge 1.20.1 mod
- atleugim: upstream Fabric port
- Tassis: NeoForge 1.21.1 implementation and expanded compatibility work

This is an independent implementation inspired by the projects above; no
upstream source code is included.

## License

Emotecraft Compat is available under the [MIT License](LICENSE).
