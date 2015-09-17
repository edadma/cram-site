app = angular.module 'admin', ['ngSanitize', 'ngResource', 'ngRoute']

app.controller 'AdminController', ['$scope', '$resource', ($scope, $resource) ->
	]

app.config ['$routeProvider', ($routeProvider) ->
	$routeProvider
		.when( '/', redirectTo: '/posts' )
		.when '/posts',
			templateUrl: '/admin/posts'
			controller: 'PostsController'
		.when '/visits',
			templateUrl: '/admin/visits'
			controller: 'VisitsController'
		.otherwise redirectTo: '/posts'
	]
