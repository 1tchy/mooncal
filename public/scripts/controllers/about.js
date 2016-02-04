'use strict';

/**
 * @ngdoc function
 * @name mooncalApp.controller:AboutCtrl
 * @description
 * # AboutCtrl
 * Controller of the mooncalApp
 */
angular.module('mooncalApp')
  .controller('AboutCtrl', function ($scope) {
  		$scope.trackSupport=function(supportType) {
  		    ga('send', 'event', 'Support', supportType);
  		}
  });
