package test.models

import java.util.{Date}

import org.specs2._
import org.specs2.execute._
import org.specs2.specification._

import models._

import play.api.test._
import play.api.test.Helpers._

class TokenSpec extends Specification { def is =
    "Token trait"                           ^
        "tell is not expired if future"     ! e1 ^
        "tell is expired if past"           ! e2

    def e1 = {
        val date = new Date(System.currentTimeMillis+1000000)
        val token: Token = AccessToken(clientId = None, userId = 0, token = "", expiresAt = date)
        token.isExpired must beFalse
    }

    def e2 = {
        val date = new Date(System.currentTimeMillis-1000000)
        val token: Token = AccessToken(clientId = None, userId = 0, token = "", expiresAt = date)
        token.isExpired must beTrue
    }
}

class AccessTokenSpec extends Specification { def is =
    "Access tokens, object and database".title      ^
    "Newly created access tokens are not expired"   ! e1 ^
                                                    p^
    "AccessToken persistence"                       ^ anInsertedToken(newAccessToken) ^
    "Expired tokens can be deleted easily"          ! withDatabase(e5(newExpiredAccessToken))

    def anInsertedToken(token: => AccessToken) =    
        "can be getted by id"                       ! withDatabase(e2(token)) ^
        "can be getted by token"                    ! withDatabase(e3(token)) ^
        "can be deleted"                            ! withDatabase(e4(token)) ^endbr

    def newAccessToken: AccessToken = {
        val userId = User.insert(User("username", "email", "password")).get
        val token = AccessToken(None, userId)
        val tokenId = AccessToken.insert(token).get
        token.copy(id = anorm.Id(tokenId))
    }

    def newExpiredAccessToken: AccessToken = {
        val userId = User.insert(User("username", "email", "password")).get
        val token = AccessToken(None, userId).copy(expiresAt = new Date(System.currentTimeMillis - 1000000))
        val tokenId = AccessToken.insert(token).get
        token.copy(id = anorm.Id(tokenId))
    }

    def e1 = AccessToken(None, 0).isExpired must beFalse

    def e2(token: AccessToken) = {
        val gotToken = AccessToken.findById(token.id.get)
        gotToken must beSome.which(_.token == token.token)
    }

    def e3(token: AccessToken) = {
        val gotToken = AccessToken.findByToken(token.token)
        gotToken must beSome.which(_.id == token.id)
    }

    def e4(token: AccessToken) = {
        val deleted = AccessToken.delete(token.id.get)
        deleted must beEqualTo(1)
    }

    def e5(token: AccessToken) = {
        val deleted = AccessToken.deleteExpired
        deleted must beEqualTo(1)

        val got = AccessToken.findById(token.id.get)
        (deleted must beEqualTo(1)) and (got must beNone)
    }

    object withDatabase extends Around {
        def around[T <% Result](t: =>T) = running(FakeApplication(additionalConfiguration = inMemoryDatabase())) {
            t
        }
    }
}

class RefreshTokenSpec extends Specification { def is =
    "Refresh token specification".title                                 ^
    "Newly created refresh tokens"                                      ^
        "have a much longer expiration than access tokens by default"   ! e1

    def e1 = {
        val accessTime = AccessToken(None, 0).expiresAt.getTime - System.currentTimeMillis
        val refreshTime = RefreshToken(None, 0, None).expiresAt.getTime - System.currentTimeMillis
        accessTime * 30 must beLessThanOrEqualTo(refreshTime)
    }
}

class WebClientSpec extends Specification { def is =
    "Web client specification".title                                ^
    "Web clients need authenticity token"                           ! e1

    def e1 = {
        WebClient.needsAuthenticityToken must beTrue
    }
}

class RegisteredClientSpec extends Specification { def is =
    "Registered client specification".title                         ^
    "Registered client objects"                                     ^
        "don't need authenticity token"                             ! e1 ^
        "generate random identifiers"                               ! e2 ^
        "generate random secrets"                                   ! e3 ^
        "and secrets are long enough"                               ! e4 ^
                                                                    endbr^
    "Registered client database"                                    ^
        "can insert"                                                ! withDatabase(e5)


    def e1 = {
        val client = RegisteredClient("MyClient", Set.empty)
        client.needsAuthenticityToken must beFalse
    }

    def e2 = {
        val client1 = RegisteredClient("MyClient", Set.empty)
        val client2 = RegisteredClient("MyClient", Set.empty)

        client1.randomId must not be equalTo(client2.randomId)
    }

    def e3 = {
        val client1 = RegisteredClient("MyClient", Set.empty)
        val client2 = RegisteredClient("MyClient", Set.empty)

        client1.secret must not be equalTo(client2.secret)
    }

    def e4 = {
        val client = RegisteredClient("MyClient", Set.empty)
        client.secret.length must beGreaterThan(14)
    }

    def e5 = {
        val id = RegisteredClient.insert(RegisteredClient("MyClient", Set.empty))
        id must beSome
    }

    object withDatabase extends Around {
        def around[T <% Result](t: =>T) = running(FakeApplication(additionalConfiguration = inMemoryDatabase())) {
            t
        }
    }
}

class AuthenticationSpec extends Specification { def is =
    "Authentication wrapper object specification".title             ^
    "Authentication database"                                       ^
        "returns web client if there is no client"                  ! withDatabase(e1(newWebToken)) ^
        "returns registered client if there is"                     ! withDatabase(e2(newRegisteredToken))

    def e1(token: String) = {
        val auth = Authentication.findByToken(token)

        auth must beLike { case Some(Authentication(_, _, WebClient)) => ok }
    }

    def e2(token: String) = {
        
        val auth = Authentication.findByToken(token)

        auth must beLike { case Some(Authentication(_, _, RegisteredClient(_, _, "My special client", _, _))) => ok }
    }

    def newWebToken: String = {
        val userId = User.insert(User("username", "email", "password")).get

        val token = AccessToken(None, userId)
        AccessToken.insert(token)

        token.token
    }

    def newRegisteredToken: String = {
        val userId = User.insert(User("username", "email", "password")).get

        val client = RegisteredClient("My special client", Set.empty)
        val clientId = RegisteredClient.insert(client).get

        val token = AccessToken(Some(clientId), userId)
        AccessToken.insert(token)

        token.token
    }

    object withDatabase extends Around {
        def around[T <% Result](t: =>T) = running(FakeApplication(additionalConfiguration = inMemoryDatabase())) {
            t
        }
    }
}