package xyz.hyperreal.cramsite

import akka.actor.{ActorSystem, Props}
import akka.io.IO
import akka.pattern.ask
import akka.util.Timeout

import spray.can.Http

import concurrent.duration._


object Main extends App {

	Startup
	
	lazy implicit val akka = ActorSystem( "on-spray-can" )
	
	import akka.dispatcher

	val service = akka.actorOf(Props[CramSiteServiceActor], "cramsite-service")

	implicit val timeout = Timeout(5.seconds)

	(IO(Http) ? Http.Bind(service, interface = "localhost", port = 8082)).map {
		case _: Http.CommandFailed => akka.shutdown
	}
}
