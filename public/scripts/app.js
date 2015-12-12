'use strict';

/**
 * @ngdoc overview
 * @name mondkalenderApp
 * @description
 * # mondkalenderApp
 *
 * Main module of the application.
 */
angular
  .module('mondkalenderApp', [
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
  });
