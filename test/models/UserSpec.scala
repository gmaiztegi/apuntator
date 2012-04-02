package test.models

import org.specs2._
import execute._
import specification._

import play.api.test._
import play.api.test.Helpers._

import _root_.models.{User}

class UserObjectSpec extends mutable.Specification {
    "Salting and hashing passwords" should {
        "produce random salts" in {
            User.generateSalt() must_!= User.generateSalt()
        }
        "hash with random salts" in {
            User.encodePassword("test") must_!= User.encodePassword("test")
        }
        "hash with specified salt when told to do so" in {
            User.encodePassword("test", "") must_== User.encodePassword("test", "")
        }
    }
}

class UserSpec extends Specification { def is =
    "An user with password ${test}"             ^ user ^
        "has password ${test}"                  ^ hasPlain ^
        "and checks ${test} properly"           ^ check ^
                                                end ^
    "Given two equal users"                     ^ users ^
        "Plain passwords must be equal"         ^ plain ^
        "The tokens must not be equal"          ^ token ^
        "Nor the hashed passwords"              ^ hashed ^
                                                end

    object user extends Given[User] {
        def extract(text: String): User = User("testuser", "testemail", extract1(text))
    }

    object hasPlain extends Then[User] {
        def extract(user: User, text: String): Result = user.plainPassword must_== Some(extract1(text))
    }

    object check extends Then[User] {
        def extract(user: User, text: String): Result = user.checkPassword(extract1(text))
    }

    object users extends Given[(User, User)] {
        def extract(text: String): (User, User) = {
            (User("testuser", "testemail", "testpassword"), User("testuser", "testemail", "testpassword"))
        }
    }

    object plain extends Then[(User, User)] {
        def extract(users: (User, User), text: String): Result = users._1.plainPassword must_== users._2.plainPassword
    }

    object token extends Then[(User, User)] {
        def extract(users: (User, User), text: String): Result = users._1.salt must_!= users._2.salt
    }

    object hashed extends Then[(User, User)] {
        def extract(users: (User, User), text: String): Result = users._1.password must_!= users._2.password
    }
}

class UserDBSpec extends mutable.Specification {
    "User model" should {
        "database should work" in {
            running(FakeApplication(additionalConfiguration = inMemoryDatabase())) {
                User.all.isEmpty must beTrue

                val user = User("testusername", "testemail", "testpass")
                val id = User.insert(user)
                id.isEmpty must beFalse
                User.all.length must beEqualTo(1)

                val dbuser = User.findById(id.get)
                dbuser.isEmpty must beFalse
                val Some(user2) = dbuser
                user2.username must beEqualTo(user.username)
                user2.email must beEqualTo(user.email)
                user2.checkPassword(user.plainPassword.get) must beTrue

                User.update(id.get, user.copy(username="username2")) must beEqualTo(1)
                val Some(user3) = User.findById(id.get)
                user3.username must beEqualTo("username2")

                User.delete(id.get)
                User.findById(id.get) must beNone
                User.all.isEmpty must beTrue
            }
        }
    }
}