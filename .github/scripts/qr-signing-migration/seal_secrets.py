"""Seal existing QR repository secrets for the owner's verified environment key."""

import argparse
import base64
import hashlib
import json
import os
import sys
from pathlib import Path

from nacl.public import PublicKey, SealedBox

REPOSITORY = "Majkey25/QR-Code-Scanner-Creator"
SECRET_NAMES = {
    "ANDROID_KEYSTORE_BASE64": "QR_SIGNING_KEYSTORE_BASE64",
    "ANDROID_KEYSTORE_PASSWORD": "QR_SIGNING_KEYSTORE_PASSWORD",
    "ANDROID_KEY_ALIAS": "QR_SIGNING_KEY_ALIAS",
    "ANDROID_KEY_PASSWORD": "QR_SIGNING_KEY_PASSWORD",
}


def seal(output: Path) -> None:
    required_context = {
        "GITHUB_EVENT_NAME": "workflow_dispatch",
        "GITHUB_REPOSITORY": REPOSITORY,
        "GITHUB_REPOSITORY_OWNER": "Majkey25",
        "GITHUB_ACTOR": "Majkey25",
        "GITHUB_TRIGGERING_ACTOR": "Majkey25",
        "GITHUB_REF": "refs/heads/main",
    }
    if any(os.environ.get(name) != value for name, value in required_context.items()):
        raise ValueError("Invalid execution context")
    key_bytes = base64.b64decode(os.environ["MIGRATION_PUBLIC_KEY"], validate=True)
    if len(key_bytes) != 32:
        raise ValueError("Invalid public key")
    key_id = os.environ["MIGRATION_KEY_ID"]
    # Keep the opaque ID unchanged; reject empty, oversized or control-bearing input.
    if not 1 <= len(key_id) <= 256 or any(
        not 33 <= ord(char) <= 126 for char in key_id
    ):
        raise ValueError("Invalid key ID")
    box = SealedBox(PublicKey(key_bytes))
    payloads: dict[str, dict[str, str]] = {}
    for source, target in SECRET_NAMES.items():
        # Never strip, normalize newlines, decode base64, or pass through a shell.
        plaintext = os.environ[source].encode("utf-8")
        if not 1 <= len(plaintext) <= 48 * 1024:
            raise ValueError("Invalid secret size")
        payloads[target] = {
            "encrypted_value": base64.b64encode(box.encrypt(plaintext)).decode("ascii"),
            "key_id": key_id,
        }
    metadata = {
        "repository": REPOSITORY,
        "environment": "android-signing",
        "key_id": key_id,
        "public_key_sha256": hashlib.sha256(key_bytes).hexdigest(),
        "workflow_sha": os.environ["GITHUB_SHA"],
        "run_id": os.environ["GITHUB_RUN_ID"],
        "run_attempt": os.environ["GITHUB_RUN_ATTEMPT"],
        "secret_names": SECRET_NAMES,
    }
    output.mkdir(mode=0o700, parents=False, exist_ok=False)
    for target, payload in payloads.items():
        with (output / f"{target}.json").open(
            "x", encoding="utf-8", newline="\n"
        ) as file:
            json.dump(payload, file)
            file.write("\n")
    with (output / "metadata.json").open("x", encoding="utf-8", newline="\n") as file:
        json.dump(metadata, file)
        file.write("\n")


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--output", required=True, type=Path)
    args = parser.parse_args()
    os.umask(0o077)
    try:
        seal(args.output)
    except Exception:
        # Error values and tracebacks can contain secrets. Fail without their details.
        print("Sealing failed.", file=sys.stderr)
        return 1
    print(
        "Sealed four values. Only ciphertext payloads and public metadata were written."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
