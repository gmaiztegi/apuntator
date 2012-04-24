package utils

import play.api.mvc._

import models._

trait TokenizedAction[A] extends Function1[Option[AuthenticityToken], Action[A]]

object TokenizedAction {

    def apply (block: => AuthenticityToken => Result): TokenizedAction[AnyContent] = {
        apply(BodyParsers.parse.anyContent)(block.andThen(result => _ => result))
    }

    def apply (block: AuthenticityToken => Request[AnyContent] => Result): TokenizedAction[AnyContent] = {
        apply(BodyParsers.parse.anyContent)(block)
    }

    def apply[A](bodyParser: BodyParser[A])
        (block: AuthenticityToken => Request[A] => Result): TokenizedAction[A] = {
        new TokenizedAction[A] {
            def apply(authToken: Option[AuthenticityToken]) = authToken match {
                case Some(token) => Action(bodyParser)(block(token))
                case None => {
                    val token = AuthenticityToken(User.generateSalt())
                    Action(bodyParser)(block(token).andThen(addTokenToResult(token)))
                }
            }
        }
    }

    def addTokenToResult(token: String): Function1[Result, Result] = {
        case result: PlainResult => {
            val cookies = Cookies(result.header.headers.get(play.api.http.HeaderNames.COOKIE))
            val session = Session.decodeFromCookie(cookies.get(Session.COOKIE_NAME))
                result.withSession(
                    session + (Secured.authenticity_token, token)
                )
        }
        case AsyncResult(promise) => AsyncResult(promise.map(addTokenToResult(token)))
    }

    implicit def action2tokenizedAction[A](action: Action[A]): TokenizedAction[A] = {
        new TokenizedAction[A] {
            def apply(authToken: Option[AuthenticityToken]) = action
        }
    }
}

case class AuthenticityToken(token: String)

object AuthenticityToken {

    implicit def authenticityToken2String(authToken: AuthenticityToken): String = authToken.token

}
