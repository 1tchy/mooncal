import {ZONE_LOCATIONS} from "./zone-locations";

const DAY_MS = 24 * 60 * 60 * 1000;
const MINUTE_MS = 60 * 1000;

/**
 * Coordinates are rounded to 0.1° (~11 km): precise enough for moonrise/-set to the minute,
 * coarse enough not to reveal the user's address.
 */
export function roundLocation(degrees: number): number;
export function roundLocation(degrees: number | null): number | null;
export function roundLocation(degrees: number | null) {
  return degrees === null ? null : Math.round(degrees * 10) / 10;
}

/** The reference city's location of the zone, or no location for an unknown zone. */
export function referenceLocation(zone: string) {
  return {lat: ZONE_LOCATIONS[zone]?.lat ?? null, lon: ZONE_LOCATIONS[zone]?.lon ?? null};
}

export function isValidLocation(lat: number | null, lon: number | null) {
  return lat !== null && lon !== null && Number.isFinite(lat) && Number.isFinite(lon)
    && Math.abs(lat) <= 90 && Math.abs(lon) <= 180;
}

function distanceKm(lat1: number, lon1: number, lat2: number, lon2: number) {
  const rad = Math.PI / 180;
  const a = Math.sin((lat2 - lat1) * rad / 2) ** 2
    + Math.cos(lat1 * rad) * Math.cos(lat2 * rad) * Math.sin((lon2 - lon1) * rad / 2) ** 2;
  return 2 * 6371 * Math.asin(Math.sqrt(a));
}

/** Reference cities further away than this are not shown as being near a location. */
const NEAR_CITY_KM = 25;

/** The reference city nearest to the location, if there is one within {@link NEAR_CITY_KM}. */
export function nearCity(lat: number, lon: number) {
  let nearest: string | null = null;
  let nearestDistance = NEAR_CITY_KM;
  for (const location of Object.values(ZONE_LOCATIONS)) {
    const distance = distanceKm(lat, lon, location.lat, location.lon);
    if (distance <= nearestDistance) {
      nearest = location.city;
      nearestDistance = distance;
    }
  }
  return nearest;
}

const offsetFormats = new Map<string, Intl.DateTimeFormat | null>();

/** UTC offset like "GMT+02:00" of the zone at the given time, or null if the browser doesn't know the zone. */
function offsetAt(zone: string, time: number) {
  if (!offsetFormats.has(zone)) {
    try {
      offsetFormats.set(zone, new Intl.DateTimeFormat('en-US', {timeZone: zone, timeZoneName: 'longOffset'}));
    } catch {
      offsetFormats.set(zone, null);
    }
  }
  const format = offsetFormats.get(zone);
  return format ? format.formatToParts(time).find(part => part.type === 'timeZoneName')!.value : null;
}

/** The zone's offset at noon (UTC) of every day from {@code from} (a noon) until a day after {@code to}. */
function dailyOffsets(zone: string, from: number, to: number) {
  const result: (string | null)[] = [];
  for (let day = from; day <= to + DAY_MS; day += DAY_MS) {
    result.push(offsetAt(zone, day));
  }
  return result;
}

/** Instants (to the minute) at which the offset changes, given the daily offsets from {@code from}. */
function transitions(zone: string, from: number, offsets: (string | null)[]) {
  const result: number[] = [];
  for (let i = 1; i < offsets.length; i++) {
    const previous = offsets[i - 1];
    if (offsets[i] !== previous) {
      let lo = from + (i - 1) * DAY_MS, hi = from + i * DAY_MS;
      while (hi - lo > MINUTE_MS) {
        const mid = lo + Math.floor((hi - lo) / 2 / MINUTE_MS) * MINUTE_MS;
        if (offsetAt(zone, mid) === previous) {
          lo = mid;
        } else {
          hi = mid;
        }
      }
      result.push(hi);
    }
  }
  return result;
}

/**
 * Whether both zones show the same local times everywhere in [from, to]: same offset at noon (UTC) of every day
 * and around every offset change of the first zone (which catches changes on the same day but at another hour).
 */
function sameOffsets(zone: string, other: string, from: number, zoneOffsets: (string | null)[], zoneTransitions: number[]) {
  if (offsetAt(other, from) === null) {
    return false;
  }
  for (const time of zoneTransitions) {
    if (offsetAt(zone, time) !== offsetAt(other, time) || offsetAt(zone, time - MINUTE_MS) !== offsetAt(other, time - MINUTE_MS)) {
      return false;
    }
  }
  return zoneOffsets.every((offset, i) => offset === offsetAt(other, from + i * DAY_MS));
}

/**
 * Of all selectable zones showing the same local times as {@code zone} in [from, to], the one whose reference city
 * is nearest to the given location. Changing to it never changes any displayed time.
 */
export function nearestZoneWithSameTimes(zone: string, lat: number, lon: number, from: Date, to: Date) {
  const start = Date.UTC(from.getUTCFullYear(), from.getUTCMonth(), from.getUTCDate(), 12);
  const end = to.getTime();
  const candidates = Object.entries(ZONE_LOCATIONS)
    .map(([candidate, location]) => ({candidate, distance: distanceKm(lat, lon, location.lat, location.lon)}))
    // the current zone wins on equal distance (e.g. aliases sharing the same reference city)
    .sort((a, b) => a.distance - b.distance || Number(b.candidate === zone) - Number(a.candidate === zone));
  const zoneOffsets = dailyOffsets(zone, start, end);
  const zoneTransitions = transitions(zone, start, zoneOffsets);
  for (const {candidate} of candidates) {
    if (candidate === zone || sameOffsets(zone, candidate, start, zoneOffsets, zoneTransitions)) {
      return candidate;
    }
  }
  return zone;
}
