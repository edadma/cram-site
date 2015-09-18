app = angular.module 'cramsite', ['ngResource']

app.controller( 'MainController', ['$scope', '$resource', ($scope, $resource) ->
	Files = $resource '/api/v1/files/:id'
	Lessons = $resource '/api/v1/lessons/:id'
	ChunkSize = 6
	$scope.message = {type: 'none'}

	home = ->
		$scope.path = []
		$scope.file = undefined
		Files.query (result, response) ->
			$scope.chunks = (result.slice(i, i + ChunkSize) for i in [0..result.length - 1] by ChunkSize)
		,	(response) ->
			$scope.message = {type: 'error', text: response.data}

	home()
	
	$scope.selectFile = (file) ->
		if file.directory
			$scope.path.push file
			directory(file)
		else
			open(file)
			
	$scope.selectHome = ->
		home()

	$scope.selectElement = (index) ->
		$scope.path.splice( index + 1, $scope.path.length - index - 1 )
		directory($scope.path[index])
		
	$scope.isUnderTopics = -> $scope.path.length > 0 && $scope.path[0].name == 'Topics'
	
	$scope.isUnderATopic = -> $scope.path.length > 1 && $scope.path[0].name == 'Topics'
	
	directory = (dir) ->
		$scope.file = undefined
		Files.query {id: dir.id}, (result, response) ->
			$scope.chunks = (result.slice(i, i + 6) for i in [0..result.length - 1] by 6)
		,	(response) ->
			$scope.message = {type: 'error', text: response.data}
		
	open = (file) ->
		$scope.file = file
		Lessons.get {id: file.id}, (result, response) ->
			$scope.pairs = result.pairs
		,	(response) ->
			$scope.message = {type: 'error', text: response.data}
		
	
	] )