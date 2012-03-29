package controllers

import java.util.{Date}

import play.api.data._
import play.api.data.Forms._
import play.api.Logger
import play.api.mvc._

import models._

object OAuth extends Controller {
    
    def token = Action(parse.urlFormEncoded) { request =>
        request.body.get("grant_type").map(_.head match {
            case "password" => Ok("Password!")
            case "token" => NotImplemented("Only password flow is supported.")
            case _ => BadRequest("Invalid grant_type")
        }).getOrElse(BadRequest("Missing grant_type"))
    }
}