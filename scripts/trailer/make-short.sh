#!/usr/bin/env bash
# Builds store/short.mp4: a 20.5 s vertical YouTube Short (1080x1920, 30 fps) from scripts/trailer/short.html,
# with the app's home music and sound effects. Needs Google Chrome, Node 22+ and ffmpeg.
# Sound cue times must match the animation in short.html.
set -euo pipefail
cd "$(dirname "$0")/../.."
RAW=app/src/main/res/raw
WORK=$(mktemp -d)
trap 'rm -rf "$WORK"' EXIT

PAGE=short.html WIDTH=1080 HEIGHT=1920 node scripts/trailer/render.mjs "$WORK/frames" 30

# Start times (ms) of the move sound: tiger drops, goat step, mode rows.
MOVES="3100 3400 3700 4000 11150 13850 14050 14250 14450"
inputs=(-i "$RAW/music_home.ogg" -i "$RAW/tiger_capture.ogg" -i "$RAW/goat_capture.ogg" -i "$RAW/player_win.ogg")
filters="[0]atrim=0:20.5,afade=t=in:d=0.4,volume=0.8[home];"
filters+="[1]adelay=7550|7550,volume=1.2[cap];"
filters+="[2]adelay=11450|11450,volume=1.1[trap];"
filters+="[3]adelay=17000|17000[win];"
mix="[home][cap][trap][win]"
n=4; i=0
for d in $MOVES; do
  inputs+=(-i "$RAW/move.ogg")
  filters+="[$((n + i))]adelay=$d|$d,volume=0.9[m$i];"
  mix+="[m$i]"; i=$((i + 1))
done
filters+="${mix}amix=inputs=$((n + i)):normalize=0,afade=t=out:st=19.6:d=0.9,atrim=0:20.5,loudnorm=I=-14:TP=-1.5:LRA=11[aout]"

ffmpeg -v error -y "${inputs[@]}" -filter_complex "$filters" -map "[aout]" -ar 48000 -ac 2 "$WORK/audio.wav"

mkdir -p store
ffmpeg -v error -y -framerate 30 -i "$WORK/frames/f%05d.png" -i "$WORK/audio.wav" \
  -c:v libx264 -preset slow -crf 18 -pix_fmt yuv420p -c:a aac -b:a 192k -shortest -movflags +faststart \
  store/short.mp4
echo "Wrote store/short.mp4"

# Shorts cover image (1080x1920, 24-bit PNG) from thumbnail-short.html.
"/Applications/Google Chrome.app/Contents/MacOS/Google Chrome" --headless=new --disable-gpu --hide-scrollbars \
  --allow-file-access-from-files --force-device-scale-factor=1 --window-size=1080,1920 --virtual-time-budget=5000 \
  --user-data-dir="$WORK/chrome" --screenshot="$WORK/thumb.png" "file://$PWD/scripts/trailer/thumbnail-short.html" 2>/dev/null
ffmpeg -v error -y -i "$WORK/thumb.png" -pix_fmt rgb24 store/short-thumbnail.png
echo "Wrote store/short-thumbnail.png"
