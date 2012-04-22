package utils

import play.api._
import play.api.data.{Form}
import play.api.data.Forms._
import play.api.mvc._

import play.api.libs.iteratee._

import models._

object Secured {
    lazy val access_token: String = Play.maybeApplication map (_.configuration.getString("session.access_token")) flatMap (e => e) getOrElse ("access_token")
    lazy val authenticity_token: String = Play.maybeApplication map (_.configuration.getString("session.authenticity_token")) flatMap (e => e) getOrElse ("post_authenticity_token")
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

    def MayAuthenticate(yes: Authentication => Request[AnyContent] => Result, no: Request[AnyContent] => Result) = {
        Authenticated(Action(no)) { auth =>
            Action(yes(auth))
        }
    }

    def MayAuthenticateTokenized(yes: Authentication => AuthenticityToken => Request[AnyContent] => Result)(no: AuthenticityToken => Request[AnyContent] => Result) = {
        Authenticated(TokenizedAction(no)) { auth =>
            TokenizedAction(yes(auth))
        }
    }

    def Authenticated[A](onUnauthorized: TokenizedAction[A])(action: Authentication => TokenizedAction[A]): Action[(Action[A], Option[Authentication], Option[AuthenticityToken], A)] = {

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
            }
        }

        Action(authenticatedBodyParser) { request =>
            val (innerAction, auth, token, innerBody) = request.body
            val realRequest = request.map(_ => innerBody)
            val realAction = checkAuthenticity(realRequest, auth, token)(innerAction)
            realAction(realRequest)
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
            token filter (_ == requestToken) map (_ => action) getOrElse (nonAuthentic(action.parser))
            val correct = for {
                token1 <- token
                token2 <- Form(Secured.authenticity_token -> text).bindFromRequest()(req).value
            } yield token1 == token2
            if (correct.isDefined) action else nonAuthentic(action.parser)
        }
    }

    private def nonAuthentic[A](bodyParser: BodyParser[A]) = Action[A](bodyParser) { _ =>
        Redirect("/")
    }
}