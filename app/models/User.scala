package models

import java.util.{Date}

import play.api.db._
import play.api.libs.json._
import play.api.Play.current

import anorm._
import anorm.SqlParser._

case class User(id: Pk[Long] = NotAssigned, username: String,
    usernameCanonical: String, email: String, emailCanonical: String,
    enabled: Boolean, plainPassword: Option[String], salt: String,
    password: String, algorithm: String, createdAt: Date,
    lastLogin: Option[Date], locked: Boolean,
    confirmationToken: Option[String], passwordRequestedAt: Option[Date]) {
    
    def checkPassword(password: String): Boolean = {
        val (_, encoded) = User.encodePassword(password, algorithm, salt)
        this.password == encoded
    }
}

object User {
    
    val defaultAlgorithm = "SHA-512"
    
    val simple = {
        get[Pk[Long]]("users.id") ~
        str("users.username") ~
        str("users.username_canonical") ~
        str("users.email") ~
        str("users.email_canonical") ~
        bool("users.enabled") ~
        str("users.salt") ~
        str("users.password") ~
        str("users.algorithm") ~
        date("users.created_at") ~
        get[Option[Date]]("users.last_login") ~
        bool("users.locked") ~
        get[Option[String]]("users.confirmation_token") ~
        get[Option[Date]]("password_requested_at") map {
            case id~username~userCan~email~emailCan~enabled~salt~password~algorithm~created~login~locked~confirmation~requested => {
                User(id, username, userCan, email, emailCan, enabled, None, salt, password, algorithm, created, login, locked, confirmation, requested)
            }
        }
    }
    
    def apply(username: String, email: String, plainPassword: String): User = {
        val (salt, hash) = User.encodePassword(plainPassword)
        User(NotAssigned, username, username.toLowerCase, email, email.toLowerCase, false, Some(plainPassword), salt, hash, defaultAlgorithm, new Date(), None, false, Some(generateSalt(128)), None)
    }

    def all(): List[User] = {
        DB.withConnection { implicit connection =>
            SQL("select * from users").as(User.simple *)
        }
    }
    
    def findById(id: Long): Option[User] = {
        DB.withConnection { implicit connection =>
            SQL("select * from users where id = {id}").onParams(id).as(simple.singleOpt)
        }
    }
    
    def findByUsername(username: String): Option[User] = {
        DB.withConnection { implicit connection =>
            SQL(
                """
                select * from users where username = {username}
                """
            ).onParams(username).as(simple.singleOpt)
        }
    }
    
    def insert(user: User): Option[Long] = {
        DB.withConnection { implicit connection =>
            SQL(
                """
                insert into users values (
                    nextval('user_seq'),
                    {username}, {userCan}, {email}, {emailCan}, {enabled},
                    {salt}, {password}, {algorithm}, {created}, {login},
                    {locked}, {confirmation}, {requested}
                )
                """
            ).on(
                'username -> user.username,
                'userCan -> user.usernameCanonical,
                'email -> user.email,
                'emailCan -> user.emailCanonical,
                'enabled -> user.enabled,
                'salt -> user.salt,
                'password -> user.password,
                'algorithm -> user.algorithm,
                'created -> user.createdAt,
                'login -> user.lastLogin,
                'locked -> user.locked,
                'confirmation -> user.confirmationToken,
                'requested -> user.passwordRequestedAt
            ).executeInsert()
        }
    }
    
    def update(id: Long, user: User) = {
        DB.withConnection { implicit connection =>
            SQL(
                """
                update users
                set username = {username}, username_canonical = {userCan},
                email = {email}, email_canonical = {emailCan}, enabled = {enabled},
                salt = {salt}, password = {password}, algorithm = {algorithm},
                created_at = {created}, last_login = {login}, locked = {locked},
                confirmation_token = {confirmation}, password_requested_at = {requested}
                where id = {id}
                """
            ).on(
                'id -> id,
                'username -> user.username,
                'userCan -> user.usernameCanonical,
                'email -> user.email,
                'emailCan -> user.emailCanonical,
                'enabled -> user.enabled,
                'salt -> user.salt,
                'password -> user.password,
                'algorithm -> user.algorithm,
                'created -> user.createdAt,
                'login -> user.lastLogin,
                'locked -> user.locked,
                'confirmation -> user.confirmationToken,
                'requested -> user.passwordRequestedAt
            ).executeUpdate()
        }
    }
    
    def delete(id: Long) = {
        DB.withConnection { implicit connection =>
            SQL("delete from users where id = {id}").onParams(id).executeUpdate()
        }
    }
    
    def usernameExists(username: String): Boolean = {
        val canonicalized = username.toLowerCase
        DB.withConnection { implicit connection =>
            val row = SQL(
                """
                select count(*) as c from users where username = {username}
                """
            ).onParams(canonicalized).single
            row[Long]("c") > 0
        }
    }
    
    def emailExists(email: String): Boolean = {
        val canonicalized = email.toLowerCase
        DB.withConnection { implicit connection =>
            val row = SQL(
                """
                select count(*) as c from users where email = {email}
                """
            ).onParams(canonicalized).single
            row[Long]("c") > 0
        }
    }
    
    def encodePassword(password: String, algorithm: String = defaultAlgorithm, salt: String = generateSalt()) = {
        val digest = java.security.MessageDigest.getInstance(algorithm)
        val hash = digest.digest(password.getBytes).map("%02X" format _).mkString
        (salt, hash)
    }
    
    def generateSalt(bits: Int = 256): String = {
        val bigint = new java.math.BigInteger(bits, new java.security.SecureRandom)
        bigint.toString(36)
    }
    
    implicit object UserWrites extends Writes[User] {
        def writes(u: User): JsValue = JsObject(List(
            "id" -> JsNumber(u.id.get),
            "username" -> JsString(u.username),
            "username_canonical" -> JsString(u.usernameCanonical),
            "email" -> JsString(u.email),
            "registered_at" -> JsString(u.createdAt.toString)
        ))
    }
}