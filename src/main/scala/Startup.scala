package xyz.hyperreal.cramsite

import slick.driver.H2Driver.api._
import slick.jdbc.meta.MTable

import com.typesafe.config.ConfigFactory
import com.github.kxbmap.configs._

import java.io.DataInputStream
import concurrent.Future
import concurrent.ExecutionContext.Implicits.global

import org.joda.time.Instant

import dao._


object Startup {
	
	val extensionRegex = """^.*\.(.*)$"""r
	
	def addImage( userid: Int, image: String ) = {
		val conn = getClass.getResource( image ).openConnection
		val img = new Array[Byte](conn.getContentLength)
		val extension = image  match {
			case extensionRegex(e) => e
			case _ => ""
		}

		new DataInputStream( conn.getInputStream ).readFully( img )
		Medias.create( userid, img, extension )
	}
	
	if (await(db.run(MTable.getTables)) isEmpty) {
		await(db.run(DBIO.seq(
			(
				Users.schema ++
				Files.schema ++
				Pairs.schema ++
				Tallies.schema ++
				Favorites.schema ++
				Medias.schema ++
				Visits.schema
			).create
		)))
	
		val conf = ConfigFactory.load
		val u = conf.get[Map[String, String]]("init.suadmin")
			Users.create( Some(u("name")), Some(u("email")), Some(u("password")), None, SUADMIN ) flatMap {
				u =>
					Future.sequence( Seq(
						addImage( u.id.get, "Books-icon.png" ),
						addImage( u.id.get, "Apps-system-users-icon.png" ),
						addImage( u.id.get, "Places-folder-icon.png" ),
						addImage( u.id.get, "Apps-accessories-text-editor-icon.png" ),
						addImage( u.id.get, "Places-folder-favorites-icon.png" ),
						addImage( u.id.get, "Places-folder-locked-icon.png" ),
						addImage( u.id.get, "Places-folder-image-icon.png" ),
						addImage( u.id.get, "App-personal-icon.png" )
					))
			} map {
				case Seq(topics, users, folder, file, favorites, locked, image, user) =>
					defaultFolderid = folder
					defaultFileid = file
					defaultUserid = user
					Files.create( "", "", None, false, None, None ) map {
						root =>
							Files.create( "Topics", "Browse learning topics", root.id, true, None, Some(topics) )
			// 				Files.create( "Topics", "Browse learning topics", root.id, true, None, None ) map {
			// 					topics =>
			// 						Files.create( "Topic 1", "A topic", topics.id, true, None, None )
			// 				}
							Files.create( "Favorites", "My favorite lessons", root.id, true, None, Some(favorites) )
							Files.create( "Users", "Browse user folders", root.id, true, None, Some(users) )
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
							Files.create( "Private", "My private lessons", root.id, true, None, Some(locked) )
							Files.create( "Images", "My images", root.id, true, None, Some(image) )
					}
			}
	}
	
}