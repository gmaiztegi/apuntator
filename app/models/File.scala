package models

import play.api.db._
import play.api.libs.json._
import play.api.Play.current

import anorm._
import anorm.SqlParser._

case class File(id: Pk[Long] = NotAssigned, name:String, description:String, path:String)

object File {
    
    val simple = {
        get[Pk[Long]]("files.id") ~
        str("files.name") ~
        str("files.description") ~
        str("files.path") map {
            case id~name~descr~path => File(id, name, descr, path)
        }
    }
    
    def all(): List[File] = {
        DB.withConnection { implicit connection =>
            SQL("select * from files").as(File.simple *)
        }
    }

    def findById(id: Long): Option[File] = {
        DB.withConnection { implicit connection =>
            SQL("select * from files where id = {id}").onParams(id).as(simple.singleOpt)
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
                'path -> file.path
            ).executeInsert()
        }
    }

    def update(id: Long, file: File) = {
        DB.withConnection { implicit connection =>
            SQL(
                """
                update files
                set name = {name}, description = {descr}, path = {path}
                where id = {id}
                """
            ).on(
                'id -> id,
                'name -> file.name,
                'descr -> file.description,
                'path -> file.path
            ).executeUpdate()
        }
    }
    
    def delete(id: Long) = {
        DB.withConnection { implicit connection =>
            SQL(
                """
                delete from files where id = {id}
                """
            ).onParams(id).executeUpdate()
        }
    }

    implicit object FileFormat extends Format[File] {
        def reads(json: JsValue): File = File(
            (json \ "id").asOpt[Long].map {id => Id(id)}.getOrElse(NotAssigned),
            (json \ "name").as[String],
            (json \ "description").as[String],
            (json \ "path").as[String]
        )
        def writes(f:File): JsValue = JsObject(List(
            "id" -> JsNumber(f.id.get),
            "name" -> JsString(f.name),
            "description" -> JsString(f.description),
            "path" -> JsString("http://apuntator.s3.amazonaws.com/files/"+f.path)
        ))
    }
}

