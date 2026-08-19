import {Injectable} from "@angular/core";

@Injectable({providedIn: 'root'})
export class AB {

  // Computed lazily so the service can be constructed during server-side prerendering,
  // where window does not exist; isA is only read from browser-guarded tracking code.
  private _isA: boolean | undefined;

  get isA(): boolean {
    if (this._isA === undefined) {
      this._isA = [...(window.navigator.userAgent + window.navigator.language + new Date().toLocaleDateString())]
        .reduce((hash, c) => ((hash << 5) + hash) + c.charCodeAt(0), 5381) % 2 == 0;
    }
    return this._isA;
  }
}
