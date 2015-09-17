app = angular.module 'register', ['ngResource']

app.controller( 'RegisterController', ['$scope', '$resource', ($scope, $resource) ->
	
	Users = $resource '/api/v1/users/:id'

	$scope.message = {type: 'none'}
	
	$scope.submit = (role, title) ->
		if angular.isDefined( role )
			$scope.user.role = role
			
		Users.save $scope.user, (result, response) ->
			if angular.isDefined( role )
				$scope.message = {type: 'success', text: "User created: " + role.role + " for '" + title + "'"}
			else
				$scope.message = {type: 'success', text: "User created"}
		, (response) ->
			$scope.message = {type: 'error', text: response.data}
	] )