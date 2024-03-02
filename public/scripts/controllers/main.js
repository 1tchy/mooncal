'use strict';

/**
 * @ngdoc function
 * @name mooncalApp.controller:MainCtrl
 * @description
 * # MainCtrl
 * Controller of the mooncalApp
 */
angular.module('mooncalApp')
	.controller('MainCtrl', function ($scope, $http, $window) {
		$scope.phases = {full: {value: true}, 'new': {value: false}, quarter: {value: false}, daily: {value: false}};
		$scope.events = {lunareclipse: {value: true}, solareclipse: {value: true}, moonlanding: {value: true}};
		$scope.from = new Date(new Date().getFullYear(), 0, 1);
		$scope.to = new Date(new Date().getFullYear(), 11, 31);
		$scope.updateCount = 0;
		$scope.lang = null;
		$scope.paramsAsString = function () {
			return "lang=" + $scope.lang
				+ "&phases[full]=" + $scope.phases.full.value
				+ "&phases[new]=" + $scope.phases["new"].value
				+ "&phases[quarter]=" + $scope.phases.quarter.value
				+ "&phases[daily]=" + $scope.phases.daily.value
				+ "&events[lunareclipse]=" + $scope.events.lunareclipse.value
				+ "&events[solareclipse]=" + $scope.events.solareclipse.value
				+ "&events[moonlanding]=" + $scope.events.moonlanding.value
				+ "&from=" + $scope.formatDate($scope.from)
				+ "&to=" + $scope.formatDate($scope.to);
		};
		$scope.paramsAsShortString = function () {
			return $scope.lang
				+ ($scope.phases.full.value ? "full," : "")
				+ ($scope.phases["new"].value ? "new," : "")
				+ ($scope.phases.quarter.value ? "quarter," : "")
				+ ($scope.phases.daily.value ? "daily," : "")
				+ ($scope.events.lunareclipse.value ? "lunareclipse," : "")
				+ ($scope.events.solareclipse.value ? "solareclipse," : "")
				+ ($scope.events.moonlanding.value ? "moonlanding," : "")
				+ $scope.formatDate($scope.from) + "-" + $scope.formatDate($scope.to);
		};
		$scope.calendar = [];
		$scope.requestOngoing = false;
		$scope.error = null;
		$scope.getEventCategoriesToInclude = function (eventCategory) {
			const eventKeysToInclude = [];
			for (let i = 0; i < eventCategory.length; i++) {
				if (eventCategory[i].include) {
					eventKeysToInclude.push(eventCategory[i].name);
				}
			}
			return eventKeysToInclude;
		};
		$scope.formatDate = function formatLocalDate(date) {
			if (!date) return date;
			const pad = function (num) {
				const norm = Math.abs(Math.floor(num));
				return (norm < 10 ? '0' : '') + norm;
			};
			return date.getFullYear()
				+ '-' + pad(date.getMonth() + 1)
				+ '-' + pad(date.getDate())
				+ 'T' + pad(date.getHours())
				+ ':' + pad(date.getMinutes())
				+ ':' + pad(date.getSeconds())
				+ $scope.zone;
		};
		$scope.$watch(function () {
			return $scope.paramsAsShortString();
		}, function () {
			if ($scope.from && $scope.to) {
				$scope.requestOngoing = true;
				$http({
					method: 'GET',
					url: '/mooncal?' + $scope.paramsAsString(),
					headers: {'Content-Type': 'application/json'}
				}).then(function successCallback(response) {
					$scope.updateCalendar(response.data);
					$scope.error = null;
					$scope.requestOngoing = false;
				}, function errorCallback(response) {
					$scope.error = response;
					$scope.calendar = null;
					$scope.requestOngoing = false;
				});
				if ($scope.updateCount++ > 0) {
					_paq.push(['trackEvent', 'Calendar', 'update', $scope.paramsAsShortString()]);
				}
			} else {
				$scope.calendar = null;
			}
		});
		$scope.updateCalendar = function (newCalendar) {
			if (!$scope.calendar) {
				$scope.calendar = newCalendar;
				return;
			}
			let newBase = 0;
			for (let o = 0; o < $scope.calendar.length;) {
				const oldElement = $scope.calendar[o];
				if ($scope.eventsEquals(oldElement, newCalendar[newBase])) {
					o++;
					newBase++; //nothing to do, event already in list
				} else {
					let oldElementInNewList = false;
					for (let n = newBase; n < newCalendar.length; n++) {
						if ($scope.eventsEquals(oldElement, newCalendar[n])) {
							oldElementInNewList = true;
							break;
						}
					}
					if (oldElementInNewList) {
						$scope.calendar.splice(o++, 0, newCalendar[newBase++]); //add new event
					} else {
						$scope.calendar.splice(o, 1); //delete old event
					}
				}
			}
			while (newBase < newCalendar.length) {
				$scope.calendar.push(newCalendar[newBase++]);
			}
		};
		$scope.eventsEquals = function (event1, event2) {
			return event1 && event2 && event1.date === event2.date && event1.title === event2.title && event1.description === event2.description;
		};
		$scope.copyIcalLink = function () {
			navigator.clipboard.writeText(document.getElementById('icalLink').value);
		};
		$scope.trackDownloadIcal = function () {
			_paq.push(['trackEvent', 'Calendar', 'downloadIcal', $scope.paramsAsShortString()]);
			$window.location.href = "/mooncal.ics?" + $scope.paramsAsString();
		};
		$scope.trackIcalSubscription = function () {
			_paq.push(['trackEvent', 'Calendar', 'subscribeIcal', $scope.paramsAsShortString()]);
		};
		$scope.trackPrint = function () {
			_paq.push(['trackEvent', 'Calendar', 'print', $scope.paramsAsShortString()]);
		};
		$scope.getTimezone = function () {
			return Intl.DateTimeFormat().resolvedOptions().timeZone
		};
		$scope.getSupportedTimezones = function () {
			//see SupportedTimeZoneTest
			return ["Etc/GMT+12", "Pacific/Midway", "Pacific/Niue", "Pacific/Pago_Pago", "Pacific/Samoa", "US/Samoa", "Pacific/Honolulu", "Pacific/Johnston", "Pacific/Rarotonga", "Pacific/Tahiti", "US/Hawaii", "America/Adak", "America/Atka", "Pacific/Marquesas", "US/Aleutian", "Pacific/Gambier", "America/Anchorage", "America/Juneau", "America/Metlakatla", "America/Nome", "America/Sitka", "America/Yakutat", "US/Alaska", "Pacific/Pitcairn", "America/Dawson", "America/Ensenada", "America/Los_Angeles", "America/Santa_Isabel", "America/Tijuana", "America/Vancouver", "America/Whitehorse", "Canada/Pacific", "Canada/Yukon", "Mexico/BajaNorte", "PST8PDT", "US/Pacific", "America/Creston", "America/Dawson_Creek", "America/Fort_Nelson", "America/Hermosillo", "America/Phoenix", "US/Arizona", "America/Boise", "America/Cambridge_Bay", "America/Chihuahua", "America/Ciudad_Juarez", "America/Denver", "America/Edmonton", "America/Inuvik", "America/Mazatlan", "America/Ojinaga", "America/Shiprock", "America/Yellowknife", "Canada/Mountain", "MST7MDT", "Mexico/BajaSur", "Navajo", "US/Mountain", "America/Belize", "America/Costa_Rica", "America/El_Salvador", "America/Guatemala", "America/Managua", "America/Regina", "America/Swift_Current", "America/Tegucigalpa", "Canada/Saskatchewan", "Pacific/Galapagos", "America/Bahia_Banderas", "America/Chicago", "America/Indiana/Knox", "America/Indiana/Tell_City", "America/Knox_IN", "America/Matamoros", "America/Menominee", "America/Merida", "America/Mexico_City", "America/Monterrey", "America/North_Dakota/Beulah", "America/North_Dakota/Center", "America/North_Dakota/New_Salem", "America/Rainy_River", "America/Rankin_Inlet", "America/Resolute", "America/Winnipeg", "CST6CDT", "Canada/Central", "Mexico/General", "US/Central", "US/Indiana-Starke", "America/Atikokan", "America/Bogota", "America/Cancun", "America/Cayman", "America/Coral_Harbour", "America/Eirunepe", "America/Guayaquil", "America/Jamaica", "America/Lima", "America/Panama", "America/Porto_Acre", "America/Rio_Branco", "Brazil/Acre", "Chile/EasterIsland", "Jamaica", "Pacific/Easter", "America/Caracas", "America/Detroit", "America/Fort_Wayne", "America/Havana", "America/Indiana/Indianapolis", "America/Indiana/Marengo", "America/Indiana/Petersburg", "America/Indiana/Vevay", "America/Indiana/Vincennes", "America/Indiana/Winamac", "America/Indianapolis", "America/Iqaluit", "America/Kentucky/Louisville", "America/Kentucky/Monticello", "America/Louisville", "America/Montreal", "America/Nassau", "America/New_York", "America/Nipigon", "America/Pangnirtung", "America/Port-au-Prince", "America/Thunder_Bay", "America/Toronto", "Canada/Eastern", "Cuba", "EST5EDT", "US/East-Indiana", "US/Eastern", "US/Michigan", "America/Anguilla", "America/Antigua", "America/Aruba", "America/Barbados", "America/Blanc-Sablon", "America/Boa_Vista", "America/Curacao", "America/Dominica", "America/Grand_Turk", "America/Grenada", "America/Guadeloupe", "America/Guyana", "America/Kralendijk", "America/La_Paz", "America/Lower_Princes", "America/Manaus", "America/Marigot", "America/Martinique", "America/Montserrat", "America/Port_of_Spain", "America/Porto_Velho", "America/Puerto_Rico", "America/Santo_Domingo", "America/St_Barthelemy", "America/St_Kitts", "America/St_Lucia", "America/St_Thomas", "America/St_Vincent", "America/Tortola", "America/Virgin", "Brazil/West", "America/Asuncion", "America/Campo_Grande", "America/Cuiaba", "America/Glace_Bay", "America/Goose_Bay", "America/Halifax", "America/Moncton", "America/Thule", "Atlantic/Bermuda", "Canada/Atlantic", "America/Araguaina", "America/Argentina/Buenos_Aires", "America/Argentina/Catamarca", "America/Argentina/ComodRivadavia", "America/Argentina/Cordoba", "America/Argentina/Jujuy", "America/Argentina/La_Rioja", "America/Argentina/Mendoza", "America/Argentina/Rio_Gallegos", "America/Argentina/Salta", "America/Argentina/San_Juan", "America/Argentina/San_Luis", "America/Argentina/Tucuman", "America/Argentina/Ushuaia", "America/Bahia", "America/Belem", "America/Buenos_Aires", "America/Catamarca", "America/Cayenne", "America/Cordoba", "America/Fortaleza", "America/Jujuy", "America/Maceio", "America/Mendoza", "America/Montevideo", "America/Paramaribo", "America/Punta_Arenas", "America/Recife", "America/Rosario", "America/Santarem", "America/Santiago", "America/St_Johns", "Antarctica/Palmer", "Antarctica/Rothera", "Atlantic/Stanley", "Canada/Newfoundland", "Chile/Continental", "America/Godthab", "America/Miquelon", "America/Nuuk", "America/Sao_Paulo", "Brazil/East", "America/Noronha", "Atlantic/South_Georgia", "Brazil/DeNoronha", "Atlantic/Cape_Verde", "America/Scoresbysund", "Atlantic/Azores", "Africa/Abidjan", "Africa/Accra", "Africa/Bamako", "Africa/Banjul", "Africa/Bissau", "Africa/Casablanca", "Africa/Conakry", "Africa/Dakar", "Africa/El_Aaiun", "Africa/Freetown", "Africa/Lome", "Africa/Monrovia", "Africa/Nouakchott", "Africa/Ouagadougou", "Africa/Sao_Tome", "Africa/Timbuktu", "America/Danmarkshavn", "Atlantic/Reykjavik", "Atlantic/St_Helena", "GMT", "GMT0", "Greenwich", "Iceland", "UCT", "UTC", "Universal", "Zulu", "Atlantic/Canary", "Atlantic/Faeroe", "Atlantic/Faroe", "Atlantic/Madeira", "Eire", "Europe/Belfast", "Europe/Dublin", "Europe/Guernsey", "Europe/Isle_of_Man", "Europe/Jersey", "Europe/Lisbon", "Europe/London", "GB", "GB-Eire", "Portugal", "WET", "Africa/Algiers", "Africa/Bangui", "Africa/Brazzaville", "Africa/Douala", "Africa/Kinshasa", "Africa/Lagos", "Africa/Libreville", "Africa/Luanda", "Africa/Malabo", "Africa/Ndjamena", "Africa/Niamey", "Africa/Porto-Novo", "Africa/Tunis", "Antarctica/Troll", "Africa/Ceuta", "Africa/Windhoek", "Arctic/Longyearbyen", "Atlantic/Jan_Mayen", "CET", "Europe/Amsterdam", "Europe/Andorra", "Europe/Belgrade", "Europe/Berlin", "Europe/Bratislava", "Europe/Brussels", "Europe/Budapest", "Europe/Busingen", "Europe/Copenhagen", "Europe/Gibraltar", "Europe/Ljubljana", "Europe/Luxembourg", "Europe/Madrid", "Europe/Malta", "Europe/Monaco", "Europe/Oslo", "Europe/Paris", "Europe/Podgorica", "Europe/Prague", "Europe/Rome", "Europe/San_Marino", "Europe/Sarajevo", "Europe/Skopje", "Europe/Stockholm", "Europe/Tirane", "Europe/Vaduz", "Europe/Vatican", "Europe/Vienna", "Europe/Warsaw", "Europe/Zagreb", "Europe/Zurich", "MET", "Poland", "Africa/Blantyre", "Africa/Bujumbura", "Africa/Cairo", "Africa/Gaborone", "Africa/Harare", "Africa/Johannesburg", "Africa/Kigali", "Africa/Lubumbashi", "Africa/Lusaka", "Africa/Maputo", "Africa/Maseru", "Africa/Mbabane", "Africa/Tripoli", "Egypt", "Europe/Kaliningrad", "Libya", "Asia/Amman", "Asia/Beirut", "Asia/Damascus", "Asia/Famagusta", "Asia/Gaza", "Asia/Hebron", "Asia/Istanbul", "Asia/Jerusalem", "Asia/Nicosia", "Asia/Tel_Aviv", "EET", "Europe/Athens", "Europe/Bucharest", "Europe/Chisinau", "Europe/Helsinki", "Europe/Istanbul", "Europe/Kiev", "Europe/Kyiv", "Europe/Mariehamn", "Europe/Nicosia", "Europe/Riga", "Europe/Sofia", "Europe/Tallinn", "Europe/Tiraspol", "Europe/Uzhgorod", "Europe/Vilnius", "Europe/Zaporozhye", "Israel", "Turkey", "Africa/Addis_Ababa", "Africa/Asmara", "Africa/Asmera", "Africa/Dar_es_Salaam", "Africa/Djibouti", "Africa/Juba", "Africa/Kampala", "Africa/Khartoum", "Africa/Mogadishu", "Africa/Nairobi", "Antarctica/Syowa", "Asia/Aden", "Asia/Baghdad", "Asia/Bahrain", "Asia/Kuwait", "Asia/Qatar", "Asia/Riyadh", "Europe/Astrakhan", "Europe/Kirov", "Europe/Minsk", "Europe/Moscow", "Europe/Saratov", "Europe/Simferopol", "Europe/Ulyanovsk", "Europe/Volgograd", "Indian/Antananarivo", "Indian/Comoro", "Indian/Mayotte", "W-SU", "Asia/Dubai", "Asia/Muscat", "Asia/Tbilisi", "Asia/Tehran", "Asia/Yerevan", "Europe/Samara", "Indian/Mahe", "Indian/Mauritius", "Indian/Reunion", "Iran", "Asia/Baku", "Asia/Kabul", "Antarctica/Mawson", "Asia/Aqtau", "Asia/Aqtobe", "Asia/Ashgabat", "Asia/Ashkhabad", "Asia/Atyrau", "Asia/Dushanbe", "Asia/Karachi", "Asia/Oral", "Asia/Samarkand", "Asia/Tashkent", "Asia/Yekaterinburg", "Indian/Kerguelen", "Indian/Maldives", "Asia/Calcutta", "Asia/Colombo", "Asia/Kolkata", "Asia/Kathmandu", "Asia/Katmandu", "Antarctica/Vostok", "Asia/Almaty", "Asia/Barnaul", "Asia/Bishkek", "Asia/Dacca", "Asia/Dhaka", "Asia/Kashgar", "Asia/Novosibirsk", "Asia/Omsk", "Asia/Qostanay", "Asia/Qyzylorda", "Asia/Thimbu", "Asia/Thimphu", "Asia/Tomsk", "Asia/Urumqi", "Indian/Chagos", "Asia/Rangoon", "Asia/Yangon", "Indian/Cocos", "Antarctica/Davis", "Asia/Bangkok", "Asia/Ho_Chi_Minh", "Asia/Jakarta", "Asia/Krasnoyarsk", "Asia/Novokuznetsk", "Asia/Phnom_Penh", "Asia/Pontianak", "Asia/Saigon", "Asia/Vientiane", "Indian/Christmas", "Asia/Hovd", "Antarctica/Casey", "Asia/Brunei", "Asia/Chita", "Asia/Chongqing", "Asia/Chungking", "Asia/Harbin", "Asia/Hong_Kong", "Asia/Irkutsk", "Asia/Kuala_Lumpur", "Asia/Kuching", "Asia/Macao", "Asia/Macau", "Asia/Makassar", "Asia/Manila", "Asia/Shanghai", "Asia/Singapore", "Asia/Taipei", "Asia/Ujung_Pandang", "Australia/Perth", "Australia/West", "Hongkong", "PRC", "Singapore", "Asia/Choibalsan", "Asia/Ulaanbaatar", "Asia/Ulan_Bator", "Asia/Pyongyang", "Australia/Eucla", "Asia/Dili", "Asia/Jayapura", "Asia/Khandyga", "Asia/Seoul", "Asia/Tokyo", "Asia/Yakutsk", "Japan", "Pacific/Palau", "ROK", "Australia/Darwin", "Australia/North", "Antarctica/DumontDUrville", "Asia/Magadan", "Asia/Sakhalin", "Asia/Ust-Nera", "Asia/Vladivostok", "Australia/Adelaide", "Australia/Brisbane", "Australia/Broken_Hill", "Australia/Lindeman", "Australia/Queensland", "Australia/South", "Australia/Yancowinna", "Pacific/Chuuk", "Pacific/Guam", "Pacific/Port_Moresby", "Pacific/Saipan", "Pacific/Truk", "Pacific/Yap", "Antarctica/Macquarie", "Australia/ACT", "Australia/Canberra", "Australia/Currie", "Australia/Hobart", "Australia/Melbourne", "Australia/NSW", "Australia/Sydney", "Australia/Tasmania", "Australia/Victoria", "Australia/LHI", "Australia/Lord_Howe", "Asia/Srednekolymsk", "Pacific/Bougainville", "Pacific/Efate", "Pacific/Guadalcanal", "Pacific/Kosrae", "Pacific/Noumea", "Pacific/Pohnpei", "Pacific/Ponape", "Pacific/Norfolk", "Asia/Anadyr", "Asia/Kamchatka", "Kwajalein", "Pacific/Funafuti", "Pacific/Kwajalein", "Pacific/Majuro", "Pacific/Nauru", "Pacific/Tarawa", "Pacific/Wake", "Pacific/Wallis", "Antarctica/McMurdo", "Antarctica/South_Pole", "NZ", "Pacific/Auckland", "Pacific/Fiji", "Pacific/Enderbury", "Pacific/Fakaofo", "Pacific/Kanton", "Pacific/Tongatapu", "NZ-CHAT", "Pacific/Chatham", "Pacific/Apia", "Pacific/Kiritimati"]
		};
		$scope.zone = $scope.getTimezone();
	});
