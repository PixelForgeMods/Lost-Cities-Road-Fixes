# Professional interchange geometry

Research date: 2026-07-15

## Scope and interpretation

This note translates official highway guidance into topology and geometry rules
for the mod. It is not a real-world design standard, and real-world dimensions
must not be converted mechanically from feet to blocks. FHWA states that clear
zone width depends on traffic, speed, and roadside geometry and should not be a
single nationally fixed value; the useful Minecraft translation is therefore to
preserve topology, continuity, separation, and configurable envelopes rather
than a literal scale ([FHWA, *Clear Zone and Horizontal Clearance*, questions
1, 2, and 5](https://www.fhwa.dot.gov/programadmin/clearzone.cfm)).

In this note, an **outer ramp** is the cloverleaf's direct right-turn ramp. It
is distinct from a high-speed directional left-turn connector in a stack or
directional interchange ([WSDOT Design Manual M 22-01.23,
§1360.02(2)(a)–(c)](https://wsdot.wa.gov/publications/manuals/fulltext/M22-01/1360.pdf)).

## Full cloverleaf topology

FHWA's full-cloverleaf example has one free-flow loop and one outer connection
in every quadrant: eight ramps in total, four acceleration lanes in the model,
and no crossroad ramp terminals ([FHWA-HRT-07-045, “Full Cloverleaf
Interchange,” figure 7 and application notes](https://www.fhwa.dot.gov/publications/research/safety/07045/applic.cfm)).
WSDOT assigns the four loops to left turns and the outer ramps to right turns;
it describes the resulting movements as merges rather than at-grade turning
conflicts ([WSDOT Design Manual M 22-01.23, §1360.02(2)(c), pp.
1360-2–3](https://wsdot.wa.gov/publications/manuals/fulltext/M22-01/1360.pdf)).

For this project's four-way directed-movement model, a full cloverleaf therefore
means four straight mainline movements plus eight turning movements: exactly
four `LEFT`/`LOOP` connections and exactly four `RIGHT`/`DIRECT` outer-ramp
connections. Four loops without four outer ramps are not a complete
cloverleaf, and routing a right turn over the same one-lane path as a loop does
not implement the official topology ([FHWA-HRT-07-045, figure
7](https://www.fhwa.dot.gov/publications/research/safety/07045/applic.cfm);
[WSDOT §1360.02(2)(c)](https://wsdot.wa.gov/publications/manuals/fulltext/M22-01/1360.pdf)).

A cloverleaf's characteristic weave is also explicit geometry. WSDOT describes
a low-speed loop entrance followed closely by another loop's exit, requires the
spacing to be checked as a weave, and favors a collector-distributor (C-D) road
to move high-speed weaving away from the mainline ([WSDOT §§1360.04(6)–(8),
pp. 1360-21–22](https://wsdot.wa.gov/publications/manuals/fulltext/M22-01/1360.pdf)).
Caltrans likewise calls for C-D roads in four-quadrant cloverleafs to separate
weaving conflicts from through traffic ([Caltrans Highway Design Manual,
§§502.2(c) and 502.3(3)(c), pp. 500-6 and
500-8](https://dot.ca.gov/-/media/dot-media/programs/design/documents/chp0500-092923-a11y.pdf)).

The conservative game-geometry inference is that an encounter between movement
paths must be a declared same-direction merge, diverge, or weave, or must be
grade-separated. An unclassified same-level crossing is incompatible with a
full cloverleaf's free-flow, merge-based topology and absence of crossroad ramp
terminals ([FHWA-HRT-07-045, figure
7](https://www.fhwa.dot.gov/publications/research/safety/07045/applic.cfm);
[WSDOT §1360.02(2)(c)](https://wsdot.wa.gov/publications/manuals/fulltext/M22-01/1360.pdf)).

## Ramp terminals, auxiliary lanes, and transitions

### Entrance and exit terminals

FHWA treats an acceleration or deceleration lane, including its taper, as part
of an elongated ramp terminal. The speed-change lane gives exiting traffic room
to slow outside the through lanes and entering traffic room to accelerate,
select a gap, and merge ([FHWA, *Freeway Management and Operations Handbook*,
§§5.4.1–5.4.2](https://ops.fhwa.dot.gov/freewaymgmt/publications/frwy_mgmt_handbook/chapter5.htm)).
WSDOT expresses the entrance geometry as an acceleration lane followed by a
taper; a parallel entrance first adds a lane beside the through lanes and then
tapers it into the mainline ([WSDOT §1360.04(4), pp.
1360-14–15](https://wsdot.wa.gov/publications/manuals/fulltext/M22-01/1360.pdf)).

An exit needs usable deceleration distance before its first controlling ramp
curve, while an entrance needs an aligned acceleration and merge region
after its last controlling curve. Caltrans requires the exit deceleration
length before the first curve beyond the exit nose and keeps an entrance profile
approximately parallel to the freeway before the inlet nose
([Caltrans §§504.2(2) and 504.2(5), pp. 500-12 and
500-16](https://dot.ca.gov/-/media/dot-media/programs/design/documents/chp0500-092923-a11y.pdf)).
Except for specialized median facilities, Caltrans also places freeway entries
and exits on the right of through traffic ([Caltrans §504.2(1), p.
500-12](https://dot.ca.gov/-/media/dot-media/programs/design/documents/chp0500-092923-a11y.pdf)).

One physical lane must not silently serve opposing entrance and exit flows.
WSDOT requires a roadway carrying both an on-ramp and an off-ramp to be designed
as separate one-way ramps with physical separation ([WSDOT §1360.03(5), p.
1360-10](https://wsdot.wa.gov/publications/manuals/fulltext/M22-01/1360.pdf)).
For same-direction movements, sharing is credible only when represented as a
declared C-D, auxiliary, merge, diverge, or weave segment with explicit
direction and lane-count semantics; it is not credible as two movement
identities accidentally occupying the same centerline ([WSDOT
§§1360.04(6)–(8), pp.
1360-21–22](https://wsdot.wa.gov/publications/manuals/fulltext/M22-01/1360.pdf)).

### Lane balance and auxiliary-lane continuity

WSDOT's lane-balance equations provide useful topological checks. At an
entrance, the downstream lane count is at least the sum of the mainline and ramp
approach lanes minus one. At a normal exit, the approach count equals the
downstream through lanes plus the exit lanes minus one. At a cloverleaf or a
closely spaced entrance-to-exit pair with a continuous auxiliary lane, that
auxiliary lane may instead drop at the exit, making the approach count equal to
the downstream through lanes plus the exit lanes ([WSDOT §1360.04(1), pp.
1360-11–12](https://wsdot.wa.gov/publications/manuals/fulltext/M22-01/1360.pdf)).

In symbols:

```text
entrance:    lanes_after >= lanes_before + ramp_lanes - 1
normal exit: lanes_before  = lanes_after  + exit_lanes - 1
aux-drop:    lanes_before  = lanes_after  + exit_lanes
```

The basic through-lane count should remain intact through the interchange, and
only one freeway lane should be removed at a time ([WSDOT §§1360.04(1)–(2),
pp. 1360-11–13](https://wsdot.wa.gov/publications/manuals/fulltext/M22-01/1360.pdf);
[Caltrans §504.6, pp. 500-38–40](https://dot.ca.gov/-/media/dot-media/programs/design/documents/chp0500-092923-a11y.pdf)).
FHWA also recommends avoiding multiple successive exits from one lane and
providing drivers clear lane assignments and enough distance to change lanes
([FHWA-HRT-17-048, treatment 1 principles](https://www.fhwa.dot.gov/publications/research/safety/17048/010.cfm)).

When an entrance is followed closely by an exit, the acceleration and
deceleration facilities should form a continuous auxiliary lane rather than two
short strips with a gap. FHWA and Caltrans both state this continuity principle,
and WSDOT applies it specifically to the loop-ramp weave in a cloverleaf
([FHWA handbook §5.4.1](https://ops.fhwa.dot.gov/freewaymgmt/publications/frwy_mgmt_handbook/chapter5.htm);
[Caltrans §504.5, pp. 500-38–39](https://dot.ca.gov/-/media/dot-media/programs/design/documents/chp0500-092923-a11y.pdf);
[WSDOT §1360.04(7), p. 1360-21](https://wsdot.wa.gov/publications/manuals/fulltext/M22-01/1360.pdf)).

### Lane-add, lane-drop, taper, and gore behavior

An entrance can merge through a long tapered connection or through a parallel
acceleration lane ending in a taper. An exit can diverge through a tapered
connection or through a taper that develops a parallel deceleration lane; these
are gradual changes with an identifiable nose and gore, not instantaneous width
jumps ([WSDOT §§1360.04(4)–(5) and exhibits 1360-15–1360-25, pp.
1360-14–35](https://wsdot.wa.gov/publications/manuals/fulltext/M22-01/1360.pdf)).
Caltrans separately specifies tapers for ramp lane drops and bay tapers for ramp
lane additions, reinforcing that adding or removing a lane is a longitudinal
transition ([Caltrans §504.3(1)(d)–(e), p.
500-17](https://dot.ca.gov/-/media/dot-media/programs/design/documents/chp0500-092923-a11y.pdf)).

The federal MUTCD distinguishes a normal exit, an exit-only lane drop, and a
continuous auxiliary lane, and it reserves a channelized neutral area at exit
ramps and parallel entrance ramps. Those distinctions are useful semantic states
even if Minecraft does not render every pavement marking ([MUTCD 11th ed.,
§§3B.07–3B.08 and figures 3B-10–3B-11, pp.
552–559](https://mutcd.fhwa.dot.gov/pdfs/11th_Edition/part3.pdf)).

## Interchange footprint and building exclusion

FHWA defines clear zone from the edge of traveled way as an unobstructed,
relatively flat roadside recovery area. It distinguishes this from the smaller
horizontal clearance to a roadside object and explicitly rejects one fixed
nationwide clear-zone width ([FHWA, *Clear Zone and Horizontal Clearance*,
questions 1, 3, and 5](https://www.fhwa.dot.gov/programadmin/clearzone.cfm)).
WSDOT applies clear-zone calculations to ramps at each location using the ramp's
design speed, including transitions between curves of different design speeds
([WSDOT §1360.03(4)(c), pp.
1360-9–10](https://wsdot.wa.gov/publications/manuals/fulltext/M22-01/1360.pdf);
[WSDOT §1600.02(3), p. 1600-4](https://wsdot.wa.gov/publications/manuals/fulltext/M22-01/1600.pdf)).

The road envelope is wider and longer than its centerlines. It includes traveled
lanes, shoulders, acceleration and deceleration lanes, tapers, gore reserve
areas, and any auxiliary or C-D road ([FHWA handbook §§5.4.1–5.4.2](https://ops.fhwa.dot.gov/freewaymgmt/publications/frwy_mgmt_handbook/chapter5.htm);
[WSDOT §1360.03(4) and exhibits 1360-12–1360-13, pp.
1360-8–10 and 1360-19–21](https://wsdot.wa.gov/publications/manuals/fulltext/M22-01/1360.pdf)).
Consequently, a nominal radius or center square is not a complete building
exclusion envelope if a ramp terminal, taper, shoulder, or approach extends
beyond it ([WSDOT §§1360.03(4), 1360.04(4)–(8)](https://wsdot.wa.gov/publications/manuals/fulltext/M22-01/1360.pdf)).

Caltrans's roadside policy says fixed objects should first be eliminated or
moved outside the clear recovery zone, with yielding design or shielding used
when relocation is not possible ([Caltrans Highway Design Manual, §309.1(2),
pp. 300-34–35](https://dot.ca.gov/-/media/dot-media/programs/design/documents/chp0300-092923-a11y.pdf)).
For elevated freeways and ramps, Caltrans also requires lateral separation from
adjoining buildings for maintenance, reconstruction, fire exposure, and
emergency access ([Caltrans §309.4, p.
300-42](https://dot.ca.gov/-/media/dot-media/programs/design/documents/chp0300-092923-a11y.pdf)).
Minecraft buildings are immovable, non-yielding objects, so exclusion from the
complete generated envelope is the closest conservative analogue; this is a
game-design translation, not a claim that clear zone and highway right of way
are interchangeable.

## Minecraft translation: testable geometric invariants

The following are proposed test oracles. Configured block lengths replace
real-world dimensional tables; the tests preserve the cited topology and
relationships rather than claiming engineering equivalence.

| ID | Geometric invariant and test oracle | Primary-source basis |
| --- | --- | --- |
| `CL-01` | **Separate ramp movements:** a full cloverleaf has 12 unique directed connections: four `STRAIGHT`/`MAINLINE`, four `RIGHT`/`DIRECT` outer ramps, and four `LEFT`/`LOOP` ramps. Each of the eight turns has its own route identity and terminal pair. Positive-length centerline coincidence is forbidden outside a declared same-direction C-D/auxiliary/merge/weave segment with explicit lane semantics. | [FHWA-HRT-07-045, figure 7](https://www.fhwa.dot.gov/publications/research/safety/07045/applic.cfm); [WSDOT §1360.02(2)(c)](https://wsdot.wa.gov/publications/manuals/fulltext/M22-01/1360.pdf) |
| `CL-02` | **No at-grade ramp crossings:** every interior centerline intersection is classified as a same-direction merge, diverge, or weave, or has at least the configured vertical clearance. A full cloverleaf has no uncontrolled same-level crossing node and no crossroad ramp terminal. | [FHWA-HRT-07-045, figure 7](https://www.fhwa.dot.gov/publications/research/safety/07045/applic.cfm); [WSDOT §1360.02(2)(c)](https://wsdot.wa.gov/publications/manuals/fulltext/M22-01/1360.pdf) |
| `TR-01` | **Unambiguous terminals:** each turn has one declared right-side exit and one declared right-side entrance. An on-flow and off-flow never occupy the same undivided one-lane centerline; if a shared roadway is modeled, it contains separated one-way lanes or an explicit same-direction lane bundle. | [Caltrans §504.2(1)](https://dot.ca.gov/-/media/dot-media/programs/design/documents/chp0500-092923-a11y.pdf); [WSDOT §1360.03(5)](https://wsdot.wa.gov/publications/manuals/fulltext/M22-01/1360.pdf) |
| `TR-02` | **Speed-change space:** every exit has a contiguous configured deceleration run before its first low-speed curve. Every entrance has a contiguous configured acceleration run after its final low-speed curve and before its taper ends or lane-add becomes permanent. | [FHWA handbook §5.4.2](https://ops.fhwa.dot.gov/freewaymgmt/publications/frwy_mgmt_handbook/chapter5.htm); [Caltrans §504.2(2)](https://dot.ca.gov/-/media/dot-media/programs/design/documents/chp0500-092923-a11y.pdf); [WSDOT §1360.04(4)](https://wsdot.wa.gov/publications/manuals/fulltext/M22-01/1360.pdf) |
| `AUX-01` | **Auxiliary-lane continuity:** where a loop entrance precedes the next loop exit, either one full-width auxiliary lane or a C-D roadway exists continuously between the two terminal regions. Rasterized coverage has no zero-width station or one-block hole between them. | [FHWA handbook §5.4.1](https://ops.fhwa.dot.gov/freewaymgmt/publications/frwy_mgmt_handbook/chapter5.htm); [WSDOT §§1360.04(6)–(8)](https://wsdot.wa.gov/publications/manuals/fulltext/M22-01/1360.pdf); [Caltrans §504.5](https://dot.ca.gov/-/media/dot-media/programs/design/documents/chp0500-092923-a11y.pdf) |
| `LB-01` | **Lane balance:** sampled cross sections satisfy the entrance and exit equations above; the basic through-lane count is continuous through the interchange, and no transition removes more than one lane at once. | [WSDOT §§1360.04(1)–(2)](https://wsdot.wa.gov/publications/manuals/fulltext/M22-01/1360.pdf); [Caltrans §504.6](https://dot.ca.gov/-/media/dot-media/programs/design/documents/chp0500-092923-a11y.pdf) |
| `TP-01` | **Taper behavior:** each lane add or drop has positive longitudinal extent and monotonic width. Under the project's parallel-terminal abstraction, a diverging lane grows to full width before its configured deceleration run, while a merging lane stays full-width through its configured acceleration run and then narrows. The raster boundary moves by at most one block per sampled station and never jumps directly between zero and full width. The one-block sampling bound is a Minecraft continuity rule, not a road-manual dimension. | [WSDOT §§1360.04(4)–(5)](https://wsdot.wa.gov/publications/manuals/fulltext/M22-01/1360.pdf); [Caltrans §504.3(1)(d)–(e)](https://dot.ca.gov/-/media/dot-media/programs/design/documents/chp0500-092923-a11y.pdf); [MUTCD §§3B.07–3B.08](https://mutcd.fhwa.dot.gov/pdfs/11th_Edition/part3.pdf) |
| `FP-01` | **Full footprint reservation:** compute the operating 2D footprint from the union of both mainlines, all eight turning ramps, shoulders, acceleration/deceleration lanes, auxiliary/C-D lanes, tapers, and gore areas. Union that with a configured clear-zone band measured outward from every exposed traveled-way edge. Every rendered road cell and terminal approach must lie inside the result. | [FHWA clear-zone guidance](https://www.fhwa.dot.gov/programadmin/clearzone.cfm); [WSDOT §§1360.03(4), 1360.04(6)–(8)](https://wsdot.wa.gov/publications/manuals/fulltext/M22-01/1360.pdf) |
| `FP-02` | **Building exclusion:** a building's horizontal footprint must not intersect `reservedFootprint2D`. Its 3D volume must also avoid deck, support, vehicle-clearance, and elevated-ramp maintenance envelopes. Test the final compiled geometry, not only the declared nominal radius. | [Caltrans §§309.1(2) and 309.4](https://dot.ca.gov/-/media/dot-media/programs/design/documents/chp0300-092923-a11y.pdf); [WSDOT §1360.03(4)(c)](https://wsdot.wa.gov/publications/manuals/fulltext/M22-01/1360.pdf) |
| `AT-01` | **Atomic fit:** if any required loop, outer ramp, terminal, auxiliary lane, taper, or exclusion envelope cannot fit, reject the full cloverleaf rather than clipping a movement or allowing a building encroachment. Interchange choice is explicitly conditioned on site and right-of-way controls, and cloverleafs require a large area. | [WSDOT §§1360.02(1)–(2)](https://wsdot.wa.gov/publications/manuals/fulltext/M22-01/1360.pdf); [Caltrans §502.1](https://dot.ca.gov/-/media/dot-media/programs/design/documents/chp0500-092923-a11y.pdf) |
| `DET-01` | **Rotation and chunk determinism:** all four cardinal rotations produce the same movement counts, terminal classifications, lane-balance results, and reserved-area measure. Replanning in a different chunk order produces the same footprint and exclusion decision. | Project-specific determinism requirement; run it over the source-backed invariants above. |

Useful test names following the repository's current style are:

- `fullCloverleafHasFourLoopsAndFourOuterRamps`
- `turningMovementsDoNotShareAnUndividedSingleLane`
- `cloverleafRouteIntersectionsAreSeparatedOrDeclared`
- `loopWeaveHasContinuousAuxiliaryCoverage`
- `terminalCrossSectionsPreserveLaneBalance`
- `laneAddsAndDropsUseMonotoneContiguousTapers`
- `reservedFootprintContainsEveryInterchangeCell`
- `buildingsDoNotIntersectReservedInterchangeVolume`
- `insufficientFootprintRejectsTheCompleteInterchange`

## Primary sources

- Federal Highway Administration, *Interchange Safety Analysis Tool (ISAT):
  User Manual*, FHWA-HRT-07-045, June 2007:
  <https://www.fhwa.dot.gov/publications/research/safety/07045/applic.cfm>
- Federal Highway Administration, *Freeway Management and Operations
  Handbook*, chapter 5:
  <https://ops.fhwa.dot.gov/freewaymgmt/publications/frwy_mgmt_handbook/chapter5.htm>
- Federal Highway Administration, *Enhancing Safety and Operations at Complex
  Interchanges With Improved Signing, Markings, and Integrated Geometry*,
  FHWA-HRT-17-048, May 2018:
  <https://www.fhwa.dot.gov/publications/research/safety/17048/010.cfm>
- Federal Highway Administration, *Clear Zone and Horizontal Clearance*:
  <https://www.fhwa.dot.gov/programadmin/clearzone.cfm>
- Federal Highway Administration, *Manual on Uniform Traffic Control Devices*,
  11th edition, part 3, December 2023:
  <https://mutcd.fhwa.dot.gov/pdfs/11th_Edition/part3.pdf>
- Washington State Department of Transportation, *Design Manual M 22-01.23*,
  chapter 1360, September 2024:
  <https://wsdot.wa.gov/publications/manuals/fulltext/M22-01/1360.pdf>
- Washington State Department of Transportation, *Design Manual M 22-01.23*,
  chapter 1600, September 2024:
  <https://wsdot.wa.gov/publications/manuals/fulltext/M22-01/1600.pdf>
- California Department of Transportation, *Highway Design Manual*, chapter
  500, September 29, 2023 accessible edition:
  <https://dot.ca.gov/-/media/dot-media/programs/design/documents/chp0500-092923-a11y.pdf>
- California Department of Transportation, *Highway Design Manual*, chapter
  300, September 29, 2023 accessible edition:
  <https://dot.ca.gov/-/media/dot-media/programs/design/documents/chp0300-092923-a11y.pdf>
