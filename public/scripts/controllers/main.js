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
			{name:'tägliche Phasen',include:false}
		];
		$scope.events = [
			{name:'Mondaufgang', include:true},
			{name:'Monduntergang', include:true},
			{name:'Mondfinsternis', include:false},
			{name:'Mondlandung', include:false}
		];
		$scope.from = new Date(new Date().getFullYear(),0,1);
		$scope.to = new Date(new Date().getFullYear(),11,31);
		$scope.calendar=[];
		$scope.requestOngoing=false;
		$scope.error=null;
		$scope.$watch(function(){
			return JSON.stringify($scope.phases)+JSON.stringify($scope.events)+$scope.from+$scope.to;
		}, function(){
		    $scope.requestOngoing=true;
			$http({
			   method: 'POST',
			   url: '/query',
			   data: JSON.stringify({phases:$scope.phases, events:$scope.events, from:$scope.from, to:$scope.to}),
			   headers: {'Content-Type': 'application/json'}
			}).then(function successCallback(response) {
				$scope.updateCalendar(response.data);
				$scope.error=null;
				$scope.requestOngoing=false;
			  }, function errorCallback(response) {
				$scope.error=response;
				$scope.calendar=null;
				$scope.requestOngoing=false;
			  });
		});
		$scope.updateCalendar=function(newCalendar) {
		    if(!$scope.calendar) {
		        $scope.calendar=newCalendar;
		        return;
		    }
		    var newBase=0;
		    for(var o=0;o<$scope.calendar.length;){
		        var oldElement=$scope.calendar[o];
		        if($scope.eventsEquals(oldElement, newCalendar[newBase])) {
		            o++;newBase++; //nothing to do, event already in list
		        } else {
		            var oldElementInNewList=false;
                    for(var n=newBase;n<newCalendar.length;n++){
                        if($scope.eventsEquals(oldElement,newCalendar[n])) {
                            oldElementInNewList=true;
                            break;
                        }
                    }
                    if(oldElementInNewList) {
                        $scope.calendar.splice(o++,0,newCalendar[newBase++]); //add new event
                    }else{
                        $scope.calendar.splice(o,1); //delete old event
                    }
		        }
		    }
		    while(newBase<newCalendar.length) {
		        $scope.calendar.push(newCalendar[newBase++]);
		    }
		}
		$scope.eventsEquals=function(event1, event2) {
		    return event1 && event2 && event1.date==event2.date && event1.title==event2.title;
		}
	});
