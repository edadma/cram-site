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

	def usersGet( userid: Int ) = Users.find(userid)
	
	def usersGet = Users.list
	
	def usersPost( u: models.UserJson ) = {
		Users.findByEmail( u.email ) flatMap {
			case None =>
				Users.create( Some(u.name), Some(u.email), Some(u.password), None, USER ) map {
					id => HttpResponse( status = StatusCodes.Created, s"""{"id": $id}""" )
				}
			case _ => Future( HttpResponse(status = StatusCodes.Conflict, "A user with that email address already exists.") )
		}
	}
	
	//def users( email: String ) = Users.find(URLDecoder.decode(email, "UTF-8")) map (u => u map (models.User.from(_)))
	
	//def users = Users.list map (u => u map (models.User.from(_)))
	
	def filesUnderRoot = {
		Files.findUnder( rootid )
	}
	
	def filesUnder( parentid: Int ) = Files.findUnder( parentid )
	
	def lessonsIn( fileid: Int ) = Pairs.find( fileid ) map {s => Map("pairs" -> s)}
	
	def response( r: models.Response ) = {
		
		Map( "complete" -> false )
	}
	
}
