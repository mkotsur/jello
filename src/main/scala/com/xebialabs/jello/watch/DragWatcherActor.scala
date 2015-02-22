package com.xebialabs.jello.watch

import akka.actor.{ActorLogging, Actor, Props}
import com.xebialabs.jello.domain.Trello.Board
import com.xebialabs.jello.watch.DragWatcherActor.Tick

import scala.concurrent.ExecutionContext.Implicits.global

object DragWatcherActor {

  def props(board: Board): Props = Props(new DragWatcherActor(board))
  case object Tick

}

class DragWatcherActor(board: Board) extends Actor with ActorLogging {

  case class Estimation(value: Int, revision: Int = 0)
  type WatchHandler = (Map[String, Estimation]) => Receive

  val watch: WatchHandler = (state: Map[String, Estimation]) => {
    case Tick =>
      log.debug("The state is: {}", state)
      board.getColumns.onSuccess {
        case columns =>
          context.become(watch(columns.flatMap({ column =>
            column.cards.map { card =>
              state.get(card.id) match {
                case Some(est) if est.value != column.name.toInt && est.revision == 1 =>
                  log.debug("Marking card: {}", card)
                  board.updateLabel(card)
                  (card.id, est.copy(revision = est.revision + 1))
                case Some(est) if est.value != column.name.toInt =>
                  log.debug("Card {} changed estimation, but should not be marked  ", card)
                  (card.id, Estimation(column.name.toInt, revision = est.revision + 1))
                case Some(est) =>
                  log.debug("Card {} has the same estimation", card)
                  (card.id, est)
                case None =>
                  (card.id, Estimation(column.name.toInt))
              }
            }
          }).toMap))
      }
  }

  override def receive: Receive = watch(Map())
}
