package com.xebialabs.jello.watch

import akka.actor.{ActorLogging, Actor, Props}
import com.xebialabs.jello.domain.Trello.Board
import com.xebialabs.jello.watch.DragWatcherActor.Tick

import scala.concurrent.ExecutionContext.Implicits.global
import com.xebialabs.jello.domain._

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
          val newState = columns.collect { case column if column.name.isNumber =>
            column.cards.map { card =>
              state.get(card.id) match {
                case Some(est) if est.value != column.name.toInt =>
                  log.debug("Estimation r{} has changed from {} to {} for card {}", est.revision, est.value, column.name, card)
                  if (est.revision == 1) {
                    log.debug("Marking card: {}", card)
                    board.updateLabel(card)
                  }
                  val newEst = Estimation(column.name.toInt, revision = est.revision + 1)
                  log.debug("New estimation object: {}", newEst)
                  (card.id, newEst)
                case Some(est) =>
                  log.debug("Card {} has the same estimation {}", card, est)
                  (card.id, est)
                case None =>
                  (card.id, Estimation(column.name.toInt, 1))
              }
            }
          }.flatten.toMap
          log.debug("The new state is: {}", newState)
          context.become(watch(newState))
      }
  }

  override def receive: Receive = watch(Map())
}
