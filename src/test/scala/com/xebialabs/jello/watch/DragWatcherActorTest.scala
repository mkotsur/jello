package com.xebialabs.jello.watch

import com.xebialabs.jello.domain.Jira.Ticket
import com.xebialabs.jello.domain.Trello
import com.xebialabs.jello.domain.Trello.{Column, Card, Board}
import com.xebialabs.jello.support.ActorTestSugar
import com.xebialabs.jello.watch.DragWatcherActor.Tick
import org.mockito.Mockito._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class DragWatcherActorTest extends ActorTestSugar {


  describe("drag watcher") {

    it("should mark issue when dragged more then once") {

      val b = mock[Board]

      val t1 = Ticket("T-1", "ticket1", Some(0))
      val t2 = Ticket("T-2", "ticket2", Some(0))
      val t3 = Ticket("T-3", "ticket3", Some(0))

      val c0 = Column("c0", "0", Seq(Card("T-1", "ticket1"), Card("T-1", "ticket1")))
      val c1 = Column("c1", "1", Seq())
      val c2 = Column("c2", "2", Seq())

      when(b.getColumns).thenReturn(Future(Seq(c0, c1, c2)))
      val watcher = system.actorOf(DragWatcherActor.props(b))

      watcher ! Tick

      verify(b).updateLabel(Card("c4", "Card 4"))

    }


  }
}
