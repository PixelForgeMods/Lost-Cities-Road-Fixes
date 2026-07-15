# Compiled interchange-corridor regression

## Reported failure

Visual validation showed that a previous collision fix had made stack geometry
effectively flat while still leaving inaccessible height steps and duplicate
ramps. Closely spaced over/under crossings could each receive an interchange,
but the approach of one crossed the approach of the next at a different height.
The requested compact diamond behavior for a small deck gap was also not being
chosen from measured city space.

## Root cause

The failure was not one bad curve. Four planning contracts were disconnected:

1. selection scored nominal declarations before proving the geometry;
2. conflict resolution reserved only each nominal core, not the compiled
   192–512-block approaches;
3. approach elevation was interpolated across the entire surveyed arm, so two
   nearby crossings could prescribe different grades for one shared arterial;
4. stack/custom structure levels could be labels without a final composed-deck
   clearance assertion.

Consequently, two diamonds centered ten chunks (160 blocks) apart had separate
cores but overlapping approaches. One planned their shared X road at elevation
160 half-blocks while the other retained 152, creating a four-block separation.
Each plan was valid alone; their composition was not.

## Scrum stories

### US-30 — Restore physical stack topology

- Require 21 blocks between stack mainlines for four seven-block tiers.
- Keep every connector monotonic and provide one common trunk before each fork
  and after each merge.
- Reject ramp/ramp and ramp/mainline encounters inside the vehicle envelope.

### US-31 — Select from measured site facts

- Derive demand from contiguous road arms and city density, not seed buckets.
- Deduplicate Lost Cities multi-building footprints and minimize displacement.
- Compile each family at its shortest safe approach before ranking it.
- Prefer a diamond at the verified compact regional low-gap site.

### US-32 — Coordinate complete compiled envelopes

- Reserve core and approach corridors with a 64-chunk pairwise survey halo.
- Allow only identical flat shared arterial profiles; suppress mismatched grades.
- Localize native-to-planned elevation changes to the minimum legal run.
- Make custom structure tiers physical and add a final surface-clearance gate.

### US-33 — Harden writing and diagnostics

- Clear all envelopes before placing any deck, then write supports last.
- Report selected family counts and retain selected/rejected/conflicted crossing
  explanations for the operator command.

## Automated evidence

The regression pair deliberately composes both incompatible diamonds at overlap
chunk `(5,0)` and verifies that the final clearance gate rejects them. Running
the conflict resolver first leaves exactly one crossing and the same chunk then
composes safely. Additional tests cover seed-independent winners, identical flat
corridor coexistence, exact seven-block clearance, minimum-run arterial grades,
physical custom tiers, too-steep custom rejection, monotonic stacks, common
trunks, and collector-height equality.

The complete JUnit suite and Gradle release build are required before packaging.
In-game validation must use fresh chunks because saved road blocks are never
rewritten.
