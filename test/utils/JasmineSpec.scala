package test.utils

import org.specs2._
import org.specs2.execute.{Result}
import org.specs2.specification._

import play.api.test._
import play.api.test.Helpers._

class JasmineSpec extends Specification { def is =
  "Javascript Jasmine specs".title            ^
    "run correctly"                           ! run

  def run = running(TestServer(3333), HTMLUNIT) { browser =>
    //browser.goTo("http://localhost:3333/@test/jasmine.html")
    todo
  }
}