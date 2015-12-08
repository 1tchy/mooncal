'use strict';

/**
 * @ngdoc function
 * @name mondkalenderApp.controller:MainCtrl
 * @description
 * # MainCtrl
 * Controller of the mondkalenderApp
 */
angular.module('mondkalenderApp')
	.controller('MainCtrl', function ($scope, $http) {
		$scope.phases = [
			{name:'Vollmond',include:true},
			{name:'Neumond',include:true},
			{name:'Halbmond',include:false},
			{name:'Viertelmond',include:false},
			{name:'tägliche Phasen',include:false}
		];
		$scope.events = [
			{name:'Mondaufgang', include:true},
			{name:'Monduntergang', include:true},
			{name:'Mondfinsternis', include:false},
			{name:'Mondlandung', include:false}
		];
		$scope.from = new Date(2015,0,1);
		$scope.to = new Date(2015,11,31);
		$scope.calendar=[];
		$scope.$watch(function(){
			return JSON.stringify($scope.phases)+JSON.stringify($scope.events)+$scope.from+$scope.to;
		}, function(){
			$http({
			   method: 'POST',
			   url: '/query',
			   data: JSON.stringify({phases:$scope.phases, events:$scope.events, from:$scope.from, to:$scope.to}),
			   headers: {'Content-Type': 'application/json'}
			}).then(function successCallback(response) {
				$scope.calendar=JSON.stringify(response.data);
				// this callback will be called asynchronously
				// when the response is available
			  }, function errorCallback(response) {
				$scope.calendar="kein hoi"+response;
				// called asynchronously if an error occurs
				// or server returns response with an error status.
			  });
//			$scope.calendar=calculateCalendar(true,true,$scope.from,$scope.to);
		});
		function calculateCalendar(showFullmoon,showNewmoon,from,to) {
			if(Math.random()>0.01) {
				return JSON.stringify($scope.phases)+JSON.stringify($scope.events)+$scope.from+$scope.to;
			}
			if(Math.random()>0.3) {
				return ['hoi',$scope.to, Math.random()];
			} else if(Math.random()>0.5) {
				return ['hoiiii',$scope.to, Math.random()];
			} else {
				return ['hoi???',$scope.to, Math.random(), showFullmoon,showNewmoon,from,to];
			}
		}
	});
