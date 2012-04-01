package models

import java.util.{Date}

import play.api.db._
import play.api.libs.json._
import play.api.Play.current

import anorm._
import anorm.SqlParser._

trait Token {
    val clientId: Option[Long]
    val userId: Long
    val token: String
    val expiresAt: Date
}

case class AccessToken(id: Pk[Long] = NotAssigned, clientId: Option[Long], userId: Long,
    token: String, expiresAt: Date) extends Token

case class RefreshToken(id: Pk[Long] = NotAssigned, clientId: Option[Long], userId: Long,
    currentTokenId: Option[Long], token: String, expiresAt: Date) extends Token

case class AuthCode(id: Pk[Long] = NotAssigned, clientId: Option[Long], userId: Long,
    token: String, expiresAt: Date, redirectUri: Option[String] = None) extends Token

case class Client(id: Pk[Long] = NotAssigned, randomId: String, name: String,
    secret: Option[String] = None, grantTypes: List[String] = Nil)

case class Authentication(user: User, token: AccessToken, client: Option[Client])

object Token {

    def generate(bits: Int = 128) = {
        val bigint = new java.math.BigInteger(bits, new java.security.SecureRandom)
        bigint.toString(36)
    }
}

object AccessToken extends  {
    
    val defaultExpirityMillis: Long = 60 * 60 * 1000
    
    // -- Parsers
    
    val simple = {
        get[Pk[Long]]("access_tokens.id") ~
        get[Option[Long]]("access_tokens.client_id") ~
        get[Long]("access_tokens.user_id") ~
        get[String]("access_tokens.token") ~
        get[Date]("access_tokens.expires_at") map {
            case id~cId~uId~token~exp => AccessToken(id, cId, uId, token, exp)
        }
    }
    
    def apply(clientId: Option[Long], userId: Long): AccessToken = {
        AccessToken(NotAssigned, clientId, userId, Token.generate(),
            new Date(System.currentTimeMillis + defaultExpirityMillis))
    }

    // -- Queries
    
    def insert(token: AccessToken) = {
        DB.withConnection { implicit connection =>
            SQL(
                """
                insert into access_tokens values (
                    nextval('access_token_seq'),
                    {cId}, {uId}, {token}, {expires}
                )
                """
            ).on(
                'cId -> token.clientId.getOrElse(null),
                'uId -> token.userId,
                'token -> token.token,
                'expires -> token.expiresAt
            ).executeInsert()
        }
    }
    
    def delete(id: Long) = {
        DB.withConnection { implicit connection =>
            SQL("delete from access_tokens where id = @id").onParams(id).executeUpdate
        }
    }

    def deleteExpired = {
        DB.withConnection { implicit connection =>
            SQL("delete from access_tokens where date < now()").executeUpdate
        }
    }
}

object RefreshToken {

    val defaultExpirityMillis: Long = 24 * 60 * 60 * 1000

    val simple = {
        get[Pk[Long]]("refresh_tokens.id") ~
        get[Option[Long]]("refresh_tokens.client_id") ~
        get[Long]("refresh_tokens.user_id") ~
        get[Option[Long]]("refresh_tokens.current_token_id") ~
        get[String]("refresh_tokens.token") ~
        get[Date]("refresh_tokens.expires_at") map {
            case id~cId~uId~cTId~token~exp => RefreshToken(id, cId, uId, cTId, token, exp)
        }
    }

    def apply(clientId: Option[Long], userId: Long, currentTokenId: Option[Long]): RefreshToken = {
        RefreshToken(NotAssigned, clientId, userId, currentTokenId, Token.generate(),
            new Date(System.currentTimeMillis + defaultExpirityMillis))
    }
}

object Client {

    val simple = {
        get[Pk[Long]]("clients.id") ~
        get[String]("clients.random_id") ~
        get[String]("clients.name") ~
        get[Option[String]]("clients.secret") map {
            case id~rand~name~secret => Client(id, rand, name, secret)
        }
    }
}

object Authentication {
    
    // -- Parsers
    
    val simple = User.simple ~ AccessToken.simple ~ (Client.simple ?) map {
        case user~token~client => Authentication(user, token, client)
    }
    
    // -- Queries
    
    def findByToken(token: String): Option[Authentication] = {
        DB.withConnection { implicit connection =>
            SQL(
                """
                select * from users inner join access_tokens t
                on users.id = t.user_id left outer join
                clients on t.client_id = clients.id
                where t.token = {token}
                """
            ).onParams(token).as(Authentication.simple.singleOpt)
        }
    }
}