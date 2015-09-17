angular.module( 'admin' ).controller 'PostsController', ['$scope', '$resource', ($scope, $resource) ->
	angular.element('#titleInput').focus()
	Posts = $resource '/api/v1/posts/:id'
	
	$scope.categories = {}
	$scope.mode = 'post'
	$scope.status = 'live'
	$scope.commenting = 'on'
	$scope.message = {type: 'none'}
	
	getPosts = ->
		Posts.query (result, response) ->
			$scope.posts = result
		,	(response) ->
			$scope.message = {type: 'error', text: response.data}
		
	getPosts()
	
	$scope.clear = ->
		$scope.categories = {}
		$scope.status = 'live'
		$scope.commenting = 'on'
		$scope.message = {type: 'none'}
		$scope.title = ""
		$scope.content = ""
		
	$scope.delete = (post) ->
		$scope.message = {type: 'info', text: 'deleting post'}
		Posts.delete {id: $scope.id},
		(result, response) ->
			if result.deleted != 1
				$scope.message = {type: 'error', text: "delete failed"}
			else
				$scope.message = {type: 'success', text: "post deleted"}
			getPosts()
		, (response) ->
			$scope.error = response.data

	$scope.edit = (post) ->
		postcopy = angular.copy( post )
		$scope.mode = 'edit'
		$scope.categories = postcopy.categories
		$scope.message = {type: 'none'}
		$scope.title = postcopy.title
		$scope.content = postcopy.content
		$scope.status = postcopy.status
		$scope.commenting = postcopy.commenting
		$scope.id = postcopy.id
		
	$scope.update = ->
		categories = []
		
		for k, v of $scope.categories
			if v
				categories.push( parseInt(v) )
				
		if !$scope.title
			$scope.message = {type: 'warning', text: "There is no title."}
		else if !$scope.content
			$scope.message = {type: 'warning', text: "There is no content."}
		else if categories.length == 0
			$scope.message = {type: 'warning', text: "There is no category."}
		else
			$scope.message = {type: 'info', text: 'updating post'}
			Posts.save {id: $scope.id},
				title: $scope.title
				content: $scope.content
				status: $scope.status
				commenting: $scope.commenting
				categories: categories
			, (result, response) ->
				if result.updated != 1
					$scope.message = {type: 'error', text: "update failed"}
				else
					$scope.message = {type: 'success', text: "post updated"}
					
				getPosts()
			, (response) ->
				$scope.message = {type: 'error', text: response.data}
	
	$scope.post = ->
		$scope.mode = 'post'
		$scope.clear()
		
	$scope.submit = ->
		categories = []
		
		for k, v of $scope.categories
			if v
				categories.push( parseInt(v) )
				
		if !$scope.title
			$scope.message = {type: 'warning', text: "There is no title."}
		else if !$scope.content
			$scope.message = {type: 'warning', text: "There is no content."}
		else if categories.length == 0
			$scope.message = {type: 'warning', text: "There is no category."}
		else
			$scope.message = {type: 'info', text: 'submitting post'}
			Posts.save
				title: $scope.title
				content: $scope.content
				status: $scope.status
				commenting: $scope.commenting
				categories: categories
			, (result, response) ->
				$scope.message = {type: 'success', text: "post submitted"}
				getPosts()
			, (response) ->
				$scope.message = {type: 'error', text: response.data}
				
	$scope.keys = (obj) ->
		(for k, v of obj
			k).join(', ')
	]
