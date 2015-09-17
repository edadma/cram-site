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
				Visits.schema
			).create
		)))
	
		val conf = ConfigFactory.load
		
		if (conf hasPath "init") {
			for (ou <- conf.opt[List[Map[String, String]]]("init.admin"); u <- ou)
				Users.create( u("name"), u("email"), u("password"), None ) map (userid => Roles.create(userid, "admin"))
		}
		
		Files.create( "/", "", Instant.now, None, true, true, None ) map {
			root =>
				Files.create( "Topics", "Browse learning topics", Instant.now, Some(root), true, true, None )
				Files.create( "Users", "Browse user folders", Instant.now, Some(root), true, true, None )
				
				Files.create( s"Folder 0", "adsf dfsalj kfsal jkdfjklfdsajlk gdf kfsal jkdfjklfdsajlk gdf", Instant.now, Some(root), true, true, None )
				Files.create( s"Folder 1", "adsf", Instant.now, Some(root), true, true, None )
				Files.create( s"Folder 2", "adsf", Instant.now, Some(root), true, true, None )
				Files.create( s"Folder 3", "adsf", Instant.now, Some(root), true, true, None )
		}
	}
	
}