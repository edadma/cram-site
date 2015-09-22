app = angular.module 'cramsite', ['ngResource']

app.controller 'LessonEditFormController', ['$scope', '$resource', ($scope, $resource) ->
	
	$scope.clear = ->
		delete $scope.value
		
	]

app.controller 'MainController', ['$scope', '$resource', ($scope, $resource) ->
	$scope.message = {type: 'none'}
	$scope.path = []
	$scope.file = undefined
	ChunkSize = 6
	Pairs = $resource '/api/v1/pairs/:id'
	Files = $resource '/api/v1/files/:id'
	Lessons = $resource '/api/v1/lessons/:id'
	Response = $resource '/api/v1/response'
	Tallies = $resource '/api/v1/tallies/:fileid/:userid'
	Folders = $resource '/api/v1/folders/:parentid'
	
	home = ->
		$scope.show = 'directory'
		Files.query (result, response) ->
			$scope.path = []
			$scope.file = undefined
			$scope.chunks = (result.slice(i, i + ChunkSize) for i in [0..result.length - 1] by ChunkSize)
		,	(response) ->
			$scope.message = {type: 'error', text: response.data}

	home()
	
	$scope.updateFront = (index, value) ->
		$scope.editingFront = undefined
		Pairs.save {id: $scope.lessonData.pairs[index].id},
			front: value
			back: $scope.lessonData.pairs[index].back
		, (result, response) ->
			open($scope.file)
		, (response) ->
			$scope.message = {type: 'error', text: response.data}
		
	$scope.updateBack = (index, value) ->
		$scope.editingBack = undefined
		Pairs.save {id: $scope.lessonData.pairs[index].id},
			front: $scope.lessonData.pairs[index].front
			back: value
		, (result, response) ->
			open($scope.file)
		, (response) ->
			$scope.message = {type: 'error', text: response.data}
		
	$scope.selectFile = (file) ->
		if file.contents
			open(file)
		else
			enter(file)
			
	$scope.selectHome = ->
		home()

	$scope.selectElement = (index) ->
		$scope.path.splice( index + 1, $scope.path.length - index - 1 )
		directory($scope.path[index])
		
	$scope.showCreateFolder = -> $scope.path.length > 0 and $scope.path[0].name == 'Topics' and not $scope.file
	
	$scope.showModifyFolder = -> $scope.path.length > 1 and $scope.path[0].name == 'Topics' and not $scope.file
	
	$scope.createFolderForm = ->
		$scope.folderName = ""
		$scope.folderDescription = ""
		$scope.show = 'create-folder'
	
	$scope.createFolder = ->
		if $scope.folderName != ""
			Folders.save {parentid: $scope.path[$scope.path.length - 1].id},				
				name: $scope.folderName
				description: $scope.folderDescription
			, (result, response) ->
				enter(result)
			, (response) ->
				$scope.message = {type: 'error', text: response.data}
	
	$scope.createLessonForm = ->
		$scope.lessonName = ""
		$scope.lessonDescription = ""
		$scope.show = 'create-lesson'
	
	$scope.createLesson = ->
		if $scope.lessonName != ""
			Files.save {id: $scope.path[$scope.path.length - 1].id},				
				name: $scope.lessonName
				description: $scope.lessonDescription
			, (result, response) ->
				open(result)
			, (response) ->
				$scope.message = {type: 'error', text: response.data}
			
	$scope.startCramming = ->
		Tallies.get
			fileid: $scope.file.id
			userid: $scope.user.id
		, (result, response) ->
			$scope.start = true
			$scope.complete = false
			$scope.challengeIndex = undefined
			$scope.lesson = angular.copy( $scope.lessonData )
			$scope.message = {type: 'none'}
			challenge()
		, (response) ->
			$scope.message = {type: 'error', text: response.data}
	
	$scope.respond = ->
		correct = $scope.response == $scope.lesson.pairs[$scope.challengeIndex].back
		
		if correct
			$scope.message = {type: 'success', text: 'Right!'}
		else
			$scope.message = {type: 'error', text: 'Wrong: "' + $scope.lesson.pairs[$scope.challengeIndex].back + '"'}
			
		Response.save
			userid: $scope.user.id
			pairid: $scope.lesson.pairs[$scope.challengeIndex].id
			fileid: $scope.file.id
			correct: correct
		, (result, response) ->
			if result.done
				if $scope.lesson.pairs.length == 1
					$scope.complete = true
					$scope.message = {type: 'success', text: 'You finished!'}
			$scope.response = ''
			challenge( result.done )
		, (response) ->
			$scope.message = {type: 'error', text: response.data}
	
	$scope.editFront = (index) ->
		$scope.value = $scope.lessonData.pairs[index].front
		$scope.editingFront = index
		$scope.editingBack = undefined
	
	$scope.editBack = (index) ->
		$scope.value = $scope.lessonData.pairs[index].back
		$scope.editingBack = index
		$scope.editingFront = undefined
	
	$scope.remove = (index) ->
		console.log index
	
	$scope.add = ->
		console.log [$scope.front, $scope.back]
	
	challenge = (done) ->
		if !$scope.complete
			if done
				$scope.lesson.pairs.splice( $scope.challengeIndex, 1 )

			if $scope.lesson.pairs.length == 1
				$scope.challengeIndex = 0
			else
				if done or $scope.challengeIndex == undefined
					$scope.challengeIndex = randomInt( 0, $scope.lesson.pairs.length )
				else
					indices = (i for i in [0..$scope.lesson.pairs.length - 1])
					indices.splice( $scope.challengeIndex, 1 )
					$scope.challengeIndex = indices[randomInt(0, $scope.lesson.pairs.length - 1)]
				
			$scope.challenge = $scope.lesson.pairs[$scope.challengeIndex].front
		
	randomInt = (min, max) -> Math.floor(Math.random()*(max - min)) + min
		
	directory = (dir) ->
		$scope.message = {type: 'none'}
		$scope.file = undefined
		$scope.show = 'directory'
		Files.query {id: dir.id}, (result, response) ->
			if result.length == 0
				$scope.chunks = []
			else
				$scope.chunks = (result.slice(i, i + ChunkSize) for i in [0..result.length - 1] by ChunkSize)
		,	(response) ->
			$scope.message = {type: 'error', text: response.data}
		
	open = (file) ->
		$scope.message = {type: 'none'}
		$scope.show = 'file'
		$scope.editingFront = undefined
		$scope.editingBack = undefined
		Lessons.get {id: file.id}, (result, response) ->
			$scope.start = false
			challengeIndex = undefined
			$scope.file = file
			$scope.lessonData = result
		,	(response) ->
			$scope.message = {type: 'error', text: response.data}
		
	enter = (dir) ->
		$scope.path.push dir
		directory(dir)

	]