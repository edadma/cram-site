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
				Roles.schema ++
				Files.schema ++
				Pairs.schema ++
				Tallies.schema ++
				Visits.schema
			).create
		)))
	
		val conf = ConfigFactory.load
		
		if (conf hasPath "init") {
			for (ou <- conf.opt[List[Map[String, String]]]("init.admin"); u <- ou)
				Users.create( u("name"), u("email"), u("password"), None ) map (userid => Roles.create(userid, "admin"))
		}
		
		Files.create( "/", "", Instant.now, None, false, None, None ) map {
			root =>
				Files.create( "Topics", "Browse learning topics", Instant.now, Some(root), true, None, None ) map {
					topics =>
						Files.create( "Topic 1", "A topic", Instant.now, Some(topics), true, None, None )
				}
				Files.create( "Users", "Browse user folders", Instant.now, Some(root), true, None, None ) map {
					users =>
						Files.create( "Bob", "Bob's folder", Instant.now, Some(users), true, None, None ) map {
							bob =>
								Files.create( "French 101", "French vocabulary", Instant.now, Some(bob), true, Some("{direction:bi}"), None ) map {
									french101 =>
										Pairs.create( french101, "one", "un(e)" )
										Pairs.create( french101, "two", "deux" )
								}
						}
				}
		}
	}
	
}