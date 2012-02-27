package controllers

import play.api.data._
import play.api.data.Forms._
import play.api.libs.concurrent._
import play.api.libs.json._
import play.api._
import play.api.mvc._

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
            "path" -> ignored(None:Option[String])
        )(File.apply)(File.unapply)
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
                    Aws.upload("files", upload)
                    upload.ref.finalize
                    val id = File.insert(File(NotAssigned, file.name, file.description, Some(filename)))
                    val newfile = File(anorm.Id(id), file.name, file.description, Some(filename))
                    Created(views.html.iframehack(Json.toJson(newfile)))
                }.getOrElse(BadRequest("Missing file"))
            })
        )
    }
    
    def read(id: Long) = TODO
    
    def update(id: Long) = TODO
    
    def delete(id: Long) = TODO
}