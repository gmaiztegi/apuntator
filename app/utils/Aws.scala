package utils

import play.api.Play.current
import play.api.libs.Files.TemporaryFile
import play.api.mvc.MultipartFormData.FilePart

import com.amazonaws.auth._
import com.amazonaws.services.s3.transfer._
import com.amazonaws.services.s3.model._

object Aws {
    
    val configuration = current.configuration
    val bucket = configuration.getString("aws.s3.bucket").getOrElse(throw configuration.globalError("Missing S3 bucket name in configuration."))
    val key = configuration.getString("aws.s3.key").getOrElse(throw configuration.globalError("Missing S3 access key in configuration."))
    val secret = configuration.getString("aws.s3.secret").getOrElse(throw configuration.globalError("Missing S3 access secret in configuration."))
    
    val credentials = new BasicAWSCredentials(key, secret)
    
    def upload(folder: String, part: FilePart[TemporaryFile]) = {
        
        val manager = new TransferManager(credentials)
        
        val request = new PutObjectRequest(bucket, folder+"/"+part.filename, part.ref.file)
        request.setCannedAcl(CannedAccessControlList.PublicRead)
        
        val metadata = new ObjectMetadata()
        metadata.setContentType(part.contentType.getOrElse("application/octet-stream"))
        metadata.setContentDisposition("attachment; filename="+part.filename+";")
        request.setMetadata(metadata)
        
        val upload = manager.upload(request)
        
        upload.waitForCompletion
    }
}