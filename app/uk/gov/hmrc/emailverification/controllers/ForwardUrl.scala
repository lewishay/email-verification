/*
 * Copyright 2017 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.emailverification.controllers

import java.net.URI

import config.AppConfig.whitelistedDomains
import play.api.libs.json._

import scala.util.Try

case class ForwardUrl(url: String)

object ForwardUrl {

  implicit val reads: Reads[ForwardUrl] = new Reads[ForwardUrl]() {
    override def reads(json: JsValue): JsResult[ForwardUrl] = {
      val url = json.as[String]
      validate(url) match {
        case Left(message) => JsError(error = message)
        case Right(forwardUrl) => JsSuccess(forwardUrl)
      }
    }
  }

  private def validate(potentialUrl: String): Either[String, ForwardUrl] = {
    def validateDomain(uri: URI): Either[String, ForwardUrl] = {
      if (whitelistedDomains.isEmpty || whitelistedDomains.contains(uri.getDomain))
        Right(ForwardUrl(uri.toString))
      else
        Left("URL is not whitelisted")
    }

    Try(new URI(potentialUrl)).map {
      case uri if uri.hasHost => validateDomain(uri)
      case uri => Right(ForwardUrl(uri.toString))
    }.getOrElse(Left("URL could not be parsed"))
  }

  private implicit class URIOps(uri: URI) {
    def getDomain: String = {
      val port = uri.getPort match {
        case 80 | -1 => ""
        case _ => s":${uri.getPort}"
      }
      s"${uri.getHost}$port"
    }

    def hasHost = {
      uri.getHost != null
    }
  }
}
