package test.utils

import org.specs2._

import play.api.libs.concurrent._
import play.api.libs.iteratee._
import play.api.mvc._

import play.api.test._
import play.api.test.Helpers._

trait TestUtils extends Specification {

  def getResult[A](action: Action[(Action[A], A)], request: Request[A]): PlainResult = {
    import java.net.URLEncoder
    import scala.collection.mutable.StringBuilder
    import org.specs2.execute.{Failure, FailureException, Skipped, SkipException}

    val iterat = action.parser(request)
    lazy val enumerator = {
      import play.api.http.Writeable
      import play.api.http.Writeable._
      import play.api.libs.json._
      val enum: Enumerator[Array[Byte]] = {
        request.body match {
          case AnyContentAsEmpty => Enumerator.eof
          case AnyContentAsRaw(raw) => { 
            val bytes = raw.asBytes().getOrElse(throw SkipException(Skipped("Buffer is too long")))
            Enumerator(implicitly[Writeable[Array[Byte]]].transform(bytes))
          }
          case AnyContentAsText(text) => Enumerator(text.getBytes)
            Enumerator(implicitly[Writeable[String]].transform(text))
          case AnyContentAsFormUrlEncoded(data) =>
            Enumerator(implicitly[Writeable[Map[String, Seq[String]]]].transform(data))
          case AnyContentAsJson(json) =>
            Enumerator(implicitly[Writeable[JsValue]].transform(json))
          case AnyContentAsXml(xml) =>
            Enumerator(implicitly[Writeable[scala.xml.NodeSeq]].transform(xml))
          case AnyContentAsMultipartFormData(MultipartFormData(dataParts, files, _, _)) => {
            val headerR = """multipart/form-data; boundary=(.+)""".r
            request.headers.get(CONTENT_TYPE) match {
              case Some(headerR(boundary)) => {

                val builder = StringBuilder.newBuilder
                dataParts.map { tuple =>
                  val (key, values) = tuple
                  values.map { value =>
                    builder
                      .append(boundary)
                      .append("\r\n")
                      .append("Content-Disposition: form-data; name=\"")
                      .append(key)
                      .append("\"\r\n\r\n")
                      .append(value)
                      .append("\r\n")
                  }
                }

                var enumerator = Enumerator(builder.toString.getBytes)

                files.map { part =>
                  val headString = {
                    val builder = StringBuilder.newBuilder
                    builder
                      .append(boundary)
                      .append("\r\n")
                      .append("Content-Disposition: form-data; name=\"")
                      .append(part.key)
                      .append("\"; filename=\"\r\n\r\n")
                      .append(part.filename)
                      .append("\"")

                    part.contentType.map { contentType =>
                      builder
                        .append("\r\n")
                        .append("Content-Type: ")
                        .append(contentType)
                    }

                    builder ++= "\"\r\n\r\n"

                    builder.toString
                  }

                  val file = part.ref match {
                    case file: java.io.File => file
                    case play.api.libs.Files.TemporaryFile(file) => file
                  }

                  enumerator =
                    enumerator >>> Enumerator(headString.getBytes) >>> Enumerator.fromFile(file) >>> Enumerator("\r\n".getBytes)
                }

                enumerator >>> Enumerator((boundary+"--").getBytes)
              }
              case _ => throw new FailureException(Failure("Incorrect multipart header"))
            }
          }
          case content => {
            val skipped = Skipped("RequestType " + content.getClass.getName + " is not supported.")
            throw new SkipException(skipped)
          }
        }
      }
      enum >>> Enumerator.eof
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
              def queryString = request.queryString
              def headers = request.headers
              def remoteAddress = request.remoteAddress
              val body = b
            })
        }
        
    val eventuallyResult = eventuallyResultOrRequest.extend(_.value match {
      case Redeemed(Left(result)) => result
      case Redeemed(Right(request)) => action(request)
      case error => throw new Exception()
    })

    def flatten(promise: Promise[Result]): Promise[PlainResult] = promise.flatMap {
      case result: PlainResult => promise.map(_ => result)
      case AsyncResult(promise) => flatten(promise)
    }

    await(flatten(eventuallyResult))
  }

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
}