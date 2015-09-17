app = angular.module 'cramsite', ['ngResource']

app.controller( 'MainController', ['$scope', '$resource', ($scope, $resource) ->
	Files = $resource '/api/v1/files/:id'
	
	Files.query (result, response) ->
		$scope.files = result
		console.log result
	,	(response) ->
		$scope.message = {type: 'error', text: response.data}

	] )