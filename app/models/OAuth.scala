package models

import java.util.{Date}

import play.api.db._
import play.api.libs.json._
import play.api.Play.current

import anorm._
import anorm.SqlParser._

case class AccessToken(id: Pk[Long] = NotAssigned, clientId: Option[Long], userId: Long,
    token: String, expiresAt: Date)

case class AuthCode(id: Pk[Long] = NotAssigned, clientId: Option[Long], userId: Long,
    token: String, expiresAt: Date, redirectUri: Option[String] = None)

case class Client(id: Pk[Long] = NotAssigned, randomId: String, name: String,
    secret: Option[String] = None, grantTypes: List[String] = Nil)

case class Authentication(user: User, token: AccessToken, client: Option[Client])

object AccessToken {
    
    val defaultExpirityMillis: Long = 7 * 24 * 60 * 60 * 1000
    
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
    
    def deleteExpired = {
        DB.withConnection { implicit connection =>
            SQL("delete from access_tokens where date < now()").executeUpdate
        }
    }
    
    def generate = {
        val bigint = new java.math.BigInteger(128, new java.security.SecureRandom)
        bigint.toString(36)
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