package com.xebialabs.jello.conf

trait DefaultConfig extends ConfigAware {

  override def config: JelloConfig = JelloConfig()

}
