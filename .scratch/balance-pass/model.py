#!/usr/bin/env python3
"""Budget model v2 for .scratch/balance-pass/spec.md.

Offense and defense are measured on the same 90s timeline, both halves reported:
- Offense: sustained full-charge DPS vs the bare reference, kit windows fired on
  their lanes and gated by their prices (Onslaught needs 10 Momentum and spends 2
  a full swing, Offload spends 5, Roulette spins free, All In on cooldown).
- Defense: effective HP vs a classless iron-sword attacker (6 damage at 1.6
  hits/s, universal 10% x1.25 crit), with armor read every hit: Momentum ramps
  its checkpoints, Scutum reads Testudo on a flip cycle, Roulette Ward rides its
  window. Armor DR via the 1.21.11 CombatRules formula.

Not a game simulation: both fighters are assumed to land every full-charge swing,
no shields, no healing, no knockback. Recompute after tuning.

    python3 .scratch/balance-pass/model.py
"""
import random

SECONDS = 600.0
REF_RATE = 1.6          # classless iron-sword attacker, full-charge hits per second
REF_CRIT = 1.025        # universal 10% x1.25 crit, expected multiplier per hit
D = 6.0                 # iron sword damage
random.seed(11)


def dr(armor, toughness, damage=D):
    """1.21.11 getDamageAfterAbsorb: damage * (1 - clamp(armor - damage/(2+t/4), armor*0.2, 20)/25)."""
    inside = armor - damage / (2.0 + toughness / 4.0)
    return min(0.8, min(20.0, max(armor * 0.2, inside)) / 25.0)


def hits(rate, seconds=SECONDS):
    """Full-charge hit times for a constant rate."""
    step = 1.0 / rate
    t = step
    while t < seconds:
        yield t
        t += step


def merge(*streams):
    return sorted(streams)


def score(dps, ehp):
    return dps * ehp


# ---------------------------------------------------------------- Juggernaut

def juggernaut(swing_penalty=-0.5):
    base_rate = 1.6 + swing_penalty
    stacks = 10.0
    last_feed = 0.0
    onslaught_until = 0.0
    onslaught_next = 0.0
    offload_until = 0.0
    offload_next = 0.0
    out = in_ = 0.0
    t_prev = 0.0

    def feed(t, amount):
        nonlocal stacks, last_feed
        stacks = min(60.0, stacks + amount)
        last_feed = t

    def tier_armor():
        return 5.0 if stacks >= 45 else 3.0 if stacks >= 30 else 1.0 if stacks >= 15 else 0.0

    next_out = 1.0 / base_rate
    next_in = 1.0 / REF_RATE
    while True:
        t = min(next_out, next_in)
        if t >= SECONDS:
            break
        # bleed while quiet
        if t - last_feed > 5.0:
            stacks = max(0.0, stacks - (t - max(last_feed + 5.0, t_prev)))
        # windows on their lanes
        if t >= offload_next and stacks >= 5.0:
            stacks -= 5.0
            offload_next = t + 10.0
            offload_until = t + 5.0
        if t >= onslaught_next and stacks >= 10.0 and t >= onslaught_until:
            onslaught_next = t + 60.0
            onslaught_until = t + 15.0
        live = t < onslaught_until
        if t == next_out:
            if live:
                stacks = max(0.0, stacks - 2.0)
            damage = D * 1.15 * (1.15 if live else 1.0)
            if random.random() < 0.10:
                damage *= 1.25
            out += damage
            if live:
                last_feed = t  # the hit pays instead of earning; keep the clock fresh
            else:
                feed(t, 1.0)
            rate = base_rate + (0.2 if t < offload_until else 0.0)
            next_out = t + 1.0 / rate
        else:
            damage = D * REF_CRIT * (1.0 - dr(tier_armor(), 0.0))
            in_ += damage
            if live:
                last_feed = t
            else:
                feed(t, 1.0)
            next_in = t + 1.0 / REF_RATE
        t_prev = t
    dps = out / SECONDS
    ehp = 20.0 * (D * REF_RATE * REF_CRIT) / (in_ / SECONDS)
    return dps, ehp


# ---------------------------------------------------------------- Legionary

def legionary(stance, take=None, rate=None):
    if stance == "gladius":
        rate, deal, take = rate if rate is not None else 1.9, 1.15, take if take is not None else 1.1
        armor, toughness = 0.0, 0.0
    else:
        rate, deal, take = 1.6, 0.9, 1.0
        armor, toughness = 3.0, 1.0
    oath_until = 0.0
    oath_next = 0.0
    flip_next = 0.0
    impetus_pending = False
    testudo_until = 0.0
    out = in_ = 0.0

    out_times = list(hits(rate))
    in_times = list(hits(REF_RATE))
    events = [(t, "out") for t in out_times] + [(t, "in") for t in in_times]
    events.sort()
    for t, side in events:
        if t >= oath_next:
            oath_next = t + 50.0
            oath_until = t + 6.0
        if t >= flip_next:
            flip_next = t + 8.0
            impetus_pending = True
            if stance == "scutum":
                testudo_until = t + 1.5
        if side == "out":
            damage = D * deal * (1.15 if t < oath_until else 1.0)
            if impetus_pending:
                damage *= 1.10
                impetus_pending = False
            if random.random() < 0.10:
                damage *= 1.25
            out += damage
        else:
            catch = 0.85 if t < testudo_until else 1.0
            damage = D * REF_CRIT * (1.0 - dr(armor, toughness)) * take * catch
            in_ += damage
    dps = out / SECONDS
    ehp = 20.0 * (D * REF_RATE * REF_CRIT) / (in_ / SECONDS)
    return dps, ehp


# ---------------------------------------------------------------- Gambler

def gambler(crit_base=0.2, allin_mult=2.0):
    rate = 1.6
    streak = 0.0
    crit_chance = crit_base
    out = in_ = 0.0
    allin_until = 0.0
    allin_next = 0.0
    spin_next = 0.0
    edge_until = 0.0
    ward_until = 0.0
    bust_until = 0.0

    out_times = list(hits(rate))
    in_times = list(hits(REF_RATE))
    events = [(t, "out") for t in out_times] + [(t, "in") for t in in_times]
    events.sort()
    for t, side in events:
        if t >= allin_next:
            allin_next = t + 75.0
            allin_until = t + 15.0
        if t >= spin_next:
            spin_next = t + 25.0
            roll = random.random()
            if roll < 0.25:
                edge_until = t + 10.0
            elif roll < 0.50:
                ward_until = t + 10.0
            elif roll < 0.60:
                edge_until = ward_until = t + 10.0
            elif roll < 0.70:
                bust_until = t + 10.0
        if side == "out":
            crit = 0.05 + crit_chance
            roll = random.random()
            if roll < 0.05:
                damage = D * 0.5
                crit_chance = min(0.95, crit_chance + 0.15)
            elif roll < crit:
                crit_chance = crit_base
                mult = allin_mult if t < allin_until else 1.5
                damage = D * mult
            else:
                crit_chance = min(0.95, crit_chance + 0.15)
                damage = D * 1.0
            if t < edge_until:
                damage *= 1.25
            if t < bust_until:
                damage *= 0.75
            out += damage
        else:
            armor = 2.0 if t < ward_until else 0.0
            in_ += D * REF_CRIT * (1.0 - dr(armor, 0.0))
    dps = out / SECONDS
    ehp = 20.0 * (D * REF_RATE * REF_CRIT) / (in_ / SECONDS)
    return dps, ehp


def classless():
    out = in_ = 0.0
    for t in hits(REF_RATE):
        out += D * (1.25 if random.random() < 0.10 else 1.0)
        in_ += D * REF_CRIT
    dps = out / SECONDS
    ehp = 20.0 * (D * REF_RATE * REF_CRIT) / (in_ / SECONDS)
    return dps, ehp


def main():
    rows = [
        ("classless", classless()),
        ("juggernaut -0.4", juggernaut(-0.4)),
        ("juggernaut -0.5", juggernaut(-0.5)),
        ("gladius today", legionary("gladius")),
        ("gladius take x1.2", legionary("gladius", take=1.2)),
        ("gladius alacrity +0.1", legionary("gladius", rate=1.7)),
        ("scutum today", legionary("scutum")),
        ("gambler today", gambler()),
        ("gambler 15/1.75", gambler(crit_base=0.15, allin_mult=1.75)),
    ]
    # The candidate five: the four class rows with the proposed knob set plus the baseline.
    candidate = [rows[0], rows[1], rows[4], rows[6], rows[8]]
    scores = {name: score(dps, ehp) for name, (dps, ehp) in rows}
    mean = sum(scores[name] for name, _ in candidate) / len(candidate)
    lo = min(scores[name] for name, _ in candidate)
    hi = max(scores[name] for name, _ in candidate)
    print(f"candidate mean {mean:.1f}, band {mean * 0.9:.1f}-{mean * 1.1:.1f}, "
          f"pairwise max/min {hi / lo:.2f}")
    print(f"{'row':<28}{'DPS':>7}{'EHP':>7}{'score':>8}{'rel cand mean':>15}")
    for name, (dps, ehp) in rows:
        print(f"{name:<28}{dps:>7.2f}{ehp:>7.2f}{scores[name]:>8.1f}{scores[name] / mean:>15.2f}")


if __name__ == "__main__":
    main()
