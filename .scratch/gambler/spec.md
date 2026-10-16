# Spec: Gambler class + mod-level crit rework

Status: ready-for-agent

## Problem Statement

Vanilla crits are a jump-mechanic freebie: any player who times a falling swing
gets a flat ×1.5, unrelated to the class system this mod is built around. That
leaves crits outside the mod's control, gives every class the same crit story,
and blocks a chance-based class: a Gambler's identity would sit next to a
vanilla crit system doing the same trick for free. Separately, the roster has
two melee classes (Juggernaut, Legionary) whose play avoids luck entirely;
there is no class where chance is the mechanic, and the mod's decisions have no
vocabulary for a passed-in random roll.

## Solution

- **The mod owns crits.** Vanilla falling crits are retired for every player on
  a modded server: the mixin cancels `Player.canCriticalAttack`, the one
  1.21.11 gate for the ×1.5, the crit sound and the crit particles. In their
  place: a universal 10% crit on full-charge landed hits at ×1.25 for every
  player, classless players included, plus a class-override seam so a class can
  declare its own crit rule. Peers were tuned without crits: ×1.25 keeps them
  near their tuned power and leaves the spikes to the class built on them.
- **Gambler** (id `infusedclasses:gambler`, display `Gambler`, accent gold
  `0xFFD4AF37`, tagline `Double or nothing`) — a melee class that bets on the
  dice:
  - **Crooked Dice** (crit rule): one roll per full-charge landed hit: 20%
    crit ×1.5, 5% betrayal ×0.5, 75% normal. The betrayal band is fixed at 5%
    and can never be bought out.
  - **Streak** (crit rule): +15 percentage points of crit per non-crit full
    hit (betrayal counts as a non-crit); any crit resets to the 20% base; the
    crit band locks at 95% from streak 5, so the lock is a moment that arrives:
    about once per 90s of sustained fighting.
  - **R — Roulette** (25s lane, free): one spin over seven bands, good and
    bad: Edge ×1.25 damage 10s (20%), Ward +2 armor 10s (20%), Haste +0.02
    speed 10s (20%), Vigor heal 3 HP (20%), Jackpot all three stats 10s + heal
    4 (8%), Bust ×0.75 melee 10s (8%), Zero (4%).
  - **G — All In** (75s lane): 15s where crits deal ×2.0 instead of ×1.5. Dice
    still decide; betrayal keeps its 5%; nothing forced or suppressed.
- **Chance as data.** Rolls enter decisions as passed-in values; no decision
  module touches a random source. The plan returns a verdict and streak
  movement; the entry draws rolls and adapters apply outcomes.

## User Stories

1. As a player of any class, I want full-charge hits to carry a flat 10% crit
   at ×1.25, so crits are a mod rule instead of a vanilla jump freebie.
2. As a Juggernaut or Legionary, I want a crit identity (10% on full swings)
   after jump crits are retired, so combat keeps small spikes without a free
   ×1.5.
3. As any melee player, I want half-charge swings to never roll, so spam stays
   unrewarded exactly like the stack gate.
4. As a player without a chosen class, I want the universal crit too, so
   retiring vanilla crits leaves no player with none.
5. As a Gambler, I want a 20% crit base at ×1.5 instead of the universal 10%
   at ×1.25, so the class's first identity is better odds and harder hits.
6. As a Gambler, I want a fixed 5% betrayal band that cuts a hit to ×0.5, so
   the dice have teeth.
7. As a Gambler, I want betrayal to never grow and never be removable, so the
   dice stay crooked no matter how loaded.
8. As a Gambler on a dry streak, I want +15 percentage points of crit per
   non-crit swing, so long misses load the pocket fast.
9. As a Gambler on a long dry streak, I want the crit band to swallow the
   normal band (95/5 lock from streak 5), so the lock is a moment that
   actually arrives.
10. As a Gambler landing any crit, I want Streak to reset to base, so the
    loaded pocket always spends itself on the crit.
11. As a Gambler betrayed, I want betrayal to still feed Streak, so even the
    bad roll pays forward.
12. As a Gambler pressing R, I want a free spin with no HP cost, so the gamble
    is the roll itself, not a health tax.
13. As a Gambler spinning Edge, I want ×1.25 damage for 10s.
14. As a Gambler spinning Ward, I want +2 armor for 10s.
15. As a Gambler spinning Haste, I want +0.02 movement speed for 10s.
16. As a Gambler spinning Vigor, I want an instant 3 HP heal.
17. As a Gambler spinning Jackpot, I want all three stats for 10s plus a 4 HP
    heal.
18. As a Gambler spinning Bust, I want my melee ×0.75 for 10s, so the wheel
    can hurt.
19. As a Gambler landing a crit or a betrayal, I want a cue for each, so I can
    tell which roll happened without damage numbers.
20. As a Gambler spinning Zero, I want nothing to happen, so the wheel has a
    dead band like a roulette zero.
21. As a Gambler pressing R on cooldown, I want the remaining seconds and
    nothing spent or armed.
22. As a Gambler pressing G, I want 15s of ×2.0 crits on a 75s lane, so crits
    get heavier without being forced.
23. As a Gambler inside All In, I want betrayal unchanged and crits to still
    reset Streak, so the dice rules never bend.
24. As a picker browser, I want Crooked Dice, Streak, Roulette and All In copy
    generated from their constants (bands, durations, multipliers, lane
    seconds), so text cannot drift from behaviour.
25. As a player switching class or respawning, I want my Streak and both
    windows wiped with everything else, so no stale dice survive.
26. As a modpack user, I want no new keybinds or payloads — the picker page
    and the reused R/G binds arrive on their own.
27. As a maintainer, I want the crit system documented (crit roll, betrayal,
    Streak) so the retired vanilla crit does not reappear in copy or docs.

## Implementation Decisions

- New pure decision module `CritRoll`: decides one swing's verdict from the
  class's crit rule, current Streak, the passed roll, the All In window and
  swing charge. It owns the universal baseline (10% ×1.25, full swings only,
  no betrayal, no Streak movement) and the class override (the Gambler's
  Crooked Dice bands at 20/5/75, step 0.15). A missing class reads as the
  universal rule. No per-class vanilla-crit policy exists: the mixin cancels
  vanilla crits unconditionally, so it holds zero logic.
- New class data `CritRule`: one record per class that has dice — crit chance
  and mult, betrayal chance and mult, Streak step. The Gambler carries
  `(0.20, 1.5, 0.05, 0.5, 0.15)`; the card's `Crooked Dice` and `Streak` lines
  are generated from it, so the Streak reset target cannot drift from the
  chance base.
- Band math: betrayal `[0, 0.05)`; crit `[0.05, 0.05 + min(0.95, 0.20 + 0.15
  × streak))`; remainder normal. At streak 5 the crit band reaches 0.95 and
  the normal band disappears; betrayal keeps its 5% forever. Universal
  baseline (classless included): crit `[0, 0.10)`, no betrayal, no deltas.
- Multipliers fold into the swing's deal mult, multiplicative with stance,
  flip and each other: crit ×1.5 (×2.0 while All In is live), betrayal ×0.5,
  Roulette Edge/Jackpot ×1.25 and Bust ×0.75 while their outcome window is
  live (any charge, like stance mults). Rend bonus hurts are unaffected. The
  mult settles through the existing second-hurt settle on the pre-armor
  `baseDamageTaken`: armor reduces both parts, near-linearly, so the net reads
  as base times mult. Caveats to keep in mind, not to fix: betrayal's heal-back
  is pre-armor (shallower against armored targets) and the bonus drops when
  the base hit already killed.
- Rolls: the server entry draws one roll per full-charge landed hit for every
  player, classless included, and passes it to the plan as a parameter. Sweeps
  count per landed hit: each swept target draws its own roll and moves Streak
  once. No decision module calls a random source; tests pass boundary values.
- Vanilla retirement: the existing player mixin injects
  `Player.canCriticalAttack` and returns false for every player — the one
  seam that stops the vanilla ×1.5, the `PLAYER_ATTACK_CRIT` sound and the
  crit particles (animate action 4) together. Mobs are untouched.
  `magicCrit`/ENCHANTED_HIT is the enchanted-hit sparkle, not a crit:
  untouched.
- Crit feedback: the verdict drives a cue from the settle adapter — a crit
  replays the vanilla cue (animate action 4 plus the crit sound), a betrayal
  gets a distinct cue. No HUD, no per-hit chat.
- Classless settle: the entry no longer skips classless melee. A classless
  full-charge hit settles the universal crit only (no stacks, no windows, no
  Streak, no betrayal); the laziest shape is a small classless branch in the
  attacker settle that draws the roll and settles the crit mult, with crit
  logic staying single-sourced in `CritRoll`.
- Plan and settle: the hit plan record gains the dice verdict (crit /
  betrayal / normal / none), the applied multiplier and the Streak movement;
  the hit settle applies the movement in the same place it already applies
  stack bumps and spends. Peeking windows happens in the plan; spending
  happens in the settle, at the same clock reading, as everywhere else.
- `RouletteAbility`: pure trigger/fire in the house gate-and-arm shape. Seven
  band cuts, in order: Edge 0–0.20, Ward 0.20–0.40, Haste 0.40–0.60, Vigor
  0.60–0.80, Jackpot 0.80–0.88, Bust 0.88–0.96, Zero 0.96–1.00. Fired spins
  announce their outcome, arm the 25s lane and stamp the outcome window;
  rejects (wrong class, on cooldown) arm nothing. Instant effects at fire:
  Vigor heals 3, Jackpot heals 4. Timed effects while the window is live:
  Ward/Haste stat auras through the passive applier's window-modifier path;
  Edge/Jackpot and Bust deal mults read by the plan.
- `AllInAbility`: pure trigger/fire; fires only for Gamblers off its 75s lane,
  arms the lane and a 15s window together. The plan reads the window and lifts
  the crit multiplier to 2.0. Betrayal, Streak resets and every other dice rule
  are untouched.
- `BindMorph`: `R` target gains `ROULETTE` (`RUSH | ROTATE | ROULETTE`), `G`
  target gains `ALL_IN` (`REND | AEQUILIBRIUM | ALL_IN`); receivers dispatch
  the existing payloads, no protocol change.
- `PlayerRuntime`: a Streak counter with read/bump/reset verbs, the Roulette
  outcome window (payload = outcome) and the All In window; all wiped by the
  existing switch/respawn reset alongside stacks and lanes.
- `Cooldowns`: two lane values (`ROULETTE` 25s, `ALL_IN` 75s) with per-lane
  verbs, same shape as the existing four lanes.
- Registry and copy: the class entry carries `[roulette, all_in]` with
  generated descriptions (pinned under 200 chars like every active), plus the
  `CritRule`; the card shows Crooked Dice and Streak under `Passives`.
- Docs: glossary gains crit roll, crit rule, Crooked Dice, betrayal, Streak
  and the Roulette outcomes; ADR-0012 records "the mod owns crits; chance
  enters decisions as data"; README/PROJECT_DESCRIPTION class lists and
  CHANGELOG note the new class and the vanilla crit retirement.
- No HP cost machinery anywhere: no stake, no floor check, no self-damage.

## Testing Decisions

Good tests assert external behaviour of pure decisions with boundary values —
no implementation details, no live game objects. Prior art: the ability
decision tests (Rend, Aequilibrium) for trigger/fire shapes, the melee plan
test for hit outcomes, the runtime test for state lifecycles.

- New `CritRoll` test: universal baseline at rolls 0.099/0.10 (mult 1.25,
  classless and classed alike); half-charge swings verdict none with no
  movement; Gambler bands at 0.049/0.05/0.249/0.25 at streak 0; Streak +1 on
  normal, +1 on betrayal, reset on crit; 95/5 lock at streak 5 and beyond; All
  In ×2.0 with betrayal and reset rules unchanged.
- Extend the melee plan test: verdict and multiplier fields on the hit record,
  crit folding into the deal mult and combined result (e.g. stance × crit),
  Rend bonus untouched, streak movement returned for the settle.
- New `RouletteAbility` test: band cuts at 0.199/0.20, 0.399/0.40, 0.599/0.60,
  0.799/0.80, 0.879/0.88, 0.959/0.96, 1.0→Zero; free fire (no HP touched) arms
  the lane and window; wrong-class and on-cooldown rejections arm and spend
  nothing; no path touches Streak; copy generated.
- New `AllInAbility` test: fire arms lane + 15s window; rejects arm nothing;
  independent of the Roulette lane.
- Extend the runtime test: Streak verbs, window payload round-trip, reset
  wipes Streak and both windows; extend the cooldown test for the two new
  lanes' independence and reset; extend the bind morph test for the two new
  targets; extend registry and presentation tests for the entry, the crit
  rule, the two generated passive card lines and the generated copy.
- Not unit-tested (house convention, manual dev-client pass via
  `gradle deployClient` and `/class select`): the mixin injection itself,
  entry wiring and roll drawing, feedback cues, aura/heal/sound appliers,
  feel and tuning.

## Out of Scope

- The scrapped Reaper: souls, kill hook, Execute, lifesteal, Ghost Form,
  falling-attack advantage.
- HP stakes and any HP-cost machinery (setHealth, floor checks).
- Wager and Snake Eyes as actives; Snake Eyes is deleted outright — no band,
  no wheel path that touches Streak.
- Crit identities for other classes beyond the universal baseline; mob and
  projectile crits; HUD; pack balance.
- Balance tuning after playtest (bands, multipliers and durations are
  constants; one-line changes later — the Gambler crit mult ×1.5 → ×1.75 is
  the first dial).

## Further Notes

- Numbers of record: universal 10% ×1.25; dice 20% ×1.5, betrayal 5% ×0.5,
  step 0.15, lock at streak 5. Expected damage: dice ≈ ×1.16, ≈ +13.3% over
  peers; full kit with wheel and All In ≈ +19%. Lock arrives about once per
  90s of sustained melee.
- Implementation issues live in `.scratch/gambler/issues/`: 01 crit floor; 02
  Crooked Dice + Streak; 03 Roulette (blocked by 01 only — parallel-friendly);
  04 All In (blocked by 02); 05 registry, copy, docs and the in-game verify
  (blocked by 02–04). The Gambler becomes pickable at 05.
- This is the third playable class and the first mod-level rework of a vanilla
  mechanic; binds are reused, no payloads move, and the picker gains a page
  with no layout change.
- The universal crit was chosen at ×1.25 (not vanilla's ×1.5) because peers
  were tuned without crits: ×1.25 keeps their power near tuned and leaves the
  crit spikes to the Gambler. If combat reads flat in playtest, the same seam
  can hand classes other crit identities later.
