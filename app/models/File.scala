package models

import java.util.{Date}

import play.api.db._
import play.api.libs.json._
import play.api.Play.current

import anorm._
import anorm.SqlParser._

case class File(
    id: Pk[Long] = NotAssigned,
    name:String,
    description:String,
    randomId: String,
    filename:String,
    userId: Long,
    createdAt: Date,
    updatedAt: Date)

object File {

    def apply(name: String, description: String, filename: String, uId: Long): File = {
        val rand = User.generateSalt(128)
        File(NotAssigned, name, description, rand, filename, uId, new Date, new Date)
    }
    
    val simple = {
        get[Pk[Long]]("files.id") ~
        str("files.name") ~
        str("files.description") ~
        str("files.random_id") ~
        str("files.filename") ~
        long("files.user_id") ~
        date("files.created_at") ~
        date("files.updated_at") map {
            case id~name~descr~rand~path~uid~created~updated => 
                File(id, name, descr, rand, path, uid, created, updated)
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
                insert into files (
                    name, description, random_id, filename, user_id,
                    created_at, updated_at
                ) values (
                    {name}, {description}, {rand}, {filename}, {uid},
                    {created}, {updated}
                )
                """
            ).on(
                'name -> file.name,
                'description -> file.description,
                'rand -> file.randomId,
                'filename -> file.filename,
                'uid -> file.userId,
                'created -> file.createdAt,
                'updated -> file.updatedAt
            ).executeInsert()
        }
    }

    def update(id: Long, file: File) = {
        DB.withConnection { implicit connection =>
            SQL(
                """
                update files
                set name = {name}, description = {descr}, filename = {filename}
                where id = {id}
                """
            ).on(
                'id -> id,
                'name -> file.name,
                'descr -> file.description,
                'filename -> file.filename
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

    implicit object FileWrites extends Writes[File] {
        def writes(f:File): JsValue = JsObject(Seq(
            "id" -> JsNumber(f.id.get),
            "name" -> JsString(f.name),
            "description" -> JsString(f.description),
            "path" -> JsString("http://"+utils.Aws.bucket+".s3.amazonaws.com/files/"+f.randomId+"/"+f.filename),
            "created_at" -> JsString(f.createdAt.toString)
        ))
    }
}

