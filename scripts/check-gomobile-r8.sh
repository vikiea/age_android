#!/usr/bin/env bash
set -euo pipefail

mapping_file="${1:-app/build/outputs/mapping/release/mapping.txt}"
apk_file="${2:-app/build/outputs/apk/release/app-universal-release.apk}"

if [[ ! -f "$mapping_file" ]]; then
  echo "Missing R8 mapping file: $mapping_file" >&2
  exit 1
fi

if [[ ! -f "$apk_file" ]]; then
  echo "Missing release APK: $apk_file" >&2
  exit 1
fi

fail() {
  echo "gomobile R8 check failed: $*" >&2
  exit 1
}

require_kept_class() {
  local class_name="$1"
  if ! grep -Fqx "$class_name -> $class_name:" "$mapping_file"; then
    fail "$class_name must keep its exact Java name for libgojni native lookups"
  fi
}

require_kept_method() {
  local class_name="$1"
  local method_name="$2"
  local method_pattern="$3"
  local block
  block="$(awk -v header="$class_name -> $class_name:" '
    $0 == header { in_block = 1; print; next }
    in_block && /^[^[:space:]].* -> / { exit }
    in_block { print }
  ' "$mapping_file")"

  if ! grep -Eq "$method_pattern(:[0-9]+)*[[:space:]]*->[[:space:]]*$method_name$" <<<"$block"; then
    fail "$class_name.$method_name must keep its exact Java method name"
  fi
}

require_kept_class "go.Seq"
require_kept_class 'go.Seq$GoObject'
require_kept_class 'go.Seq$GoRef'
require_kept_class 'go.Seq$GoRefQueue'
require_kept_class 'go.Seq$GoRefQueue$1'
require_kept_class 'go.Seq$Ref'
require_kept_class 'go.Seq$RefMap'
require_kept_class 'go.Seq$RefTracker'

require_kept_method "go.Seq" "getRef" 'getRef\(int\)'
require_kept_method "go.Seq" "trackGoRef" 'trackGoRef\(int,go\.Seq\$GoObject\)'

if grep -Eq '^go\.Seq(\$[^[:space:]]+)? -> R8\$\$REMOVED' "$mapping_file"; then
  fail "go.Seq runtime classes must not be removed"
fi

dex_strings="$(for dex in $(unzip -Z1 "$apk_file" | grep -E '^classes.*\.dex$'); do unzip -p "$apk_file" "$dex" | strings; done)"

for symbol in 'Lgo/Seq;' 'Lgo/Seq$GoRef;' 'Lgo/Seq$RefTracker;' getRef trackGoRef incGoRef; do
  if ! grep -Fqx "$symbol" <<<"$dex_strings"; then
    fail "$symbol must be present in release dex"
  fi
done

echo "gomobile R8 check passed: go.Seq runtime names are preserved"
