# RavenCoin

**An economy mod for Minecraft on NeoForge.** A currency that is also an item
you can hold, an ATM that banks it, player and server shops that trade out of
the container behind them, and permission ranks bought with in-game money.

> **🧪 BETA — RavenCoin works and has been exercised on a live server, but it is
> in active development. The data format is not frozen yet, and details may
> change between releases. Bug reports, feedback and contributions are very
> welcome!**

Originally written for **White Ravens Forge**, a NeoForge tech server. It
depends on nothing from that pack and runs on any server.

NOT AN OFFICIAL MINECRAFT PRODUCT. NOT APPROVED BY OR ASSOCIATED WITH MOJANG OR MICROSOFT.

---

## Supported versions

| | |
|---|---|
| **Minecraft** | 1.21.1 — declared range `[1.21.1, 1.22)`, tested on 1.21.1 |
| **Mod loader** | NeoForge 21.1.248 or newer |
| **Java** | 21 |
| **Sides** | **Both.** The server owns the economy; the client needs the mod for the shop and ATM screens and the floating shop labels |
| **Optional** | [LuckPerms](https://luckperms.net/) 5+ — without it the economy still runs and only rank buying goes away |

Fabric, Quilt and Forge are not supported and are not planned.

---

## What it does

**Currency.** `RavenCoin` is an ordinary item you carry, and nine of them make a
`Block of RavenCoin`, the way an ingot makes a block. It is craftable, and that
recipe can be switched off — which is the point. The recipe is the server's
money supply valve: on, and players mint their own at the cost of the recipe;
off, and every coin in the world came from the server shop, a quest or a rank.

**Account.** An ATM block moves coins between your hand and an account that
survives death and cannot be stolen. The account is what the leaderboard ranks
and what ranks are bought with; shops take the coins in your inventory.

**Shops.** A block placed beside a container, configured in-game by its owner —
goods, price, and optionally a rank you must hold to buy. It finds its container
by looking around itself every second, so a chest added later is picked up, and
any mod's inventory works as long as it exposes an item handler. A coin block
counts as nine coins on both sides of the counter, so a customer paying from
savings and a till filling up both work. Prices are any item stack, so
item-for-item trade keeps working and RavenCoin is a convention rather than a
rule.

**Server shops.** The same block with infinite stock, no container, and takings
that leave circulation — the economy's tap and its drain. It cannot be broken in
survival and has no loot table.

**Labels.** Each shop draws what it sells, its price and its stock above itself,
readable through walls at range, and switchable off per shop.

**Ranks.** Bought in the ATM through the LuckPerms API rather than by running
`lp` as a console command, so a failure surfaces instead of vanishing.

**Everything is optional.** Every feature switches off. Shop layout is edited
in-game, while ranks, prices and toggles live in files that ship with a pack and
are version-controlled with it.

---

## Blocks and items

| | ID | Notes |
|---|---|---|
| RavenCoin | `ravencoin:coin` | The currency, as an item |
| Block of RavenCoin | `ravencoin:coin_block` | Nine coins, reversible |
| RavenCoin ATM | `ravencoin:atm` | Deposit, withdraw, buy ranks |
| Shop | `ravencoin:shop` | Player-owned, stocked from an adjacent container |
| Server Shop | `ravencoin:server_shop` | Infinite stock, unbreakable, creative-only |

## Commands

Everything lives under `/rc` (or `/ravencoin`). Bare aliases are off by default
and enabled with `shortCommandAliases`.

| Command | Who | What |
|---|---|---|
| `/rc balance [player]` | anyone | Your balance, or anyone's — balances are public, the same as the leaderboard |
| `/rc pay <player> <amount>` | anyone | Transfer from your account |
| `/rc top` | anyone | Leaderboard by balance |
| `/rc rank` | anyone | List the ladder, prices and what you hold |
| `/rc rank buy <rank>` | anyone | Buy a rank with banked money |
| `/rc eco get\|set\|add\|take …` | op | Read and change balances |
| `/rc rank set\|requires\|remove\|reload` | op | Edit the ladder |

With `shortCommandAliases = true` you also get `/balance`, `/bal`, `/pay` and
`/baltop`.

## Configuration

`config/ravencoin-common.toml`, created on first run:

| Key | Default | What it does |
|---|---|---|
| `currency.mintingEnabled` | `true` | Whether the coin recipe exists at all |
| `currency.startingBalance` | `0` | Balance a new account opens with |
| `commands.payEnabled` | `true` | Whether players can transfer to each other |
| `commands.shortCommandAliases` | `false` | Register bare `/balance`, `/pay`, `/baltop` |
| `shops.shopsEnabled` | `true` | `false` leaves shops standing and configured, but closed |
| `ranks.ranksEnabled` | `true` | Whether ranks can be bought |
| `ranks.requireLadderOrder` | `true` | Whether each rank needs the one below it first |

The rank ladder itself is `ravencoin-ranks.json`, so it can ship with a pack and
be version-controlled. `/rc rank reload` re-reads it.

---

## Installing

Drop the jar into `mods/` on the server **and** on every client, alongside
NeoForge 21.1.248+. LuckPerms on the server is optional and only affects ranks.

Jars are named `ravencoin-<minecraft>-<version>.jar`, so
`ravencoin-1.21.1-0.1.0-beta.1.jar` is version `0.1.0-beta.1` for Minecraft
1.21.1. Releases are on the [releases page](https://github.com/pavlojs/raven-economy/releases).

## Building

Java 21. The wrapper brings its own Gradle.

```bash
./gradlew build          # compiles, checks translations, produces the jar
./gradlew runClient      # a dev client with the mod loaded
./gradlew runServer      # a dev server
```

The jar lands in `build/libs/`.

## Layout

```
src/main/java/net/whiteravens/ravencoin/   the mod
src/main/resources/META-INF/               neoforge.mods.toml
src/main/resources/assets/ravencoin/lang/  pl_pl and en_us, both complete
```

Both language files are kept complete from the first commit, and CI fails if
they disagree. The mod this replaces shipped a Polish translation with five
empty strings, so `/money` printed nothing at all to a Polish player. That is the
bar to clear.

---

## Contributing

| Document | What is in it |
|---|---|
| [CONTRIBUTING.md](CONTRIBUTING.md) | Setup, structure, coding and commit rules, and how to verify a change that touches money |
| [CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md) | Community expectations |
| [SECURITY.md](SECURITY.md) | The trust boundary, known gaps, and how to report a vulnerability privately |

`dev` is the working branch and tracks the Minecraft line under active
development; `main` is a release snapshot synced from it by the maintainer. Fork
and open your PR against `dev`, never `main`. When a second Minecraft line
opens, the one being left behind gets a `mc/<version>` maintenance branch.

Commits follow [Conventional Commits](https://www.conventionalcommits.org/), one
topic per commit, subject line only. There is **no `CHANGELOG.md`** — release
notes are generated by GitHub from everything that landed since the previous
tag, grouped by [`.github/release.yml`](.github/release.yml).

Releases are tagged `v<minecraft>-<version>` — `v1.21.1-0.1.0-beta.1` — because
the same mod version can exist for two Minecraft lines at once and a tag has to
say which one it is.

> **Found a security bug?** Do not open a public issue — use [private vulnerability reporting](https://github.com/pavlojs/raven-economy/security/advisories/new). And strip player names, UUIDs and IPs from any server log you attach.

---

## Licence

**Apache License 2.0.** See [LICENSE](LICENSE) and [NOTICE](NOTICE).

Do what you like with it — use it, change it, ship it in a closed-source pack,
sell it. Two things come with you: keep the copyright notice and the licence
text, and carry the `NOTICE` file, which names the author and where this came
from. If you change a file, say so in it.

That last part is why this is Apache-2.0 rather than MIT: MIT obliges you to
keep a copyright line and nothing more, so a fork can end up with no trace of
where it started. `NOTICE` is the mechanism that makes the origin travel with
the code.

---

## Built with AI — How We Fight AI Slop

This project was built with the help of **AI coding assistants**. We believe in transparency about AI involvement and in keeping quality high despite using AI tools.

### What "AI slop" is and how we fight it

"AI slop" is low-quality, bloated, copy-paste code that AI generates when used carelessly — dead code, unnecessary abstractions, hallucinated APIs, cargo-culted patterns, and verbose boilerplate nobody asked for. It's the software equivalent of SEO spam articles.

Here's how this project stays above that bar:

1. **Human-driven architecture** — Every design decision (physical coins over a pure ledger, one block entity behind both shop blocks, the open menu as the authorisation, a renderer instead of an entity for labels) was made by a human. AI executed the plan, not the other way around.
2. **Strict compilation gates** — Every change must pass `./gradlew build`, which includes `checkTranslations` — a missing or empty key in any language file fails the build. No "it looks right" — it compiles or it doesn't ship.
3. **No dead code policy** — Unused fields, unreachable branches, placeholder stubs and speculative abstractions are removed rather than left "in case". If something is unused, it is deleted.
4. **Minimal abstraction** — No premature DRY, no "just in case" wrappers. There is exactly one place that knows a coin block is nine coins, and that is the whole design.
5. **Exercised in the game, not just compiled** — Every feature here was placed, opened, clicked and relogged on a real server before it was committed, with the money and items counted by hand on both sides of every transaction. Two bugs in the shop code were found that way and fixed in the same session; a third was found by reading the diff afterwards.
6. **Comments record why, not what** — The comments worth having in this codebase are the ones naming the bug that forced a guard. Comments restating the line above them are noise and get deleted.

If you spot AI slop in this codebase — dead code, nonsensical comments, hallucinated APIs, over-engineered abstractions — please open an issue. Keeping code clean is a shared responsibility.
