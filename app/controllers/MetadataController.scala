/*
 * Copyright (C) 2016 Language Technology Group and Interactive Graphics Systems Group, Technische Universität Darmstadt, Germany
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package controllers

import javax.inject.Inject

import model.faceted.search.{ FacetedSearch, Facets, MetaDataBucket }
import model.Document
import play.api.libs.json.Json
import play.api.mvc.{ Action, Controller, Results }
import util.TimeRangeParser

/**
 * Created by f. zouhar on 26.05.16.
 */
class MetadataController @Inject extends Controller {

  private val defaultExcludeTypes = List("Subject", "Header", "ReferenceId", "References", "Keywords", "Entities", "Created", "EventTimes")
  private val defaultFetchSize = 50

  /**
   * Get all metadata types
   * @return list of metadata types
   */
  def getMetadataTypes = Action {
    Results.Ok(Json.toJson(Document.getMetadataKeysAndTypes().map(x => x._1).filter(!defaultExcludeTypes.contains(_)))).as("application/json")
  }

  /**
   * Gets document counts for all metadata types corresponding to their keys
   * @param fullText Full text search term
   * @param generic mapping of metadata key and a list of corresponding tags
   * @param entities list of entity ids to filter
   * @param timeRange string of a time range readable for [[TimeRangeParser]]
   * @return list of matching metadata keys and document count
   */
  def getMetadata(fullText: List[String], generic: Map[String, List[String]], entities: List[Long], timeRange: String) = Action {
    val times = TimeRangeParser.parseTimeRange(timeRange)
    val facets = Facets(fullText, generic, entities, times.from, times.to)
    val res = FacetedSearch.aggregateAll(facets, defaultFetchSize, defaultExcludeTypes)
      .map(agg => Json.obj(agg.key -> agg.buckets.map {
        case MetaDataBucket(key, count) => Json.obj("key" -> key, "count" -> count)
        case _ => Json.obj()
      }))
    Results.Ok(Json.toJson(res)).as("application/json")
  }

  /**
   * Gets document counts for one metadata types corresponding to their keys for given list of instances
   * @param fullText Full text search term
   * @param key metadata type key to aggregate on
   * @param generic mapping of metadata key and a list of corresponding tags
   * @param entities list of entity ids to filter
   * @param instances list of metadata instaces to buckets for
   * @param timeRange string of a time range readable for [[TimeRangeParser]]
   * @return list of matching metadata keys and document count
   */
  def getSpecificMetadata(
    fullText: List[String],
    key: String,
    generic: Map[String, List[String]],
    entities: List[Long],
    instances: List[String],
    timeRange: String
  ) = Action {
    val times = TimeRangeParser.parseTimeRange(timeRange)
    val facets = Facets(fullText, generic, entities, times.from, times.to)
    val agg = FacetedSearch.aggregate(facets, key, defaultFetchSize, instances)
    if (instances.isEmpty) {
      val res = Json.obj(key -> agg.buckets.map {
        case MetaDataBucket(metaKey, count) => Json.obj("key" -> metaKey, "count" -> count)
        case _ => Json.obj()
      })
      Results.Ok(Json.toJson(res)).as("application/json")
    } else {
      val res = instances.zip(instances.map(agg.buckets.map {
        case MetaDataBucket(metaKey, count) => metaKey -> count
        case _ => "" -> 0.0
      }.toMap)).map(x => Json.obj("key" -> x._1, "count" -> x._2.asInstanceOf[Number].longValue()))
      Results.Ok(Json.obj(key -> res)).as("application/json")
    }
  }

  /**
   * Gets document counts for keywords
   * @param fullText Full text search term
   * @param generic mapping of metadata key and a list of corresponding tags
   * @return list of matching keywords and document count
   */
  def getKeywords(fullText: List[String], generic: Map[String, List[String]], entities: List[Long], timeRange: String) = Action {
    val times = TimeRangeParser.parseTimeRange(timeRange)
    val facets = Facets(fullText, generic, entities, times.from, times.to)
    val res = FacetedSearch.aggregateKeywords(facets, defaultFetchSize, List()).buckets.map {
      case MetaDataBucket(key, count) => Json.obj("key" -> key, "count" -> count)
      case _ => Json.obj()
    }

    Results.Ok(Json.toJson(res)).as("application/json")
  }
}
