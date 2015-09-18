app = angular.module 'cramsite', ['ngResource']

app.controller( 'MainController', ['$scope', '$resource', ($scope, $resource) ->
	Files = $resource '/api/v1/files/:id'
	
	$scope.message = {type: 'none'}

	home = ->
		$scope.path = []
		$scope.file = undefined
		Files.query (result, response) ->
			$scope.chunks = (result.slice(i, i + 6) for i in [0..result.length - 1] by 6)
		,	(response) ->
			$scope.message = {type: 'error', text: response.data}

	home()
	
	$scope.selectFile = (file) ->
		if file.directory
			$scope.path.push file
			directory(file)
		else
			$scope.file = file
			
	$scope.selectHome = ->
		home()

	$scope.selectElement = (index) ->
		$scope.path.splice( index + 1, $scope.path.length - index - 1 )
		directory($scope.path[index])
		
	directory = (dir) ->
		$scope.file = undefined
		Files.query {id: dir.id}, (result, response) ->
			$scope.chunks = (result.slice(i, i + 6) for i in [0..result.length - 1] by 6)
		,	(response) ->
			$scope.message = {type: 'error', text: response.data}
		
	] )