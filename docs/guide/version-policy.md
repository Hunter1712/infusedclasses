# Version policy

- **0.x is 1.21.11-only, locked.** The mod metadata declares `~1.21.11`; wrong-version reports close as wontfix.
- **No 1.20.x backports in 0.x.** Do not ask; the answer is the same every time.
- **A 26.x port comes later as a separate branch.** When it exists it ships as a second jar on the same project page (`infusedclasses-<mod>+26.x.jar`), picked by the `+<mc>` suffix. No multi-module split is paid before that branch exists.
- **Local builds keep the short name** (`infusedclasses-0.1.0.jar`) until listing. The suffixed name is reserved for listing time so the future port slots in with zero rename.
- **Wipes and resets at any time in 0.x.** Worlds may break between alpha jars. No data migration until beta.
