package xyz.hyperreal.cramsite

import com.typesafe.config.ConfigFactory
import com.github.kxbmap.configs._

import spray.http.{StatusCodes, HttpResponse, HttpHeaders, HttpEntity}
import spray.routing.directives.RouteDirectives._

import org.joda.time.Instant

import in.azeemarshad.common.sessionutils.Session
import in.azeemarshad.common.sessionutils.SessionDirectives

import concurrent.ExecutionContext.Implicits.global
import collection.mutable.ListBuffer
import concurrent.Future

import java.net.URLDecoder

import dao._


object API extends SessionDirectives {
	
  val conf = ConfigFactory.load
	val reserved = conf.opt[List[String]]( "blog.domain.reserved" )

	def visitsCount = Queries.visitsCount map (c => Map( "count" -> c ))
	
	def visits = Queries.visits
	
// 	def domainsGet( domain: String ) =
// 		if (reserved.get exists (_ == domain))
// 			Future( Map("available" -> false) )
// 		else
// 			Blogs.find( domain ) map (u => Map( "available" -> (u == None) ))

	def usersGet( userid: Int ) = Users.find(userid) map (u => u map (models.User.from(_)))
	
	def usersPost( u: models.UserJson ) = {
		Users.find( u.email ) flatMap {
			case None =>
				Users.create( u.name, u.email, u.password, None ) flatMap {
					id =>
						val response = HttpResponse( status = StatusCodes.Created, s"""{"id": $id}""" )
						
						u.role match {
							case Some( r ) =>
								Roles.create( id, r ) map (_ => response)
							case None => Future( response )
						}
				}
			case _ => Future( HttpResponse(status = StatusCodes.Conflict, "A user with that email address already exists.") )
		}
	}
	
	//def users( email: String ) = Users.find(URLDecoder.decode(email, "UTF-8")) map (u => u map (models.User.from(_)))
	
	//def users = Users.list map (u => u map (models.User.from(_)))
	
	def root = Files.findUnder( rootid )
	
	def files( parentid: Int ) = Files.findUnder( parentid )
	
// 	def comments( postid: Int, authorid: Option[Int], name: Option[String], email: Option[String], url: String, replyto: Option[Int], content: String ) =
// 		Comments.create( postid, authorid, name, email, if (url == "") None else Some(url), Instant.now, replyto, content ) map (id => Map( "id" -> id ))
	
}
