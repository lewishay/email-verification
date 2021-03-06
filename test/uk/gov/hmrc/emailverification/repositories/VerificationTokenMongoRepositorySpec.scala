/*
 * Copyright 2020 HM Revenue & Customs
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

package uk.gov.hmrc.emailverification.repositories

import org.joda.time.{DateTime, DateTimeZone, Period}
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import play.modules.reactivemongo.ReactiveMongoComponent
import reactivemongo.api.indexes.Index
import reactivemongo.api.indexes.IndexType.Ascending
import reactivemongo.bson.BSONDocument
import uk.gov.hmrc.emailverification.models.VerificationDoc
import uk.gov.hmrc.gg.test.UnitSpec
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.mongo.{MongoConnector, MongoSpecSupport}
import scala.concurrent.ExecutionContext.Implicits.global

class VerificationTokenMongoRepositorySpec extends UnitSpec with BeforeAndAfterEach with BeforeAndAfterAll with MongoSpecSupport {

  def now: DateTime = DateTime.now(DateTimeZone.UTC)

  val repo: VerificationTokenMongoRepository = new VerificationTokenMongoRepository(new ReactiveMongoComponent {
    override def mongoConnector: MongoConnector = mongoConnectorForTest
  })

  val token = "theToken"
  val email = "user@email.com"
  implicit val hc: HeaderCarrier = HeaderCarrier()

  "upsert" should {
    "always update the existing document for a given email address" in {
      await(repo.findAll()) shouldBe empty

      val token1 = token + "1"

      await(repo.upsert(token1, email, Period.minutes(10)))
      await(repo.findAll()) === Seq(VerificationDoc(email, token1, now.plusMinutes(10)))

      val token2 = token + "2"

      await(repo.upsert(token2, email, Period.minutes(10)))
      await(repo.findAll()) === Seq(VerificationDoc(email, token2, now.plusMinutes(10)))
    }
  }

  "find" should {
    "return the verification document" in {
      await(repo.upsert(token, email, Period.minutes(10)))

      await(repo.findToken(token)) === Some(VerificationDoc(email, token, now.plusMinutes(10)))
    }

    "return None whet token does not exist or has expired" in {
      await(repo.findToken(token)) === None
    }
  }

  "ensureIndexes" should {
    "create ttl on updatedAt field" in {
      await(repo.ensureIndexes)
      val indexes = await(mongo().indexesManager.onCollection("verificationToken").list())

      val index = indexes.find(_.name.contains("expireAtIndex")).get

      //version of index is managed by mongodb. We don't want to assert on it.
      index shouldBe Index(Seq("expireAt" -> Ascending), name = Some("expireAtIndex"), options = BSONDocument("expireAfterSeconds" -> 0)).copy(version = index.version)
    }
  }

  override def beforeEach() {
    super.beforeEach()
    await(repo.drop)
    ()
  }

  override protected def afterAll() {
    await(repo.drop)
    super.afterAll()
  }
}
