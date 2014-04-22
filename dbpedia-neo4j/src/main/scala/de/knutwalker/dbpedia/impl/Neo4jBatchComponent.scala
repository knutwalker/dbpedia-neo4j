package de.knutwalker.dbpedia.impl

import com.carrotsearch.hppc.{ ObjectLongMap, ObjectLongOpenHashMap }
import de.knutwalker.dbpedia.importer.{ MetricsComponent, SettingsComponent, GraphComponent }
import java.util
import org.neo4j.graphdb.{ DynamicRelationshipType, DynamicLabel, Label, RelationshipType }
import org.neo4j.helpers.collection.MapUtil
import org.neo4j.unsafe.batchinsert.BatchInserters
import scala.collection.mutable
import scala.collection.mutable.ListBuffer

trait Neo4jBatchComponent extends GraphComponent {
  this: SettingsComponent with MetricsComponent ⇒

  type NodeType = Long
  type RelType = RelationshipType

  val graph: Graph = new Neo4jBatchGraph

  private final class Neo4jBatchGraph extends Graph {

    private val megs: Double = 1000 * 1000

    private def mem(n: Int) = f"${n / megs}%.0fM"

    private def inserterConfig = {
      val res = settings.approximatedResources

      // TODO: allow for fine grained settings
      val relsPerNode = 3
      val propsPerNode = 4

      // as per http://docs.neo4j.org/chunked/stable/configuration-caches.html
      val bytesPerNode = 14
      val bytesPerRel = 33
      val bytesPerProp = 42
      val bytesPerStringProp = 128 // might be totally off

      val nodes = res
      val relationships = nodes * relsPerNode
      val properties = nodes * propsPerNode
      val stringProperties = properties

      val nodesMem = mem(nodes * bytesPerNode)
      val relsMem = mem(relationships * bytesPerRel)
      val propsMem = mem(properties * bytesPerProp)
      val stringPropsMem = mem(stringProperties * bytesPerStringProp)

      MapUtil.stringMap(
        // TODO: make cache_type configurable
        "cache_type", "none",
        "use_memory_mapped_buffers", "true",
        "neostore.nodestore.db.mapped_memory", nodesMem,
        "neostore.relationshipstore.db.mapped_memory", relsMem,
        "neostore.propertystore.db.mapped_memory", propsMem,
        "neostore.propertystore.db.strings.mapped_memory", stringPropsMem,
        "neostore.propertystore.db.arrays.mapped_memory", "0M",
        "neostore.propertystore.db.index.keys.mapped_memory", "5M",
        "neostore.propertystore.db.index.mapped_memory", "5M")
    }

    private val inserter = {
      val config = inserterConfig
      BatchInserters.inserter(settings.graphDbDir, config)
    }

    if (settings.createDeferredIndices) {
      inserter.createDeferredSchemaIndex(DynamicLabel.label(Labels.resource)).on(Properties.uri).create()
      inserter.createDeferredSchemaIndex(DynamicLabel.label(Labels.literal)).on(Properties.value).create()
    }

    private val labels = new mutable.AnyRefMap[String, Label](32)

    private val resources: ObjectLongMap[String] = ObjectLongOpenHashMap.newInstanceWithExpectedSize(settings.approximatedResources)
    private val bnodes: ObjectLongMap[String] = ObjectLongOpenHashMap.newInstanceWithExpectedSize(settings.txSize)

    private def getLabel(label: String): Label = {
      labels.getOrElseUpdate(label, DynamicLabel.label(label))
    }

    private def makeLabels(dynamicLabels: List[String]): List[Label] = dynamicLabels.map(getLabel)

    private def get(cache: ObjectLongMap[String], key: String): Option[Long] = {
      val n = cache.getOrDefault(key, -1)
      if (n == -1) None else Some(n)
    }

    private def set(cache: ObjectLongMap[String], key: String, properties: util.Map[String, AnyRef], labels: List[Label]): NodeType = {
      val n = inserter.createNode(properties, labels: _*)
      cache.put(key, n)
      n
    }

    private def props(k: String, v: AnyRef): java.util.Map[String, AnyRef] = {
      val p = new java.util.HashMap[String, AnyRef](1)
      //      if (v ne null)
      p.put(k, v)
      p
    }

    private def props(k1: String, v1: AnyRef, k2: String, v2: String): java.util.Map[String, AnyRef] = {
      val p = new java.util.HashMap[String, AnyRef](2)
      //      if (v1 ne null)
      p.put(k1, v1)
      //      if (v2 ne null)
      p.put(k2, v2)
      p
    }

    protected def getBNode(subject: String) = get(bnodes, subject)

    protected def createBNode(subject: String, labels: List[String]) = {
      set(bnodes, subject, null, makeLabels(labels))
    }

    protected def getResourceNode(uri: String) = get(resources, uri)

    protected def createResourceNode(uri: String, value: Option[String], labels: List[String]) = {
      val ls = makeLabels(labels)
      val p = value.fold(props(Properties.uri, uri)) { v ⇒
        props(Properties.uri, uri, Properties.value, v)
      }
      set(resources, uri, p, ls)
    }

    def updateResourceNode(id: Long, value: Option[String], labels: List[String]) = {

      val valuesUpdated = maybeUpdateValue(id, value)
      val labelsUpdated = maybeUpdateLabels(id, labels)

      if (valuesUpdated || labelsUpdated) {

        metrics.nodeUpdated()
      }

      id
    }

    private def maybeUpdateValue(id: Long, value: Option[String]) = {

      value.fold(false) { v ⇒
        val propsBefore = inserter.getNodeProperties(id)
        if (v != propsBefore.get(Properties.value)) {
          propsBefore.put(Properties.value, v)
          inserter.setNodeProperties(id, propsBefore)

          true
        }
        else false
      }
    }

    private def maybeUpdateLabels(id: Long, labels: List[String]) = {

      val newLabels = makeLabels(labels)
      val oldLabels = inserter.getNodeLabels(id)

      mergeLabels(oldLabels, newLabels).fold(false) { finalLabels ⇒
        inserter.setNodeLabels(id, finalLabels: _*)
        true
      }
    }

    private def mergeLabels(jLabels: java.lang.Iterable[Label], newLabels: List[Label]): Option[List[Label]] = {
      var oldLabels = Set.empty[Label]
      val builder = new ListBuffer[Label]
      val iter = jLabels.iterator()

      var hasNewLabels = false

      while (iter.hasNext) {
        val label = iter.next()
        builder += label
        oldLabels += label
      }

      newLabels foreach { label ⇒
        if (!oldLabels(label)) {
          builder += label
          hasNewLabels = true
        }
      }

      if (hasNewLabels) Some(builder.result())
      else None
    }

    def createLiteralNode(literal: String, labels: List[String]) = {
      val p = props(Properties.value, literal)
      val ls = makeLabels(Labels.literal :: labels)
      val n = inserter.createNode(p, ls: _*)
      n
    }

    def createRelationship(src: Long, dest: Long, rel: String, relType: RelationshipType): Unit = {
      val p = props(Properties.uri, rel)
      inserter.createRelationship(src, dest, relType, p)
    }

    def createRelTypeFor(name: String) = DynamicRelationshipType.withName(name)

    def subjectAdded() = ()

    def shutdown() = {
      inserter.shutdown()
    }
  }

}
