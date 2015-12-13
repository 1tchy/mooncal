'use strict';

/**
 * @ngdoc function
 * @name mondkalenderApp.controller:MainCtrl
 * @description
 * # MainCtrl
 * Controller of the mondkalenderApp
 */
angular.module('mondkalenderApp')
	.controller('MainCtrl', function ($scope, $http, $window) {
		$scope.phases = {full:{value:true},'new':{value:false},quarter:{value:false},daily:{value:false}};
		$scope.events = {lunareclipse:{value:true},moonlanding:{value:true}};
		$scope.from = new Date(new Date().getFullYear(),0,1);
		$scope.to = new Date(new Date().getFullYear(),11,31);
		$scope.paramsAsString=function() {
		    return "phases[full]="+$scope.phases.full.value
		        +"&phases[new]="+$scope.phases["new"].value
		        +"&phases[quarter]="+$scope.phases.quarter.value
		        +"&phases[daily]="+$scope.phases.daily.value
		        +"&events[lunareclipse]="+$scope.events.lunareclipse.value
		        +"&events[moonlanding]="+$scope.events.moonlanding.value
		        +"&from="+$scope.formatDate($scope.from)
		        +"&to="+$scope.formatDate($scope.to);
		};
		$scope.calendar=[];
		$scope.requestOngoing=false;
		$scope.error=null;
		$scope.getEventCategoriesToInclude=function(eventCategory) {
		    var eventKeysToInclude=[];
		    for(var i=0;i<eventCategory.length;i++){
		        if(eventCategory[i].include) {
		            eventKeysToInclude.push(eventCategory[i].name);
		        }
		    }
		    return eventKeysToInclude;
		};
		$scope.formatDate=function formatLocalDate(date) {
		    if(!date) return date;
            var tzo = -date.getTimezoneOffset();
            var pad = function(num) {
                var norm = Math.abs(Math.floor(num));
                return (norm < 10 ? '0' : '') + norm;
            };
            return date.getFullYear()
                + '-' + pad(date.getMonth()+1)
                + '-' + pad(date.getDate())
                + 'T' + pad(date.getHours())
                + ':' + pad(date.getMinutes())
                + ':' + pad(date.getSeconds())
                + (tzo >= 0 ? '+' : '-') + pad(tzo / 60)
                + ':' + pad(tzo % 60);
        }
		$scope.$watch(function(){
			return $scope.paramsAsString();
		}, function(){
		    console.log($scope.paramsAsString());
		    $scope.requestOngoing=true;
			$http({
			   method: 'GET',
			   url: '/mondkalender?'+$scope.paramsAsString(),
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
		$scope.downloadIcal=function() {
		    $window.location.href="/mondkalender.ics?"+$scope.paramsAsString();
		};
	});
