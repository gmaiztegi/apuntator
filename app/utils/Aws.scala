package utils

import play.api.libs.Files.TemporaryFile
import play.api.mvc.MultipartFormData.FilePart

import com.amazonaws.auth._
import com.amazonaws.services.s3.transfer._
import com.amazonaws.services.s3.model._

object Aws {
    
    val bucket = "apuntator"
    val credentials = new BasicAWSCredentials("AKIAI6L4BGFPKZLQ77UA", "DxvugUxEOHL8snd/C16g3XeSuyu5KBBTjL4qEoyG")
    
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