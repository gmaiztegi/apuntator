package models

import play.api.db._
import play.api.libs.json._
import play.api.Play.current

import anorm._
import anorm.SqlParser._

case class File(id: Pk[Long] = NotAssigned, name:String, description:String, path:Option[String])

object File {
    
    val simple = {
        get[Pk[Long]]("files.id") ~
        str("files.name") ~
        str("files.description") ~
        str("files.path") map {
            case id~name~descr~path => File(id, name, descr, Some(path))
        }
    }
    
    def all(): List[File] = {
        DB.withConnection { implicit connection =>
            SQL("select * from files").as(File.simple *)
        }
    }
    
    def insert(file: File) = {
        DB.withConnection { implicit connection =>
            SQL(
                """
                insert into files values (
                    nextval('file_seq'),
                    {name}, {description}, {path}
                )
                """
            ).on(
                'name -> file.name,
                'description -> file.description,
                'path -> file.path.getOrElse(null)
            ).executeUpdate()
        }
    }
    
    implicit object FileFormat extends Format[File] {
        def reads(json: JsValue): File = File(
            (json \ "id").asOpt[Long].map {id => Id(id)}.getOrElse(NotAssigned),
            (json \ "name").as[String],
            (json \ "description").as[String],
            (json \ "path").asOpt[String]
        )
        def writes(f:File): JsValue = JsObject(List(
            "id" -> JsNumber(f.id.get),
            "name" -> JsString(f.name),
            "description" -> JsString(f.description),
            "path" -> f.path.map{ee => JsString("http://apuntator.s3.amazonaws.com/files/"+ee)}.getOrElse(JsNull)
        ))
    }
}

