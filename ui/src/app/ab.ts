import {Injectable} from "@angular/core";

@Injectable({providedIn: 'root'})
export class AB {

  private readonly _isA: boolean;

  constructor() {
    this._isA = [...(window.navigator.userAgent + window.navigator.language + new Date().toLocaleDateString())]
      .reduce((hash, c) => ((hash << 5) + hash) + c.charCodeAt(0), 5381) % 2 == 0;
  }


  get isA(): boolean {
    return this._isA;
  }
}
