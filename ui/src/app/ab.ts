import {Injectable} from "@angular/core";

@Injectable({providedIn: 'root'})
export class AB {

  private readonly _isA: boolean;

  constructor() {
    this._isA = [...(window.navigator.userAgent + window.navigator.language + new Date().toLocaleDateString())]
      .reduce((acc, curr) => acc ^ curr.charCodeAt(0), 0) >= 64;
  }


  get isA(): boolean {
    return this._isA;
  }
}
