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
    .run(function($rootScope, $location) {
        $rootScope.$on("$routeChangeStart", function() {
            $rootScope.path=$location.path();
            ga('send', 'pageview', { page: $location.url() });
        })
        $rootScope.trackLink=function($event) {
            ga('send', 'event', 'Exit', 'leave', $event.target.href);
        }
        $rootScope.trackLanguageChange=function(newLanguage, oldLanguage, $event) {
            ga('send', 'event', 'Language', 'changeTo'+newLanguage, oldLanguage, {
                hitCallback: function() {
                    $location.href=$event.target.href;
                }
            });
        }
    });
