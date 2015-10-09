package xyz.hyperreal.cramsite

import com.typesafe.config.ConfigFactory
import com.github.kxbmap.configs._

import akka.actor.ActorSystem

import spray.routing._
import spray.http._
import spray.json._
import spray.json.DefaultJsonProtocol._
import spray.httpx.SprayJsonSupport._
import MediaTypes._

import shapeless._

import in.azeemarshad.common.sessionutils.SessionDirectives

import concurrent.duration._
import util.{Success, Failure}

import models._


object Main extends App with SimpleRoutingApp with SessionDirectives {

	Startup
	
	lazy implicit val akka = ActorSystem( "on-spray-can" )
	implicit val context = akka.dispatcher

  val conf = ConfigFactory.load

  startServer( "localhost", 8082 ) {
	
// 		def blog: Directive[dao.Blog :: HNil] = hostName hflatMap {
// 			case h :: HNil =>
// 				await( dao.Blogs.find(h) ) match {
// 					case None => reject
// 					case Some( b ) => provide( b )
// 				}
//		}
		
		def optionalUser: Directive[Option[dao.User] :: HNil] = optionalSession hflatMap {
			case None :: HNil => provide( None )
			case Some( s ) :: HNil => provide( await(dao.Users.find(s.data("id").toInt)) )
		}
		
		def guest: Directive[dao.User :: HNil] = {
			val priv = await( dao.Files.create( dao.Users.count toString, "", Some(privateid), false, None, None ) )
			val u = await( dao.Users.create(None, None, None, None, GUEST) )
			val uid = u.id.get.toString
			
			await( dao.Users.update(u.id.get, priv.id.get) )
			
			setSession( "id" -> uid ) & provide( u )
		}
		
		def user: Directive[dao.User :: HNil] = optionalSession hflatMap {
			case None :: HNil => guest
			case Some( s ) :: HNil =>
				await( dao.Users.find(s.data("id").toInt) ) match {
					case Some( u ) => provide( u )
					case None => guest
				}
		}
		
// 		def admin: Directive[dao.Blog :: models.User :: HNil] = (blog & session) hflatMap {
// 			case b :: s :: HNil =>
// 				Queries.findUser( s.data("id").toInt ) match {
// 					case Some( u ) if u.roles.exists(r => r.blogid == b.id.get && r.role == "admin") => hprovide( b :: u :: HNil )
// 					case _ => reject( AuthorizationFailedRejection )
// 				}
// 		}
		
		//
		// robots.txt request logging
		//
		(get & pathPrefixTest( "robots.txt" ) & clientIP & unmatchedPath) { (ip, path) =>
			Application.logVisit( ip, path toString, None, None )
			reject } ~
		//
		// resource renaming routes (these will mostly be removed as soon as possible)
		//
		pathPrefix("sass") {
			getFromResourceDirectory("resources/public") } ~
		(pathPrefix("js") | pathPrefix("css")) {
			getFromResourceDirectory("public") } ~
		pathSuffixTest( """.*(?:\.(?:html|png|ico|txt))"""r ) { _ =>
			getFromResourceDirectory( "public" ) } ~
		pathPrefix("coffee") {
			getFromResourceDirectory("public/js") } ~
		pathPrefix("webjars") {
			getFromResourceDirectory("META-INF/resources/webjars") } ~
		//
		// application request logging (ignores admin and api requests)
		//
		(get & pathPrefixTest( !("api"|"setup-admin"|"admin") ) & clientIP & unmatchedPath & optionalHeaderValueByName( "Referer" ) & optionalUser) {
			(ip, path, referrer, user) =>
				Application.logVisit( ip, path toString, referrer, user )
				reject } ~
		//
		// application routes
		//
		//hostName {h => complete(h)} ~
		(get & pathSingleSlash & user) {
			u => complete( Application.index(u) ) } ~
		(get & path( "image"/IntNumber )) {
			img => complete( Application.image(img) ) } ~
		path( "login" ) {
			(get & user) { u =>
				if (u.status != GUEST)
					redirect( "/", StatusCodes.SeeOther )
				else
					complete( Application.login ) } ~
			(post & formFields( 'email, 'password, 'rememberme ? "no" )) {
				(email, password, rememberme) => Application.authenticate( email, password ) } } ~
		(get & path( "register" ) & user) {
			u =>
				if (u.status != GUEST)
					redirect( "/", StatusCodes.SeeOther )
				else
					complete( Application.register ) } ~
// 		(get & path( "admin" ) & admin) {
// 			(b, _) => complete( Views.admin(b) ) } ~
// 		(post & path( "post" ) & admin & formFields( 'category.as[Int], 'headline, 'text )) {
// 			(b, u, category, headline, text) => complete( Application.post(b, u, category, headline, text) ) } ~
		(get & path( "logout" ) & session) {
			_ => clearSession & redirect( "/", StatusCodes.SeeOther ) } ~
		//
		// API routes
		//
		pathPrefix( "api"/"v1" ) {
			(get & path("files")) {
				complete( API.filesUnderRoot ) } ~
			(get & path("files"/IntNumber)) { id =>
				complete( API.filesUnder(id) ) } ~
			(post & path("files") & parameters("parentid".as[Int], "content".as[Boolean]) & entity(as[FileContent]) & session) { (parentid, _, content, _) =>
				complete( API.filesPostCreate(parentid, content) ) } ~
			(post & path("files") & parameter("parentid".as[Int]) & entity(as[FileInfo]) & session) { (parentid, info, _) =>
				complete( API.filesPostCreate(parentid, info) ) } ~
			(post & path("files"/IntNumber) & entity(as[FileInfo]) & session) { (id, info, _) =>
				complete( API.filesPost(id, info) ) } ~
			(post & path("pairs"/IntNumber) & entity(as[PairJson]) & session) { (id, pair, _) =>
				complete( API.pairsPost(id, pair) ) } ~
			(delete & path("pairs"/IntNumber) & session) { (id, _) =>
				complete( API.pairsDelete(id) ) } ~
			(get & path("lessons"/IntNumber)) { fileid =>
				complete( API.lessonsGet(fileid) ) } ~
			(post & path("lessons"/IntNumber) & entity(as[PairJson]) & session) { (fileid, pair, _) =>
				complete( API.lessonsPost(fileid, pair) ) } ~
			(get & path("tallies"/IntNumber/IntNumber)) { (fileid, userid) =>
				complete( API.talliesGet(fileid, userid) ) } ~
			(post & path("tallies"/IntNumber/IntNumber) & entity(as[TallyUpdate])) { (userid, pairid, update) =>
				complete( API.talliesPost(userid, pairid, update) ) } ~
			(post & path("folders") & parameter("parentid".as[Int]) & entity(as[FileInfo]) & session) { (parentid, info, _) =>
				complete( API.foldersPostCreate(parentid, info) ) } ~
			(post & path("favorites") & entity(as[FavoriteInfo]) & session) { (fav, _) =>
				complete( API.favoritesPost(fav) ) } ~
			(get & path("favorites"/IntNumber) & session) { (userid, _) =>
				complete( API.favoritesGet(userid) ) } ~
// 			(post & path("private") & entity(as[FileInfo]) & session) { (f, _) =>
// 				complete( API.privatePost(f) ) } ~
// 			(get & path("private"/IntNumber) & session) { (userid, _) =>
// 				complete( API.privateGet(userid) ) } ~
			(get & path("users"/"exists") & parameter("name")) {
				name => complete( API.usersExistsName(name) ) } ~
			(get & path("users"/"exists") & parameter("email")) {
				email => complete( API.usersExistsEmail(email) ) } ~
// 			(get & path( "visits"/"count" ) & admin) {
// 				(b, _) => complete( API.visitsCount(b) ) } ~
// 			(get & path( "visits" ) & admin) {
// 				(b, _) => complete( API.visits(b) ) } ~
// 			(get & path("users"/IntNumber)) {
// 				userid => complete( API.usersGet(userid) ) } ~
			(post & path("users") & detach(context) & entity(as[UserJson]) & user) {
				(u, g) => API.usersPost( u, g ) } ~
// 			(get & path("users"/Segment)) {
// 				email => complete( API.users(email) ) } ~
			(get & path("users")) {
				complete( API.usersGet ) }
		}
	}
}
