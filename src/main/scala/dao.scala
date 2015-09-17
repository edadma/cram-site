package xyz.hyperreal.cramsite

import slick.driver.H2Driver.api._
import slick.dbio.{DBIOAction, NoStream}


package object dao {
	
	val db = Database.forConfig( "db" )
	
	def dbrun[R]( a: DBIOAction[R, NoStream, Nothing] ) = await( dao.db.run(a) )
	
}