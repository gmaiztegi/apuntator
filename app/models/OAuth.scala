package models

import java.util.{Date}

import play.api.db._
import play.api.libs.json._
import play.api.Play.current

import anorm._
import anorm.SqlParser._

case class Token(id: Pk[Long] = NotAssigned, clientId: Option[Long], userId: Long,
    token: String, expiresAt: Date, redirectUri: Option[String] = None)

case class Client(id: Pk[Long] = NotAssigned, randomId: String, name: String,
    secret: Option[String] = None, grantTypes: List[String] = Nil)

case class Authentication(user: User, client: Client, token: Token)

object Token {
    
    val defaultExpirityMillis: Long = 7 * 24 * 60 * 60 * 1000
    
    // -- Parsers
    
    val simple = {
        get[Pk[Long]]("tokens.id") ~
        get[Option[Long]]("tokens.clientid") ~
        get[Long]("tokens.userid") ~
        get[String]("tokens.token") ~
        get[Date]("tokens.expires_at") ~
        get[Option[String]]("tokens.redirectUri") map {
            case id~cId~uId~token~exp~red => Token(id, cId, uId, token, exp, red)
        }
    }
    
    val withUser = Token.simple ~ User.simple map {
        case token~user => (token, user)
    }
    
    // -- Queries
    
    def findUserByToken(token: String): Option[(Token, User)] = {
        DB.withConnection { implicit connection =>
            SQL(
                """
                select * from users inner join tokens 
                on users.id = tokens.userid
                where token = {token}
                """
            ).onParams(token).as(Token.withUser.singleOpt)
        }
    }
    
    def insert(token: Token) = {
        DB.withConnection { implicit connection =>
            SQL(
                """
                insert into access_tokens () values (
                    (select next value for token_seq),
                    {cId}, {uId}, {token}, {expires}
                )
                """
            ).on(
                'cId -> token.clientId,
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
    
    val simple = User.simple ~ Client.simple ~ Token.simple map {
        case user~client~token => Authentication(user, client, token)
    }
    
    // -- Queries
    
    def findByToken(token: String): Option[Authentication] = {
        DB.withConnection { implicit connection =>
            SQL(
                """
                select * from users inner join tokens
                on users.id = tokens.user_id inner join
                clients on tokens.client_id = clients.id
                where redirect_uri is null and token.token = {token}
                """
            ).onParams(token).as(Authentication.simple.singleOpt)
        }
    }
}