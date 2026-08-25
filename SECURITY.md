# Security Policy — RavenCoin

RavenCoin runs on a **multiplayer server**, and every one of its features is
reachable by an ordinary player: a packet, a command, a block they placed. That
makes its security model narrow but real — the attacker is a player with a
modified client, and the thing being attacked is other players' money and items.

## Reporting a Vulnerability

**Do not open a public issue.**

Use [GitHub's private vulnerability reporting](https://github.com/pavlojs/raven-economy/security/advisories/new)
(Security → Report a vulnerability). If that is unavailable to you, reach White
Ravens through [whiteravens.net](https://whiteravens.net) and say only that you
have a security report — no details in the first message.

Please include:

- What the flaw allows (duplicate items, mint money, read or move another
  player's balance, crash the server, bypass a rank check)
- The mod version and Minecraft/NeoForge versions
- Steps to reproduce, ideally on a fresh world with no other mods
- Whether a vanilla client can do it, or whether a modified client is required

You can expect an acknowledgement within a few days. This is a solo project;
there is no bounty programme, and there is no SLA — but a report that lets
someone drain a server's economy will be treated as the emergency it is.

### Redact before sharing

Server logs and crash reports carry player names, UUIDs and IP addresses. Those
belong to third parties, not to you or to us. Strip them before attaching a log
to a report.

## Supported Versions

Only the **latest release on the current Minecraft line** receives security
fixes. When a maintenance branch exists for an older Minecraft line
(`mc/<version>`), a fix will be backported to it if the flaw is exploitable
there and the backport is mechanical.

The mod is in **beta** and is not yet shipped in a released pack. Fixes land in
a new release, never as a patch to an existing tag.

## Security Model

### The trust boundary is the network payload

Every payload in `network/` travels **client → server**, and every one of them
is written on the assumption that the client is hostile.

- **An open menu is the authorisation.** A payload handler verifies that the
  sender has that specific menu open and that the menu is still valid — the
  distance check, the block-entity type, the block still existing. It never
  trusts a position, an owner or a permission that arrived in the packet.
- **No `ItemStack` travels client → server.** When a shop's product or price is
  set from the item on the cursor, the server reads *its own* copy of that
  cursor (`player.containerMenu.getCarried()`). A client cannot describe an item
  into existence.
- **Numbers arriving from a client are clamped, not validated-and-trusted.** A
  quantity field is bounded on the server side regardless of what the screen
  allowed the player to type.

### Economy integrity

The invariant is that money and items are conserved: every transaction removes
exactly what it adds, or it does not happen at all.

- Balances are `long`, and every arithmetic path that could overflow is checked
  before it is performed, not after.
- A transaction verifies room, stock, balance and permission **before** the first
  item moves.
- A partial transfer is a normal outcome. Handlers check what a transfer
  actually moved and restore exactly that much on failure — restoring the
  *requested* amount is how a shop invents items, and that bug has been written
  once already.
- Anything that cannot be placed back is dropped into the world rather than
  discarded.

### Permissions

Rank purchases go through the **LuckPerms API**, not by running `lp` as a
console command. A console command that fails does so silently into the log; an
API call that fails throws, and the purchase is refused rather than taking the
money and granting nothing.

Administrative commands are gated on the vanilla permission level. The
server-shop block cannot be broken in survival and has no loot table, so
possession of one is not obtainable by breaking it.

### Supply chain

- **One dependency**, `net.luckperms:api`, and it is `compileOnly` — nothing
  from it is shipped inside the jar.
- Dependabot runs weekly with a **7-day cooldown**: a version that has not been
  public for a week is never proposed, so a freshly compromised release cannot
  be merged before anyone has looked at it. Security advisories are exempt and
  open immediately, by design.
- CI runs CodeQL, a Trivy secret and misconfiguration scan, and dependency
  review on pull requests.
- Releases are built by the tagged workflow from a commit on `main`, and the jar
  attached to a release is the one that workflow produced.

### No network, no credentials, no telemetry

The mod opens **no outbound connections**, reads no API keys, and stores no
credentials. It writes exactly two things: its config file, and player balances
in the world's saved data. It reports nothing anywhere.

## Known Gaps

Stated plainly, because a security policy that lists only strengths is
marketing:

- **A player shop's stock lives in an ordinary container.** Anything that can
  reach into that container can reach the shop's goods — hoppers, pipes, other
  mods' automation, and any player who can open it. Protecting the container is
  the server's job (claims, CoreProtect), not this mod's.
- **The mod trusts the server operator completely.** An operator can set any
  balance. There is no audit trail of administrative changes beyond the server
  log.
- **Balances are stored unencrypted** in the world's saved data, and anyone with
  filesystem access to the world can edit them. This is true of every mod's data
  and is not a threat this mod can address.
- **The beta data format is not frozen.** A future version may migrate saved
  balances or shop configuration.

## Out of Scope

- Vulnerabilities in Minecraft, NeoForge, LuckPerms or any other mod — report
  those to their maintainers
- Griefing that a server's own protection mods are meant to prevent
- Anything requiring operator privileges or filesystem access to the server
- Denial of service by a client doing an ordinary thing very quickly, unless it
  is disproportionately cheap for the client compared to the server
