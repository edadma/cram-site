package xyz.hyperreal.cramsite

import spray.json._

import com.typesafe.config.ConfigFactory
import com.github.kxbmap.configs._

import in.azeemarshad.common.sessionutils.Session

import org.joda.time.{DateTime, Instant}
import org.joda.time.format.DateTimeFormat

import models._


object Views {
	val ANGULARJS = "1.4.7"
	
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
				
				{xml.Unparsed(conf.get[String]("init.endcode"))}
			</body>
		</html>
	
	def index( user: dao.User ) =
		main( "The Cram Site" ) {
			<xml:group>
				<link href="/sass/main.css" rel="stylesheet"/>
				<script src={s"/webjars/angularjs/$ANGULARJS/angular.min.js"}></script>
				<script src={s"/webjars/angularjs/$ANGULARJS/angular-resource.min.js"}></script>
				<script src="/webjars/nervgh-angular-file-upload/2.1.1/angular-file-upload.min.js"></script>
				<script src="/coffee/main.js"></script>
				<script src="/js/ng-target.js"></script>
			</xml:group>
		} {
			<div ng-app="cramsite" ng-controller="MainController" ng-init={"user = " + user.toJson.compactPrint} ng-cloak="">
				<div class="jumbotron">
					<div class="container"> {
						if (user.status == GUEST)
							<div class="pull-right"><h1><a class="btn btn-primary thin-right" href="/login">Sign in</a><a class="btn btn-default" href="/register">Sign up</a></h1></div>
						else
							<div class="pull-right"><h1><a class="btn btn-primary" href="/logout">Sign out</a></h1><span class="well well-sm">{user.name.get}</span></div>
						}
						<h1><!-- <img src="/Books-icon.png"/> -->The Cram Site</h1>
						<p>for cramming information into your head <em>fast</em></p>
					</div>
				</div>
				
				<div class="breadcrumb">
					<ol class="container breadcrumb">
						<li><a class="clickable" ng-click="selectHome()">Home</a></li>
						<li ng-repeat="e in path"><a class="clickable" ng-click="selectElement($index)">{xml.Unparsed("{{e.name + ($last ? '' : '\u00A0')}}")}</a></li>
						<li class="active">{"{{file.name}}"}</li>
					</ol>
				</div>
				
				<nav class="navbar navbar-default">
					<div class="container">
						<button ng-show={"file && !start && lessonData.pairs.length > 0"} ng-click="startCramming()" class="btn btn-success navbar-btn">Start Cramming!</button>
						<button ng-show={"file && !start && canModifyContents()"} ng-click="editLessonForm()" class="btn btn-default navbar-btn">Edit Lesson</button>
						<button ng-show={"file && start"} ng-click="selectFile(file)" class="btn btn-danger navbar-btn">Stop Cramming</button>
						<button ng-show={"file && start"} ng-click="startCramming()" class="btn btn-success navbar-btn">Restart Cram Session</button>
						<button ng-show={"file && notPrivate()"} ng-click="favoriteLesson()" class="btn btn-default navbar-btn"><img src="/Places-favorites-icon.png"/> Add to Favorites</button>
						<button ng-show="canCreateLesson()" ng-click="createLessonForm()" class="btn btn-primary navbar-btn">Create Lesson</button>
						<button ng-show="canCreateLesson()" ng-click="inputLessonForm()" class="btn btn-primary navbar-btn">Input Lesson</button>
						<button ng-show="canCreateFolder()" ng-click="createFolderForm()" class="btn btn-default navbar-btn">Create Topic</button>
						<button ng-show="canModifyFolder()" ng-click="editFolderForm()" class="btn btn-default navbar-btn">Edit Topic</button>
					</div>
				</nav>
				
				<div class="main container">
					
					<div ng-show="show == 'file'">
						<div class="row">
							<div ng-show={"start && !complete"} ng-keypress="next()">
								<div class="col-md-6">
									<form ng-submit="respond()">
										<div class="form-group">
											<label>Challenge ({"{{side}}"}):</label>
											<p>{"{{challenge}}"}</p>
										</div>
										<div class="form-group">
											<label>Response:</label>
											<input ng-hide="inputDisabled" type="text" class="form-control" ng-model="response" placeholder="Enter your answer" ng-target="responseInputTarget"/>
											<div ng-show="inputDisabled" class="well well-sm">{"You typed: {{response}}"}</div>
										</div>
										<div class="form-group">
											<input ng-hide="inputDisabled" type="submit" class="btn btn-primary"/>
											<button ng-show="inputDisabled" class="btn btn-primary" ng-target="responseButtonTarget">Continue</button> <!-- ng-click="next()" -->
										</div>
									</form>
								</div>
								<div class="col-md-4">
									<p>Progress</p>
									<div class="progress">
										<div class="progress-bar" role="progressbar" style="min-width: 2em; width: {{progress}}%;">
											{"{{progress}}"}%
										</div>
									</div>
								</div>
							</div>
							
							<div ng-hide="start">
								<div class="col-md-12">
									<div class="panel panel-default">
										<div class="panel-heading">{"{{file.name}}"}</div>
										<div class="panel-body"><p>{"{{file.description}}"}</p></div>
										<table class="table table-hover" style="width: 100%">
											<tr>
												<th>Front</th>
												<th>Back</th>
											</tr>
											<tr ng-repeat="pair in lessonData.pairs" ng-show="canModifyContents()">
												<td ng-click="editFront($index)">
													<span ng-hide="editingFront == $index" ng-bind="pair.front"></span>
													<form ng-show="editingFront == $index" ng-submit="updateFront($index, value)" ng-controller="LessonEditFormController">
														<input type="text" class="form-control" ng-model="value" ng-blur="clear()" ng-target="pair.frontInputTarget"/>
													</form>
												</td>
												<td ng-click="editBack($index)">
													<span ng-hide="editingBack == $index" ng-bind="pair.back"></span>
													<form ng-show="editingBack == $index" ng-submit="updateBack($index, value)" ng-controller="LessonEditFormController">
														<input type="text" class="form-control" ng-model="value" ng-blur="clear()" ng-target="pair.backInputTarget"/>
													</form>
												</td>
												<td width="1"><button class="btn btn-danger" ng-click="remove($index)">Remove</button></td>
											</tr>
											<tr ng-repeat="pair in lessonData.pairs" ng-show="!canModifyContents()">
												<td>{"{{pair.front}}"}</td>
												<td>{"{{pair.back}}"}</td>
											</tr>
											<tr ng-show="canModifyContents()">
												<form ng-submit="add()">
													<td><input type="text" class="form-control" ng-model="front" placeholder="New flashcard front" ng-target="addInputTarget"/></td>
													<td><input type="text" class="form-control" ng-model="back" placeholder="New flashcard back"/></td>
													<td width="1"><input type="submit" class="btn btn-primary form-control" value="Add"/></td>
												</form>
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
								<input type="text" class="form-control" ng-model="fileName" placeholder="Topic name" ng-target="createFolderInputTarget"/>
							</div>
							<div class="form-group">
								<input type="text" class="form-control" ng-model="fileDescription" placeholder="Topic description"/>
							</div>
							<button type="submit" class="btn btn-default">Submit</button>
						</form>
					</div>
					
					<div ng-show="show == 'edit-folder'">
						<form ng-submit="editFolder()" class="form-inline">
							<div class="form-group">
								<input type="text" class="form-control" ng-model="fileName" placeholder="New Topic name" ng-target="editFolderInputTarget"/>
							</div>
							<div class="form-group">
								<input type="text" class="form-control" ng-model="fileDescription" placeholder="New Topic description"/>
							</div>
							<button type="submit" class="btn btn-default">Submit</button>
						</form>
					</div>
					
					<div ng-show="show == 'create-lesson'">
						<form ng-submit="createLesson()" class="form-inline">
							<div class="form-group">
								<input type="text" class="form-control" ng-model="lessonName" placeholder="Enter lesson name" ng-target="createLessonInputTarget"/>
							</div>
							<div class="form-group">
								<input type="text" class="form-control" ng-model="lessonDescription" placeholder="Enter lesson description"/>
							</div>
							<button type="submit" class="btn btn-default">Submit</button>
						</form>
					</div>
					
					<div ng-show="show == 'input-lesson'">
						<form ng-submit="inputLesson()">
							<div class="form-group">
								<textarea class="form-control" rows="4" cols="50" ng-model="lessonData" placeholder="Enter lesson data" ng-target="inputLessonInputTarget"></textarea>
							</div>
							<button type="submit" class="btn btn-default">Submit</button>
						</form>
					</div>
					
					<div ng-show="show == 'edit-lesson'">
						<form ng-submit="editLesson()" class="form-inline">
							<div class="form-group">
								<input type="text" class="form-control" ng-model="fileName" placeholder="New Topic name" ng-target="editLessonInputTarget"/>
							</div>
							<div class="form-group">
								<input type="text" class="form-control" ng-model="fileDescription" placeholder="New Topic description"/>
							</div>
							<button type="submit" class="btn btn-default">Submit</button>
						</form>
					</div>
					
					<!-- <div ng-show="show == 'upload-lesson'">
						<div ng-controller="uploadFormController">
							<input type="file" nv-file-select="" uploader="uploader"/>
							<div ng-repeat="item in uploader.queue">
								<button ng-disabled="uploaded" ng-click="item.upload()">upload</button>
							</div>
						</div>
					</div> -->
					
					<div ng-show="show == 'directory'">
						<div class="row" ng-repeat="chunk in chunks">
							<div class="col-md-2" ng-repeat="file in chunk">
								<a class="clickable" ng-click="selectFile(file)">
									<img class="center-block img-rounded" ng-src="/image/{{file.imageid}}"/>
									<div class="caption">
										<h4 class="text-center text-wrap">{"{{file.name}}"}</h4>
										<p class="text-center text-wrap">{"{{file.description}}"}</p>
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

// 				<footer class="footer">
// 					<div class="container">
// 						<div class="row">
// 							<div class="col-md-12">
// 								<image class="center-block" src=""/>
// 							</div>
// 						</div>
// 					</div>
// 				</footer>

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
	
	def register =
		main( "Registration" ) {
			<xml:group>
				<link href="/css/register.css" rel="stylesheet"/>
				<script src={s"/webjars/angularjs/$ANGULARJS/angular.min.js"}></script>
				<script src={s"/webjars/angularjs/$ANGULARJS/angular-resource.min.js"}></script>
				<script src="/coffee/register.js"></script>
			</xml:group>
		} {
			<div class="container" ng-app="register" ng-controller="RegisterController">
				<form class="form-register" ng-submit="submit()">
					<h2 class="form-register-heading">Registration</h2>
					<div class="form-group">
						<input type="text" class="form-control" ng-model="user.name" ng-model-options="{debounce: 300}" ng-change="checkName()"
							placeholder="Name*" required="" autofocus=""/></div>
						<p class="text-danger" ng-show="nameStatus == 'exists'">This name is not available. Try another one.</p>
						<p class="text-success" ng-show="nameStatus == 'available'">This name is available.</p>
					<div class="form-group">
						<input type="email" class="form-control" ng-model="user.email" ng-model-options="{debounce: 300}" ng-change="checkEmail()"
							placeholder="Email address*" required=""/></div>
						<p class="text-danger" ng-show="emailStatus == 'exists'">This email is associated with an registered user.</p>
					<div class="form-group">
						<input type="password" class="form-control" ng-model="user.password" placeholder="Password*" required=""/></div>
					<div class="form-group">
						<textarea class="form-control" rows="4" cols="50" ng-model="user.description" placeholder="Description"></textarea></div>
					<div class="form-group"> {
						<button type="submit" class="btn btn-lg btn-primary btn-block">Register</button>
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
