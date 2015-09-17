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
	
	def visitsCount = db.run( dao.Visits.length result )
	
	def visits = db.run( dao.Visits joinLeft Users on (_.userid === _.id) result ) map {
		s => s map {case (v, u) => models.VisitJson.from( v, u )}
	}
	
	def visits( index: Int, count: Int ) = db.run( dao.Visits drop index take count joinLeft Users on (_.userid === _.id) result ) map {
		s => s map {case (v, u) => models.VisitJson.from( v, u )}
	}
	
	def toMonth( time: Instant ) = time.toDateTime withDayOfMonth 1 withTime (0, 0, 0, 0)
	
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
	
	def findUser( userid: Int ) = await(Users.find(userid)) map (models.User.from( _ ))

}