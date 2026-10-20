# Version policy

- **0.x is 1.21.11-only, locked.** The mod metadata declares `~1.21.11`; wrong-version reports close as wontfix.
- **No 1.20.x backports in 0.x.** Do not ask; the answer is the same every time.
- **A 26.x port comes later as a separate branch.** When it exists it ships as a second jar on the same project page (`infusedclasses-<mod>+26.x.jar`), picked by the `+<mc>` suffix. No multi-module split is paid before that branch exists.
- **Every jar carries the game version** (`infusedclasses-<mod>+<mc>.jar`, currently `infusedclasses-0.5.0+1.21.11.jar`), so a future 26.x jar slots onto the same page with zero rename. Local builds use the same suffixed name.
