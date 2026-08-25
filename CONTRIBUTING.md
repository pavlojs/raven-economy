# Contributing to RavenCoin

Thank you for wanting to help. RavenCoin is the economy mod behind the
**White Ravens Forge** modpack: one currency that is also an item, an ATM, shops
that trade out of the container behind them, and ranks bought with in-game
money.

> **Beta.** The mod runs and has been exercised on a live server, but the pack
> that uses it is not released and the data format is not frozen yet. A change
> that moves saved balances or shop configuration is allowed at this stage —
> say so in the PR, and say what happens to a world that already has them.

## Table of Contents

- [Before You Start](#before-you-start)
- [Scope of Contributions](#scope-of-contributions)
- [Development Setup](#development-setup)
- [Project Structure](#project-structure)
- [Coding Guidelines](#coding-guidelines)
- [Versions and Minecraft lines](#versions-and-minecraft-lines)
- [Verifying Your Change](#verifying-your-change)
- [Handling Money Safely](#handling-money-safely)
- [Submitting Changes](#submitting-changes)
- [Reporting Security Vulnerabilities](#reporting-security-vulnerabilities)

---

## Before You Start

- **Open an issue first** for anything larger than a bug fix. This mod exists to
  serve one specific server's design, and a feature that is excellent in
  general may still not belong here.
- **Search existing issues** — the thing you noticed may already be known and
  deliberate.
- Be aware that the maintainer works on this solo. Reviews take as long as they
  take.

## Scope of Contributions

**Welcome:**

- Bug fixes, especially anything where money or items can be lost, duplicated,
  or created out of nothing
- Translations — a new `<locale>.json` alongside `en_us.json` and `pl_pl.json`
- Compatibility fixes for other mods in the pack's ecosystem
- Performance work in the block-entity tick or the label renderer
- Documentation

**Out of scope:**

- Ports to Fabric, Quilt or Forge. This mod targets NeoForge, and a multiloader
  abstraction would cost more than it returns for a single-server mod.
- A different currency model. Physical coins that also have a bank account is
  the design, not an implementation detail.
- Anything that requires a web service, a database server, or an account
  system. The mod must work on a server with no outbound network access.

## Development Setup

### Requirements

- **JDK 21.** Minecraft 1.21.1 runs on Java 21, and the Gradle toolchain is
  pinned to it so a local build and CI produce the same bytecode. If your
  default `java` is older, point `JAVA_HOME` at a 21 JDK — Gradle itself
  refuses to start on Java 8.
- Nothing else. The wrapper brings its own Gradle.

```bash
git clone https://github.com/pavlojs/raven-economy.git
cd raven-economy
./gradlew build          # compiles, checks translations, produces the jar
./gradlew runClient      # a dev client with the mod loaded
./gradlew runServer      # a dev server, --nogui
```

The jar lands in `build/libs/` as `ravencoin-<mc>-<version>.jar`.

## Project Structure

```
src/main/java/net/whiteravens/ravencoin/
├── block/            ATM and the two shop blocks
│   └── entity/       ShopBlockEntity — one entity behind both shop blocks
├── client/           screens and the block-entity renderer for shop labels
├── command/          /balance, /pay, /eco, /rank …
├── config/           the TOML config; every feature has an off switch
├── economy/          accounts, physical coins, the transaction result type
├── menu/             menus, and the rule that an open menu is the authorisation
├── network/          payloads — all of them client → server requests
├── rank/             the ladder, purchases, and the LuckPerms bridge
├── recipe/           the condition that switches minting off
├── registry/         Deferred* registries
└── shop/             stock arithmetic and the strings shops render
```

Two boundaries are worth knowing before you change anything:

- **`shop/ShopStock.java` is the only place that knows a coin block is nine
  coins.** If you find that constant anywhere else, that is a bug.
- **`network/` never trusts a payload's contents beyond its own bounds.** The
  authorisation is that the sender has the relevant menu open; the block decides
  which menu they got. No `ItemStack` travels client → server — the server reads
  its own copy of the cursor.

## Coding Guidelines

### General

- Match the surrounding code. It has a style; follow it rather than importing
  another one.
- No dead code, no speculative abstraction, no compatibility shims. If something
  is unused, delete it.
- Comments explain **why**, never **what**. A comment restating the line above
  it is noise; a comment recording the bug that forced a guard is the most
  valuable thing in the file.

### Dependencies

The mod has exactly one, `net.luckperms:api`, and it is `compileOnly` — the
LuckPerms mod is a runtime dependency of the pack, never something we ship. A
new dependency needs a justification in the PR, and "it would be convenient"
is not one.

### Commits

[Conventional Commits](https://www.conventionalcommits.org/), **one topic per
commit**, subject line only:

```
feat: add shops, blocks that trade from the container behind them
fix: restore what a short take actually moved, not what it asked for
chore: bump neoforge to 21.1.248
```

Do **not** add `Co-Authored-By:` trailers. Only the physical author is
referenced on a commit, whether or not the work was AI-assisted.

There is **no `CHANGELOG.md`.** Release notes are generated by GitHub from what
landed since the previous tag, grouped by [`.github/release.yml`](.github/release.yml).

### Translations

**Every language file must carry every key, and no key may be empty.**

This is enforced by `./gradlew checkTranslations`, which runs as part of
`check`, which runs as part of `build`. It is not a style rule. The mod this one
replaces shipped a Polish translation with five empty strings, and one of them
was the reply to `/money` — so a Polish player asking their balance got a blank
line and no way to tell whether the command had worked. A missing key at least
falls back to English; an empty one is invisible breakage.

A new user-facing string goes into **both** `en_us.json` and `pl_pl.json` in the
same commit that introduces it.

## Versions and Minecraft lines

A mod version alone does not identify a build, because the same mod version can
exist for two Minecraft lines at once. So the Minecraft version comes first,
everywhere:

| | Form | Example |
|---|---|---|
| Jar | `ravencoin-<mc>-<version>.jar` | `ravencoin-1.21.1-0.1.0-beta.1.jar` |
| Tag | `v<mc>-<version>` | `v1.21.1-0.1.0-beta.1` |
| Release title | `RavenCoin <version> — Minecraft <mc>` | `RavenCoin 0.1.0-beta.1 — Minecraft 1.21.1` |

`<version>` is semver, and stays on a `-beta.N` pre-release until the pack it
serves ships. Sizing a bump:

- **patch** — a fix that changes no behaviour anyone configured
- **minor** — a new feature, a new config key, a new block or command
- **major** — saved data or configuration from the previous version stops
  loading unchanged

**Branches.** `dev` tracks the Minecraft line under active development, and is
where everything lands. When a second line opens, the line being left behind
gets a maintenance branch named after it — `mc/1.21.1` — and `dev` moves on.
A fix that applies to both is authored on `dev` and cherry-picked, never the
other way round.

## Verifying Your Change

"It compiles" is not a verification result.

```bash
./gradlew build              # includes checkTranslations
```

Then **run it**. A mod that compiles can still deregister a block, desync a
menu, or hand a player an item that vanishes on relog.

- Anything touching **blocks, menus or screens** — place the block, open the
  screen, use it, break the block, and relog to confirm what you saw survived.
- Anything touching **money or stock** — do the arithmetic by hand before and
  after. Count what the player holds, what the container holds, and what the
  account says. All three must add up to what they added up to before, minus
  exactly what was spent.
- Anything touching the **label renderer** — check it in an empty, flat, well-lit
  area. Foliage and mobs in front of a hologram look exactly like a clipping
  bug.

Say in the PR what you actually ran and what you observed.

## Handling Money Safely

This is the part of the mod where bugs cost players something they cannot get
back. Four rules, each of which exists because breaking it once caused real
damage:

1. **Check everything before moving anything.** Room, stock, balance and
   permission are all verified before the first item leaves a slot.
2. **Verify what a transfer actually moved**, not what it was asked to move.
   `take` returning less than requested is a normal outcome, not an exception.
3. **On failure, restore exactly what moved.** Restoring the requested amount
   instead of the moved amount is how a shop invents items.
4. **Spill, never delete.** If something cannot be put back, drop it in the
   world. An item on the ground is a bug report; an item that silently ceased to
   exist is a mystery.

## Submitting Changes

`dev` is the working branch. Fork, branch from `dev`, and open your PR against
`dev` — never against `main`, which is a release snapshot the maintainer syncs.

Fill in the PR template, including the verification section. A PR that says only
"it works" will be sent back with a question you could have answered yourself.

## Reporting Security Vulnerabilities

Do **not** open a public issue. Use
[private vulnerability reporting](https://github.com/pavlojs/raven-economy/security/advisories/new).
See [SECURITY.md](SECURITY.md) for what counts and what does not.
