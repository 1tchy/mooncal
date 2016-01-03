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
        })
    });
