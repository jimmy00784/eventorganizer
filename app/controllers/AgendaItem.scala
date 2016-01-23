package controllers

import play.api.data.Forms._
import play.api.data._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc.{Action, Controller}

import scala.concurrent.Future

/**
  * Created by karim on 1/21/16.
  */
class AgendaItem extends Controller {
  import models.Database.Agenda._
  import models.Database.Events._

  import models.DbConfig.current.driver.api._
  import models.DbConfig.current.db
  def index = Action.async {

    val agendaItems = for {
      ((ai,et),at) <- agendaItemsTable join eventTypesTable on (_.eventTypeId === _.id) join
        agendaTypesTable on (_._1.agendaTypeId === _.id)
    } yield {
      (ai.id,et.name,at.name)
    }

    for {
      items <- db.run(agendaItems.result)
      eventTypes <- db.run(eventTypesTable.result)
      agendaTypes <- db.run(agendaTypesTable.result)
    } yield {
      Ok(views.html.index("Agenda Items")(views.html.aggregator(views.html.agendaitem.list(items.toList))(views.html.agendaitem.add(eventTypes.toList, agendaTypes.toList))))
    }
  }

  def get(id: Int) = Action.async { implicit request =>
    val q = for { a <- agendaItemsTable if a.id === id } yield a

    for {
      agendaItem <- db.run(q.take(1).result)
      eventTypes <- db.run(eventTypesTable.result)
      agendaTypes <- db.run(agendaTypesTable.result)
    } yield {
      if (agendaItem.size > 0)
        Ok(views.html.index("Agenda Item")(views.html.agendaitem.edit(agendaItem.head, eventTypes.toList, agendaTypes.toList)))
      else
        NotFound
    }
  }

  def edit(id: Int) = Action.async { implicit request =>
    val form = Form(
      tuple(
        "eventTypeId" -> number,
        "agendaTypeId" -> number
      ))

    form.bindFromRequest.fold(
      hasErrors => Future successful BadRequest,
      t => {
        val ai = models.AgendaItem(id,t._1,t._2)
        val q = for { a <- agendaItemsTable if a.id === id } yield (a.eventTypeId,a.agendaTypeId)
        db.run(q.update(ai.eventTypeId, ai.agendaTypeId)).map(_ => Redirect("/agendaitem")) recover {
          case e:Throwable => {
            e.printStackTrace()
            BadRequest
          }
        }
      }
    )
  }

  def submit = Action.async { implicit request =>
    val form = Form(
      mapping(
        "id" -> number,
        "eventTypeId" -> number,
        "agendaTypeId" -> number
      )(models.AgendaItem.apply)(models.AgendaItem.unapply))

    form.bindFromRequest.fold(
      hasErrors => Future successful BadRequest,
      agendaItem => {

        db.run(agendaItemsTable += agendaItem).map(_ => Redirect("/agendaitem")) recover {
          case e:Throwable => {
            e.printStackTrace()
            BadRequest
          }
        }
      }
    )
  }

  def remove(id: Int) = Action.async { implicit request =>
    val q = agendaItemsTable.filter(_.id === id)
    db.run(q.delete).map (_ => Redirect("/agendaitem")) recover {
      case e:Throwable => {
        e.printStackTrace()
        BadRequest
      }
    }
  }
}
