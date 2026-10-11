import {isValidLocation, nearCity, nearestZoneWithSameTimes, roundLocation} from "./location";
import {ZONE_LOCATIONS} from "./zone-locations";

describe('location', () => {
  const from = new Date(2026, 0, 1);
  const to = new Date(2026, 11, 31, 23, 59, 59);

  it('rounds to a tenth of a degree', () => {
    expect(roundLocation(47.3769)).toBe(47.4);
    expect(roundLocation(-8.5417)).toBe(-8.5);
  });

  it('validates the coordinate ranges', () => {
    expect(isValidLocation(47.4, 8.5)).toBeTrue();
    expect(isValidLocation(-90, 180)).toBeTrue();
    expect(isValidLocation(90.1, 8.5)).toBeFalse();
    expect(isValidLocation(47.4, -180.1)).toBeFalse();
    expect(isValidLocation(null, 8.5)).toBeFalse();
  });

  it('has a reference city for every selectable zone the browser knows', () => {
    for (const [zone, location] of Object.entries(ZONE_LOCATIONS)) {
      expect(isValidLocation(location.lat, location.lon)).withContext(zone).toBeTrue();
      expect(location.city).withContext(zone).not.toEqual('');
    }
  });

  it('keeps the zone when its own reference city is the nearest', () => {
    expect(nearestZoneWithSameTimes('Europe/Zurich', 47.4, 8.5, from, to)).toBe('Europe/Zurich');
  });

  it('switches to the nearest zone with the same times', () => {
    // Paris has the same offsets as Zurich in 2026, and is the nearest reference city to Brest
    expect(nearestZoneWithSameTimes('Europe/Zurich', 48.4, -4.5, from, to)).toBe('Europe/Paris');
  });

  it('does not switch to a nearer zone with other times', () => {
    // London is nearer to Brest than Paris, but one hour behind
    expect(nearestZoneWithSameTimes('Europe/Paris', 48.4, -4.5, from, to)).toBe('Europe/Paris');
  });

  it('does not switch to a zone with the same winter offset but without daylight saving time', () => {
    // Algiers (+01:00 all year) is the nearest reference city to Palma de Mallorca
    expect(nearestZoneWithSameTimes('Europe/Madrid', 39.6, 2.6, from, to)).not.toBe('Africa/Algiers');
  });

  it('does not switch to a zone whose history differs within the time frame', () => {
    // Zurich had no daylight saving time in 1980, Paris had
    expect(nearestZoneWithSameTimes('Europe/Zurich', 48.9, 2.3, new Date(1980, 0, 1), new Date(1980, 11, 31))).not.toBe('Europe/Paris');
  });

  it('names a reference city only when it is near', () => {
    expect(nearCity(47.4, 8.5)).toBe('Zurich');
    expect(nearCity(47.5, 8.7)).toBe('Zurich'); // Winterthur, ~20 km
    expect(nearCity(46.2, 6.1)).toBeNull(); // Geneva
  });
});
