package utils

import java.io.{File}

import play.api.Play.current
import play.api.libs.concurrent._
import play.api.libs.Files.TemporaryFile
import play.api.Logger
import play.api.mvc.MultipartFormData.FilePart

import com.amazonaws.auth._
import com.amazonaws.services.s3.transfer._
import com.amazonaws.services.s3.transfer.model._
import com.amazonaws.services.s3.model._

object Aws {

    val defaultContentType = "application/octet-stream"
    
    val configuration = current.configuration
    val bucket = configuration.getString("aws.s3.bucket").getOrElse(throw configuration.globalError("Missing S3 bucket name in configuration."))
    val key = configuration.getString("aws.s3.key").getOrElse(throw configuration.globalError("Missing S3 access key in configuration."))
    val secret = configuration.getString("aws.s3.secret").getOrElse(throw configuration.globalError("Missing S3 access secret in configuration."))
    
    val credentials = new BasicAWSCredentials(key, secret)
    
    def upload(part: FilePart[TemporaryFile], folder: Option[String]): Promise[UploadResult] = {
        upload(part.filename, part.ref.file, folder, part.contentType)
    }

    def upload(filename: String, file: File, folder: Option[String] = None, contentType: Option[String] = None): Promise[UploadResult] = {
        
        val key = folder.map(_+"/").getOrElse("")+filename

        val manager = new TransferManager(credentials)
        
        val request = new PutObjectRequest(bucket, key, file)
        request.setCannedAcl(CannedAccessControlList.PublicRead)
        
        val metadata = new ObjectMetadata()
        metadata.setContentType(contentType.getOrElse(defaultContentType))
        metadata.setContentDisposition("attachment; filename="+filename+";")
        request.setMetadata(metadata)
        
        Akka.future[UploadResult] {
            val upload = manager.upload(request)
            val result = upload.waitForUploadResult
            Logger.info("File \""+filename+"\" uploaded to Amazon S3.")
            result
        }
    }
}