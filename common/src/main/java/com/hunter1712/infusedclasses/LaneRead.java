package com.hunter1712.infusedclasses;

/**
 * One lane's transient read at one clock: its cooldown remainder, its window's remainder and
 * whether that window still carries its one-shot. Shared by the runtime state read and the wire
 * snapshot, so a lane read cannot mean two things in the two places that cross it.
 *
 * @param remainingMs milliseconds left on the lane's cooldown, 0 when ready
 * @param windowRemainingMs milliseconds left on the lane's window, 0 when dead
 * @param windowPending whether the lane's one-shot read is pending — Rotate's Impetus
 *                      (a Gladius landing only); true while live for the timed windows
 *                      nothing spends
 */
public record LaneRead(long remainingMs, long windowRemainingMs, boolean windowPending) {
}
