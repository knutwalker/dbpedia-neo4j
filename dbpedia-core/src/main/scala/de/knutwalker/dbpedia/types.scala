package de.knutwalker.dbpedia

sealed trait Node {
  def n3: String
}

case class Resource(uri: String) extends Node {
  private[this] final val dataValue = uri

  lazy final val n3 = s"<$dataValue>"

  override def toString: String = dataValue
}

case class Literal(value: String, lang: Option[String], dt: Option[Resource]) extends Node {
  private[this] final val dataValue = value
  private[this] lazy final val langValue = lang.fold("")("@" + _)
  private[this] lazy final val dtValue = dt.fold("")("^^" + _.n3)
  private[this] lazy final val stringValue = s""""$dataValue"${langValue}$dtValue"""

  lazy final val n3 = stringValue

  override def toString: String = dataValue
}

case class BNode(nodeId: String) extends Node {
  private[this] final val dataValue = nodeId

  lazy final val n3 = s"_:$nodeId"

  override def toString: String = dataValue
}

sealed abstract class Statement(val s: Node, val p: Resource, val o: Node) extends Node {
  lazy val n3 = s"${s.n3} ${p.n3} ${o.n3} ."

  override def toString: String = s"$s $p $o"
}

object Statement {
  val Empty: Statement = Triple(BNode(""), Resource(""), BNode(""))
}

case class Triple(override val s: Node, override val p: Resource, override val o: Node) extends Statement(s, p, o)

case class Quad(override val s: Node, override val p: Resource, override val o: Node, ng: Resource) extends Statement(s, p, o) {
  override lazy final val n3 = s"${s.n3} ${p.n3} ${o.n3} ${ng.n3} ."

  override def toString: String = s"${super.toString} $ng"
}
