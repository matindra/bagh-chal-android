#!/usr/bin/env bash
# Builds store/trailer.mp4 (1920x1080, 30 fps, ~31 s): renders scripts/trailer/trailer.html frame by frame
# in headless Chrome, then mixes the app's home music (whole video) and sound effects from app/src/main/res/raw.
# Needs Google Chrome, Node 22+ and ffmpeg. Sound cue times must match the animation in trailer.html.
set -euo pipefail
cd "$(dirname "$0")/../.."
RAW=app/src/main/res/raw
WORK=$(mktemp -d)
trap 'rm -rf "$WORK"' EXIT

node scripts/trailer/render.mjs "$WORK/frames" 30

# Start times (ms) of the move sound: tiger drops, goat step, difficulty pills, code digits.
MOVES="7000 7350 7700 8050 15750 19200 19900 20600 23300 23520 23740 23960"
inputs=(-i "$RAW/music_home.ogg" -i "$RAW/tiger_capture.ogg" -i "$RAW/goat_capture.ogg" -i "$RAW/player_win.ogg")
filters="[0]atrim=0:31,afade=t=in:d=0.8,volume=0.8[home];"
filters+="[1]adelay=12450|12450,volume=1.2[cap];"
filters+="[2]adelay=16050|16050,volume=1.1[trap];"
filters+="[3]adelay=27900|27900[win];"
mix="[home][cap][trap][win]"
n=4; i=0
for d in $MOVES; do
  inputs+=(-i "$RAW/move.ogg")
  filters+="[$((n + i))]adelay=$d|$d,volume=0.9[m$i];"
  mix+="[m$i]"; i=$((i + 1))
done
filters+="${mix}amix=inputs=$((n + i)):normalize=0,afade=t=out:st=29.8:d=1.2,atrim=0:31,loudnorm=I=-16:TP=-1.5:LRA=11[aout]"

ffmpeg -v error -y "${inputs[@]}" -filter_complex "$filters" -map "[aout]" -ar 48000 -ac 2 "$WORK/audio.wav"

mkdir -p store
ffmpeg -v error -y -framerate 30 -i "$WORK/frames/f%05d.png" -i "$WORK/audio.wav" \
  -c:v libx264 -preset slow -crf 18 -pix_fmt yuv420p -c:a aac -b:a 192k -shortest -movflags +faststart \
  store/trailer.mp4
echo "Wrote store/trailer.mp4"

# YouTube thumbnail (1280x720, 24-bit PNG) from thumbnail.html.
"/Applications/Google Chrome.app/Contents/MacOS/Google Chrome" --headless=new --disable-gpu --hide-scrollbars \
  --allow-file-access-from-files --force-device-scale-factor=1 --window-size=1280,720 --virtual-time-budget=5000 \
  --user-data-dir="$WORK/chrome" --screenshot="$WORK/thumb.png" "file://$PWD/scripts/trailer/thumbnail.html" 2>/dev/null
ffmpeg -v error -y -i "$WORK/thumb.png" -pix_fmt rgb24 store/youtube-thumbnail.png
echo "Wrote store/youtube-thumbnail.png"
