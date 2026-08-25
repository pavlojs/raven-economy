## Description

<!-- What changed and why. The PR *title* becomes a release-note line
     (see .github/release.yml) — write it accordingly. -->

## Type of Change

- [ ] Bug fix (non-breaking)
- [ ] New feature (non-breaking)
- [ ] Breaking change (saved balances, shop configuration, the rank ladder or the config file stop loading unchanged)
- [ ] Documentation
- [ ] Performance
- [ ] Refactor
- [ ] Dependencies

## Related Issue

Closes #(issue number)

## How Has This Been Verified?

<!-- "It compiles" is not a verification result. Say what you actually ran,
     and what you saw. -->

- [ ] `./gradlew build` — clean, `checkTranslations` included
- [ ] **Exercised in the game** (`runClient` / `runServer`, or a real server) — describe below

Details:

<!-- e.g. "Placed a shop against a barrel, set it to 4 diamonds for 30 RC,
     bought 3 lots from a second account with 270 RC in coin blocks; buyer left
     with 30 RC and 12 diamonds, till held 240 RC as 26 blocks + 6 coins." -->

### If money or items can move

- [ ] Counted the player's inventory, the container and the account **before and after** — the totals add up to exactly what was spent
- [ ] Tried the failure paths: not enough money, not enough stock, no free inventory slot, no room in the container
- [ ] Confirmed a failed transaction moved **nothing**, and a partial one restored exactly what it took
- [ ] Nothing is deleted when it cannot be placed — it is dropped in the world

### If a screen, block or block entity changed

- [ ] Placed it, opened it, used it, broke it
- [ ] Relogged and confirmed what was configured survived
- [ ] Checked the label renderer somewhere empty and well lit, if labels were touched

### If it is client-facing

- [ ] All new user-facing strings are in **both** `en_us.json` and `pl_pl.json`
- [ ] Screenshots attached below

## Security Checklist

<!-- Required for anything touching network payloads, menus, permissions, or
     the exchange itself. -->

- [ ] Every new payload handler verifies the sender has the relevant menu open and that it is still valid
- [ ] No `ItemStack` is read from a client payload — the server reads its own copy of the cursor
- [ ] Numbers arriving from a client are clamped server-side, whatever the screen allowed
- [ ] Permission checks are on the server, and a failed LuckPerms call refuses the purchase rather than swallowing it
- [ ] Nothing new can be reached by a player who does not own the block or hold the permission

## New Dependencies

<!-- List each one with a justification, or write "none". This mod has exactly
     one dependency and it is compileOnly. -->

## Screenshots

## Additional Information
