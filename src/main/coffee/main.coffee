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
	Tallies = $resource '/api/v1/tallies/:id1/:id2'
	Folders = $resource '/api/v1/folders/:id'
	LIMIT = 3
	
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
		$scope.folderName = ''
		$scope.folderDescription = ''
		$scope.show = 'create-folder'
	
	$scope.createFolder = ->
		if $scope.folderName != ''
			Folders.save {parentid: $scope.path[$scope.path.length - 1].id},				
				name: $scope.folderName
				description: $scope.folderDescription
			, (result, response) ->
				enter(result)
			, (response) ->
				$scope.message = {type: 'error', text: response.data}
	
	$scope.editFolderForm = ->
		$scope.folderName = ''
		$scope.folderDescription = ''
		$scope.show = 'edit-folder'
	
	$scope.editFolder = ->
		if $scope.folderName != ''
			Folders.save {id: $scope.path[$scope.path.length - 1].id},				
				name: $scope.folderName
				description: $scope.folderDescription
			, (result, response) ->
				$scope.path[$scope.path.length - 1].name = $scope.folderName
				$scope.path[$scope.path.length - 1].description = $scope.folderDescription
				$scope.show = 'directory'
			, (response) ->
				$scope.message = {type: 'error', text: response.data}
	
	$scope.createLessonForm = ->
		$scope.lessonName = ''
		$scope.lessonDescription = ''
		$scope.show = 'create-lesson'
	
	$scope.createLesson = ->
		if $scope.lessonName != ''
			Files.save {id: $scope.path[$scope.path.length - 1].id},				
				name: $scope.lessonName
				description: $scope.lessonDescription
			, (result, response) ->
				open(result)
			, (response) ->
				$scope.message = {type: 'error', text: response.data}
			
	$scope.startCramming = ->
		$scope.start = true
		$scope.complete = false
		$scope.challengeIndex = undefined
		$scope.message = {type: 'none'}
		$scope.lesson = angular.copy $scope.lessonData
		$scope.lesson.tallies = ({foreward: 0, backward: 0} for i in [0..$scope.lesson.pairs.length - 1])
		challenge()
		Tallies.get
			id1: $scope.file.id
			id2: $scope.user.id
		, (result, response) ->
			null
		, (response) ->
			$scope.message = {type: 'error', text: response.data}
	
	$scope.respond = ->
		standard = if $scope.side == 'front' then $scope.lesson.pairs[$scope.challengeIndex].back else $scope.lesson.pairs[$scope.challengeIndex].front
		change =
			if $scope.response == standard
				$scope.message = {type: 'success', text: 'Right!'}
				1
			else
				$scope.message = {type: 'error', text: 'Wrong: "' + standard + '"'}
				-2
		
		if $scope.side == 'front'
			$scope.lesson.tallies[$scope.challengeIndex].foreward += change
		else
			$scope.lesson.tallies[$scope.challengeIndex].backward += change
	
		if $scope.lesson.tallies[$scope.challengeIndex].foreward < 0
			$scope.lesson.tallies[$scope.challengeIndex].foreward = 0
			
		if $scope.lesson.tallies[$scope.challengeIndex].backward < 0
			$scope.lesson.tallies[$scope.challengeIndex].backward = 0
		
		done =
			$scope.lesson.tallies[$scope.challengeIndex].foreward >= LIMIT and
				(if $scope.lesson.info.direction == 'simplex' then true else ($scope.lesson.tallies[$scope.challengeIndex].backward >= LIMIT))
		
		if done
			if $scope.lesson.pairs.length == 1
				$scope.complete = true
				$scope.message = {type: 'success', text: 'You finished the lesson!'}
				
		$scope.response = ''
		Tallies.save
			id1: $scope.user.id
			id2: $scope.lesson.pairs[$scope.challengeIndex].id
		,
			foreward: $scope.lesson.tallies[$scope.challengeIndex].foreward
			backward: $scope.lesson.tallies[$scope.challengeIndex].backward
		, (result, response) ->
			null
		, (response) ->
			$scope.message = {type: 'error', text: response.data}
		challenge( done )
	
	$scope.editFront = (index) ->
		$scope.value = $scope.lessonData.pairs[index].front
		$scope.editingFront = index
		$scope.editingBack = undefined
	
	$scope.editBack = (index) ->
		$scope.value = $scope.lessonData.pairs[index].back
		$scope.editingBack = index
		$scope.editingFront = undefined
	
	$scope.remove = (index) ->
		Pairs.delete {id: $scope.lessonData.pairs[index].id}, (result, response) ->
			open($scope.file)
		,	(response) ->
			$scope.message = {type: 'error', text: response.data}
	
	$scope.add = ->
		Lessons.save {id: $scope.file.id}
			front: $scope.front
			back: $scope.back
		, (result, response) ->
			open($scope.file)
		,	(response) ->
			$scope.message = {type: 'error', text: response.data}
		
	challenge = (done) ->
		if !$scope.complete
			if done
				$scope.lesson.pairs.splice( $scope.challengeIndex, 1 )
				$scope.lesson.tallies.splice( $scope.challengeIndex, 1 )

			if $scope.lesson.pairs.length == 1
				$scope.challengeIndex = 0
			else
				if done or $scope.challengeIndex == undefined
					$scope.challengeIndex = random( $scope.lesson.pairs.length )
				else
					indices = (i for i in [0..$scope.lesson.pairs.length - 1])
					indices.splice( $scope.challengeIndex, 1 )
					$scope.challengeIndex = indices[random( $scope.lesson.pairs.length - 1 )]
				
			if $scope.lesson.info.direction == 'duplex'
				$scope.side = if random( 2 ) == 0 then 'front' else 'back'
			else
				$scope.side = 'front'
				
			$scope.challenge = if $scope.side == 'front' then $scope.lesson.pairs[$scope.challengeIndex].front else $scope.lesson.pairs[$scope.challengeIndex].back
		
	random = (range) -> Math.floor( Math.random()*range )
		
	directory = (dir) ->
		$scope.message = {type: 'none'}
		$scope.file = undefined
		Files.query {id: dir.id}, (result, response) ->
			if result.length == 0
				$scope.chunks = []
			else
				$scope.chunks = (result.slice(i, i + ChunkSize) for i in [0..result.length - 1] by ChunkSize)
			$scope.show = 'directory'
		,	(response) ->
			$scope.message = {type: 'error', text: response.data}
		
	open = (file) ->
		$scope.message = {type: 'none'}
		$scope.front = ''
		$scope.back = ''
		$scope.editingFront = undefined
		$scope.editingBack = undefined
		Lessons.get {id: file.id}, (result, response) ->
			$scope.start = false
			challengeIndex = undefined
			$scope.file = file
			$scope.lessonData = result
			$scope.show = 'file'
		,	(response) ->
			$scope.message = {type: 'error', text: response.data}
		
	enter = (dir) ->
		$scope.path.push dir
		directory(dir)

	]
