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

package ch.chuv.lren.woken.service

import akka.NotUsed
import akka.stream.OverflowStrategy
import akka.stream.scaladsl.{ Flow, Source }
import ch.chuv.lren.woken.fp.Traverse
import ch.chuv.lren.woken.messages.query.{ ExperimentQuery, MiningQuery, QueryResult }
import ch.chuv.lren.woken.cromwell.core.ConfigUtil.Validation
import cats.implicits.catsStdInstancesForOption
import com.typesafe.scalalogging.Logger
import ch.chuv.lren.woken.backends.woken.WokenService
import ch.chuv.lren.woken.messages.datasets.{ Dataset, DatasetId }
import ch.chuv.lren.woken.messages.remoting.RemoteLocation

class DispatcherService(datasets: Map[DatasetId, Dataset], wokenService: WokenService) {

  def dispatchTo(dataset: DatasetId): Option[RemoteLocation] =
    if (datasets.isEmpty)
      None
    else
      datasets.get(dataset).flatMap(_.location)

  def dispatchTo(datasets: Set[DatasetId]): (Set[RemoteLocation], Boolean) = {
    val maybeLocations = datasets.map(dispatchTo)
    val local          = maybeLocations.isEmpty || maybeLocations.contains(None)
    val maybeSet       = Traverse.sequence(maybeLocations.filter(_.nonEmpty))

    (maybeSet.getOrElse(Set.empty), local)
  }

  def remoteDispatchMiningFlow(): Flow[MiningQuery, (RemoteLocation, QueryResult), NotUsed] =
    Flow[MiningQuery]
      .map(q => dispatchTo(q.datasets)._1.map(ds => ds -> q))
      .mapConcat(identity)
      .buffer(100, OverflowStrategy.backpressure)
      .map { case (l, q) => l.copy(url = l.url.withPath(l.url.path / "mining" / "job")) -> q }
      .via(wokenService.queryFlow)

  def remoteDispatchExperimentFlow()
    : Flow[ExperimentQuery, (RemoteLocation, QueryResult), NotUsed] =
    Flow[ExperimentQuery]
      .map(q => dispatchTo(q.trainingDatasets)._1.map(ds => ds -> q))
      .mapConcat(identity)
      .buffer(100, OverflowStrategy.backpressure)
      .map {
        case (l, q) => l.copy(url = l.url.withPath(l.url.path / "mining" / "experiment")) -> q
      }
      .via(wokenService.queryFlow)

  def localDispatchFlow(datasets: Set[DatasetId]): Source[QueryResult, NotUsed] = ???

}

object DispatcherService {

  private val logger = Logger("DispatcherService")

  private[service] def loadDatasets(
      datasets: Validation[Map[DatasetId, Dataset]]
  ): Map[DatasetId, Dataset] =
    datasets.fold({ e =>
      logger.info(s"No datasets configured: $e")
      Map[DatasetId, Dataset]()
    }, identity)

  def apply(datasets: Validation[Map[DatasetId, Dataset]],
            wokenService: WokenService): DispatcherService =
    new DispatcherService(loadDatasets(datasets), wokenService)

}
