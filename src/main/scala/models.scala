package xyz.hyperreal.cramsite

package models

import spray.json.DefaultJsonProtocol._
import spray.json._
import spray.httpx.unmarshalling.MalformedContent

import org.joda.time.Instant


case class UserJson(
	name: String,
	email: String,
	password: String,
	role: Option[String]
)

object UserJson {
	implicit val userJson = jsonFormat4( UserJson.apply )
}

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
			case userid => VisitJson( v.id.get, v.ip, v.host, v.path, v.referrer, v.date, userid, Some(u.get.name.getOrElse("-")) )
		}
}
