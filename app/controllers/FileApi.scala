package controllers

import play.api.data._
import play.api.data.Forms._
import play.api.libs.json._
import play.api._
import play.api.mvc._

import views._
import models._
import utils._
import utils.Security._

import anorm._

object FileApi extends Controller {
    
    def uploadForm(filename: String, uId: Long): Form[File] = Form(
        mapping(
            "name" -> nonEmptyText,
            "description" -> text,
            "filename" -> ignored(filename),
            "user_id" -> ignored(uId)
        )(File.apply)(
        (file: File) => Some(file.name, file.description, file.filename, file.userId))
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
    
    def create = Authenticated { auth =>
        Action(parse.multipartFormData) { implicit request =>
            request.body.file("file").map { upload =>
                val userId = auth.user.id.get
                val filename = upload.filename
                uploadForm(filename, userId).bindFromRequest.fold(
                    (formWithErrors => BadRequest("Error!")),
                    (file => {
                        Aws.upload(upload, Some("files/"+file.randomId)).map { _ =>
                            upload.ref.finalize
                        }
                        File.insert(file).map { id =>
                            val newfile = file.copy(id = anorm.Id(id))
                            Accepted(views.html.iframehack(Json.toJson(newfile)))
                        }.getOrElse(BadRequest("Error!"))
                    })
                )
            }.getOrElse(BadRequest("Missing file"))
        }
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
                val newfile = file.copy(name = name, description = desc)
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