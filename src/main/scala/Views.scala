package xyz.hyperreal.cramsite

import com.typesafe.config.ConfigFactory
import com.github.kxbmap.configs._

import in.azeemarshad.common.sessionutils.Session

import org.joda.time.{DateTime, Instant}
import org.joda.time.format.DateTimeFormat

import models._


object Views {
	val ANGULARJS = "1.4.4"
	
  val conf = ConfigFactory.load

	def main( title: String )( head: xml.Node = xml.Group(Nil) )( content: xml.Node ) =
		<html lang="en">
			<head>
				<meta charset="utf-8"/>
				<meta http-equiv="X-UA-Compatible" content="IE=edge"/>
				<meta name="viewport" content="width=device-width, initial-scale=1"/>
				
				<title>{title}</title>

				<link rel="shortcut icon" href="/favicon.ico"/>
				
				<link href="/webjars/bootstrap/3.3.5/css/bootstrap.min.css" rel="stylesheet"/>
				<link href="/webjars/bootstrap/3.3.5/css/bootstrap-theme.min.css" rel="stylesheet"/>
				
				<script src="/webjars/jquery/1.11.1/jquery.min.js"></script>
				{head}
			</head>
			
			<body>
				{content}
				
				<script src="/webjars/bootstrap/3.3.5/js/bootstrap.min.js"></script>
			</body>
		</html>
	
	def index( user: dao.User ) =
		main( "The Cram Site" ) {
			<xml:group>
				<link href="/sass/main.css" rel="stylesheet"/>
				<script src={s"/webjars/angularjs/$ANGULARJS/angular.min.js"}></script>
				<script src={s"/webjars/angularjs/$ANGULARJS/angular-resource.min.js"}></script>
				<script src="/coffee/main.js"></script>
			</xml:group>
		} {
			<div ng-app="cramsite" ng-controller="MainController" ng-init={"user = {id: " + user.id.get + ", name: " + user.name + ", status: " + user.status + "}"} ng-cloak="">
				<div class="jumbotron">
					<div class="container"> {
						if (user.status == GUEST)
							<div class="pull-right"><h1><button class="btn btn-primary thin-right">Sign in</button><button class="btn btn-default">Sign up</button></h1></div>
						else
							<div class="pull-right"><h1><span class="label thin-right">{user.name}</span><button class="btn btn-primary">Sign out</button></h1></div>
						}
						<h1>The Cram Site</h1>
						<p>for cramming information into your head <em>fast</em></p>
					</div>
				</div>
				
				<div class="breadcrumb">
					<ol class="container breadcrumb">
						<li><a ng-click="selectHome()">Home</a></li>
						<li ng-repeat="e in path"><a ng-click="selectElement($index)">{xml.Unparsed("{{e.name + ($last ? '' : '\u00A0')}}")}</a></li>
						<li class="active">{"{{file.name}}"}</li>
					</ol>
				</div>
				
				<nav class="navbar navbar-default">
					<div class="container">
						<button ng-show={"file && !start && lessonData.pairs.length > 0"} ng-click="startCramming()" class="btn btn-success navbar-btn">Start Cramming!</button>
						<button ng-show={"file && !start"} class="btn btn-default navbar-btn">Edit</button>
						<button ng-show={"file && start"} ng-click="selectFile(file)" class="btn btn-danger navbar-btn">Stop Cramming</button>
						<button ng-show={"file && start"} ng-click="startCramming()" class="btn btn-success navbar-btn">Restart Cram Session</button>
						<button ng-show="showModifyFolder()" ng-click="createLessonForm()" class="btn btn-primary navbar-btn">Create Lesson</button>
						<button ng-show="showCreateFolder()" ng-click="createFolderForm()" class="btn btn-default navbar-btn">Create Topic</button>
						<button ng-show="showModifyFolder()" ng-click="renameFolder()" class="btn btn-default navbar-btn">Edit Topic</button>
					</div>
				</nav>
				
				<div class="main container">
					
					<div ng-show="show == 'file'">
						<div class="row">
							<div ng-show={"start && !complete"}>
								<div class="col-md-6">
									<form ng-submit="respond()">
										<div class="form-group">
											<label>Challenge:</label>
											<p>{"{{challenge}}"}</p>
										</div>
										<div class="form-group">
											<label>Response:</label>
											<input type="text" class="form-control" ng-model="response" placeholder="Enter your answer" autofocus=""/>
										</div>
										<div class="form-group">
											<input class="btn btn-primary" type="submit"/>
										</div>
									</form>
								</div>
							</div>
							
							<div ng-hide="start">
								<div class="col-md-8">
									<div class="panel panel-default">
										<div class="panel-heading">{"{{file.name}}"}</div>
										<div class="panel-body"><p>{"{{file.description}}"}</p></div>
										<table class="table">
											<tr>
												<th>Front</th>
												<th>Back</th>
											</tr>
											<tr ng-repeat="pair in lessonData.pairs">
												<td>{"{{pair.front}}"}</td>
												<td>{"{{pair.back}}"}</td>
											</tr>
										</table>
									</div>
								</div>
							</div>
						</div>
					</div>
					
					<div ng-show="show == 'create-folder'">
						<form ng-submit="createFolder()" class="form-inline">
							<div class="form-group">
								<input type="text" class="form-control" ng-model="folderName" placeholder="Enter topic name" autofocus=""/>
							</div>
							<div class="form-group">
								<input type="text" class="form-control" ng-model="folderDescription" placeholder="Enter topic description"/>
							</div>
							<button type="submit" class="btn btn-default">Create</button>
						</form>
					</div>
					
					<div ng-show="show == 'create-lesson'">
						<form ng-submit="createLesson()" class="form-inline">
							<div class="form-group">
								<input type="text" class="form-control" ng-model="lessonName" placeholder="Enter lesson name" autofocus=""/>
							</div>
							<div class="form-group">
								<input type="text" class="form-control" ng-model="lessonDescription" placeholder="Enter lesson description"/>
							</div>
							<button type="submit" class="btn btn-default">Create</button>
						</form>
					</div>
					
					<div ng-show="show == 'directory'">
						<div class="row" ng-repeat="chunk in chunks">
							<div class="col-md-2" ng-repeat="file in chunk">
								<a class="thumbnail" ng-click="selectFile(file)">
									<img class="img-rounded"/>
									<div class="caption">
										<h4>{"{{file.name}}"}</h4>
										<p>{"{{file.description}}"}</p>
									</div>
								</a>
							</div>
						</div>
					</div>
					
					<div class="row">
						<div class="col-md-6">
							<ng-include src="'/message.html'"></ng-include>
						</div>
					</div>
					
				</div>

			</div>
		}

	def login = {
		main( "Login" ) {
			<link href="/css/signin.css" rel="stylesheet"/>
		} {
			<div class="container">

				<form class="form-signin" action="/login" method="POST">
					<h2 class="form-signin-heading">Please sign in</h2>
					<label for="inputEmail" class="sr-only">Email address</label>
					<input type="email" name="email" id="inputEmail" class="form-control" placeholder="Email address" required="" autofocus=""/>
					<label for="inputPassword" class="sr-only">Password</label>
					<input type="password" name="password" id="inputPassword" class="form-control" placeholder="Password" required=""/>
					<!-- <div class="checkbox">
						<label>
							<input type="checkbox" name="rememberme" value="yes"/> Remember me
						</label>
					</div> -->
					<button class="btn btn-lg btn-primary btn-block" type="submit">Sign in</button>
				</form>

			</div>
		}
	}
	
	def register( role: Option[(Int, String, String, String)] ) =
		main( "Registration" ) {
			<xml:group>
				<link href="/css/register.css" rel="stylesheet"/>
				<script src={s"/webjars/angularjs/$ANGULARJS/angular.min.js"}></script>
				<script src={s"/webjars/angularjs/$ANGULARJS/angular-resource.min.js"}></script>
				<script src="/coffee/register.js"></script>
			</xml:group>
		} {
			<div class="container" ng-app="register" ng-controller="RegisterController">
				<form class="form-register" ng-submit={if (role == None) "submit()" else s"submit({blogid: ${role.get._1}, role: '${role.get._2}'}, '${role.get._3}')"}>
					<h2 class="form-register-heading">Registration</h2>
					<div class="form-group">
						<input type="text" class="form-control" ng-model="user.name" placeholder="Name*" required="" autofocus=""/></div>
					<div class="form-group">
						<input type="email" class="form-control" ng-model="user.email" placeholder="Email address*" required=""/></div>
					<div class="form-group">
						<input type="password" class="form-control" ng-model="user.password" placeholder="Password*" required=""/></div>
					<div class="form-group">
						<input type="url" class="form-control" ng-model="user.url" placeholder="URL"/></div>
					<div class="form-group">
						<textarea class="form-control" rows="4" cols="50" ng-model="user.bio" placeholder="Bio"></textarea></div>
					<div class="form-group"> {
						if (role == None)
							<button type="submit" class="btn btn-lg btn-primary btn-block">Register</button>
						else {
							<button ng-hide="message.type == 'success'" type="submit" class="btn btn-lg btn-primary btn-block">Register</button>
							<a ng-show="message.type == 'success'" class="btn btn-lg btn-success btn-block" ng-href={s"http://{{'${role.get._4}'}}"}>Check it out!</a>
						}
					}</div>
					<div><ng-include src="'/message.html'"></ng-include></div>
				</form>
			</div>
		}
	
	def admin =
		main( "Dashboard" ) {
			<xml:group>
				<link href="/css/admin.css" rel="stylesheet"/>
				<script src={s"/webjars/angularjs/$ANGULARJS/angular.min.js"}></script>
				<script src={s"/webjars/angularjs/$ANGULARJS/angular-sanitize.min.js"}></script>
				<script src={s"/webjars/angularjs/$ANGULARJS/angular-resource.min.js"}></script>
				<script src={s"/webjars/angularjs/$ANGULARJS/angular-route.min.js"}></script>
				<script src="/coffee/admin.js"></script>
				<script src="/coffee/posts.js"></script>
				<script src="/coffee/visits.js"></script>
			</xml:group>
		} {
			<div ng-app="admin" ng-controller="AdminController">
				<nav class="navbar navbar-default navbar-fixed-top">
					<div class="container-fluid">
						<div class="navbar-header">
							<button type="button" class="navbar-toggle collapsed" data-toggle="collapse" data-target="#navbar" aria-expanded="false" aria-controls="navbar">
								<span class="sr-only">Toggle navigation</span>
								<span class="icon-bar"></span>
								<span class="icon-bar"></span>
								<span class="icon-bar"></span>
							</button>
							<a class="navbar-brand" href="/">main</a>
						</div>
						<div id="navbar" class="navbar-collapse collapse">
							<ul class="nav navbar-nav navbar-right">
								<!-- <li><a href="/admin">Dashboard</a></li>
								<li><a href="#">Settings</a></li>
								<li><a href="#">Profile</a></li> -->
								<li><a href="/logout">Logout</a></li>
							</ul>
							<form class="navbar-form navbar-right">
								<input type="search" class="form-control" placeholder="Search..."/>
							</form>
						</div>
					</div>
				</nav>
				
				<div class="container-fluid">

					<div class="row">
						
						<div class="col-sm-3 col-md-2 sidebar">
							<ul class="nav nav-sidebar">
								<!-- <li class="active"><a href="#">Overview <span class="sr-only">(current)</span></a></li> -->
								<li><a href="/admin#/posts">Posts</a></li>
								<li><a href="/admin#/visits">Visits</a></li>
							</ul>
						</div>
						
						<div class="col-sm-9 col-sm-offset-3 col-md-10 col-md-offset-2 main">
								<div ng-view=""></div>
						</div>
								
					</div>
					
				</div>
			</div>
		}
		
	def adminVisits =
		<xml:group>
			<h1 class="page-header">Visits</h1>

			<div class="table-responsive">
				<table class="table table-striped table-hover">
					<thead>
						<tr>
							<th>#</th>
							<th>Date</th>
							<th>Time</th>
							<th>IP</th>
							<th>Host</th>
							<th>Path</th>
							<th>Referrer</th>
							<th>User #</th>
							<th>User Name</th>
						</tr>
					</thead>
					<tbody>
						<tr ng-repeat="visit in visits" ng-cloak="">
							<td>{"{{visit.id}}"}</td>
							<td>{"{{visit.date.millis | date: 'yy-MM-dd'}}"}</td>
							<td>{"{{visit.date.millis | date: 'HH:mm'}}"}</td>
							<td>{"{{visit.ip}}"}</td>
							<td>{"{{visit.host}}"}</td>
							<td>{"{{visit.path}}"}</td>
							<td>{"{{visit.referrer}}"}</td>
							<td>{"{{visit.userid}}"}</td>
							<td>{"{{visit.username}}"}</td>
						</tr>
					</tbody>
				</table>
			</div>
			
			<div><ng-include src="'/message.html'"></ng-include></div>
		</xml:group>
	
}
