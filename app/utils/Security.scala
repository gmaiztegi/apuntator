package utils

import play.api._
import play.api.mvc._
import play.api.mvc.Results._

import play.api.libs.iteratee._

import models._

object Security {
    
    lazy val access_token: String = Play.maybeApplication map (_.configuration.getString("session.access_token")) flatMap (e => e) getOrElse ("access_token")

    private val getAuth: RequestHeader => Option[Authentication] = { req =>
        req.cookies.get(access_token).flatMap { cookie => Authentication.findByToken(cookie.value) }
    }

    private val defaultUnAuth: RequestHeader => Result = { _ =>
        Unauthorized(views.html.defaultpages.unauthorized())
    }

    private def Authenticated[A](
        authentication: RequestHeader => Option[Authentication]
    )(action: Authentication => Action[A], onUnauthorized: RequestHeader => Result): Action[(Action[A], A)] = {

        val authenticatedBodyParser = BodyParser { request =>
            authentication(request).map { auth =>
                val innerAction = action(auth)
                innerAction.parser(request).mapDone { body =>
                    body.right.map(innerBody => (innerAction, innerBody))
                }
            }.getOrElse {
                Done(Left(onUnauthorized(request)), Input.Empty)
            }
        }

        Action(authenticatedBodyParser) { request =>
            val (innerAction, innerBody) = request.body
            innerAction(request.map(_ => innerBody))
        }
    }

    def Authenticated[A](action: Authentication => Action[A], onUnauthorized: RequestHeader => Result = defaultUnAuth): Action[(Action[A], A)] = Authenticated(getAuth)(action, onUnauthorized)

}