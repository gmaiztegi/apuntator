package utils

import play.api._
import play.api.mvc._
import play.api.mvc.Results._

import play.api.libs.iteratee._

import models._

object OAuth {
    
    lazy val access_token: String = Play.maybeApplication map (_.configuration.getString("session.access_token")) flatMap (e => e) getOrElse ("access_token")
    
    def Authenticated[A](
        authentication: RequestHeader => Option[Authentication],
        onUnauthorized: RequestHeader => Result)(action: Authentication => Action[A]): Action[(Action[A], A)] = {

        val authenticatedBodyParser = BodyParser { request =>
            authentication(request).map { user =>
                val innerAction = action(user)
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
    
    def Authenticated[A](action: Authentication => Action[A]): Action[(Action[A], A)] = Authenticated(
        req => req.cookies.get(access_token).flatMap{ cookie =>
            Authentication.findByToken(cookie.value)
        },
        _ => Unauthorized(views.html.defaultpages.unauthorized()))(action)
}