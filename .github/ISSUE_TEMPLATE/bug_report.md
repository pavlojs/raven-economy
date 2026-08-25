---
name: Bug Report
about: Report a reproducible bug in RavenCoin
title: '[BUG] '
labels: bug
assignees: ''
---

> **Security vulnerability?** Do not open a public issue.
> Use [private vulnerability reporting](../../security/advisories/new) instead.
> That includes anything that duplicates items, creates money, or moves somebody
> else's balance.

> **Did the game or server crash?** Use the [Crash](?template=crash_report.md)
> form instead.

> **Before pasting logs:** server logs carry player names, UUIDs and IP
> addresses belonging to other people. Redact them.

## Bug Description

A clear and concise description of what the bug is.

## Steps to Reproduce

1.
2.
3.

## Expected Behavior

## Actual Behavior

Include the exact message shown in chat or on the screen, if any.

## If money or items are involved

This is the part that matters most, and it is the part reports usually leave
out. Count all three, before and after:

| | Before | After |
| ---------------------------- | ------ | ----- |
| In the player's inventory    |        |       |
| In the shop's container      |        |       |
| In the account (`/rc balance`) |      |       |

- [ ] Items or money were **created** out of nothing
- [ ] Items or money **vanished**
- [ ] The totals are right but the transaction was refused, or allowed when it should not have been

## Environment

| Field                  | Value                                        |
| ---------------------- | -------------------------------------------- |
| RavenCoin version      | e.g. 0.1.0-beta.1 (from the jar name)        |
| Minecraft version      | e.g. 1.21.1                                  |
| NeoForge version       | e.g. 21.1.248                                |
| LuckPerms              | installed (version) / not installed          |
| Singleplayer or server | and whether the client is modified           |
| Other mods             | the whole list, or a link to the pack        |

## Does it happen with RavenCoin alone?

- [ ] Yes — reproduced with only RavenCoin and NeoForge installed
- [ ] No — it needs another mod present (name it above)
- [ ] Not tested

## Where does it fail?

- [ ] Coins, the coin block, or the minting recipe
- [ ] ATM — deposit, withdraw
- [ ] Ranks — buying, the ladder, LuckPerms
- [ ] Player shop — configuring, stock, buying
- [ ] Server shop
- [ ] Shop labels (the floating text)
- [ ] Commands
- [ ] Config file
- [ ] Saved data — something did not survive a restart

## Logs

<!-- The server log (logs/latest.log) and, for a client-side problem, the client
     one. Redact player names, UUIDs and IPs. -->

```
paste the relevant fragment here
```

## Screenshots

## Additional Context
