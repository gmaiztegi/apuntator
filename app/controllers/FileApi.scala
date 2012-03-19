package controllers

import play.api.data._
import play.api.data.Forms._
import play.api.libs.concurrent._
import play.api.libs.json._
import play.api._
import play.api.mvc._
import play.api.Play.current

import views._
import models._
import utils._

import anorm._

object FileApi extends Controller {
    
    val fileForm = Form(
        mapping(
            "id" -> ignored(NotAssigned:Pk[Long]),
            "name" -> nonEmptyText,
            "description" -> text,
            "path" -> ignored(null:String)
        )(File.apply)(File.unapply)
    )
    
    val updateForm = Form(
        tuple(
            "name" -> nonEmptyText,
            "description" -> text
        )
    )

    def list = Action {
        val files = File.all
        Ok(Json.toJson(files))
    }
    
    def create = Action(parse.multipartFormData) { implicit request =>
        fileForm.bindFromRequest.fold(
            (formWithErrors => BadRequest("Error!")),
            (file => {
                request.body.file("file").map { upload =>
                    val filename = upload.filename
                    Akka.future {
                        Aws.upload("files", upload)
                        upload.ref.finalize
                        Logger.info("File \""+filename+"\" uploaded to Amazon S3.")
                    }
                    File.insert(File(NotAssigned, file.name, file.description, filename)).map { id =>
                        val newfile = File(anorm.Id(id), file.name, file.description, filename)
                        Accepted(views.html.iframehack(Json.toJson(newfile)))
                    }.getOrElse(BadRequest("Error!"))
                }.getOrElse(BadRequest("Missing file"))
            })
        )
    }
    
    def read(id: Long) = Action {
        File.findById(id).map { file =>
            Ok(Json.toJson(file))
        }.getOrElse(NotFound("No file with id "+id))
    }
    
    def update(id: Long) = Action { implicit request =>
        updateForm.bindFromRequest.fold (
            formWithErrors => BadRequest("Formulario no valido"),
            newdata => File.findById(id).map { file =>
                val (name, desc) = newdata
                val newfile = File(file.id, name, desc, file.path)
                File.update(id, newfile)
                Ok(Json.toJson(newfile))
            }.getOrElse(NotFound("No file with id "+id))
        )
    }
    
    def delete(id: Long) = Action {
        File.findById(id).map { file =>
            File.delete(id)
            Ok("File deleted")
        }.getOrElse(NotFound("No file with id "+id))
    }
}