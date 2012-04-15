package controllers

import java.util.{Date}

import play.api.data._
import play.api.data.Forms._
import play.api.Logger
import play.api.mvc._

import models._
import utils._

object Account extends Controller with Secured {

    val loginForm = Form(
        tuple(
            "username" -> nonEmptyText,
            "password" -> nonEmptyText,
            "remember_me" -> boolean
        )
    )

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
            "password" -> nonEmptyText
        )(
            (username, email, password) => User(username, email, password))(
            (user: User) => Some(user.username, user.email, "")
        )
    )

    private def getCallbackUrl()(implicit request: RequestHeader): Option[String] = {
        request.queryString.get("callback").map(_.head)
    }

    private def getCallbackMap(callback: Option[String]): Map[String, Seq[String]] = {
        callback.map { callbackUrl =>
            Map("callback" -> Seq(callbackUrl))
        }.getOrElse(Map.empty)
    }

    def login() = IsNotAuthenticated { implicit request =>
        Ok(views.html.Account.login(authenticityToken, false, getCallbackUrl))
    }

    def loginError() = IsNotAuthenticated { implicit request =>
        Ok(views.html.Account.login(authenticityToken, true, getCallbackUrl))
    }

    def loginPost() = IsNotAuthenticated(Action { implicit request =>
        loginForm.bindFromRequest.fold(
            formWithErrors => Redirect(routes.Account.loginError.url, getCallbackMap(getCallbackUrl)),
            data => {
                val (username, password, remember) = data
                User.findByUsername(username).filter(_.checkPassword(password)).map { user =>
                    val expires = AccessToken.defaultExpirityMillis
                    val token = AccessToken(None, user.id.get)
                    AccessToken.insert(token)
                    Redirect(getCallbackUrl.getOrElse("/")).withCookies(
                        Cookie(utils.Secured.access_token, token.token, (expires/1000).toInt)
                    ).withSession(
                        session + (Secured.authenticity_token, User.generateSalt())
                    )
                }.getOrElse(Redirect(routes.Account.loginError.url, getCallbackMap(getCallbackUrl)))
            }
        )
    })

    def logout() = Action { implicit request =>
        Redirect(routes.Application.index).discardingCookies(utils.Secured.access_token).withSession(
            session + (Secured.authenticity_token, User.generateSalt())
        )
    }

    def signup() = IsNotAuthenticated { implicit req =>
        Ok(views.html.Account.signup(authenticityToken, signupForm))
    }

    def signupPost() = IsNotAuthenticated(Action { implicit request =>
        signupForm.bindFromRequest.fold(
            formWithErrors => Ok(views.html.Account.signup(authenticityToken, formWithErrors)),
            user => {
                User.insert(user).map { id =>
                    val newuser = user.copy(id = anorm.Id(id))
                    Redirect(routes.Application.index)
                }.getOrElse(Ok(views.html.Account.signup(authenticityToken, signupForm.fill(user))))
            }
        )
    })
}