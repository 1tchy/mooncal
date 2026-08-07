import {Injectable} from '@angular/core';

type options = { [key: string]: boolean };

@Injectable({providedIn: 'root'})
export class MainSettingsService {
  phases: options = {
    full: true,
    'new': false,
    quarter: false,
    daily: false
  };
  style = "withDescription";
  hemisphere: 'northern' | 'southern' = 'northern';
  // The Home page and the Garden-calendar page keep independent event selections, each with its own
  // defaults, so that events hidden on one page (garden on Home, moonlanding on Garden) can never leak
  // into the other page's calendar.
  events: options = {lunareclipse: true, solareclipse: true, moonlanding: true, 'garden-biodynamic': false};
  gardenEvents: options = {lunareclipse: true, solareclipse: true, moonlanding: false, 'garden-biodynamic': true};
  from = MainSettingsService.initialFrom();
  to = MainSettingsService.initialTo();
  zone = MainSettingsService.detectTimezone();

  static initialFrom() {
    return new Date().getFullYear() + "-01-01";
  }

  static initialTo() {
    return new Date().getFullYear() + "-12-31";
  }

  static detectTimezone() {
    return Intl.DateTimeFormat().resolvedOptions().timeZone;
  }
}
