package test.utils

import org.specs2._
import org.specs2.execute.{Result}
import org.specs2.specification._

import play.api.libs.concurrent._
import play.api.mvc._
import play.api.mvc.BodyParsers._

import utils._

import play.api.test._
import play.api.test.Helpers._

class TokenizedActionSpec extends Specification { def is =
    "Tokenized action specification".title                      ^
    "TokenizedAction creator can"                               ^
        "create with a response"                                ! e1 ^
        "create with an action"                                 ! e2 ^
        "create with an action and body parser"                 ! e3 ^
                                                                endbr ^
    "A tokenized action should"                                 ^
        "pass the token to the action if it is given"           ! e4 ^
        "generate the token if it is not given"                 ! withApplication(e5) ^
        "work with asyncronous too"                             ! withApplication(e6)

    def e1 = {
        val tokenizedAction = TokenizedAction { _: AuthenticityToken => Results.Ok("Hey, I'm a response!") }
        tokenizedAction must beLike { case _: TokenizedAction[_] => ok }
    }

    def e2 = {
        val tokenizedAction = TokenizedAction(_ => _ => Results.Ok("This is an action!"))
        tokenizedAction must beLike { case _: TokenizedAction[_] => ok }
    }

    def e3 = {
        val tokenizedAction = TokenizedAction(parse.anyContent)(_ => _ => Results.Ok("This is an action!"))
        tokenizedAction must beLike { case _: TokenizedAction[_] => ok }
    }

    def e4 = {
        val token = "dummytoken"
        val tokenObj = AuthenticityToken(token)

        val tokenizedAction = TokenizedAction { token: AuthenticityToken =>
            Results.Ok("The token is: "+token)
        }

        val result = tokenizedAction(Some(tokenObj))(FakeRequest())

        val session = Session.decodeFromCookie(cookies(result).get(Session.COOKIE_NAME))
        (contentAsString(result) must contain(token)) and (session.get(Secured.authenticity_token) must beNone)
    }

    def e5 = {
        val tokenizedAction = TokenizedAction { token: AuthenticityToken =>
            Results.Ok("The token is: "+token)
        }

        val result = tokenizedAction(None)(FakeRequest())
        val text = contentAsString(result)

        val session = Session.decodeFromCookie(cookies(result).get(Session.COOKIE_NAME))
        (text must be matching("^The token is: .+$")) and (session.get(Secured.authenticity_token) must beSome.which(text.contains(_)))
    }

    def e6 = {
        val tokenizedAction = TokenizedAction { token: AuthenticityToken =>
            AsyncResult(Promise.pure(Results.Ok("The token is: "+token)))
        }

        val asyncResult = tokenizedAction(None)(FakeRequest())
        val result = await(asyncResult.asInstanceOf[AsyncResult].result)
        val text = contentAsString(result)

        val session = Session.decodeFromCookie(cookies(result).get(Session.COOKIE_NAME))
        (text must be matching("^The token is: .+$")) and (session.get(Secured.authenticity_token) must beSome.which(text.contains(_)))
    }

    object withApplication extends Around {
        def around[T <% Result](t: =>T) = running(FakeApplication(additionalConfiguration = inMemoryDatabase())) {
            t
        }
    }
}
