app = angular.module 'cramsite', ['ngResource']

app.controller( 'MainController', ['$scope', '$resource', ($scope, $resource) ->
	Files = $resource '/api/v1/files/:id'
	
	$scope.file = undefined
	$scope.path = []
	
	Files.query (result, response) ->
		$scope.chunks = (result.slice(i, i + 6) for i in [0..result.length - 1] by 6)
	,	(response) ->
		$scope.message = {type: 'error', text: response.data}

	] )