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
            "username" -> text(minLength = 4).verifying(
                "El usuario no se encuentra disponible",
                username => !User.usernameExists(username)
            ),
            "email" -> email.verifying(
                "El email no se encuentra disponible",
                email => !User.emailExists(email)
            ),
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
    
    def editForm(username: String, addr: String) = Form(
        tuple(
            "username" -> text(minLength = 4).verifying(
                "El usuario no se encuentra disponible",
                name => username != name && !User.usernameExists(name)
            ),
            "email" -> email.verifying(
                "El email no se encuentra disponible",
                addrin => addr != addrin && !User.emailExists(addrin)
            )
        )
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
    
    def update(id: Long) = Action { implicit request =>
        User.findById(id).map { user =>
            editForm(user.username, user.email).bindFromRequest.fold(
                formWithErrors => BadRequest("Errors"),
                data => {
                    val (username, email) = data
                    val newuser = User(user.id, username, email, None, user.salt, user.password, user.algorithm)
                    User.update(id, newuser)
                    Ok(Json.toJson(newuser))
                }
            )
        }.getOrElse(NotFound("No user with id "+id))
    }
    
    def delete(id: Long) = Action {
        User.findById(id).map { user =>
            User.delete(id)
            Ok("User deleted.")
        }.getOrElse(NotFound("No user with id "+id))
    }
}