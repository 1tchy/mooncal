export interface Messages {
  app: App
  lang: Lang
  navigation: Navigation
  general: General
  phases: Phases
  events: Events
  time: Time
  calendar: Calendar
  export: Export
  about: About
  error: Error
}

export interface App {
  title: string
  introduction: string
  moon: string
}

export interface Lang {
  [key: string]: any

  current: string
  de: string
  en: string
  nl: string
  es: string
  fr: string
  change: string
}

export interface Navigation {
  home: string
  about: string
  toggle: string
}

export interface General {
  and: string
}

export interface Phases {
  [key: string]: any

  title: string
  full: string
  new: string
  quarter: string
  daily: string
}

export interface Events {
  [key: string]: any

  title: string
  lunareclipse: string
  solareclipse: string
  moonlanding: string
}

export interface Time {
  title: string
  fromTo: FromTo
}

export interface FromTo {
  from: string
  to: string
  inTimezone: string
  required: string
  date: string
  minmax: string
}

export interface Calendar {
  title: string
  reloading: string
}

export interface Export {
  title: string
  ical: Ical
  print: string
}

export interface Ical {
  title: string
  download: string
  subscribe: Subscribe
}

export interface Subscribe {
  title: string
  instructions: string
  explanations: string
  copy: string
  close: string
}

export interface About {
  title: string
  introduction: string
  sources: Sources
  support: Support
}

export interface Sources {
  title: string
  moonimage: Moonimage
  phases: Phases2
  lunareclipse: Lunareclipse2
  solareclipse: Solareclipse2
  moonlanding: Moonlanding
  various: string
  mainly: string
  space: Space
}

export interface Moonimage {
  title: string
}

export interface Phases2 {
  title: string
}

export interface Lunareclipse2 {
  title: string
}

export interface Solareclipse2 {
  title: string
}

export interface Moonlanding {
  title: string
}

export interface Space {
  devs: Devs
}

export interface Devs {
  reason: string
}

export interface Support {
  title: string
  donate: string
  coffee: string
  develop: string
}

export interface Error {
  title: string
  invalid: string
  required: string
  fromTo: FromTo2
  notfound: Notfound
}

export interface FromTo2 {
  tolargefordaily: string
}

export interface Notfound {
  title: string
  lead: string
  problem: Problem
}

export interface Problem {
  title: string
  typo: string
  link: string
}
