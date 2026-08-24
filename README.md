# RavenCoin

The economy mod for **White Ravens Forge** — a NeoForge 1.21.1 tech server.

Written because no combination of published mods delivers the shape this server
needs: one currency that is also a physical item, an ATM, a server shop with
infinite stock, ranks bought with in-game money, and a leaderboard by balance.
The closest candidate had no idea LuckPerms existed and no server shop at all.

> **Alpha.** Nothing here is released, and the pack does not ship it yet.

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
goods, price, and optionally a rank you must hold to buy. Prices are any item
stack, so item-for-item trade keeps working and RavenCoin is a convention rather
than a rule. A server shop variant has infinite stock, needs no container, and
its takings leave circulation: the economy's tap and its drain in one block.

**Ranks.** Bought in the ATM through the LuckPerms API rather than by running
`lp` as a console command, so a failure surfaces instead of vanishing.

**Everything is optional.** Every feature switches off, and shop layout is edited
in-game while ranks, prices and toggles live in a JSON file that ships with the
pack and is version-controlled with it.

## Building

Java 21. The wrapper brings its own Gradle.

```
./gradlew build
```

The jar lands in `build/libs/`.

## Layout

```
src/main/java/net/whiteravens/ravencoin/   the mod
src/main/resources/META-INF/               neoforge.mods.toml
src/main/resources/assets/ravencoin/lang/  pl_pl and en_us, both complete
```

Both language files are kept complete from the first commit. The mod this
replaces shipped a Polish translation with five empty strings, so `/money`
printed nothing at all to a Polish player. That is the bar to clear.

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
