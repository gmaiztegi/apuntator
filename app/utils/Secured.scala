package utils

import play.api._
import play.api.data.{Form}
import play.api.data.Forms._
import play.api.mvc._

import play.api.libs.iteratee._

import models._

object Secured {
    lazy val access_token: String =
        Play.maybeApplication
            .flatMap(_.configuration.getString("session.access_token"))
            .getOrElse("access_token")

    lazy val authenticity_token: String =
        Play.maybeApplication
            .flatMap (_.configuration.getString("session.authenticity_token"))
            .getOrElse ("post_authenticity_token")
}

trait Secured extends Results {

    def IsAuthenticated(action: Authentication => Request[AnyContent] => Result) = {
        Authenticated(defaultUnAuth) { auth =>
            Action(action(auth))
        }
    }

    def IsAuthenticatedTokenized(action: Authentication => AuthenticityToken => Request[AnyContent] => Result) = {
        Authenticated(defaultUnAuth) { auth =>
            TokenizedAction(token => request => action(auth)(token)(request))
        }
    }

    def IsNotAuthenticated(action: Request[AnyContent] => Result) = {
        Authenticated(Action(action))(defaultAuth)
    }

    def IsNotAuthenticatedTokenized(action: AuthenticityToken => Request[AnyContent] => Result) = {
        Authenticated(TokenizedAction(action))(defaultAuth)
    }

    def MayAuthenticate(yes: Authentication => Request[AnyContent] => Result)(no: Request[AnyContent] => Result) = {
        Authenticated(Action(no)) { auth =>
            Action(yes(auth))
        }
    }

    def MayAuthenticateTokenized(yes: Authentication => AuthenticityToken => Request[AnyContent] => Result)(no: AuthenticityToken => Request[AnyContent] => Result) = {
        Authenticated(TokenizedAction(no)) { auth =>
            TokenizedAction(yes(auth))
        }
    }

    def Authenticated[A](onUnauthorized: TokenizedAction[A])(action: Authentication => TokenizedAction[A]): Action[(Action[A], A)] = {

        val authenticatedBodyParser = BodyParser { request =>
            val token = authenticityToken(request)
            authentication(request).map { auth =>
                val innerAction = action(auth)(token)
                innerAction.parser(request).mapDone { body =>
                    body.right.map(innerBody => (innerAction, Some(auth).asInstanceOf[Option[Authentication]], token, innerBody))
                }
            }.getOrElse {
                val innerAction = onUnauthorized(token)
                innerAction.parser(request).mapDone { body =>
                    body.right.map(innerBody => (innerAction, None, token, innerBody))
                }
            }.mapDone(_.right.map { innerBody =>
                val realRequest = new Request[A] {
                    def headers = request.headers
                    def method = request.method
                    def path = request.path
                    def queryString = request.queryString
                    def uri = request.uri
                    def body = innerBody._4
                }
                val realAction = checkAuthenticity(realRequest, innerBody._2, innerBody._3)(innerBody._1)
                (realAction, innerBody._4)
            })
            
        }

        Action(authenticatedBodyParser) { request =>
            val (innerAction, innerBody) = request.body
            val realRequest = request.map(_ => innerBody)
            innerAction(realRequest)
        }

    }

    private val defaultAuth = { _ : Authentication => TokenizedAction.action2tokenizedAction(Action(Redirect("/"))) }

    private val defaultUnAuth = Action(Unauthorized("Access denied"))

    private val authentication: RequestHeader => Option[Authentication] = { req =>
        req.cookies.get(Secured.access_token).flatMap { cookie => Authentication.findByToken(cookie.value) }
    }

    private def authenticityToken(implicit req: RequestHeader): Option[AuthenticityToken] = {
        req.session.get(Secured.authenticity_token).map(AuthenticityToken(_))
    }

    private def checkAuthenticity[A](req: Request[A], auth: Option[Authentication], token: Option[AuthenticityToken])(action: Action[A]): Action[A] = {
        if (req.method == "GET" || auth.filterNot(_.client.needsAuthenticityToken).isDefined) action else {
            val requestToken = Form(Secured.authenticity_token -> text).bindFromRequest()(req).value.getOrElse("")
            token filter (_.token == requestToken) map(_ => action) getOrElse (nonAuthentic(action.parser))
        }
    }

    private def nonAuthentic[A](bodyParser: BodyParser[A]) = Action[A](bodyParser) { _ =>
        Redirect("/")
    }
}