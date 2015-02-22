package com.xebialabs.jello

import com.xebialabs.jello.domain.Jira.Ticket
import com.xebialabs.jello.domain.Trello.{Column, Card}

import scala.language.implicitConversions

package object domain {

  implicit def column2tickets(column: Column): Seq[Ticket] = {
    val estimation = Some(column.name.toInt)
    column.cards.map {
      card => Ticket(
        card.name.split(" ").head,
        card.name.split(" ").tail.mkString(" "),
        estimation
      )
    }
  }

  implicit def columns2tickets(columns: Seq[Column]): Seq[Ticket] = columns.flatMap(column2tickets)

}
