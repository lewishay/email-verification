package uk.gov.hmrc

import _root_.play.api.libs.json.Json
import org.scalatest.GivenWhenThen
import support.EmailStub._
import support.IntegrationBaseSpec
import uk.gov.hmrc.emailverification.repositories.VerifiedEmail
import uk.gov.hmrc.play.http.HeaderCarrier

class TokenValidationISpec extends IntegrationBaseSpec with GivenWhenThen {
  implicit val hc = HeaderCarrier()

  "validating the token" should {
    "return 201 if the token is valid and 204 if email is already verified" in {
      Given("a verification request exists")
      val token = tokenFor("user@email.com")

      When("a token verification request is submitted")
      val response = appClient("/verified-email-addresses").post(Json.obj("token" -> token)).futureValue

      Then("Service responds with Created")
      response.status shouldBe 201

      When("the same token is submitted again")
      val response2 = appClient("/verified-email-addresses").post(Json.obj("token" -> token)).futureValue

      Then("Service responds with NoContent")
      response2.status shouldBe 204
    }

    "return 400 if the token is invalid or expired" in {
      When("an invalid token verification request is submitted")
      val response = appClient("/verified-email-addresses").post(Json.obj("token" -> "invalid")).futureValue
      response.status shouldBe 400
      response.body shouldBe "Token not found or expired"
    }
  }

  "getting verified email" should {

    "return 404 if email does not exist" in {
      Given("an unverified email does not  exist")
      val email = "user@email.com"
      stubSendEmailRequest(202)
      await(appClient("/verification-requests").post(verificationRequest(emailToVerify = email)))

      When("the email is checked if it is verified")
      val response = appClient(s"/verified-email-addresses/$email").get().futureValue
      Then("404 is returned")
      response.status shouldBe 404
    }
  }

  // We have sperated this in two should block because combining them was failing. Couldn't find a cause.
  it should {
    "return verified email if it exist" in {
      Given("a verified email already exist")
      val email = "user@email.com"
      val token = tokenFor(email)
      appClient("/verified-email-addresses").post(Json.obj("token" -> token)).futureValue

      When("an email is checked if it is verified")
      val response = appClient(s"/verified-email-addresses/$email").get().futureValue

      Then("email resource is returned")
      response.status shouldBe 200
      VerifiedEmail.format.reads(response.json).get shouldBe VerifiedEmail(email)
    }
  }
}