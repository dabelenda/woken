/*
 * Copyright (C) 2017  LREN CHUV for Human Brain Project
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package ch.chuv.lren.woken.core.model

import java.time.OffsetDateTime
import java.util.Base64

import ch.chuv.lren.woken.core.model.Shapes.{ pfa => pfaShape, _ }
import ch.chuv.lren.woken.messages.query.{ AlgorithmSpec, QueryResult, queryProtocol }
import spray.json._

/**
  * Result produced during the execution of an algorithm
  */
sealed trait JobResult extends Product with Serializable {

  /** Id of the job */
  def jobId: String

  /** Node where the algorithm is executed */
  def node: String

  /** Date of execution */
  def timestamp: OffsetDateTime

  /** Name of the algorithm */
  def algorithm: String

  /** Shape of the results (mime type) */
  def shape: Shape

}

/**
  * A PFA result (Portable Format for Analytics http://dmg.org/pfa/)
  *
  * @param jobId Id of the job
  * @param node Node where the algorithm is executed
  * @param timestamp Date of execution
  * @param algorithm Name of the algorithm
  * @param model PFA model
  */
case class PfaJobResult(jobId: String,
                        node: String,
                        timestamp: OffsetDateTime,
                        algorithm: String,
                        model: JsObject)
    extends JobResult {

  def shape: Shape = pfaShape

  def injectCell(name: String, value: JsValue): PfaJobResult = {
    val cells        = model.fields.getOrElse("cells", JsObject()).asJsObject
    val updatedCells = JsObject(cells.fields + (name -> value))
    val updatedModel = JsObject(model.fields + ("cells" -> updatedCells))

    copy(model = updatedModel)
  }

}

/**
  * Result of an experiment, i.e. the execution of multiple algorithms over the same datasets
  *
  * @param jobId Id of the job
  * @param node Node where the algorithm is executed
  * @param timestamp Date of execution
  * @param models List of models produced by the experiment
  */
case class PfaExperimentJobResult(jobId: String,
                                  node: String,
                                  timestamp: OffsetDateTime,
                                  models: JsArray)
    extends JobResult {

  def shape: Shape = pfaExperiment

  override val algorithm = "experiment"
}

object PfaExperimentJobResult {

  def apply(results: Map[AlgorithmSpec, JobResult],
            experimentJobId: String,
            experimentNode: String): PfaExperimentJobResult = {

    implicit val offsetDateTimeJsonFormat: RootJsonFormat[OffsetDateTime] =
      queryProtocol.OffsetDateTimeJsonFormat

    // Concatenate results while respecting order of algorithms
    val output = JsArray(
      results
        .map(r => {
          val code = r._1.code
          r._2 match {
            case PfaJobResult(jobId, node, timestamp, algorithm, model) =>
              // TODO: inform if algorithm is predictive...
              JsObject(
                "type"      -> JsString(pfaShape.mime),
                "algorithm" -> JsString(algorithm),
                "code"      -> JsString(code),
                "jobId"     -> JsString(jobId),
                "node"      -> JsString(node),
                "timestamp" -> timestamp.toJson,
                "data"      -> model
              )
            case ErrorJobResult(jobId, node, timestamp, algorithm, errorMsg) =>
              JsObject(
                "type"      -> JsString(error.mime),
                "algorithm" -> JsString(algorithm),
                "code"      -> JsString(code),
                "jobId"     -> JsString(jobId),
                "node"      -> JsString(node),
                "timestamp" -> timestamp.toJson,
                "error"     -> JsString(errorMsg)
              )
            case JsonDataJobResult(jobId, node, timestamp, shape, algorithm, data) =>
              JsObject(
                "type"      -> JsString(shape.mime),
                "algorithm" -> JsString(algorithm),
                "code"      -> JsString(code),
                "jobId"     -> JsString(jobId),
                "node"      -> JsString(node),
                "timestamp" -> timestamp.toJson,
                "data"      -> data
              )
            case OtherDataJobResult(jobId, node, timestamp, shape, algorithm, data) =>
              JsObject(
                "type"      -> JsString(shape.mime),
                "algorithm" -> JsString(algorithm),
                "code"      -> JsString(code),
                "jobId"     -> JsString(jobId),
                "node"      -> JsString(node),
                "timestamp" -> timestamp.toJson,
                "data"      -> JsString(data)
              )
            case SerializedModelJobResult(jobId, node, timestamp, shape, algorithm, data) =>
              JsObject(
                "type"      -> JsString(shape.mime),
                "algorithm" -> JsString(algorithm),
                "code"      -> JsString(code),
                "jobId"     -> JsString(jobId),
                "node"      -> JsString(node),
                "timestamp" -> timestamp.toJson,
                "data"      -> JsString(Base64.getEncoder.encodeToString(data))
              )
            case PfaExperimentJobResult(jobId, node, timestamp, models) =>
              JsObject(
                "type"      -> JsString(Shapes.pfaExperiment.mime),
                "code"      -> JsString("experiment"),
                "jobId"     -> JsString(jobId),
                "node"      -> JsString(node),
                "timestamp" -> timestamp.toJson,
                "models"    -> models
              )
          }
        })
        .toVector
    )

    PfaExperimentJobResult(
      jobId = experimentJobId,
      node = experimentNode,
      timestamp = OffsetDateTime.now(),
      models = output
    )
  }
}

/**
  * Result of a failed job
  *
  * @param jobId Id of the job
  * @param node Node where the algorithm is executed
  * @param timestamp Date of execution
  * @param algorithm Name of the algorithm
  * @param error Error to report
  */
case class ErrorJobResult(jobId: String,
                          node: String,
                          timestamp: OffsetDateTime,
                          algorithm: String,
                          error: String)
    extends JobResult {

  def shape: Shape = Shapes.error

}

/**
  * Result producing a visualisation or a table or any other output to display to the user
  */
sealed trait VisualisationJobResult extends JobResult

/**
  * A visualisation result encoded in Json
  *
  * @param jobId Id of the job
  * @param node Node where the algorithm is executed
  * @param timestamp Date of execution
  * @param shape Shape of the data (mime type)
  * @param algorithm Name of the algorithm
  * @param data Json encoded data result
  */
case class JsonDataJobResult(jobId: String,
                             node: String,
                             timestamp: OffsetDateTime,
                             shape: Shape,
                             algorithm: String,
                             data: JsValue)
    extends VisualisationJobResult {

  assert(Shapes.visualisationJsonResults.contains(shape))

}

/**
  * A visualisation or other type of user-facing information encoded as a string
  *
  * @param jobId Id of the job
  * @param node Node where the algorithm is executed
  * @param timestamp Date of execution
  * @param shape Shape of the data (mime type)
  * @param algorithm Name of the algorithm
  * @param data Data result as a string
  */
case class OtherDataJobResult(jobId: String,
                              node: String,
                              timestamp: OffsetDateTime,
                              shape: Shape,
                              algorithm: String,
                              data: String)
    extends VisualisationJobResult {

  assert(Shapes.visualisationOtherResults.contains(shape))

}

/**
  * A model serialized in binary
  *
  * @param jobId Id of the job
  * @param node Node where the algorithm is executed
  * @param timestamp Date of execution
  * @param shape Shape of the data (mime type)
  * @param algorithm Name of the algorithm
  * @param data Binary for the serialized model
  */
case class SerializedModelJobResult(jobId: String,
                                    node: String,
                                    timestamp: OffsetDateTime,
                                    shape: Shape,
                                    algorithm: String,
                                    data: Array[Byte])
    extends VisualisationJobResult {

  assert(Shapes.serializedModelsResults.contains(shape))

}

object JobResult {

  def asQueryResult(jobResult: JobResult): QueryResult =
    jobResult match {
      case pfa: PfaJobResult =>
        QueryResult(
          jobId = pfa.jobId,
          node = pfa.node,
          timestamp = pfa.timestamp,
          shape = pfaShape.mime,
          algorithm = pfa.algorithm,
          data = Some(pfa.model),
          error = None
        )
      case pfa: PfaExperimentJobResult =>
        QueryResult(
          jobId = pfa.jobId,
          node = pfa.node,
          timestamp = pfa.timestamp,
          shape = pfaExperiment.mime,
          algorithm = pfa.algorithm,
          data = Some(pfa.models),
          error = None
        )
      case v: JsonDataJobResult =>
        QueryResult(
          jobId = v.jobId,
          node = v.node,
          timestamp = v.timestamp,
          shape = v.shape.mime,
          algorithm = v.algorithm,
          data = Some(v.data),
          error = None
        )
      case v: OtherDataJobResult =>
        QueryResult(
          jobId = v.jobId,
          node = v.node,
          timestamp = v.timestamp,
          shape = v.shape.mime,
          algorithm = v.algorithm,
          data = Some(JsString(v.data)),
          error = None
        )
      case v: SerializedModelJobResult =>
        QueryResult(
          jobId = v.jobId,
          node = v.node,
          timestamp = v.timestamp,
          shape = v.shape.mime,
          algorithm = v.algorithm,
          data = Some(JsString(Base64.getEncoder.encodeToString(v.data))),
          error = None
        )
      case e: ErrorJobResult =>
        QueryResult(
          jobId = e.jobId,
          node = e.node,
          timestamp = e.timestamp,
          shape = error.mime,
          algorithm = e.algorithm,
          data = None,
          error = Some(e.error)
        )
    }

  implicit class ToQueryResult(val jobResult: JobResult) extends AnyVal {
    def asQueryResult: QueryResult = JobResult.asQueryResult(jobResult)
  }

}
