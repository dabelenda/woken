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

package ch.chuv.lren.woken.core.features

import ch.chuv.lren.woken.messages.query.{ AlgorithmSpec, CodeValue, MiningQuery, UserId }
import org.scalatest.{ Matchers, WordSpec }
import ch.chuv.lren.woken.messages.query.filters._
import ch.chuv.lren.woken.messages.variables.VariableId

class QueriesTest extends WordSpec with Matchers {

  "QueryEnhanced" should {
    import Queries.QueryEnhanced

    val algorithm: AlgorithmSpec = AlgorithmSpec(
      code = "knn",
      parameters = List(CodeValue("k", "5"), CodeValue("n", "1"))
    )

    val user: UserId = UserId("test")

    // a < 10
    val rule = SingleFilterRule("a", "a", "number", InputType.number, Operator.less, List("10"))

    val query = MiningQuery(
      user = user,
      variables = List("target").map(VariableId),
      covariables = List("a", "b", "c").map(VariableId),
      grouping = List("grp1", "grp2").map(VariableId),
      filters = Some(rule),
      targetTable = None,
      datasets = Set(),
      algorithm = algorithm,
      executionPlan = None
    )

    "generate the SQL query" in {
      val featuresQuery = query.features("inputTable", excludeNullValues = false, None)
      featuresQuery.query shouldBe
      """SELECT "target","a","b","c","grp1","grp2" FROM inputTable WHERE "target" IS NOT NULL AND "a" < 10"""
    }

    "generate valid database field names" in {
      val badQuery = MiningQuery(
        user = user,
        variables = List("tarGet-var").map(VariableId),
        covariables = List("1a", "12-b", "c").map(VariableId),
        grouping = List("grp1", "grp2").map(VariableId),
        filters =
          Some(SingleFilterRule("a", "1a", "number", InputType.number, Operator.less, List("10"))),
        targetTable = None,
        datasets = Set(),
        algorithm = algorithm,
        executionPlan = None
      )

      val featuresQuery = badQuery.features("inputTable", excludeNullValues = false, None)
      featuresQuery.query shouldBe
      """SELECT "target_var","_1a","_12_b","c","grp1","grp2" FROM inputTable WHERE "target_var" IS NOT NULL AND "_1a" < 10"""
    }

    "include offset information in query" in {
      val featuresQuery = query.features("inputTable",
                                         excludeNullValues = false,
                                         Some(QueryOffset(start = 0, count = 20)))

      featuresQuery.query shouldBe
      """SELECT "target","a","b","c","grp1","grp2" FROM inputTable WHERE "target" IS NOT NULL AND "a" < 10
        |EXCEPT ALL (SELECT "target","a","b","c","grp1","grp2" FROM inputTable WHERE "target" IS NOT NULL AND "a" < 10 OFFSET 0 LIMIT 20)""".stripMargin
        .replace("\n", " ")
    }

  }
}
