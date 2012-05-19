package test.utils

import org.specs2._
import org.specs2.execute.{Result}
import org.specs2.specification._

import play.api.http._
import play.api.libs.concurrent._
import play.api.libs.iteratee._
import play.api.mvc._
import play.api.mvc.{Action}
import play.api.mvc.BodyParsers._

import models._
import utils._

import play.api.test._
import play.api.test.Helpers._

class SecuredSpec extends Specification with Secured { def is =
  "Secured action trait specification".title                    ^
  "IsAuthenticated method"                                      ^
    "passes when authenticated"                                 ! withDatabase(checkStatus(isAuthenticated, webAuthentication, OK)) ^
    "redirects when not authenticated"                          ! checkStatus(isAuthenticated, notAuthenticated, UNAUTHORIZED) ^
    "redirects when there isn't a token on POST"                ! withDatabase(checkStatus(isAuthenticated, webAuthentication.post, SEE_OTHER)) ^
    "doesn't redirect when there is a token on POST"            ! withDatabase(checkStatus(isAuthenticated, tokenize(webAuthentication), OK)) ^
    "doesn't redirect when client is an app on POST"            ! withDatabase(checkStatus(isAuthenticated, appAuthentication, OK)) ^
                                                                endbr ^
  "IsAuthenticatedTokenized"                                    ^
    "doesn't redirect when the method is GET"                   ! withDatabase(checkStatus(isAuthenticatedTokenized, webAuthentication, OK)) ^
    "does set session var when there isn't a token"             ! withDatabase(checkSetCookie(isAuthenticatedTokenized, webAuthentication, Secured.authenticity_token)) ^
    "doesn't set session var when there actually is"            ! withDatabase(checkSetCookie(isAuthenticatedTokenized, tokenize(webAuthentication), Secured.authenticity_token, true)) ^
                                                                endbr ^
  "IsNotAuthenticated"                                          ^
    "passes when not authenticated"                             ! checkStatus(isNotAuthenticated, notAuthenticated, OK) ^
    "redirects when authenticated"                              ! withDatabase(checkStatus(isNotAuthenticated, webAuthentication, SEE_OTHER)) ^
                                                                endbr ^
  "IsNotAuthenticatedTokenized"                                 ^
    "does set session var when there isn't a token"             ! withDatabase(checkSetCookie(isNotAuthenticatedTokenized, notAuthenticated, Secured.authenticity_token)) ^
    "doesn't set session var when there actually is"            ! withDatabase(checkSetCookie(isNotAuthenticatedTokenized, tokenize(notAuthenticated), Secured.authenticity_token, true)) ^
                                                                endbr ^
  "MayAuthenticate"                                             ^
    "executes first action when authenticated"                  ! withDatabase(checkStatus(mayAuthenticate, webAuthentication, OK)) ^
    "executes second action when not authenticated"             ! checkStatus(mayAuthenticate, notAuthenticated, ACCEPTED) ^
                                                                endbr ^
  "MayAuthenticateTokenized"                                    ^
    "does set session var when there isn't a token"             ! withDatabase(checkSetCookie(mayAuthenticateTokenized, webAuthentication, Secured.authenticity_token)) ^
    "doesn't set session var when there actually is"            ! withDatabase(checkSetCookie(mayAuthenticateTokenized, tokenize(webAuthentication), Secured.authenticity_token, true)) ^
    "does set session var when there isn't a token"             ! withDatabase(checkSetCookie(mayAuthenticateTokenized, notAuthenticated, Secured.authenticity_token)) ^
    "doesn't set session var when there actually is"            ! withDatabase(checkSetCookie(mayAuthenticateTokenized, tokenize(notAuthenticated), Secured.authenticity_token, true)) ^
                                                                endbr

  def checkSetCookie[A](action: Action[(Action[A], A)], request: Request[A], key: String, negate: Boolean = false) = {
    val result = getResult(action, request)
    val cookies = Helpers.cookies(result)
    val session = Session.decodeFromCookie(cookies.get(Session.COOKIE_NAME))
    if (negate) session.data must not beDefinedAt(key)  else session.data must beDefinedAt(key)
  }

  def checkStatus[A](action: Action[(Action[A], A)], request: Request[A], code: Int) = {
    val result = getResult(action, request)
    status(result) must beEqualTo(code)
  }

  def getResult[A](action: Action[(Action[A], A)], request: Request[A]): PlainResult = {
    import java.net.URLEncoder

    val iterat = action.parser(request)
    lazy val enumerator = {
      val body: Array[Byte] = {
        request.body match {
          case AnyContentAsFormUrlEncoded(data) => 
            data.map(item => item._2.map(c => item._1 + "=" + URLEncoder.encode(c, "UTF-8")))
              .flatten
              .mkString("&").getBytes
            case _ => Array():Array[Byte]
          }
      }
      Enumerator(body).andThen(Enumerator.enumInput(Input.EOF))
    }

    val eventuallyResultOrBody = enumerator |>> iterat

    val eventuallyResultOrRequest =
      eventuallyResultOrBody
        .flatMap(it => it.run)
        .map {
          _.right.map(b =>
            new Request[action.BODY_CONTENT] {
              def uri = request.uri
              def path = request.path
              def method = request.method
              def queryString = Map.empty
              def headers = request.headers
              val body = b
            })
        }
        
    val eventuallyResult = eventuallyResultOrRequest.extend(_.value match {
      case Redeemed(Left(result)) => result
      case Redeemed(Right(request)) => action(request)
      case error => throw new Exception()
    })

    await(eventuallyResult).asInstanceOf[PlainResult]
  }

  val isAuthenticated = IsAuthenticated { _ => _ =>
    Ok("Okay")
  }

  val isAuthenticatedTokenized = IsAuthenticatedTokenized { _ => _ => _ =>
    Ok("Okay")
  }

  val isNotAuthenticated = IsNotAuthenticated { _ =>
    Ok("Okay")
  }

  val isNotAuthenticatedTokenized = IsNotAuthenticatedTokenized { _ => _ =>
    Ok("Okay")
  }

  val mayAuthenticate = MayAuthenticate { _ => _ =>
    Ok("I'm authenticated")
  } { _ =>
    Accepted("I'm not authenticated")
  }

  val mayAuthenticateTokenized = MayAuthenticateTokenized { _ => _ => _ =>
    Ok("I'm authenticated")
  } { _ => _ =>
    Accepted("I'm not authenticated")
  }

  def tokenize(request: FakeRequest[_]): FakeRequest[AnyContentAsFormUrlEncoded] = {
    import scala.math.{BigInt}
    import scala.util.{Random}
    import java.net.URLEncoder

    val token = BigInt(32, new Random()).toString(36)

    val cookieHeader = {

      val sessionCookie = {
        val session = request.session + (Secured.authenticity_token, token)
        Session.encodeAsCookie(session)
      }

      val oldHeader = request.headers.get(HeaderNames.COOKIE)

      Cookies.merge(oldHeader.getOrElse(""), Seq(sessionCookie))
    }

    FakeRequest(
      POST,
      request.uri,
      request.headers,
      AnyContentAsFormUrlEncoded(Map(Secured.authenticity_token -> Seq(token))))
      .withHeaders(
        (HeaderNames.CONTENT_TYPE, ContentTypes.FORM),
        (HeaderNames.COOKIE, cookieHeader))
    }

  def authenticatedRequest: FakeRequest[AnyContent] = {
    val user = User("username", "email@example.com", "password")
    val userid = User.insert(user).get

    val token = AccessToken(None, userid)
    AccessToken.insert(token)

    val tokenCookie = Cookie(utils.Secured.access_token, token.token, (AccessToken.defaultExpirityMillis/1000).toInt)
    val cookies = Cookies.encode(Seq(tokenCookie))
    val headers = FakeHeaders(Map(HeaderNames.COOKIE -> Seq(cookies)))
    FakeRequest().copy(headers = headers)
  }

  object withDatabase extends Around {

    def around[T <% Result](t: => T) =
      running(FakeApplication(additionalConfiguration = inMemoryDatabase())) {
        t
      }
    }

  trait RequestHolder {

    def post: FakeRequest[AnyContent] = {
      request.copy(method = "POST")
    }

    def request: FakeRequest[AnyContent]
  }

  object RequestHolder {
    implicit def toRequest(holder: RequestHolder): FakeRequest[AnyContent] = holder.request
  }

  object notAuthenticated extends RequestHolder {
    def request = FakeRequest()
  }

  trait authenticatedWithDatabase extends RequestHolder {
    def userid: Long = {
      val user = User("username", "email@example.com", "password")
        User.insert(user).get
    }

    def token: String

    def request: FakeRequest[AnyContent] = {
      val tokenCookie = Cookie(utils.Secured.access_token, token, (AccessToken.defaultExpirityMillis/1000).toInt)
      val cookies = Cookies.encode(Seq(tokenCookie))
      val headers = FakeHeaders(Map(HeaderNames.COOKIE -> Seq(cookies)))
      FakeRequest().copy(headers = headers)
    }
  }

  object webAuthentication extends authenticatedWithDatabase {
    def token = {
      val token = AccessToken(None, userid)
      AccessToken.insert(token)

      token.token
    }
  }

  object appAuthentication extends authenticatedWithDatabase {
    def token = {
      val client = RegisteredClient("myclient", Set.empty)
      val clientId = RegisteredClient.insert(client)

      val token = AccessToken(clientId, userid)
      AccessToken.insert(token)

      token.token
    }
  }
}