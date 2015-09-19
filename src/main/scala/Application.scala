package xyz.hyperreal.cramsite

import spray.http.{StatusCodes, HttpResponse, HttpHeaders, RemoteAddress}
import spray.routing.directives.RouteDirectives._

import org.joda.time.Instant

import in.azeemarshad.common.sessionutils.Session
import in.azeemarshad.common.sessionutils.SessionDirectives

import concurrent.ExecutionContext.Implicits.global
import collection.mutable.ListBuffer
import concurrent.Future
import util.{Try, Success, Failure}


object Application extends SessionDirectives {
	
	def logVisit( ip: RemoteAddress, path: String, referrer: Option[String], user: Option[dao.User] ) {
		Future( ip.toOption.map(_.getHostName) ) onComplete {
			case Success(host) =>
				dao.Visits.create( ip.toString, host, path, referrer, Instant.now, user map (_.id.get) )
			case Failure(e) =>
				log.info( e toString )
		}
	}
	
	def index( user: dao.User ) = Views.index( user )
	
	def login = Views.login
	
	def authenticate( email: String, password: String ) = {
		await( dao.Users.find(email, password) ) match {
			case Some( u ) =>
				setSession( "id" -> u.id.get.toString ) & redirect( "/", StatusCodes.SeeOther )
			case None =>
				redirect( "/", StatusCodes.SeeOther )
		}
	}
	
	def register = Views.register( None )
	
// 	def post( blog: dao.Blog, user: models.User, category: Int, headline: String, text: String ) = {
// 		dao.Posts.create( blog.id.get, user.id, headline, text, Instant.now ) map (dao.Categorizations.create( _, category ))
// 		redirectResponse( "/" )
// 	}
	
// 	def comment( session: Session, postid: Int, replytoid: Option[Int], text: String ) = {
// 		dao.Comments.create( postid, Some(session.data("id").toInt), None, None, None, Instant.now, replytoid, text )
// 		redirectResponse( "/" )
// 	}
// 	
// 	def comment( name: String, email: Option[String], url: String, postid: Int, replytoid: Option[Int], text: String ) = {
// 		dao.Comments.create( postid, None, Some(name), email, if (url == "") None else Some(url), Instant.now, replytoid, text )
// 		redirectResponse( "/" )
// 	}
		
	private def redirectResponse( uri: String ) = HttpResponse( status = StatusCodes.SeeOther, headers = List(HttpHeaders.Location(uri)) )
	
}
