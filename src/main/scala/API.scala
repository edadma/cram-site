package xyz.hyperreal.cramsite

import com.typesafe.config.ConfigFactory
import com.github.kxbmap.configs._

import spray.http.{StatusCodes, HttpResponse, HttpHeaders, HttpEntity}
import spray.routing.directives.RouteDirectives._
import spray.json._
import spray.json.DefaultJsonProtocol._
import spray.httpx.SprayJsonSupport._

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

	def usersGet( userid: Int ) = Users.find(userid)
	
	def usersGet = Users.list
	
	def usersPost( u: models.UserJson, g: Option[User] ) = {
		await( Users.findByEmail(u.email) ) match {
			case None =>
				Files.create( u.name, u.description getOrElse "", Some(usersid), true, None, None )
				
				val id =
					g match {
						case Some( gu ) =>
							Users.update( gu.id.get, u.name, u.email, u.password, USER )
							gu.id.get.toString
						case None => await( Users.create(Some(u.name), Some(u.email), Some(u.password), None, USER) ).id.get.toString
					}

				(setSession( "id" -> id ) & complete( HttpResponse( status = StatusCodes.Created, s"""{"id": $id}""") ))
			case _ => complete( conflict("A user with that email address already exists.") )
		}
	}
	
	def usersExistsName( name: String ) = Users.findByName( name ) map {u => Map( "exists" -> (u map (_ => true) getOrElse false ))}
	
	def usersExistsEmail( email: String ) = Users.findByEmail( email ) map {u => Map( "exists" -> (u map (_ => true) getOrElse false ))}
	
	//def users( email: String ) = Users.find(URLDecoder.decode(email, "UTF-8")) map (u => u map (models.User.from(_)))
	
	//def users = Users.list map (u => u map (models.User.from(_)))
	
	def filesUnderRoot = Files.findUnder( rootid )
	
	def filesUnder( parentid: Int ) = Files.findUnder( parentid )
	
	def filesPost( id: Int, info: models.FileInfo ) = {
		Files.find( id ) flatMap {
			file =>
				Files.find( file.get.parentid.get, info.name ) flatMap {
					case None =>
						Files.update(id, info.name, info.description.getOrElse("")) map {
							u => ok( Map("updated" -> u).toJson.compactPrint )
						}
					case Some(_) =>
						Future {conflict(s"'${info.name}' already exists")}
				}
		}
	}
	
	def lessonsGet( fileid: Int ) = {
		Files.find( fileid ) flatMap { f =>
			Pairs.find( fileid ) map { ps =>
				models.Lesson( f.get.contents.get.parseJson, ps )
			}
		}
	}
	
	def lessonsPost( fileid: Int, pair: models.PairJson ) = {
		Pairs.create( fileid, pair.front, pair.back ) map (id => Map("id" -> id))
	}
	
	def filesPostCreate( parentid: Int, info: models.FileInfo ) = {
		Files.find( parentid, info.name ) flatMap {
			case None =>
				Files.create(info.name, info.description.getOrElse(""), Some(parentid), true, Some("""{"direction": "duplex"}"""), None) map {
					f => ok( f.toJson.compactPrint )
				}
			case Some(_) =>
				Future {conflict(s"'${info.name}' already exists")}
		}
	}
	
	def filesPostCreate( parentid: Int, file: models.FileContent ) = {
		val src = io.Source.fromString( file.content ).getLines.toList
		
		if (src.length < 4)
			Future {badRequest("File contents should comprise at least three lines of text.")}
		else if (src.head.trim == "")
			Future {badRequest("File name is empty.")}
		else {
			val cards = src drop 3 map (_ split "::")
			
			if (cards exists (_.length != 2))
				Future {badRequest("Each flashcard should have a '::' separating front and back.")}
			else {
				val filename = src.head.trim
				
				Files.find( parentid, filename ) flatMap {
					case None =>
						Files.create( filename, src.drop(1).head.trim, Some(parentid), true, Some(src.drop(2).head.trim), None ) flatMap {
							f =>
								Future.sequence( cards map {case Array(front, back) => Pairs.create(f.id.get, front.trim, back.trim)} ) map { _ =>
									ok( f.toJson.compactPrint )
								}
						}
					case Some(_) =>
						Future {conflict(s"'$filename' already exists")}
				}
			}
		}
	}
	
	def pairsPost( id: Int, pair: models.PairJson ) = {
		Pairs.update( id, pair.front, pair.back ) map (u => Map("updated" -> u))
	}
	
	def pairsDelete( id: Int ) = Pairs.delete( id ) map (d => Map("deleted" -> d))
	
	def talliesPost( userid: Int, pairid: Int, update: models.TallyUpdate ) =
		Tallies.update( pairid, userid, update.foreward, update.backward ) map {
			u => Map( "updated" -> u )
		}
	
	def talliesGet( fileid: Int, userid: Int ) = {
		Tallies.delete( fileid, userid ) flatMap { _ =>
			Pairs.find( fileid )
		} flatMap {
			ps => Future sequence (ps map (p => Tallies.create( userid, p.id.get, fileid )))
		} map (_ => Map[String, String]())
	}
	
	def foldersPostCreate( parentid: Int, info: models.FileInfo ) = {
		Files.find( parentid, info.name ) flatMap {
			case None =>
				Files.create(info.name, info.description.getOrElse(""), Some(parentid), true, None, None) map {
					f => ok( f.toJson.compactPrint )
				}
			case Some(_) =>
				Future {conflict(s"'${info.name}' already exists")}
		}
	}
}
