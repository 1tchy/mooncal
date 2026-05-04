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
  events: options = {lunareclipse: true, solareclipse: true, moonlanding: true};
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
