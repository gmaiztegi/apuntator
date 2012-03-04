package controllers

import play.api.data._
import play.api.data.Forms._
import play.api.libs.concurrent._
import play.api.libs.json._
import play.api._
import play.api.mvc._

import views._
import models._

object UserApi extends Controller {
    
    val signupForm: Form[User] = Form(
        mapping(
            "username" -> text(minLength = 4),
            "email" -> email,
            "password" -> tuple(
                "main" -> text(minLength = 6),
                "confirm" -> text
            ).verifying(
                "Las contraseñas no coinciden", passwords => passwords._1 == passwords._2
            )
        )
        {
            (username, email, passwords) => User(anorm.NotAssigned, username, email, Some(passwords._1))
        }
        {
            user => Some(user.username, user.email, (user.password.getOrElse(""), ""))
        }
    )
    
    def list = Action {
        val users = User.all
        Ok(Json.toJson(users))
    }
    
    def create = Action { implicit request =>
        signupForm.bindFromRequest.fold(
            formWithErrors => BadRequest(Json.toJson(JsObject(formWithErrors.errors.map{ error =>
                error.key -> JsString(error.message)
            }))),
            user => {
                val (salt, hash) = User.encodePassword(user.plainPassword.get)
                user.salt = Some(salt)
                user.password = Some(hash)
                User.insert(user).map { id =>
                    user.id = anorm.Id(id)
                    Created(Json.toJson(user))
                }.getOrElse(BadRequest("Server error"))
            }            
        )
    }
    
    def read(id: Long) = Action {
        User.findById(id).map { user =>
            Ok(Json.toJson(user))
        }.getOrElse(NotFound("No user with id "+id))
    }
    
    def update(id: Long) = TODO
    
    def delete(id: Long) = TODO
}