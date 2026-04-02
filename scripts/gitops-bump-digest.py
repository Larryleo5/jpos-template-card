#!/usr/bin/env python3
"""Update gitops/overlays/prod/kustomization.yaml image newName + digest (Kustomize)."""
from __future__ import annotations

import os
import re
import sys
from pathlib import Path

KUSTOMIZE = Path("gitops/overlays/prod/kustomization.yaml")


def main() -> int:
    new_name = os.environ.get("IMAGE_NEW_NAME", "").strip()
    digest = os.environ.get("IMAGE_DIGEST", "").strip()
    if not new_name or not digest:
        print("IMAGE_NEW_NAME and IMAGE_DIGEST must be set", file=sys.stderr)
        return 1
    if not digest.startswith("sha256:"):
        digest = f"sha256:{digest}"
    if not KUSTOMIZE.is_file():
        print(f"Missing {KUSTOMIZE}", file=sys.stderr)
        return 1
    text = KUSTOMIZE.read_text(encoding="utf-8")
    # demo/app image block
    block_re = re.compile(
        r"(\n\s+- name: demo/app\n)(?:\s+newName:.*\n)(?:\s+newTag:.*\n)?(?:\s+digest:.*\n)?",
        re.MULTILINE,
    )
    replacement = (
        f"\\1"
        f'    newName: "{new_name}"\n'
        f"    digest: {digest}\n"
    )
    new_text, n = block_re.subn(replacement, text, count=1)
    if n != 1:
        print("Could not find images entry for name: demo/app", file=sys.stderr)
        return 1
    KUSTOMIZE.write_text(new_text, encoding="utf-8")
    print(f"Updated {KUSTOMIZE} -> {new_name} @ {digest}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
