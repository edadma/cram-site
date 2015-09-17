package xyz.hyperreal.cramsite

package models

import spray.json.DefaultJsonProtocol._
import spray.json._
import spray.httpx.unmarshalling.MalformedContent

import org.joda.time.Instant


case class User(
	id: Int,
	name: String,
	email: String,
	roles: Seq[dao.Role],
	avatar: Option[String],
	thumb: Option[String],
	registered: Instant
) {
	def is( role: String ) = roles exists (_.role == role)
}

object User {
	implicit val user = jsonFormat7( User.apply )
	
	def from( u: dao.User ) = User( u.id.get, u.name, u.email, await(dao.Roles.find(u.id.get)), u.avatar map (_ => s"/api/v1/users/${u.id.get}/avatar"),
																	u.thumb map (_ => s"/api/v1/users/${u.id.get}/thumb"), u.registered )
}

case class UserJson(
	name: String,
	email: String,
	password: String,
	bio: Option[String],
	url: Option[String],
	role: Option[String]
)

object UserJson {
	implicit val userJson = jsonFormat6( UserJson.apply )
}

// case class Comment(
// 	id: Int,
// 	postid: Int,
// 	authorid: Option[Int],
// 	author: String,
// 	date: Instant,
// 	replyto: Option[Int],
// 	content: String
// )
// 
// object Comment {
// 	implicit val comment = jsonFormat7( Comment.apply )
// 	
// 	def from( c: dao.Comment ) = Comment( c.id.get, c.postid, None, c.name.get, c.date, c.replyto, c.content )
// 	
// 	def from( c: dao.Comment, u: dao.User ) =
// 		c.authorid match {
// 			case None => Comment( c.id.get, c.postid, None, c.name.get, c.date, c.replyto, c.content )
// 			case authorid => Comment( c.id.get, c.postid, authorid, u.name, c.date, c.replyto, c.content )
// 		}
// }
// 
// case class CommentWithReplies( comment: Comment, replies: Seq[CommentWithReplies] )
// 
// object CommentWithReplies {
// 	implicit val commentWithRepliesFormat: JsonFormat[CommentWithReplies] = lazyFormat(jsonFormat(CommentWithReplies.apply, "comment", "replies"))
// }

case class VisitJson(
	id: Int,
	ip: String,
	host: Option[String],
	path: String,
	referrer: Option[String],
	date: Instant,
	userid: Option[Int],
	username: Option[String]
)

object VisitJson {
	implicit val visitJson = jsonFormat8( VisitJson.apply )
	
	def from( v: dao.Visit, u: Option[dao.User] ) =
		v.userid match {
			case None => VisitJson( v.id.get, v.ip, v.host, v.path, v.referrer, v.date, None, None )
			case userid => VisitJson( v.id.get, v.ip, v.host, v.path, v.referrer, v.date, userid, Some(u.get.name) )
		}
}
