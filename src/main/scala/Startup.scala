package xyz.hyperreal.cramsite

import slick.driver.H2Driver.api._
import slick.jdbc.meta.MTable

import com.typesafe.config.ConfigFactory
import com.github.kxbmap.configs._

import concurrent.ExecutionContext.Implicits.global

import org.joda.time.Instant

import dao._


object Startup {
	
	if (await(db.run(MTable.getTables)) isEmpty) {
		await(db.run(DBIO.seq(
			(
				Users.schema ++
				Files.schema ++
				Pairs.schema ++
				Tallies.schema ++
				Medias.schema ++
				Visits.schema
			).create
		)))
	
		val conf = ConfigFactory.load
		
		if (conf hasPath "init") {
			val u = conf.get[Map[String, String]]("init.suadmin")
				Users.create( Some(u("name")), Some(u("email")), Some(u("password")), None, SUADMIN )
		}
		
		Files.create( "/", "", None, false, None, None ) map {
			root =>
				Files.create( "Topics", "Browse learning topics", root.id, true, None, None )
// 				Files.create( "Topics", "Browse learning topics", root.id, true, None, None ) map {
// 					topics =>
// 						Files.create( "Topic 1", "A topic", topics.id, true, None, None )
// 				}
				Files.create( "Users", "Browse user folders", root.id, true, None, None )
// 				Files.create( "Users", "Browse user folders", root.id, true, None, None ) map {
// 					users =>
// 						Files.create( "Bob", "Bob's folder", users.id, true, None, None ) map {
// 							bob =>
// 								Files.create( "French 101", "French vocabulary", bob.id, true, Some("""{"direction": "duplex"}"""), None ) map {
// 									french101 =>
// 										Pairs.create( french101.id.get, "one", "un" )
// 										Pairs.create( french101.id.get, "two", "deux" )
// 								}
// 						}
// 				}
		}
	}
	
}