"""Verify migrated QR credentials by signing public dummy content, then discard it."""

import base64
import os
import subprocess
import sys
import tempfile
import zipfile
from pathlib import Path

MARKER = b"QR signing credential continuity check. No application code.\n"
SECRET_NAMES = (
    "ANDROID_KEYSTORE_BASE64",
    "ANDROID_KEYSTORE_PASSWORD",
    "ANDROID_KEY_ALIAS",
    "ANDROID_KEY_PASSWORD",
)


def verify() -> None:
    required_context = {
        "GITHUB_EVENT_NAME": "workflow_dispatch",
        "GITHUB_REPOSITORY": "Majkey25/QR-Code-Scanner-Creator",
        "GITHUB_REPOSITORY_OWNER": "Majkey25",
        "GITHUB_ACTOR": "Majkey25",
        "GITHUB_TRIGGERING_ACTOR": "Majkey25",
        "GITHUB_REF": "refs/heads/main",
    }
    if any(os.environ.get(name) != value for name, value in required_context.items()):
        raise ValueError("Invalid execution context")
    secrets = {name: os.environ.pop(name) for name in SECRET_NAMES}
    if not all(secrets.values()):
        raise ValueError("Missing credential")
    keystore_bytes = base64.b64decode(
        "".join(secrets["ANDROID_KEYSTORE_BASE64"].split()), validate=True
    )
    with tempfile.TemporaryDirectory(
        prefix="qr-signing-check-", dir=os.environ["RUNNER_TEMP"]
    ) as tmp:
        directory = Path(tmp)
        keystore = directory / "existing-keystore.jks"
        keystore.write_bytes(keystore_bytes)
        unsigned = directory / "dummy.jar"
        signed = directory / "dummy-signed.jar"
        with zipfile.ZipFile(unsigned, "w") as archive:
            archive.writestr("migration-proof.txt", MARKER)
        signing_env = os.environ.copy()
        signing_env.update(
            {
                name: secrets[name]
                for name in ("ANDROID_KEYSTORE_PASSWORD", "ANDROID_KEY_PASSWORD")
            }
        )
        subprocess.run(
            [
                "jarsigner",
                "-keystore",
                str(keystore),
                "-storepass:env",
                "ANDROID_KEYSTORE_PASSWORD",
                "-keypass:env",
                "ANDROID_KEY_PASSWORD",
                "-signedjar",
                str(signed),
                str(unsigned),
                secrets["ANDROID_KEY_ALIAS"],
            ],
            env=signing_env,
            check=True,
            timeout=60,
            stdout=subprocess.DEVNULL,
            stderr=subprocess.DEVNULL,
        )
        subprocess.run(
            ["jarsigner", "-verify", str(signed)],
            check=True,
            timeout=60,
            stdout=subprocess.DEVNULL,
            stderr=subprocess.DEVNULL,
        )
        subprocess.run(
            [
                "java",
                str(Path(__file__).with_name("VerifySigningJar.java")),
                str(signed),
            ],
            check=True,
            timeout=60,
            stdout=subprocess.DEVNULL,
            stderr=subprocess.DEVNULL,
        )


def main() -> int:
    os.umask(0o077)
    try:
        verify()
    except Exception:
        print("Signing credential verification failed.", file=sys.stderr)
        return 1
    print(
        "Dummy JAR verified with original certificate 3d83947a2d2b3ae2016a1b7026cd938151a9beaeeb82e8a1c088e7bcf9d8459b."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
