export interface Messages {
  app: App
  lang: Lang
  navigation: Navigation
  general: General
  phases: Phases
  styles: Style
  events: Events
  time: Time
  calendar: Calendar
  export: Export
  about: About
  thank: Thank
  improveTranslation: ImproveTranslation
  error: Error
}

export interface App {
  title: string
  introduction: string
  moon: string
}

export interface Lang {
  current: string
  currentName: string
  change: string
}

export interface Navigation {
  home: string
  about: string
  toggle: string
  paths: Paths
}

export interface Paths {
  home: string
  about: string
  thank: string
  buymeacoffee: string
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

export interface Style {
  title: string
  iconOnly: string
  fullmoon: string
  fullmoonAndName: string
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
  addToCalendar: string
  ical: Ical
  print: string
}

export interface Ical {
  download: string
  downloadWarning: string
  subscribe: Subscribe
}

export interface Subscribe {
  title: string
  instructions: SubscribeInstructions
  explanations: string
  copy: string
  close: string
}

export interface SubscribeInstructions {
  lead: string
  iOS: AppSubscribeInstructions
  macOS: AppSubscribeInstructions
  googleGoto: string
  googleCalendar: AppSubscribeInstructions
  androidName: string
  androidIntro: string
  androidSeeGooglePrefix: string
  androidSeeGoogleLink: string
  androidSeeGooglePostfix: string
  androidIfNotAutomatic: string
  androidExtraSteps: string[]
  thunderbird: AppSubscribeInstructions
  outlook: AppSubscribeInstructions
  enjoyInstruction: string
  sayThanks: string
}

export interface AppSubscribeInstructions {
  name: string
  steps: string[]
}

export interface About {
  title: string
  introduction: string
  sources: Sources
  support: Support
}

export interface Thank {
  title: string
  introduction: string
  review: Review
}

export interface Review {
  title: string
  drunk: string
  notDrunk: string
  pleaseHelp: string
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
  developedWith: string
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
  paypal: string
  coffee: string
  develop: string
}

export interface ImproveTranslation {
  title: string
  lead: string
  currentText: string
  betterTextSuggestion: string
  submit: string
  thanksForFeedback: string
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
