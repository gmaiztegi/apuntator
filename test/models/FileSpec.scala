package test.models

import org.specs2._
import execute._
import specification._

import play.api.test._
import play.api.test.Helpers._

import models._

class FileSpec extends Specification { def is =
  "File class object specification".title         ^
  "A file class"                                  ^
    "can be created easily"                       ! e1 ^
                                                  endbr

  def e1 = {
    val file = File("The Name", "A long description of the file", "filename.ext", 0)
    file must anInstanceOf[File]
  }
}

class FileDBSpec extends Specification { def is =
  "File database specification".title             ^
  "File database allows to"                       ^
    "insert files"                                ! database(insert) ^
    "retrieve file list"                          ! database(retrieveList) ^
    "retrieve files by id"                        ! database(retrieveById) ^
    "udate files"                                 ! database(update) ^
    "delete files"                                ! database(delete)

  def insert(user: User) = {
    val file = File("File name", "File description", "filename.ext", user.id.get)
    val fileId = File.insert(file)

    fileId must beSome[Long]
  }

  def retrieveList(user: User) = {
    val file1 = {
      val file = File("File 1", "First file", "fileone.ext", user.id.get)
      val fileId = File.insert(file).get
      file.copy(id = anorm.Id(fileId))
    }

    val file2 = {
      val file = File("File 2", "Second file", "filetwo.ext", user.id.get)
      val fileId = File.insert(file).get
      file.copy(id = anorm.Id(fileId))
    }

    val files = File.all

    files.map(_.name) must contain(file1.name, file2.name)
  }

  def retrieveById(user: User) = {
    val file = File("File name", "File description", "filename.ext", user.id.get)
    val fileId = File.insert(file).get

    val fileOpt = File.findById(fileId)

    fileOpt must beSome.which(_.name == file.name)
  }

  def update(user: User) = {
    val file1 = {
      val file = File("File 1", "First file", "fileone.ext", user.id.get)
      val fileId = File.insert(file).get
      file.copy(id = anorm.Id(fileId))
    }

    val file2 = file1.copy(name = "File 2", description = "Otherfile")
    File.update(file2.id.get, file2)

    val lastFile = File.findById(file2.id.get)

    lastFile must beSome.which(_.description == "Otherfile")
  }

  def delete(user: User) = {
    val file = File("File name", "File description", "filename.ext", user.id.get)
    val fileId = File.insert(file).get

    File.delete(fileId)

    File.findById(fileId) must beNone
  }

  object database extends AroundOutside[User] {
    def outside: User = {
      val user = User("username", "email@test.tld", "password")
      val userId = User.insert(user).get
      user.copy(id = anorm.Id(userId))
    }

    def around[T <% Result](t: => T) =
      running(FakeApplication(additionalConfiguration = inMemoryDatabase())) { t }
  }
}