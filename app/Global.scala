import scala.collection.mutable.{MutableList}

import play.api._
import play.api.libs.concurrent._

import akka.actor._
import akka.util._

import java.util.concurrent.{TimeUnit}

import models._

object Global extends GlobalSettings {

    private val tasks: MutableList[Cancellable] = new MutableList()

    override def onStart(app: Application) {
        val delay = Duration(10, TimeUnit.SECONDS)
        val period = Duration(30, TimeUnit.MINUTES)

        tasks += Akka.system(app).scheduler.schedule(delay, period) {
            val howmany = AccessToken.deleteExpired
            Logger.debug("Removed "+howmany+" expired access tokens.")
        }
    }

    override def onStop(app: Application) {
        tasks.foreach (_.cancel)
    }
}