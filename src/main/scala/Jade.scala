package xyz.hyperreal.cramsite

import org.fusesource.scalate._

import spray.http._
import MediaTypes._


object Jade {

	val engine = new TemplateEngine
	
	def apply( file: String, vars: (String, Any)* ) =
		html( engine.layout(source(file), Map(vars: _*)) )

	def html( response: String ) = HttpResponse( entity = HttpEntity(ContentType(`text/html`), response) )

	def source( file: String ) = TemplateSource.fromSource( s"public/$file.jade", io.Source.fromInputStream(getClass.getClassLoader.getResourceAsStream(s"public/$file.jade")) )
	
}