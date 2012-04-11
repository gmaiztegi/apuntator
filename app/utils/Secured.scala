package utils

import play.api._
import play.api.data._
import play.api.data.Forms._
import play.api.mvc._
import play.api.mvc.Results._

import play.api.libs.iteratee._

import models._

trait Secured {

    private var newAuthenticityToken: Option[String] = None

    private val getAuth: RequestHeader => Option[Authentication] = { req =>
        req.cookies.get(Secured.access_token).flatMap { cookie => Authentication.findByToken(cookie.value) }
    }

    private val defaultUnAuth: RequestHeader => Result = { _ =>
        Unauthorized(views.html.defaultpages.unauthorized())
    }

    private val defaultAuthed = Action(BodyParsers.parse.anyContent)(_ => Redirect("/"))

    private def nonAuthentic[A](bodyParser: BodyParser[A])(implicit req: RequestHeader) = Action[A](bodyParser) { _ =>
        addToken(req)(Redirect("/"))
    }

    private def checkAuthenticity[A](defaultAction: RequestHeader => Result, auth: Option[Authentication])(implicit request: RequestHeader): RequestHeader => Result = {
        if (auth.flatMap(_.client).isDefined || sessionAuthenticityToken.isDefined) defaultAction else {
            defaultAction.andThen(addToken)
        }
    }

    private def checkAuthenticity[A](defaultAction: Action[A], auth: Option[Authentication])(implicit request: Request[A]): Action[A] = {
        if (auth.flatMap(_.client).isDefined) defaultAction else {
            if (request.method == "GET") {
                if (sessionAuthenticityToken.isDefined) defaultAction else {
                    Action(defaultAction.parser)(defaultAction.andThen(addToken))
                }
            }
            else {
                val requestToken = Form(Secured.authenticity_token -> text).bindFromRequest.value.getOrElse("")
                sessionAuthenticityToken filter (_ == requestToken) map (_ => defaultAction) getOrElse (nonAuthentic(defaultAction.parser))
            }
        }
    }

    private def addToken[A](implicit request: RequestHeader): Result => Result = {
        case result: PlainResult => result.withSession(request.session + (Secured.authenticity_token, User.generateSalt()))
        case AsyncResult(promise) => AsyncResult(promise.map(addToken))
        case result => result
    }

    private def sessionAuthenticityToken(implicit req: RequestHeader): Option[String] = {
        req.session.get(Secured.authenticity_token)
    }

    def authenticityToken(implicit req: RequestHeader): String = {
        sessionAuthenticityToken.orElse(newAuthenticityToken).getOrElse {
            val newToken = User.generateSalt()
            newAuthenticityToken = Some(newToken)
            newToken
        }
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

    def IsNotAuthenticated(f: Action[AnyContent]) = Authenticated(getAuth, f) { _ =>
        defaultAuthed
    }

    def IsNotAuthenticated(f: RequestHeader => Result) = Authenticated(getAuth, f) { _ =>
        defaultAuthed
    }

    def Authenticated[A](
        username: RequestHeader => Option[Authentication],
        onUnauthorized: RequestHeader => Result)(action: Authentication => Action[A]): Action[(Action[A], Option[Authentication], A)] = {

        val authenticatedBodyParser = BodyParser { request =>
            val realAction = checkAuthenticity(onUnauthorized, None)(request)
            username(request).map { user =>
                val innerAction = action(user)
                innerAction.parser(request).mapDone { body =>
                    body.right.map(innerBody => (innerAction, Some(user).asInstanceOf[Option[Authentication]], innerBody))
                }
            }.getOrElse {
                Done(Left(realAction(request)), Input.Empty)
            }
        }

        Action(authenticatedBodyParser) { request =>
            val (innerAction, auth, innerBody) = request.body
            val realRequest = request.map(_ => innerBody)
            val realAction = checkAuthenticity(innerAction, auth)(realRequest)
            realAction(realRequest)
        }

    }

    def Authenticated[A](
        username: RequestHeader => Option[Authentication],
        onUnauthorized: Action[A])(action: Authentication => Action[A]): Action[(Action[A], Option[Authentication], A)] = {

        val authenticatedBodyParser = BodyParser { request =>
            username(request).map { user =>
                val innerAction = action(user)
                innerAction.parser(request).mapDone { body =>
                    body.right.map(innerBody => (innerAction, Some(user).asInstanceOf[Option[Authentication]], innerBody))
                }
            }.getOrElse {
                onUnauthorized.parser(request).mapDone { body =>
                    body.right.map(innerBody => (onUnauthorized, None, innerBody))
                }
            }
        }

        Action(authenticatedBodyParser) { request =>
            val (innerAction, auth, innerBody) = request.body
            val realRequest = request.map(_ => innerBody)
            val realAction = checkAuthenticity(innerAction, auth)(realRequest)
            realAction(realRequest)
        }

    }

}

object Secured {
    lazy val access_token: String = Play.maybeApplication map (_.configuration.getString("session.access_token")) flatMap (e => e) getOrElse ("access_token")
    lazy val authenticity_token: String = Play.maybeApplication map (_.configuration.getString("session.authenticity_token")) flatMap (e => e) getOrElse ("post_authenticity_token")
}