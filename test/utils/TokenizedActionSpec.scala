package test.utils

import org.specs2.mutable._

import play.api.libs.concurrent._
import play.api.mvc._
import play.api.mvc.BodyParsers._

import utils._

import play.api.test._
import play.api.test.Helpers._

class TokenizedActionSpec extends Specification {

    "TokenizedAction creator" should {
        "allow to create with a response" in {
            val tokenizedAction = TokenizedAction { _: AuthenticityToken => Results.Ok("Hey, I'm a response!") }
            tokenizedAction.isInstanceOf[TokenizedAction[_]]
        }

        "allow to create with an action" in {
            val tokenizedAction = TokenizedAction(_ => _ => Results.Ok("This is an action!"))
            tokenizedAction.isInstanceOf[TokenizedAction[_]]
        }

        "allow to create with an action and body parser" in {
            val tokenizedAction = TokenizedAction(parse.anyContent)(_ => _ => Results.Ok("This is an action!"))
            tokenizedAction.isInstanceOf[TokenizedAction[_]]
        }
    }

    "A tokized action should" should {
        "pass the token to the action if it is given" in {
            val token = "dummytoken"
            val tokenObj = AuthenticityToken(token)

            val tokenizedAction = TokenizedAction { token: AuthenticityToken =>
                Results.Ok("The token is: "+token)
            }

            val result = tokenizedAction(Some(tokenObj))(FakeRequest())
            contentAsString(result) must contain(token)

            val session = Session.decodeFromCookie(cookies(result).get(Session.COOKIE_NAME))
            session.get(Secured.authenticity_token) must beNone
        }

        "generate the token if it is not given" in {

            running(FakeApplication(additionalConfiguration = inMemoryDatabase())) {
                val tokenizedAction = TokenizedAction { token: AuthenticityToken =>
                    Results.Ok("The token is: "+token)
                }

                val result = tokenizedAction(None)(FakeRequest())
                val text = contentAsString(result)
                text must be matching("^The token is: .+$")

                val session = Session.decodeFromCookie(cookies(result).get(Session.COOKIE_NAME))
                session.get(Secured.authenticity_token) must beSome.which(text.contains(_))
            }
        }

        "work with async results too" in {
            running(FakeApplication(additionalConfiguration = inMemoryDatabase())) {
                val tokenizedAction = TokenizedAction { token: AuthenticityToken =>
                    AsyncResult(Promise.pure(Results.Ok("The token is: "+token)))
                }

                val asyncResult = tokenizedAction(None)(FakeRequest())
                val result = await(asyncResult.asInstanceOf[AsyncResult].result)
                val text = contentAsString(result)
                text must be matching("^The token is: .+$")

                val session = Session.decodeFromCookie(cookies(result).get(Session.COOKIE_NAME))
                session.get(Secured.authenticity_token) must beSome.which(text.contains(_))
            }
        }
    }
}