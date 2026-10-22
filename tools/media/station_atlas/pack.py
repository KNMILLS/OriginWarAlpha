"""Pack original station sprites through the same lossless origin-preserving exporter."""
from pathlib import Path
import sys

sys.path.insert(0, str(Path(__file__).resolve().parents[1] / "officer_atlas"))
from pack import ASSETS, pack

if __name__ == "__main__":
    pack(ASSETS / "art/production/station", "station")
