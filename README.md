# Emotecraft Compat

A compatibility mod for Forge 1.20.1, inspired by
[catman0221/emotecraft-fix-animate-texture](https://github.com/catman0221/emotecraft-fix-animate-texture).
It keeps Emotecraft animations compatible with Entity Model Features, Player
Animator, and BendyLib while leaving unrelated render paths untouched.

## Requirements

- Minecraft 1.20.1
- Forge 47.2.0 or newer in the 47.x line
- Emotecraft 2.2.7

The compatibility behavior is client-side. EMF, Player Animator, and BendyLib
are optional, and each integration activates only when its required stack is
present.

Emotecraft 2.2.7's Forge server already sends each server emote through its
native custom-payload path with a 1 MiB serialization limit. This mod deliberately
does not replace that path; an automated bytecode contract guards that decision.

## Targeted Compatibility

- Entity Model Features 3.3.12
- Player Animator 1.0.2-rc1
- BendyLib 4.0.0

The Player Animator integration also repairs BendyLib mutators on Minecraft's
vanilla jacket, sleeve, and pants overlay parts so the second skin layer bends
with the underlying body parts. It does not require 3D Skin Layers.

## Build with Docker

No local Java or Gradle installation is required.

```sh
docker compose run --rm build
```

The built mod is written to `build/libs/`.

To launch the Forge development client from the same container:

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
- Tassis: Forge 1.20.1 compatibility work

This is an independent implementation inspired by the projects above; no
upstream source code is included.

## License

Emotecraft Compat is available under the [MIT License](LICENSE).
