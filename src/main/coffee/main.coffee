app = angular.module 'cramsite', ['ngResource']

app.controller( 'MainController', ['$scope', '$resource', ($scope, $resource) ->
	Files = $resource '/api/v1/files/:id'
	
	Files.query (result, response) ->
		$scope.files = (result.slice(i, i + 6) for i in [0..result.length - 1] by 6)
	,	(response) ->
		$scope.message = {type: 'error', text: response.data}

	] )