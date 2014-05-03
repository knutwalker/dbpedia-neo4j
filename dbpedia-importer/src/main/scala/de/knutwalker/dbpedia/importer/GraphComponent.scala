package de.knutwalker.dbpedia.importer

trait GraphComponent {
  this: MetricsComponent ⇒

  type NodeType
  type RelType

  def graph: Graph

  object Labels {
    val subj = "Subject"
    val pred = "Predicate"
    val obj = "Object"
    val bNode = "BNode"
    val resource = "Resource"
    val literal = "Literal"
    val bNodeType = "BNodeType"
    val resourceType = "ResourceType"
  }

  object Properties {
    val uri = "uri"
    val value = "value"
    val nodeType = "nodeType"
    val dataType = "dataType"
  }

  trait Graph {

    // API methods

    def createRelTypeFor(name: String): RelType

    def subjectAdded(): Unit

    def startup(settings: SettingsComponent#Settings): Unit

    def shutdown(): Unit

    final def getOrCreateBNode(subject: String, labels: List[String] = Nil) =
      timeGetBNode(subject).getOrElse(timeCreateBNode(subject, labels))

    final def getOrCreateResource(uri: String, value: Option[String], labels: List[String] = Nil) =
      timeGetResource(uri).getOrElse(timeCreateResource(uri, value, labels))

    final def getAndUpdateResource(uri: String, value: Option[String], labels: List[String]): NodeType =
      timeGetResource(uri) match {
        case Some(id) ⇒ timeUpdateResource(id, value, labels)
        case None     ⇒ timeCreateResource(uri, value, labels)
      }

    final def createLiteral(literal: String, labels: List[String]) =
      timeCreateLiteral(literal, labels)

    final def createRel(src: NodeType, dest: NodeType, rel: String, relType: RelType) =
      timeCreateRelationship(src, dest, rel, relType)

    // internal graph methods to implement

    protected def getBNode(subject: String): Option[NodeType]

    protected def createBNode(subject: String, labels: List[String]): NodeType

    protected def getResourceNode(uri: String): Option[NodeType]

    protected def createResourceNode(uri: String, value: Option[String], labels: List[String]): NodeType

    protected def updateResourceNode(id: NodeType, value: Option[String], labels: List[String]): NodeType

    protected def createLiteralNode(literal: String, labels: List[String]): NodeType

    protected def createRelationship(src: NodeType, dest: NodeType, rel: String, relType: RelType): Unit

    // timing wrappers

    private val timeGetBNode = makeTimeWrapper1("lookup-bnode", getBNode)
    private val timeGetResource = makeTimeWrapper1("lookup-resource", getResourceNode)
    private val timeUpdateResource = makeTimeWrapper3("update-resource", updateResourceNode)

    private val timeCreateBNode = makeTimeWrapper2("create-bnode", makeMarkWrapper2(metrics.nodeAdded(), createBNode))
    private val timeCreateResource = makeTimeWrapper3("create-resource", makeMarkWrapper3(metrics.nodeAdded(), createResourceNode))
    private val timeCreateLiteral = makeTimeWrapper2("create-literal", makeMarkWrapper2(metrics.nodeAdded(), createLiteralNode))
    private val timeCreateRelationship = makeTimeWrapper4("create-rel", makeMarkWrapper4(metrics.relAdded(), createRelationship))

    private[this] def makeTimeWrapper1[A, R](timeName: String, fn: A ⇒ R) = (v1: A) ⇒ metrics.time(timeName) { fn(v1) }

    private[this] def makeTimeWrapper2[A, B, R](timeName: String, fn: (A, B) ⇒ R) = (v1: A, v2: B) ⇒ metrics.time(timeName) { fn(v1, v2) }

    private[this] def makeTimeWrapper3[A, B, C, R](timeName: String, fn: (A, B, C) ⇒ R) = (v1: A, v2: B, v3: C) ⇒ metrics.time(timeName) { fn(v1, v2, v3) }

    private[this] def makeTimeWrapper4[A, B, C, D, R](timeName: String, fn: (A, B, C, D) ⇒ R) = (v1: A, v2: B, v3: C, v4: D) ⇒ metrics.time(timeName) { fn(v1, v2, v3, v4) }

    private[this] def makeMarkWrapper2[A, B, R](markFn: ⇒ Unit, fn: (A, B) ⇒ R) = (v1: A, v2: B) ⇒ {
      val r = fn(v1, v2)
      markFn
      r
    }

    private[this] def makeMarkWrapper3[A, B, C, R](markFn: ⇒ Unit, fn: (A, B, C) ⇒ R) = (v1: A, v2: B, v3: C) ⇒ {
      val r = fn(v1, v2, v3)
      markFn
      r
    }

    private[this] def makeMarkWrapper4[A, B, C, D, R](markFn: ⇒ Unit, fn: (A, B, C, D) ⇒ R) = (v1: A, v2: B, v3: C, v4: D) ⇒ {
      val r = fn(v1, v2, v3, v4)
      markFn
      r
    }
  }

}
