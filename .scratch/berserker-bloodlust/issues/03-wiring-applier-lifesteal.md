# 03: Bloodlust intent, receiver, applier, and lifesteal hook

Spec: `.scratch/berserker-bloodlust/spec.md`

**Blocked by:** 02 (needs the decision + lane).

**Status:** wontfix

## Task

Client-to-server path and the adapters that land Bloodlust, replacing the
War Cry scan with a self buff + attacker hook:

- `BloodlustPayload` unit intent on `infusedclasses:bloodlust`, registered in
  `InfusedClasses.registerPayloads` replacing the `war_cry` line
  (`InfusedClasses.java:26`). `R` keybind (`InfusedClassesClient.java:39-40`)
  sends it; `en_us.json:3` entry renamed (text stays the key name the picker
  copy derives from — ticket 04 owns display copy).
- Receiver in `InfusedClasses.registerReceivers` replacing the
  `WarCryPayload` block (`InfusedClasses.java:72-83`): fire the 02 decision,
  apply, then grant bucket stacks. Delete `WarCryPayload.java`.
- New `BloodlustApplier.apply(player, result)`: announce through the shared
  `AbilityApplier` path, `applyCastSlow` on FIRED only, ravager roar kept
  (even on whiffs — press must land), no mob scan, no radius. Fizzles
  (wrong class / cooldown / too healthy) say their line and touch nothing.
  Delete `WarCryApplier.java`.
- Lifesteal branch in `ServerEvents` `AFTER_DAMAGE` next to the victim Bulwark
  bump (`ServerEvents.java:38-48`): source attacker is a Berserker player with
  a live Bloodlust window and melee damage → `attacker.heal(lifestealFor
  (liveBucket, damageTaken))`. Any melee, no charge gate, self-damage
  excluded. Hearts puff per heal is implementer's call.

## Acceptance

- Rejected presses leave lanes, window, and stacks untouched (fizzle arms
  nothing, no root).
- Manual only for feel (particles/sound/roar); logic covered by 02 JUnit.
- `gradle test` green.

## Comments

Triage: wontfix — superseded by juggernaut-rush-rend: implemented in 2545677,
then removed in fe24f6d, which renames Berserker→Juggernaut and replaces
Bloodlust/Second Wind with Rush/Rend. See `.scratch/juggernaut-rush-rend/spec.md`.
