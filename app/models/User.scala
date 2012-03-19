package models

import play.api.db._
import play.api.libs.json._
import play.api.Play.current

import anorm._
import anorm.SqlParser._

case class User(id: Pk[Long] = NotAssigned, username: String, email: String,
    plainPassword: Option[String], salt: String,
    password: String, algorithm: String = User.defaultAlgorithm) {
    
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
        str("users.email") ~
        str("users.salt") ~
        str("users.password") ~
        str("users.algorithm") map {
            case id~username~email~salt~password~algorithm => User(id, username, email, None, salt, password, algorithm)
        }
    }
    
    def apply(username: String, email: String, plainPassword: String): User = {
        val (salt, hash) = User.encodePassword(plainPassword)
        User(NotAssigned, username, email, Some(plainPassword), salt, hash, defaultAlgorithm)
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
                    {username}, {email}, {salt}, {password}, {algorithm}
                )
                """
            ).on(
                'username -> user.username, 
                'email -> user.email,
                'salt -> user.salt,
                'password -> user.password,
                'algorithm -> user.algorithm
            ).executeInsert()
        }
    }
    
    def update(id: Long, user: User) = {
        DB.withConnection { implicit connection =>
            SQL(
                """
                update users
                set username = {username}, email = {email}, salt = {salt}, password = {password}, algorithm = {algorithm}
                where id = {id}
                """
            ).on(
                'id -> id,
                'username -> user.username, 
                'email -> user.email,
                'salt -> user.salt,
                'password -> user.password,
                'algorithm -> user.algorithm
            ).executeUpdate()
        }
    }
    
    def delete(id: Long) = {
        DB.withConnection { implicit connection =>
            SQL("delete from users where id = {id}").onParams(id).executeUpdate()
        }
    }
    
    def usernameExists(username: String): Boolean = {
        DB.withConnection { implicit connection =>
            val row = SQL(
                """
                select count(*) as c from users where username = {username}
                """
            ).onParams(username).single
            row[Long]("c") > 0
        }
    }
    
    def emailExists(email: String): Boolean = {
        DB.withConnection { implicit connection =>
            val row = SQL(
                """
                select count(*) as c from users where email = {email}
                """
            ).onParams(email).single
            row[Long]("c") > 0
        }
    }
    
    def encodePassword(password: String, algorithm: String = defaultAlgorithm, salt: String = generateSalt()) = {
        val digest = java.security.MessageDigest.getInstance(algorithm)
        val hash = digest.digest(password.getBytes).map("%02X" format _).mkString
        (salt, hash)
    }
    
    def generateSalt(): String = {
        val bigint = new java.math.BigInteger(256, new java.security.SecureRandom)
        bigint.toString(36)
    }
    
    implicit object UserFormat extends Format[User] {
        def reads(json: JsValue): User = User(
            (json \ "username").as[String],
            (json \ "email").as[String],
            (json \ "password").as[String]
        )
        def writes(u: User): JsValue = JsObject(List(
            "id" -> JsNumber(u.id.get),
            "username" -> JsString(u.username),
            "email" -> JsString(u.email),
            "salt" -> JsString(u.salt),
            "password" -> JsString(u.password)
        ))
    }
}