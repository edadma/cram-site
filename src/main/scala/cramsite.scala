package xyz.hyperreal

import slick.driver.H2Driver.api._

import akka.event.Logging

import spray.json.DefaultJsonProtocol._
import spray.json._
import spray.httpx.unmarshalling.MalformedContent

import org.joda.time.Instant

import concurrent._
import concurrent.duration._


package object cramsite {

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
	
}