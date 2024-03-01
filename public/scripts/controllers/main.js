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
			var eventKeysToInclude = [];
			for (var i = 0; i < eventCategory.length; i++) {
				if (eventCategory[i].include) {
					eventKeysToInclude.push(eventCategory[i].name);
				}
			}
			return eventKeysToInclude;
		};
		$scope.formatDate = function formatLocalDate(date) {
			if (!date) return date;
			var pad = function (num) {
				var norm = Math.abs(Math.floor(num));
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
			var newBase = 0;
			for (var o = 0; o < $scope.calendar.length;) {
				var oldElement = $scope.calendar[o];
				if ($scope.eventsEquals(oldElement, newCalendar[newBase])) {
					o++;
					newBase++; //nothing to do, event already in list
				} else {
					var oldElementInNewList = false;
					for (var n = newBase; n < newCalendar.length; n++) {
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
			return event1 && event2 && event1.date == event2.date && event1.title == event2.title;
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
			var jOffset = -1 * new Date(Date.UTC(2015, 6, 30, 0, 0, 0, 0)).getTimezoneOffset();
			var dOffset = -1 * new Date(Date.UTC(2015, 12, 30, 0, 0, 0, 0)).getTimezoneOffset();
			if (-120 == jOffset && -120 == dOffset) return 'America/Noronha';
			if (-120 == jOffset && -180 == dOffset) return 'America/Godthab';
			if (-150 == jOffset && -210 == dOffset) return 'America/St_Johns';
			if (-180 == jOffset && -120 == dOffset) return 'Brazil/East';
			if (-180 == jOffset && -180 == dOffset) return 'America/Jujuy';
			if (-180 == jOffset && -240 == dOffset) return 'SystemV/AST4ADT';
			if (-240 == jOffset && -180 == dOffset) return 'Chile/Continental';
			if (-240 == jOffset && -240 == dOffset) return 'SystemV/AST4';
			if (-240 == jOffset && -300 == dOffset) return 'SystemV/EST5EDT';
			if (-300 == jOffset && -300 == dOffset) return 'SystemV/EST5';
			if (-300 == jOffset && -360 == dOffset) return 'SystemV/CST6CDT';
			if (-360 == jOffset && -300 == dOffset) return 'Pacific/Easter';
			if (-360 == jOffset && -360 == dOffset) return 'SystemV/CST6';
			if (-360 == jOffset && -420 == dOffset) return 'SystemV/MST7MDT';
			if (-420 == jOffset && -420 == dOffset) return 'SystemV/MST7';
			if (-420 == jOffset && -480 == dOffset) return 'SystemV/PST8PDT';
			if (-480 == jOffset && -480 == dOffset) return 'SystemV/PST8';
			if (-480 == jOffset && -540 == dOffset) return 'SystemV/YST9YDT';
			if (-540 == jOffset && -540 == dOffset) return 'SystemV/YST9';
			if (-540 == jOffset && -600 == dOffset) return 'US/Aleutian';
			if (-570 == jOffset && -570 == dOffset) return 'Pacific/Marquesas';
			if (-60 == jOffset && -60 == dOffset) return 'Atlantic/Cape_Verde';
			if (-600 == jOffset && -600 == dOffset) return 'SystemV/HST10';
			if (-660 == jOffset && -660 == dOffset) return 'US/Samoa';
			if (-720 == jOffset && -720 == dOffset) return 'Etc/GMT+12';
			if (0 == jOffset && -60 == dOffset) return 'Atlantic/Azores';
			if (0 == jOffset && 0 == dOffset) return 'UTC';
			if (120 == jOffset && 0 == dOffset) return 'Antarctica/Troll';
			if (120 == jOffset && 120 == dOffset) return 'Africa/Kigali';
			if (120 == jOffset && 60 == dOffset) return 'CET';
			if (180 == jOffset && 120 == dOffset) return 'EET';
			if (180 == jOffset && 180 == dOffset) return 'Africa/Juba';
			if (240 == jOffset && 180 == dOffset) return 'W-SU';
			if (240 == jOffset && 240 == dOffset) return 'Etc/GMT-4';
			if (270 == jOffset && 210 == dOffset) return 'Iran';
			if (270 == jOffset && 270 == dOffset) return 'Asia/Kabul';
			if (300 == jOffset && 240 == dOffset) return 'Asia/Yerevan';
			if (300 == jOffset && 300 == dOffset) return 'Etc/GMT-5';
			if (330 == jOffset && 330 == dOffset) return 'Asia/Kolkata';
			if (345 == jOffset && 345 == dOffset) return 'Asia/Katmandu';
			if (360 == jOffset && 300 == dOffset) return 'Asia/Yekaterinburg';
			if (360 == jOffset && 360 == dOffset) return 'Etc/GMT-6';
			if (390 == jOffset && 390 == dOffset) return 'Indian/Cocos';
			if (420 == jOffset && 360 == dOffset) return 'Asia/Omsk';
			if (420 == jOffset && 420 == dOffset) return 'Asia/Saigon';
			if (480 == jOffset && 420 == dOffset) return 'Asia/Hovd';
			if (480 == jOffset && 480 == dOffset) return 'PRC';
			if (525 == jOffset && 525 == dOffset) return 'Australia/Eucla';
			if (540 == jOffset && 480 == dOffset) return 'Asia/Ulaanbaatar';
			if (540 == jOffset && 540 == dOffset) return 'ROK';
			if (570 == jOffset && 570 == dOffset) return 'Australia/North';
			if (570 == jOffset && 630 == dOffset) return 'Australia/South';
			if (60 == jOffset && 0 == dOffset) return 'WET';
			if (60 == jOffset && 120 == dOffset) return 'Africa/Windhoek';
			if (60 == jOffset && 60 == dOffset) return 'Africa/Lagos';
			if (600 == jOffset && 540 == dOffset) return 'Asia/Yakutsk';
			if (600 == jOffset && 600 == dOffset) return 'Australia/Brisbane';
			if (600 == jOffset && 660 == dOffset) return 'Australia/NSW';
			if (630 == jOffset && 660 == dOffset) return 'Australia/LHI';
			if (660 == jOffset && 600 == dOffset) return 'Asia/Vladivostok';
			if (660 == jOffset && 660 == dOffset) return 'Etc/GMT-11';
			if (690 == jOffset && 690 == dOffset) return 'Pacific/Norfolk';
			if (720 == jOffset && 660 == dOffset) return 'Asia/Ust-Nera';
			if (720 == jOffset && 720 == dOffset) return 'Etc/GMT-12';
			if (720 == jOffset && 780 == dOffset) return 'NZ';
			if (765 == jOffset && 825 == dOffset) return 'NZ-CHAT';
			if (780 == jOffset && 720 == dOffset) return 'Asia/Anadyr';
			if (780 == jOffset && 780 == dOffset) return 'Pacific/Enderbury';
			if (840 == jOffset && 840 == dOffset) return 'Pacific/Kiritimati';
			return 'UTC';
		};
		$scope.zone = $scope.getTimezone();
	});
