package xyz.hyperreal.cramsite

import slick.driver.H2Driver.api._

import com.github.tototoshi.slick.H2JodaSupport._

import org.joda.time.{DateTime, Instant}

import concurrent._
import concurrent.ExecutionContext.Implicits.global
import collection.mutable.{HashSet, ListBuffer}

import dao._


object Queries {
	
	//
	// visits
	//
	
	def visitsCount = db.run( Visits.length result )
	
	def visits = db.run( Visits joinLeft Users on (_.userid === _.id) result ) map {
		s => s map {case (v, u) => models.VisitJson.from( v, u )}
	}
	
	def visits( index: Int, count: Int ) = db.run( Visits drop index take count joinLeft Users on (_.userid === _.id) result ) map {
		s => s map {case (v, u) => models.VisitJson.from( v, u )}
	}
	
	def toMonth( time: Instant ) = time.toDateTime withDayOfMonth 1 withTime (0, 0, 0, 0)
	
	//
	// Favorites
	//
	
	def favorites( userid: Int ) = db.run( Favorites.findByUserid( userid ) join Files on (_.fileid === _.id) result ) map {
		s => s map {
			case (_, f) =>
				val buf = new StringBuilder
				
				def element( f: File ) {
					if (buf.nonEmpty && f.name != "")
						buf.insert( 0, '/' )
						
					buf.insert( 0, f.name )
					
					f.parentid match {
						case None =>
						case Some( parentid ) => element( await(Files.find(parentid)).get )
					}
				}
				
				element( f )
				models.FavoriteJson( f.id.get, buf.toString, f.description, f.contents, f.imageid )
		}
	}
	
// 	def findCommentsReplies( postid: Int, replyto: Int ) = db.stream( Comments.findByPostid(postid, replyto) result )
// 	
// 	def findComments( postid: Int ) = {
// 		var count = 0
// 		
// 		def replies( replyto: Int ): Seq[models.CommentWithReplies] = {
// 			val comments = new ListBuffer[models.CommentWithReplies]
// 		
// 			await( findCommentsReplies( postid, replyto ) foreach { c =>
// 				val comment =
// 					c.authorid match {
// 						case Some( authorid ) => models.Comment.from( c, await(Users.find(authorid)).get )
// 						case None => models.Comment.from( c )
// 					}
// 				
// 				comments += models.CommentWithReplies(comment, replies( comment.id ))
// 				count += 1
// 				} )
// 			comments.toList
// 		}
// 		
// 		val comments = new ListBuffer[models.CommentWithReplies]
// 		
// 		await( findCommentsNoReply(postid) foreach { c =>
// 			val comment =
// 				c.authorid match {
// 					case Some( authorid ) => models.Comment.from( c, await(Users.find(authorid)).get )
// 					case None => models.Comment.from( c )
// 				}
// 		
// 			comments += models.CommentWithReplies(comment, replies( comment.id ))
// 			count += 1
// 			} )
// 		(comments.toList, count)
// 	}

}