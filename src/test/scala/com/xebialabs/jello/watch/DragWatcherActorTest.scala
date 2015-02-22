package com.xebialabs.jello.watch

import com.xebialabs.jello.domain.Trello.{Board, Card, Column}
import com.xebialabs.jello.support.ActorTestSugar
import com.xebialabs.jello.watch.DragWatcherActor.Tick
import org.mockito.Mockito._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class DragWatcherActorTest extends ActorTestSugar {


  describe("drag watcher") {

    it("should mark issue when dragged more then once") {

      val b = mock[Board]

      when(b.getColumns).thenReturn(Future(Seq(
        Column("c0", "0", Seq(Card("c1", "ticket1"), Card("c2", "ticket2"))),
        Column("c1", "1", Seq()),
        Column("c2", "2", Seq())
      )))
      val watcher = system.actorOf(DragWatcherActor.props(b))

      watcher ! Tick
      watcher ! Tick

      Thread.sleep(100)
      verify(b, never()).updateLabel(Card("c4", "Card 4"))

      when(b.getColumns).thenReturn(
        Future(Seq(
          Column("c0", "0", Seq(Card("c1", "ticket1"))),
          Column("c1", "1", Seq(Card("c2", "ticket2"))),
          Column("c2", "2", Seq())
        ))
      )

      watcher ! Tick
      watcher ! Tick

      Thread.sleep(100)
      verify(b, never()).updateLabel(Card("c4", "Card 4"))

      when(b.getColumns).thenReturn(
        Future(Seq(
          Column("c0", "0", Seq(Card("c1", "ticket1"))),
          Column("c1", "1", Seq()),
          Column("c2", "2", Seq(Card("c2", "ticket2")))
        ))
      )

      watcher ! Tick

      Thread.sleep(100)
      verify(b).updateLabel(Card("c2", "ticket2"))

    }


  }
}
