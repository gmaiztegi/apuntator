package controllers

import play.api.data._
import play.api.data.Forms._
import play.api.libs.concurrent._
import play.api.libs.json._
import play.api._
import play.api.mvc._

import anorm._

import views._
import models._

import com.amazonaws.auth._
import com.amazonaws.services.s3.transfer._
import com.amazonaws.services.s3.model._

object ApiFile extends Controller {
    
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
    
    def show(id: Long) = TODO
    
    def edit(id: Long) = TODO
    
    def delete(id: Long) = TODO
    
    def create = Action(parse.multipartFormData) { implicit request =>
        fileForm.bindFromRequest.fold(
            (formWithErrors => BadRequest("Error!")),
            (file => {
                request.body.file("file").map { upload =>
                    val filename = upload.filename
                    send(upload)
                    val id = File.insert(File(NotAssigned, file.name, file.description, Some(filename)))
                    val newfile = File(anorm.Id(id), file.name, file.description, Some(filename))
                    Created(views.html.iframehack(Json.toJson(newfile)))
                }.getOrElse(BadRequest("Missing file"))
            })
        )
    }
    
    def send (part: MultipartFormData.FilePart[play.api.libs.Files.TemporaryFile]) = {
        
        
        val credentials = new BasicAWSCredentials("AKIAI6L4BGFPKZLQ77UA", "DxvugUxEOHL8snd/C16g3XeSuyu5KBBTjL4qEoyG")
        val manager = new TransferManager(credentials)
        
        val request = new PutObjectRequest("apuntator", "files/"+part.filename, part.ref.file)
        request.setCannedAcl(CannedAccessControlList.PublicRead)
        
        val metadata = new ObjectMetadata()
        metadata.setContentType(part.contentType.getOrElse("application/octet-stream"))
        metadata.setContentDisposition("attachment; filename="+part.filename+";")
        request.setMetadata(metadata)
        
        val upload = manager.upload(request)
        
        upload.waitForCompletion
        part.ref.finalize
    }
}