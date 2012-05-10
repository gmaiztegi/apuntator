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
            try {
                val howmany = AccessToken.deleteExpired
                Logger.debug("Removed "+howmany+" expired access tokens.")
            } catch {
                case ex: java.sql.SQLException => if (!ex.getMessage.startsWith("Attempting to obtain a connection from a pool that has already been shutdown")) throw ex
                case ex: RuntimeException => if (ex.getMessage != "There is no started application") throw ex
            }
        }
    }

    override def onStop(app: Application) {
        tasks.foreach (_.cancel)
    }
}