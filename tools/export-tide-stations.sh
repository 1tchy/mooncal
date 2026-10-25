#!/usr/bin/env bash
#
# Exports the tide stations of the Slackwater database into conf/logics/calculation/tides/tide-stations.json.gz
# Run from the repository root when updating the data: tools/export-tide-stations.sh
# Needs npm and node, as only the database's own module reads its FlatBuffers file, and jq.

set -euo pipefail

DATABASE='@slackwater/database@1.0.0-beta.20261017'
MIN_MEAN_RANGE_METRES=0.3
TARGET='conf/logics/calculation/tides/tide-stations.json.gz'

DIR="$(mktemp -d "${TMPDIR:-/tmp}/slackwater-XXXXXX")"
trap 'rm -rf "$DIR"' EXIT

npm install --prefix "$DIR" --no-audit --no-fund --no-save "$DATABASE"
# the fields used below; those of reference stations only, as a subordinate one has its reference's
node --input-type=module -e '
const {stations} = await import(process.argv[1]);
console.log(JSON.stringify(stations.map(s => ({
  id: s.id, name: s.name, locality: s.locality, kind: s.kind, type: s.type, latitude: s.latitude, longitude: s.longitude,
  attribution: s.attribution, commercialUse: s.license?.commercial_use, offsets: s.offsets,
  ...(s.type === "reference" && {constituents: s.harmonic_constituents, datums: s.datums}),
}))));
' "$DIR/node_modules/@slackwater/database/dist/node/index.js" > "$DIR/stations.json"

jq -r --arg database "$DATABASE" --argjson minMeanRange "$MIN_MEAN_RANGE_METRES" '
def round($decimals): pow(10; $decimals) as $factor | (. * $factor + 0.5 | floor) / $factor;
def present: . != null and . != "";
def ownMeanRange:
  if .datums.MHW != null and .datums.MLW != null then .datums.MHW - .datums.MLW
  else ((.constituents // []) | map(select(.name == "M2")) | first | if . then 2 * .amplitude else 0 end)
  end;
# accented letters as their base letter, like Unicode NFD without the marks
def folds: {
  "a": "ÀÁÂÃÄÅàáâãäåĀāĂăĄąǍǎǞǟǠǡǺǻȀȁȂȃȦȧḀḁẠạẢảẤấẦầẨẩẪẫẬậẮắẰằẲẳẴẵẶặ",
  "b": "ḂḃḄḅḆḇ",
  "c": "ÇçĆćĈĉĊċČčḈḉ",
  "d": "ĎďḊḋḌḍḎḏḐḑḒḓ",
  "e": "ÈÉÊËèéêëĒēĔĕĖėĘęĚěȄȅȆȇȨȩḔḕḖḗḘḙḚḛḜḝẸẹẺẻẼẽẾếỀềỂểỄễỆệ",
  "f": "Ḟḟ",
  "g": "ĜĝĞğĠġĢģǦǧǴǵḠḡ",
  "h": "ĤĥȞȟḢḣḤḥḦḧḨḩḪḫẖ",
  "i": "ÌÍÎÏìíîïĨĩĪīĬĭĮįİǏǐȈȉȊȋḬḭḮḯỈỉỊị",
  "j": "Ĵĵǰ",
  "k": "ĶķǨǩḰḱḲḳḴḵ",
  "l": "ĹĺĻļĽľḶḷḸḹḺḻḼḽ",
  "m": "ḾḿṀṁṂṃ",
  "n": "ÑñŃńŅņŇňǸǹṄṅṆṇṈṉṊṋ",
  "o": "ÒÓÔÕÖòóôõöŌōŎŏŐőƠơǑǒǪǫǬǭȌȍȎȏȪȫȬȭȮȯȰȱṌṍṎṏṐṑṒṓỌọỎỏỐốỒồỔổỖỗỘộỚớỜờỞởỠỡỢợ",
  "p": "ṔṕṖṗ",
  "r": "ŔŕŖŗŘřȐȑȒȓṘṙṚṛṜṝṞṟ",
  "s": "ŚśŜŝŞşŠšȘșṠṡṢṣṤṥṦṧṨṩ",
  "t": "ŢţŤťȚțṪṫṬṭṮṯṰṱẗ",
  "u": "ÙÚÛÜùúûüŨũŪūŬŭŮůŰűŲųƯưǓǔǕǖǗǘǙǚǛǜȔȕȖȗṲṳṴṵṶṷṸṹṺṻỤụỦủỨứỪừỬửỮữỰự",
  "v": "ṼṽṾṿ",
  "w": "ŴŵẀẁẂẃẄẅẆẇẈẉẘ",
  "x": "ẊẋẌẍ",
  "y": "ÝýÿŶŷŸȲȳẎẏẙỲỳỴỵỶỷỸỹ",
  "z": "ŹźŻżŽžẐẑẒẓẔẕ"
} | [to_entries[] | .key as $base | .value | explode[] | {key: ([.] | implode), value: $base}] | from_entries;
# Some names lost their umlauts (Hrnum for Hörnum), others are technical (Bhvalterleuchtturm), so the locality helps
def stationLabel($folds):
  def simple: explode | map([.] | implode | if test("^[a-zA-Z]$") then ascii_downcase else $folds[.] // empty end) | join("");
  .locality as $locality
  | (if ($locality | present) and ($locality | explode | map(select(. < 128)) | implode) == .name then $locality else .name end) as $name
  | if ($locality | present | not) or (($name | simple) | contains($locality | simple)) or (($locality | simple) | contains($name | simple))
    then $name else "\($name), \($locality)" end;

folds as $folds
| (map(select(.kind != "tide")) | length) as $notTide
| (map(select(.kind == "tide" and .commercialUse == false)) | length) as $nonCommercial
| map(select(.kind == "tide" and .commercialUse != false)) as $candidates
| ($candidates | INDEX(.id)) as $byId
| def isReference: . != null and .type == "reference" and (.constituents // [] | length) > 0;
  def predictable: isReference or (.type == "subordinate" and ($byId[.offsets.reference // ""] | isReference));
  def meanRange:
    if .type == "reference" then ownMeanRange
    else $byId[.offsets.reference] as $reference | .offsets.height as {high: $high, low: $low, type: $type}
      | if $reference.datums.MHW == null or $reference.datums.MLW == null then
          ($reference | ownMeanRange) as $range | if $type == "ratio" then $range * ($high + $low) / 2 else $range + $high - $low end
        elif $type == "ratio" then $reference.datums.MHW * $high - $reference.datums.MLW * $low
        else ($reference.datums.MHW + $high) - ($reference.datums.MLW + $low)
        end
    end;
  ($candidates | map(select(predictable | not)) | length) as $notPredictableAtFirst
| ($candidates | map(select(predictable)) | map(. + {meanRange: meanRange})) as $predictable
| ($predictable | map(select(.meanRange < $minMeanRange)) | length) as $barelyAnyTide
| ($predictable | map(select(.meanRange >= $minMeanRange) | del(.meanRange))) as $tidal
| (reduce $tidal[] as $s ({attributions: [], stations: []};
    (.attributions | index([$s.attribution])) as $known
    | (if $known then . else .attributions += [$s.attribution] end)
    | ({id: $s.id, label: ($s | stationLabel($folds)), lat: ($s.latitude | round(5)), lon: ($s.longitude | round(5)),
        attribution: ($known // (.attributions | length - 1))}
       + if $s.type == "reference" then
           {constituents: [$s.constituents[] | [.name, (.amplitude | round(4)), (.phase | round(2))]]}
         else
           {reference: $s.offsets.reference, highMinutes: $s.offsets.time.high, lowMinutes: $s.offsets.time.low,
            heightType: $s.offsets.height.type, heightHigh: ($s.offsets.height.high | round(4)), heightLow: ($s.offsets.height.low | round(4))}
         end) as $station
    | .stations += [$station])) as $export
  # subordinate stations whose reference was removed for its range are not predictable either
| ($export.stations | map({key: .id, value: true}) | from_entries) as $exported
| ($export.stations | map(select(.reference == null or $exported[.reference]))) as $result
| ({database: $database, attributions: $export.attributions, stations: $result} | tojson),
  "exported \($result | length) stations (\($result | map(select(.reference == null)) | length) reference, \($result | map(select(.reference != null)) | length) subordinate)",
  "removed: not tide \($notTide), non-commercial \($nonCommercial), not predictable \($notPredictableAtFirst + ($export.stations | length) - ($result | length)), barely any tide \($barelyAnyTide)",
  "attributions:",
  ($export.attributions[] | "  \(.)")
' "$DIR/stations.json" > "$DIR/export.txt"

mkdir -p "$(dirname "$TARGET")"
head -n 1 "$DIR/export.txt" | tr -d '\n' > "$DIR/stations-export.json"
gzip -9 -n -c "$DIR/stations-export.json" > "$TARGET"
tail -n +2 "$DIR/export.txt"
echo "$TARGET: $(wc -c < "$TARGET" | awk '{printf "%.2f", $1 / 1e6}') MB ($(wc -c < "$DIR/stations-export.json" | awk '{printf "%.2f", $1 / 1e6}') MB uncompressed)"
