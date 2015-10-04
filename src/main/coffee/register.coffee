app = angular.module 'register', ['ngResource']

app.controller( 'RegisterController', ['$scope', '$resource', '$window', ($scope, $resource, $window) ->
	
	Users = $resource '/api/v1/users/:id'

	$scope.message = {type: 'none'}
	
	$scope.checkName = ->
		if $scope.user.name
			Users.get {id: 'exists', name: $scope.user.name}, (result, response) ->
				$scope.nameStatus = if result.exists then 'exists' else 'available'
			, (response) ->
				$scope.message = {type: 'error', text: response.data}
		else
			$scope.nameStatus = ''
	
	$scope.checkEmail = ->
		if $scope.user.email
			Users.get {id: 'exists', email: $scope.user.email}, (result, response) ->
				$scope.emailStatus = if result.exists then 'exists' else 'available'
			, (response) ->
				$scope.message = {type: 'error', text: response.data}
		else
			$scope.nameStatus = ''
	
	$scope.submit = ->
		Users.save $scope.user, (result, response) ->
			$scope.message = {type: 'success', text: "User created"}
			$window.location.href = '/'
		, (response) ->
			$scope.message = {type: 'error', text: response.data}
	] )