package com.xebialabs.jello

import com.xebialabs.jello.domain.{Jira, Trello}

import scala.concurrent.Await
import scala.concurrent.duration._


object Main extends App {

  val jello = new Jello(new Jira(), new Trello())
  val listsFuture = jello.prepareForEstimation(Seq("REL-2218")).thenPrint()
  
  val lists = Await.result(listsFuture, 10 seconds)

  lists.lists.foreach(println)
  //
  //  new Jira().getTicket("REL-2218").onComplete {
  //    case Success(t) => println(t)
  //    case Failure(e) => e.printStackTrace()
  //  }
//
//  printFuture(new Trello().createBoard().flatMap {
//    case b =>
//      println("Creating ", b)
//      b.putTickets(Seq(Ticket("D-111", "Hello"), Ticket("D-222", "Hello333")))
//  })



}
