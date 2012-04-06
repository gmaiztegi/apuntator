package utils

import play.api._
import play.api.mvc._
import play.api.mvc.Results._

import play.api.libs.iteratee._

import models._

trait Secured {
    
    lazy val access_token: String = Play.maybeApplication map (_.configuration.getString("session.access_token")) flatMap (e => e) getOrElse ("access_token")

    private val getAuth: RequestHeader => Option[Authentication] = { req =>
        req.cookies.get(access_token).flatMap { cookie => Authentication.findByToken(cookie.value) }
    }

    private val defaultUnAuth: RequestHeader => Result = { _ =>
        Unauthorized(views.html.defaultpages.unauthorized())
    }

    def MayAuthenticate(f: => Authentication => Request[AnyContent] => Result, g: Action[AnyContent]) = Authenticated(getAuth, g) { user =>
        Action(request => f(user)(request))
    }

    def MayAuthenticate(f: => Authentication => Request[AnyContent] => Result, g: RequestHeader => Result) = Authenticated(getAuth, g) { user =>
        Action(request => f(user)(request))
    }

    def IsAuthenticated(f: => Authentication => Request[AnyContent] => Result) = Authenticated(getAuth, defaultUnAuth) { user =>
        Action(request => f(user)(request))
    }

    def Authenticated[A](
        username: RequestHeader => Option[Authentication],
        onUnauthorized: RequestHeader => Result)(action: Authentication => Action[A]): Action[(Action[A], A)] = {

        val authenticatedBodyParser = BodyParser { request =>
            username(request).map { user =>
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

    def Authenticated[A](
        username: RequestHeader => Option[Authentication],
        onUnauthorized: Action[A])(action: Authentication => Action[A]): Action[(Action[A], A)] = {

        val authenticatedBodyParser = BodyParser { request =>
            username(request).map { user =>
                val innerAction = action(user)
                innerAction.parser(request).mapDone { body =>
                    body.right.map(innerBody => (innerAction, innerBody))
                }
            }.getOrElse {
                onUnauthorized.parser(request).mapDone { body =>
                    body.right.map(innerBody => (onUnauthorized, innerBody))
                }
            }
        }

        Action(authenticatedBodyParser) { request =>
            val (innerAction, innerBody) = request.body
            innerAction(request.map(_ => innerBody))
        }

    }

}

object Secured {
    lazy val access_token: String = Play.maybeApplication map (_.configuration.getString("session.access_token")) flatMap (e => e) getOrElse ("access_token")
}