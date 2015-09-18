package xyz.hyperreal.cramsite.dao

import slick.driver.H2Driver.api._

import com.github.tototoshi.slick.H2JodaSupport._

import org.joda.time.Instant

import spray.json.DefaultJsonProtocol._

import concurrent._
import concurrent.ExecutionContext.Implicits.global

import xyz.hyperreal.cramsite._


case class User(
	name: String,
	email: String,
	password: String,
	avatar: Option[Array[Byte]],
	thumb: Option[Array[Byte]],
	registered: Instant,
	id: Option[Int] = None
)

class UsersTable(tag: Tag) extends Table[User](tag, "users") {
	def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
	def name = column[String]("name")
	def email = column[String]("email")
	def password = column[String]("password")
	def avatar = column[Option[Array[Byte]]]("avatar")
	def thumb = column[Option[Array[Byte]]]("thumb")
	def registered = column[Instant]("registered")
	
	def * = (name, email, password, avatar, thumb, registered, id.?) <> (User.tupled, User.unapply)
	def idx_users_email = index("idx_users_email", email, unique = true)
	def idx_users_email_password = index("idx_users_email_password", (email, password), unique = true)
}

object Users extends TableQuery(new UsersTable(_)) {
	def find(id: Int): Future[Option[User]] = db.run( filter(_.id === id).result ) map (_.headOption)

	def find( email: String ) = db.run( filter(_.email === email).result ) map (_.headOption)

	def find( email: String, password: String ) = db.run( filter(r => r.email === email && r.password === password).result ) map (_.headOption)
	
	def create( name: String, email: String, password: String, avatar: Option[Array[Byte]] ) =
		db.run( this returning map(_.id) += User(name, email, password, avatar, avatar, Instant.now) )

	def delete(id: Int): Future[Int] = {
		db.run(filter(_.id === id).delete)
	}
	
	def list: Future[Seq[User]] = db.run(this.result)
}

case class Role(
	userid: Int,
	role: String,
	id: Option[Int] = None
)

object Role {
	implicit val role = jsonFormat3(Role.apply)
}

class RolesTable(tag: Tag) extends Table[Role](tag, "roles") {
	def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
	def userid = column[Int]("userid")
	def role = column[String]("role")
	
	def * = (userid, role, id.?) <> (Role.apply _ tupled, Role.unapply)
	def user_fk = foreignKey("roles_user_fk", userid, Users)(_.id, onDelete=ForeignKeyAction.Cascade)
	def idx_roles_userid = index("idx_roles_userid", userid)
	def idx_roles_role = index("idx_roles_role", role)
}

object Roles extends TableQuery(new RolesTable(_)) {
	def find(userid: Int): Future[Seq[Role]] = db.run( filter(_.userid === userid) result )

	def find(blogid: Int, role: String): Future[Seq[Role]] = db.run( filter(r => r.role === role) result )

	def create( userid: Int, role: String ) = db.run( this += Role(userid, role) )

	def delete(userid: Int): Future[Int] = {
		db.run(filter(r => r.userid === userid).delete)
	}
	
	def list: Future[Seq[Role]] = db.run(this.result)
}

case class Pair(
	fileid: Int,
	front: String,
	back: String,
	id: Option[Int] = None
)

object Pair {
	implicit val pair = jsonFormat4(Pair.apply)
}

class PairsTable(tag: Tag) extends Table[Pair](tag, "pairs") {
	def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
	def fileid = column[Int]("fileid")
	def front = column[String]("front")
	def back = column[String]("back")
	
	def * = (fileid, front, back, id.?) <> (Pair.apply _ tupled, Pair.unapply)
	def file_fk = foreignKey("pairs_file_fk", fileid, Files)(_.id, onDelete=ForeignKeyAction.Cascade)
}

object Pairs extends TableQuery(new PairsTable(_)) {
	def find(fileid: Int): Future[Seq[Pair]] = db.run( filter(_.fileid === fileid) result )

	def create(
		fileid: Int,
		front: String,
		back: String
		) = db.run( this += Pair(fileid, front, back) )

	def delete(fileid: Int): Future[Int] = {
		db.run(filter(_.fileid === fileid).delete)
	}
	
	def list: Future[Seq[Pair]] = db.run(this.result)
}

case class File(
	name: String,
	description: String,
	created: Instant,
	parentid: Option[Int],
	visible: Boolean,
	contents: Option[String],
	imageid: Option[Int],
	id: Option[Int] = None
)

object File {
	implicit val file = jsonFormat8(File.apply)
}

class FilesTable(tag: Tag) extends Table[File](tag, "files") {
	def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
	def name = column[String]("name")
	def description = column[String]("description")
	def created = column[Instant]("date")
	def parentid = column[Option[Int]]("parentid")
	def visible = column[Boolean]("visible")
	def contents = column[Option[String]]("contents")
	def imageid = column[Option[Int]]("imageid")
	
	def * = (name, description, created, parentid, visible, contents, imageid, id.?) <> (File.apply _ tupled, File.unapply)
	def parent_fk = foreignKey("files_parent_fk", parentid, Files)(_.id.?, onDelete=ForeignKeyAction.Cascade)
	def idx_files_name = index("idx_files_name", name)
	def idx_files_name_parent = index("idx_files_name_parent", (name, parentid), unique = true )
}

object Files extends TableQuery(new FilesTable(_)) {
	def find(id: Int): Future[Option[File]] = db.run( filter(_.id === id) result ) map (_.headOption)

	def findUnder(parentid: Int) = db.run( filter (f => f.visible && f.parentid === parentid) sortBy (_.name.asc) result )

	def find(parentid: Int, name: String) = db.run( filter (f => f.parentid.isDefined && f.name === name && f.parentid === parentid) result ) map (_.headOption)

	def findRoot = dbrun( filter (_.parentid.isEmpty) result )

	def create(
		name: String,
		description: String,
		created: Instant,
		parentid: Option[Int],
		visible: Boolean,
		contents: Option[String],
		imageid: Option[Int]
		) = db.run( this returning map(_.id) += File(name, description, created, parentid, visible, contents, imageid) )

	def delete(id: Int): Future[Int] = {
		db.run(filter(_.id === id).delete)
	}
	
	def list: Future[Seq[File]] = db.run(this.result)
}

case class Media(
	userid: Int,
	data: Array[Byte],
	mime: String,
	id: Option[Int] = None
)

class MediasTable(tag: Tag) extends Table[Media](tag, "medias") {
	def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
	def userid = column[Int]("userid")
	def data = column[Array[Byte]]("data")
	def mime = column[String]("mime")
	
	def * = (userid, data, mime, id.?) <> (Media.tupled, Media.unapply)
}

object Medias extends TableQuery(new MediasTable(_)) {
	def find(id: Int): Future[Option[Media]] = db.run( filter(_.id === id) result ) map (_.headOption)

	def findByPostid(userid: Int) = filter (_.userid === userid)

	def create(userid: Int, data: Array[Byte], mime: String) = db.run( this returning map(_.id) += Media(userid, data, mime) )

	def delete(userid: Int): Future[Int] = {
		db.run(filter(_.userid === userid).delete)
	}
	
	def list: Future[Seq[Media]] = db.run(this.result)
}

case class Visit(
	ip: String,
	host: Option[String],
	path: String,
	referrer: Option[String],
	date: Instant,
	userid: Option[Int],
	id: Option[Int] = None
)

object Visit {
	implicit val visit = jsonFormat7(Visit.apply)
}

class VisitsTable(tag: Tag) extends Table[Visit](tag, "visits") {
	def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
	def ip = column[String]("ip")
	def host = column[Option[String]]("host")
	def path = column[String]("path")
	def referrer = column[Option[String]]("referrer")
	def date = column[Instant]("date")
	def userid = column[Option[Int]]("userid")
	
	def * = (ip, host, path, referrer, date, userid, id.?) <> (Visit.apply _ tupled, Visit.unapply)
	def user_fk = foreignKey("visits_user_fk", userid, Users)(_.id.?, onDelete=ForeignKeyAction.SetNull)
	def idx_visits_ip = index("idx_visits_ip", ip)
	def idx_visits_referrer = index("idx_visits_referrer", referrer)
}

object Visits extends TableQuery(new VisitsTable(_)) {
	def find(id: Int): Future[Option[Visit]] = db.run( filter(_.id === id) result ) map (_.headOption)

	def create(
		ip: String,
		host: Option[String],
		path: String,
		referrer: Option[String],
		date: Instant,
		userid: Option[Int]
		) = db.run( this returning map(_.id) += Visit(ip, host, path, referrer, date, userid) )
	
	def list: Future[Seq[Visit]] = db.run(this.result)
}
