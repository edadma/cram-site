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
	
	def createImage( userid: Int, image: String ) = {
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
				Medias.schema ++
				Visits.schema
			).create
		)))
	
		val conf = ConfigFactory.load
		val u = conf.get[Map[String, String]]("init.suadmin")
			Users.create( Some(u("name")), Some(u("email")), Some(u("password")), None, SUADMIN ) flatMap {
				u =>
					Future.sequence( Seq(
						createImage( u.id.get, "Places-folder-icon.png" ),
						createImage( u.id.get, "Apps-system-users-icon.png" ),
						createImage( u.id.get, "Apps-accessories-text-editor-icon.png" )
					))
			} map {
				case Seq(folderimg, usersimg, fileimg) =>
					folderimgid = folderimg
					fileimgid = fileimg
					Files.create( "/", "", None, false, None, None ) map {
						root =>
							Files.create( "Topics", "Browse learning topics", root.id, true, None, Some(folderimg) )
			// 				Files.create( "Topics", "Browse learning topics", root.id, true, None, None ) map {
			// 					topics =>
			// 						Files.create( "Topic 1", "A topic", topics.id, true, None, None )
			// 				}
							Files.create( "Users", "Browse user folders", root.id, true, None, Some(usersimg) )
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
	
}