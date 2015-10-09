package xyz.hyperreal

import slick.driver.H2Driver.api._

import akka.event.Logging

import spray.http.{StatusCodes, HttpResponse, HttpHeaders, HttpEntity}
import spray.json.DefaultJsonProtocol._
import spray.json._
import spray.httpx.unmarshalling.MalformedContent

import org.joda.time.Instant

import concurrent._
import concurrent.duration._


package object cramsite {

	val GUEST = 0
	val USER = 1
	val ADMIN = 2
	val SUADMIN = 3
	
	def await[T]( a: Awaitable[T] ) = Await.result( a, Duration.Inf )
	
	val log = Logging( Main.akka, getClass )
	
	implicit object InstantJsonFormat extends JsonFormat[Instant] {
		def write(x: Instant) = JsObject(Map("millis" -> JsNumber(x.getMillis)))
		def read(value: JsValue) = value match {
			case JsObject(x) => new Instant(x("millis").asInstanceOf[JsNumber].value.longValue)
			case x => sys.error("Expected Instant as JsObject, but got " + x)
		}
	}
	
	lazy val rootid = dao.Files.findRoot.head.id.get
	lazy val usersid = await( dao.Files.find(rootid, "Users") ).head.id.get
	lazy val privateid = await( dao.Files.find(rootid, "Private") ).head.id.get

	var defaultFolderid: Int = _
	var defaultFileid: Int = _
	var defaultUserid: Int = _

	def ok( message: String = "" ) = HttpResponse( status = StatusCodes.OK, message )
	
	def conflict( message: String ) = HttpResponse( status = StatusCodes.Conflict, message )
	
	def badRequest( message: String ) = HttpResponse( status = StatusCodes.BadRequest, message )
}