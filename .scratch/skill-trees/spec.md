# Spec: Skill Trees (Warrior Juggernaut first)

Status: ready-for-agent

## Problem Statement

Classes are flat: one pick, static passives, one active ability. There is no progression, no build choice, no reason to keep playing a class. Separately, the chosen class is lost on death, which breaks trust in the whole class system.

## Solution

Per-class XP earned by playing, levels granting skill points, and one branching tree per class. Warrior ships the Juggernaut branch first as a 6-8 node vertical slice: small attribute nodes, one conditional passive (Thick Skin), and one War Cry upgrade that adds Resistance. Other branches and classes stay visible but locked. Class choice and tree progress survive death, relog, and respawn.

## User Stories

1. As a new player, I want class XP for kills, so that I progress by playing.
2. As a Warrior, I want bonus XP for sword and axe kills, so that my weapon choice matters.
3. As a player, I want levels 1-20 with rising cost, so that early levels feel fast and the cap takes hours.
4. As a player, I want vanilla XP drops to keep working separately, so that enchanting is not broken.
5. As a player, I want 1 skill point per level, so that each level gives a choice.
6. As a Warrior, I want a Juggernaut branch for tank fantasy, so that I know what I am building toward.
7. As a player, I want Berserker and Warlord branches visible but locked, so that I know more is coming.
8. As a player, I want small nodes that grant max health, armor, and toughness, so that I get steady power between big picks.
9. As a Juggernaut, I want Thick Skin that grants armor stacks when I am hit, so that taking hits makes me tougher.
10. As a Juggernaut, I want Thick Skin stacks capped with timed decay out of combat, so that I cannot snowball forever.
11. As a Juggernaut, I want War Cry to grant Strength plus Resistance, so that my shout holds the line.
12. As a player, I want War Cry upgrades to be a tree choice between duration and potency, so that my one active still feels like a build.
13. As a player, I want to open the tree with a key and a command fallback, so that I am never stuck.
14. As a player, I want a level-up toast and sound, so that progress feels rewarding.
15. As a player, I want to spend points by clicking nodes with gate requirements, so that I cannot grab the capstone at level 2.
16. As a player, I want free respec via command in MVP, so that early balance testing is frictionless.
17. As a player, I want my stacks wiped on death but my unlocks kept, so that death stings without forcing a rebuild.
18. As a player, I want my class and tree progress kept across death and respawn, so that I never lose my character.
19. As a player, I want my class and passives re-applied silently on rejoin, so that relog just works.
20. As a player with progress in one class, I want per-class XP stored separately, so that a future Ranger does not wipe my Warrior.
21. As a player on a server, I want unlocks validated server-side, so that clients cannot cheat points.
22. As a server admin, I want to set and respec classes by command, so that I can fix mistakes.

## Implementation Decisions

- Progression: class-affinity kill XP to level 20 with an exponential curve that is fast early and slow late; melee affinity bonus for Warrior, other affinities reserved for Ranger and Mage.
- Economy: 1 skill point per class level, most nodes cost 1, War Cry capstone costs more and requires points spent in-branch as a gate.
- Tree shape: one tree per class with 2-3 branches; Warrior MVP is Juggernaut only at 6-8 nodes (small fillers plus Thick Skin plus War Cry Resistance upgrade); other branches render locked.
- Node kinds for MVP: attribute small nodes reusing the existing passive modifier path, one on-hit-taken conditional with cap and decay, one active upgrade mutating duration and adding Resistance.
- Triggers for MVP are hardcoded to three hooks: on damage taken, on melee kill, on active cast; no generic trigger engine until a second branch needs it.
- Stacks are in-memory only with server tick decay, capped, wiped on death; only unlocks, XP, level, and spent points persist.
- Persistence extends the existing player NBT blob with XP, level, points, and unlocked node ids; the same save and load path covers death, respawn, and relog.
- Respawn fix: the respawn handler copies persisted class state from the old player entity to the new one before re-applying modifiers, because the new entity starts with empty in-memory state.
- Authority stays server-side throughout: client sends intents (select, spend, shout), server validates class, level, points, gates, and cooldowns.
- UI reuses the existing picker screen pattern for the talent panel with click-to-unlock and a command fallback for every action.
- Respec clears unlocks and refunds points, then re-applies base passives; free in MVP with cost deferred.

## Testing Decisions

- Good tests assert external behavior of pure logic: XP math per level, point gating rejects early capstone, Thick Skin cap and decay, War Cry upgrade adds Resistance, class id plus tree fields round-trip through serialization, respawn copy preserves class on a fresh instance.
- Tested: XP curve, gate math, conditional stack machine, NBT round-trip of class plus tree progress, respawn copy from old state to new state.
- Not tested: GUI layout, particle and sound plumbing, keybind wiring, live attribute application — manual verification in dev client.
- Prior art: plain JUnit tests for registry math, passive values, cooldown state machine, and class state transitions; same style, no gametest harness.

## Out of Scope

- Berserker and Warlord branches beyond locked placeholders; Ranger and Mage trees; shared rage bar HUD; permanent stacks; survival respec cost; datapack JSON extensibility; custom art; balance tuning beyond Juggernaut baseline numbers.

## Further Notes

- RPG references used: Diablo and WoW for exponential levels and gated branches, Borderlands for a tight early tree, World of Warcraft Warrior Rage and Diablo Barbarian Fury simplified to decaying buff stacks instead of a resource bar, Path of Exile small filler nodes plus keystones as the model for small versus conditional split.
- Ponytail cuts kept: no new HUD, no new save system, no generic event engine, Juggernaut-only first slice to prove small plus conditional plus active-upgrade before scaling.
