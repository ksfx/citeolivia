#!/usr/bin/env bash
# Builds Tailwind CSS + daisyUI with the standalone Tailwind binary (no Node required).
# Downloads binary and daisyUI plugin on first run into .tools/ (git-ignored).
# Usage: scripts/tailwind.sh            -> one-off minified build (used by Maven)
#        scripts/tailwind.sh --watch    -> rebuild on template changes (dev)
set -euo pipefail

BASEDIR="$(cd "$(dirname "$0")/.." && pwd)"
TAILWIND_VERSION="${TAILWIND_VERSION:-4.3.5}"
DAISYUI_VERSION="${DAISYUI_VERSION:-5.8.1}"
TOOLS="$BASEDIR/.tools"
TW="$TOOLS/tailwindcss-$TAILWIND_VERSION"
DAISY_DIR="$BASEDIR/src/main/tailwind/vendor"
IN="$BASEDIR/src/main/tailwind/app.css"
OUT="${CSS_OUT:-$BASEDIR/target/classes/static/css/app.css}"

case "$(uname -s)-$(uname -m)" in
  Linux-x86_64)  ASSET=tailwindcss-linux-x64 ;;
  Linux-aarch64) ASSET=tailwindcss-linux-arm64 ;;
  Darwin-arm64)  ASSET=tailwindcss-macos-arm64 ;;
  Darwin-x86_64) ASSET=tailwindcss-macos-x64 ;;
  *) echo "Unsupported platform $(uname -s)-$(uname -m)" >&2; exit 1 ;;
esac

mkdir -p "$TOOLS" "$DAISY_DIR" "$(dirname "$OUT")"

if [[ ! -x "$TW" ]]; then
  echo "Downloading Tailwind CSS $TAILWIND_VERSION ($ASSET)"
  curl -fsSL -o "$TW.tmp" "https://github.com/tailwindlabs/tailwindcss/releases/download/v$TAILWIND_VERSION/$ASSET"
  chmod +x "$TW.tmp" && mv "$TW.tmp" "$TW"
fi

if [[ ! -f "$DAISY_DIR/.version-$DAISYUI_VERSION" ]]; then
  echo "Downloading daisyUI $DAISYUI_VERSION"
  rm -f "$DAISY_DIR"/*.mjs "$DAISY_DIR"/.version-*
  for f in daisyui.mjs daisyui-theme.mjs; do
    curl -fsSL -o "$DAISY_DIR/$f" "https://github.com/saadeghi/daisyui/releases/download/v$DAISYUI_VERSION/$f"
  done
  touch "$DAISY_DIR/.version-$DAISYUI_VERSION"
fi

if [[ "${1:-}" == "--watch" ]]; then
  exec "$TW" -i "$IN" -o "$BASEDIR/src/main/resources/static/css/app.css" --watch
fi
"$TW" -i "$IN" -o "$OUT" --minify
