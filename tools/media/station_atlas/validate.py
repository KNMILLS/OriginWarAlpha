"""Verify station atlas source pixels, logical anchors and mechanical bindings."""
from pathlib import Path
import sys

sys.path.insert(0, str(Path(__file__).resolve().parents[1] / "officer_atlas"))
from validate import validate
from pack import ASSETS

if __name__ == "__main__":
    validate(ASSETS / "art/production/station", "station")
