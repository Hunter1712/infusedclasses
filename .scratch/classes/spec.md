# Spec: Class System (Warrior)

Status: ready-for-agent

## Problem Statement

Minecraft has no character classes: every player plays identically. Player wants a mod that assigns players a class with distinct stats and an active ability, starting with one class (Warrior) and a framework that makes adding more classes cheap.

## Solution

A Fabric mod that detects a player's first join, shows a class-selection GUI, persists the choice on the player, and applies class passives (attribute modifiers) plus one keybind-triggered active ability per class. Warrior ships first: buffed health/armor, slower movement, War Cry as the damage tool. Other classes appear greyed out in the GUI.

## User Stories

1. As a new player, I want to see a class selection screen on first join, so that I understand the mod and make a choice.
2. As a new player, I want Warrior available and other classes visibly listed but locked, so that I know more classes are coming.
3. As a player, I want my class choice to persist across logouts and relogs, so that I keep my class.
4. As a server admin, I want class changes possible via command, so that I can fix mistakes.
5. As a Warrior, I want increased max health, so that I feel tankier.
6. As a Warrior, I want increased armor, so that I take less damage.
7. As a Warrior, I want War Cry as my melee damage tool (no flat melee bonus), so that melee feels like my role.
8. As a Warrior, I want slightly reduced movement speed, so that the class has a real weakness.
9. As a Warrior, I want a War Cry active ability on a keybind, so that I have an active tool.
10. As a Warrior, I want War Cry to grant a temporary Strength buff, so that it feels powerful.
11. As a Warrior, I want a cooldown on War Cry, so that it can't be spammed.
12. As a player on a server, I want my class effects to work the same as in singleplayer, so that multiplayer is fully supported.
13. As a player switching class via command, I want old class modifiers removed before new ones apply, so that buffs don't stack.
14. As a player, I want to see War Cry cooldown feedback, so that I know when it's ready.
15. As a player rejoining, I want my existing class loaded silently with no GUI, so that the GUI only appears once.

## Implementation Decisions

- Fabric mod, latest stable Minecraft 1.21.x, Java 21 toolchain via Gradle.
- Mod id `infusedclasses`, name "Infused Classes"; Fabric template scaffolded at repo root.
- A `ClassId`/`ClassDefinition` registry in code: id, display name, passive modifiers, active ability, locked flag. Adding a class = adding one definition.
- Class choice persisted in player NBT (`entity data`), travels with the player; also covered by player data backups.
- First-join detection: server checks player's NBT on join; if unset, flag client to open the class GUI. GUI is a client screen listing all classes; locked ones greyed.
- On selection, client sends choice to server; server writes NBT, applies modifiers, closes GUI.
- Passives implemented as attribute modifiers on the player (`max_health`, armor, armor_toughness, `movement_speed` negative; no `attack_damage` — War Cry is the damage tool). Old class modifiers removed by namespacing modifier identifiers per class.
- Active ability: client keybind sends packet to server; server validates class + cooldown, applies effect (Strength II, 8s), starts 30s cooldown tracked server-side, sends particles/sound to the player.
- War Cry cooldown feedback via action bar message (remaining seconds on cooldown, shout confirmation on fire). An item-cooldown-style HUD visual is deferred.
- Admin command `/class set <player> <class>` sets class, strips previous modifiers, applies new. `/class select <player>` reopens the picker for GUI iteration; picking stays server-validated.
- MP-correct throughout: server authoritative; client only sends intents.
- Warrior baseline: +8 max health points (+4 hearts), armor/toughness bonus, -10% movement speed. No flat damage bonus — War Cry is the damage tool.

## Testing Decisions

- Good tests assert external behavior of the class-logic module: given a class id, the produced modifiers match spec; cooldown state machine rejects second use inside cooldown; class switching removes prior modifiers.
- Tested: class definition/passive modifier math, cooldown tracking, NBT save/load serialization of class id.
- Not tested: Minecraft rendering, GUI layout, keybind plumbing, mixin internals — manual verification in dev client.
- Prior art: none (greenfield). Plain JUnit, no Fabric gametest harness for now.

## Out of Scope

- Classes other than Warrior (greyed in GUI, no behavior).
- War Cry radius/team buffs; taunt/dash/slam abilities.
- Class-specific items beyond passives/kit; starting kit items deferred.
- Custom GUI art/assets, config files, datapack extensibility.
- Balance tuning beyond the baseline numbers.

## Further Notes

- Baseline numbers from planning: "+4 max hearts" was shorthand; final documented as increased max health via `max_health` attribute modifier — exact value recorded in code and this spec kept as source of intent.
- Session decisions: repo folder renamed to `infusedclasses` (rename may need manual completion if the OS lock persists).
