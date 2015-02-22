package com.xebialabs.jello.watch

import akka.actor.{Props, Actor}
import akka.actor.Actor.Receive
import com.xebialabs.jello.domain.Trello
import com.xebialabs.jello.domain.Trello.Board
import com.xebialabs.jello.watch.DragWatcherActor.Tick
import scala.concurrent.ExecutionContext.Implicits.global

import scala.concurrent.Future

object DragWatcherActor {

  def props(board: Board): Props = Props(new DragWatcherActor(board))
  case object Tick

}

class DragWatcherActor(board: Board) extends Actor {


  override def receive: Receive = {
    case Tick =>
      board.getColumns.onSuccess {
        case columns =>
          println("GOT some tickets...")
      }
      println("Ticking!")
  }
}
