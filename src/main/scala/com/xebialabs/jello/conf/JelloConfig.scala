package com.xebialabs.jello.conf

import com.typesafe.config.{Config, ConfigFactory}

import scala.collection.JavaConversions._

object JelloConfig {

  def apply() = new JelloConfig(ConfigFactory.load())

}

sealed class JelloConfig(val conf: Config) {

  object trello {

    lazy val appKey = conf.getString("trello.appKey")

    lazy val token = conf.getString("trello.appToken")

    lazy val apiUri = conf.getString("trello.apiUri")

    lazy val lists: Seq[Int] = conf.getIntList("trello.lists").map(_.toInt)
  }

  object jira {

    lazy val apiUri = conf.getString("jira.apiUri")

    lazy val estimationFieldId = conf.getString("jira.estimationFieldId")

    lazy val username = conf.getString("jira.username")

    lazy val password = conf.getString("jira.password")
  }

}
