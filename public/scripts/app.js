'use strict';

/**
 * @ngdoc overview
 * @name mooncalApp
 * @description
 * # mooncalApp
 *
 * Main module of the application.
 */
angular
	.module('mooncalApp', [
		'ngAnimate',
		'ngRoute'
	])
	.config(function ($routeProvider) {
		$routeProvider
			.when('/', {
				templateUrl: 'views/main.html',
				controller: 'MainCtrl',
				controllerAs: 'main'
			})
			.when('/about', {
				templateUrl: 'views/about.html',
				controller: 'AboutCtrl',
				controllerAs: 'about'
			})
			.otherwise({
				redirectTo: '/'
			});
	})
	.run(function ($rootScope, $location) {
		$rootScope.pageVisits = 0;
		$rootScope.$on("$routeChangeStart", function () {
			$rootScope.path = $location.path();
			if ($location.url() !== "" && $rootScope.pageVisits++ > 0) {
				_paq.push(['setCustomUrl', $location.url()]);
				_paq.push(['trackPageView']);
			}
		});
		$rootScope.trackLanguageChange = function (newLanguage, oldLanguage, $event) {
			_paq.push(['trackEvent', 'Language', 'changeTo' + newLanguage, oldLanguage], {
				hitCallback: function () {
					$location.href = $event.target.href;
				}
			});
		}
	});
