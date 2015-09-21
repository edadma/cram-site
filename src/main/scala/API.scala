package xyz.hyperreal.cramsite

import com.typesafe.config.ConfigFactory
import com.github.kxbmap.configs._

import spray.http.{StatusCodes, HttpResponse, HttpHeaders, HttpEntity}
import spray.routing.directives.RouteDirectives._
import spray.json._

import org.joda.time.Instant

import in.azeemarshad.common.sessionutils.Session
import in.azeemarshad.common.sessionutils.SessionDirectives

import concurrent.ExecutionContext.Implicits.global
import collection.mutable.ListBuffer
import concurrent.Future

import java.net.URLDecoder

import dao._


object API extends SessionDirectives {
	
	val LIMIT = 2
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
	
	def filesUnderRoot = Files.findUnder( rootid )
	
	def filesUnder( parentid: Int ) = Files.findUnder( parentid )
	
	def lessonsIn( fileid: Int ) = Pairs.find( fileid ) map {s => Map("pairs" -> s)}
	
	def response( r: models.Response ) = {
		var done = false
		
		Tallies.findByPairid( r.pairid, r.userid ) flatMap {
			t => Tallies.update( r.pairid, r.userid,
				(t.get.correct, r.correct) match {
					case (c, false) if c <= 2 => 0
					case (c, false) => c - 2
					case (LIMIT, true) =>
						done = true
						LIMIT
					case (c, true) =>
						done = c + 1 == LIMIT
						c + 1
				})
		} map {
			ts => Map( "done" -> done )
		}
	}
	
	def talliesGet( fileid: Int, userid: Int ) = {
		Tallies.delete( fileid, userid ) flatMap { _ =>
			Pairs.find( fileid )
		} flatMap {
			ps => Future sequence (ps map (p => Tallies.create( userid, p.id.get, fileid, 0 )))
		} map (_ => Map[String, String]())
	}
	
	def folderCreate( parentid: Int, info: models.FolderInfo ) = {
		Files.find( parentid, info.name ) flatMap {
			case None =>
				Files.create(info.name, info.description.getOrElse(""), Some(parentid), true, None, None) map {
					f => ok( f.toJson.compactPrint )
				}
			case Some(_) =>
				Future {conflict(s"Folder '${info.name}' already exists")}
		}
	}
}
