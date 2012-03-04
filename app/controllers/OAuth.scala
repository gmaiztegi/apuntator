package controllers

import java.util.{Date}

import play.api.data._
import play.api.data.Forms._
import play.api.Logger
import play.api.mvc._

import models._

object OAuth extends Controller {
    
    val loginForm = Form(
        tuple(
            "username" -> nonEmptyText,
            "password" -> nonEmptyText,
            "remember" -> boolean
        )
    )
    
    def login = Action { implicit request =>
        loginForm.bindFromRequest.fold(
            formWithErrors => BadRequest("Bad request"),
            data => {
                val (username, password, remember) = data
                User.findByUsername(username).map { user =>
                    if (user.checkPassword(password)) {
                        val expires = System.currentTimeMillis + Token.defaultExpirityMillis
                        val token = Token(anorm.NotAssigned, None, user.id.get,
                            Token.generate, new Date(expires))
                        Ok("Successfuly logged in.").withCookies(
                            Cookie("access_code", token.token, (expires/1000).toInt)
                        )
                    } else BadRequest("Username or password incorrect.")
                }.getOrElse(BadRequest("Username or password incorrect."))
            }
        )
    }
    
    def token = Action(parse.urlFormEncoded) { request =>
        request.body.get("grant_type").map(_.head match {
            case "password" => Ok("Password!")
            case "token" => NotImplemented("Only password flow is supported.")
            case _ => BadRequest("Invalid grant_type")
        }).getOrElse(BadRequest("Missing grant_type"))
    }
}