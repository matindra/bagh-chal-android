#!/usr/bin/env bash
# Converts design/sounds/* (source files, kept) to app/src/main/res/raw/*.ogg:
# mono, 44.1 kHz, leading/trailing silence trimmed, loudness-normalised, OGG Vorbis quality 4.
# Needs ffmpeg and oggenc (brew install ffmpeg vorbis-tools); this ffmpeg build lacks libvorbis,
# so encoding is done by oggenc, the reference libvorbis encoder.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
SRC="$ROOT/design/sounds"
OUT="$ROOT/app/src/main/res/raw"
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT

TARGET_LUFS=-18         # every sound ends up at about this loudness
PEAK_LIMIT=0.891        # -1 dBFS: only peaks pushed past this by the gain are limited
SILENCE=-50dB           # quieter than this counts as silence when trimming
QUALITY=4

# source file : resource name
SOUNDS="
move.mp3:move
tiger_capture.wav:tiger_capture
goat_capture.wav:goat_capture
play_win.wav:player_win
player_lose.mp3:player_lose
"

TRIM="silenceremove=start_periods=1:start_threshold=$SILENCE:start_silence=0.005"
TRIM="$TRIM,areverse,$TRIM,areverse"

# Integrated loudness of the clip played back-to-back for 8 s: EBU R128 needs at least 400 ms,
# and some effects are shorter than that.
loudness() {
  ffmpeg -hide_banner -nostats -stream_loop -1 -i "$1" -t 8 -af ebur128 -f null - 2>&1 |
    awk '/^ +I:/ { value = $2 } END { print value }'
}

mkdir -p "$OUT"
for entry in $SOUNDS; do
  source="${entry%%:*}"
  name="${entry##*:}"
  trimmed="$TMP/$name.trim.wav"
  normalised="$TMP/$name.wav"

  ffmpeg -hide_banner -loglevel error -y -i "$SRC/$source" -ac 1 -ar 44100 -af "$TRIM" "$trimmed"
  gain=$(awk -v target="$TARGET_LUFS" -v measured="$(loudness "$trimmed")" 'BEGIN { printf "%.2f", target - measured }')
  ffmpeg -hide_banner -loglevel error -y -i "$trimmed" \
    -af "volume=${gain}dB,alimiter=limit=$PEAK_LIMIT:level=false:latency=true:attack=1:release=50" \
    -ar 44100 -ac 1 -map_metadata -1 -fflags +bitexact "$normalised"
  oggenc --quiet --serial 1 -q "$QUALITY" -o "$OUT/$name.ogg" "$normalised"  # fixed serial: reproducible output

  duration=$(ffprobe -v error -show_entries format=duration -of csv=p=0 "$OUT/$name.ogg")
  printf '%-22s -> raw/%-18s %6.3fs  gain %+6.2f dB  now %s LUFS\n' \
    "$source" "$name.ogg" "$duration" "$gain" "$(loudness "$normalised")"
done
