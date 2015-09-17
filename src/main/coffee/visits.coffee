angular.module( 'admin' ).controller 'VisitsController', ['$scope', '$resource', ($scope, $resource) ->
	Visits = $resource '/api/v1/visits/:id'
	$scope.message = {type: 'none'}

	Visits.query (result, response) ->
		$scope.visits = result
	,	(response) ->
		$scope.message = {type: 'error', text: response.data}
	]