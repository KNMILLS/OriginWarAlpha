"""Pack mutually exclusive equipment sets without increasing active texture residency.

All generated manifests reference original exported PNGs; no image is repainted.
The complete authoring manifest stays at officer/clips.json.
"""
import copy
import json
from pathlib import Path
from pack import FOLDER, pack, write


def loadouts():
    sources = [FOLDER / "clips.json"]
    sample = FOLDER / "samples/rifle-heavy/clips.json"
    if sample.is_file():
        sources.append(sample)
    definitions, bindings = {}, []
    for source in sources:
        data = json.loads(source.read_text(encoding="utf-8-sig"))
        for clip in data["clips"]:
            if clip["id"] in definitions:
                raise ValueError("Duplicate loadout source clip " + clip["id"])
            definitions[clip["id"]] = clip
        bindings.extend(data["bindings"])
    groups = sorted({(b["weapon"], b["armor"]) for b in bindings})
    result = {}
    for weapon, armor in groups:
        folder = FOLDER / "loadouts" / f"{weapon}-{armor}"
        folder.mkdir(parents=True, exist_ok=True)
        chosen = [b for b in bindings if (b["weapon"], b["armor"]) == (weapon, armor)]
        ids = {b["clip"] for b in chosen}
        data = dict(version=1, clips=[copy.deepcopy(c) for k,c in definitions.items() if k in ids], bindings=chosen,
                    authoringSources=[p.relative_to(FOLDER).as_posix() for p in sources],
                    residency="one active equipment set; source frames retained unmodified")
        write(folder / "clips.json", data)
        result[f"{weapon}-{armor}"] = pack(folder, "officer")
    # The ordinary entry point is a byte-identical manifest, not a second set of pages.
    primary = FOLDER / "loadouts/pistol-suit/atlas.json"
    if not primary.is_file():
        raise ValueError("Missing primary pistol/suit loadout")
    (FOLDER / "atlas.json").write_bytes(primary.read_bytes())
    all_bound = {(b["weapon"],b["armor"],b["action"],b["facing"]): b["clip"] for b in bindings}
    coverage = []
    actions = sorted({b["action"] for b in bindings} | {"idle","start","walk","stop","ready","aim","fire","work","hit","death"})
    for weapon in ("unarmed","pistol","rifle","scattergun"):
        for armor in ("suit","light","heavy"):
            for action in actions:
                for face in ("north","east","south","west"):
                    if action == "turn_from_" + face:
                        continue
                    clip = all_bound.get((weapon,armor,action,face),"")
                    applicable = not (weapon == "unarmed" and action in ("fire","aim"))
                    coverage.append(dict(weapon=weapon,armor=armor,action=action,facing=face,clip=clip,
                        status="not-applicable" if not applicable else "candidate" if clip else "missing"))
    write(FOLDER / "coverage.json",dict(status="candidate-not-artistic-acceptance",bindings=coverage))
    write(FOLDER / "loadouts/index.json", dict(version=1,sets=result,activeSetLimit=1))
    return result


if __name__ == "__main__":
    print(json.dumps(loadouts(), indent=2))
