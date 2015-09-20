app = angular.module 'cramsite', ['ngResource']

app.controller( 'MainController', ['$scope', '$resource', ($scope, $resource) ->
	$scope.message = {type: 'none'}
	$scope.path = []
	$scope.file = undefined
	ChunkSize = 6
	Files = $resource '/api/v1/files/:id'
	Lessons = $resource '/api/v1/lessons/:id'
	Response = $resource '/api/v1/response'
	Tallies = $resource '/api/v1/tallies/:fileid/:userid'
	
	home = ->
		Files.query (result, response) ->
			$scope.path = []
			$scope.file = undefined
			$scope.chunks = (result.slice(i, i + ChunkSize) for i in [0..result.length - 1] by ChunkSize)
		,	(response) ->
			$scope.message = {type: 'error', text: response.data}

	home()
	
	$scope.selectFile = (file) ->
		if file.contents
			open(file)
		else
			$scope.path.push file
			directory(file)
			
	$scope.selectHome = ->
		home()

	$scope.selectElement = (index) ->
		$scope.path.splice( index + 1, $scope.path.length - index - 1 )
		directory($scope.path[index])
		
	$scope.isUnderTopics = -> $scope.path.length > 0 && $scope.path[0].name == 'Topics'
	
	$scope.isUnderATopic = -> $scope.path.length > 1 && $scope.path[0].name == 'Topics'
	
	$scope.startCramming = ->
		$scope.start = true
		$scope.complete = false
		Tallies.get
			fileid: $scope.file.id
			userid: $scope.userid
		, (result, response) ->
			null
		, (response) ->
			$scope.message = {type: 'error', text: response.data}		
		challenge()
	
	$scope.respond = ->
		correct = $scope.response == $scope.lesson.pairs[$scope.challengeIndex].back
		
		if correct
			$scope.message = {type: 'success', text: "Right!"}
		else
			$scope.message = {type: 'error', text: "Wrong: \"" + $scope.lesson.pairs[$scope.challengeIndex].back + "\""}
			
		$scope.response = ""
		Response.save
			userid: $scope.userid
			pairid: $scope.lesson.pairs[$scope.challengeIndex].id
			fileid: $scope.file.id
			correct: correct
		, (result, response) ->
			if result.done
				$scope.lesson.pairs.splice( $scope.challengeIndex, 1 )
				
				if $scope.lesson.pairs.length == 0
					$scope.complete = true
					$scope.message = {type: 'success', text: "You finished!"}
		, (response) ->
			$scope.message = {type: 'error', text: response.data}
			
		challenge()
	
	challenge = ->
		$scope.challengeIndex = randomInt(0, $scope.lesson.pairs.length)
		$scope.challenge = $scope.lesson.pairs[$scope.challengeIndex].front
		
	randomInt = (min, max) -> Math.floor(Math.random() * (max - min)) + min
		
	directory = (dir) ->
		Files.query {id: dir.id}, (result, response) ->
			$scope.file = undefined
			$scope.chunks = (result.slice(i, i + ChunkSize) for i in [0..result.length - 1] by ChunkSize)
		,	(response) ->
			$scope.message = {type: 'error', text: response.data}
		
	open = (file) ->
		Lessons.get {id: file.id}, (result, response) ->
			$scope.start = false
			challengeIndex = undefined
			$scope.file = file
			$scope.lesson = result
		,	(response) ->
			$scope.message = {type: 'error', text: response.data}
		
	
	] )