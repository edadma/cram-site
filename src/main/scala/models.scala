package xyz.hyperreal.cramsite

package models

import spray.json.DefaultJsonProtocol._
import spray.json._
import spray.httpx.unmarshalling.MalformedContent

import org.joda.time.Instant

import xyz.hyperreal.cramsite.dao.Pair


case class UserJson(
	name: String,
	email: String,
	password: String,
	description: Option[String]
)

object UserJson {
	implicit val userJson = jsonFormat4( UserJson.apply )
}

case class FileInfo(
	name: String,
	description: Option[String]
)

object FileInfo {
	implicit val fileInfo = jsonFormat2( FileInfo.apply )
}

case class FileContent(
	content: String
)

object FileContent {
	implicit val fileContent = jsonFormat1( FileContent.apply )
}

case class Lesson(
	info: JsValue,
	pairs: Seq[Pair]
)

object Lesson {
	implicit val lesson = jsonFormat2( Lesson.apply )
}

case class PairJson(
	front: String,
	back: String
)

object PairJson {
	implicit val pairjson = jsonFormat2(PairJson.apply)
}

case class TallyUpdate(
	foreward: Int,
	backward: Int
)

object TallyUpdate {
	implicit val tallyUpdate = jsonFormat2( TallyUpdate.apply )
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
