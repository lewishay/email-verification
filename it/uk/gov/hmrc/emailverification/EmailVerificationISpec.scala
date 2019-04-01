package uk.gov.hmrc.emailverification

import play.api.libs.json.Json
import org.scalatest.GivenWhenThen
import support.EmailStub._
import support.IntegrationBaseSpec

class EmailVerificationISpec extends IntegrationBaseSpec with GivenWhenThen {
  val emailToVerify = "example@domain.com"

  "email verification" should {
    "send the verification email to the specified address successfully" in new Setup {
      Given("The email service is running")
      stubSendEmailRequest(202)

      When("a client submits a verification request")

      val response = appClient("/verification-requests").post(verificationRequest(emailToVerify, templateId, continueUrl)).futureValue
      response.status shouldBe 201

      Then("an email is sent")
      verifyEmailSent(emailToVerify, continueUrl, templateId, paramsWithVerificationLink)
    }

    "only latest email verification request token for a given email should be valid" in new Setup {
      Given("The email service is running")
      stubSendEmailRequest(202)

      When("client submits a verification request")
      val response1 = appClient("/verification-requests").post(verificationRequest(emailToVerify, templateId, continueUrl)).futureValue
      response1.status shouldBe 201
      val token1 = decryptedToken(lastVerificationEMail)._1.get

      When("client submits a second verification request for same email")
      val response2 = appClient("/verification-requests").post(verificationRequest(emailToVerify, templateId, continueUrl)).futureValue
      response2.status shouldBe 201
      val token2 = decryptedToken(lastVerificationEMail)._1.get

      Then("only the last verification request token should be valid")
      appClient("/verified-email-addresses").post(Json.obj("token" -> token1)).futureValue.status shouldBe 400
      appClient("/verified-email-addresses").post(Json.obj("token" -> token2)).futureValue.status shouldBe 201
    }

    "second verification request should return successful 204 response" in new Setup {
      Given("The email service is running")
      stubSendEmailRequest(202)

      When("client submits a verification request")
      val response1 = appClient("/verification-requests").post(verificationRequest(emailToVerify, templateId, continueUrl)).futureValue
      response1.status shouldBe 201
      val token = decryptedToken(lastVerificationEMail)._1.get

      Then("the verification request with the token should be successful")
      appClient("/verified-email-addresses").post(Json.obj("token" -> token)).futureValue.status shouldBe 201
      Then("an additional verification requests with the token should be successful, but return with a 204 response")
      appClient("/verified-email-addresses").post(Json.obj("token" -> token)).futureValue.status shouldBe 204
    }

    "email verification for two different emails should be successful" in new Setup {
      def submitVerificationRequest(emailToVerify: String, templateId: String, continueUrl: String) = {
        val response = appClient("/verification-requests").post(verificationRequest(emailToVerify, templateId, continueUrl)).futureValue
        response.status shouldBe 201
        val token = decryptedToken(lastVerificationEMail)._1.get
        And("the client verifies the token")
        appClient("/verified-email-addresses").post(Json.obj("token" -> token)).futureValue.status shouldBe 201
        Then("the email should be verified")
        appClient(s"/verified-email-addresses/$emailToVerify").get().futureValue.status shouldBe 200
      }

      Given("The email service is running")
      stubSendEmailRequest(202)

      When("client submits first verification request ")
      submitVerificationRequest("example1@domain.com", templateId, continueUrl)

      When("client submits second verification request ")
      submitVerificationRequest("example2@domain.com", templateId, continueUrl)
    }

    "return 502 error if email sending fails" in new Setup {
      val body = "some-5xx-message"
      stubSendEmailRequest(500, body)
      val response = appClient("/verification-requests").post(verificationRequest()).futureValue
      response.status shouldBe 502
      response.body should include(body)
    }

    "return BAD_EMAIL_REQUEST error if email sending fails with 400" in new Setup {
      val body = "some-400-message"
      stubSendEmailRequest(400, body)
      val response = appClient("/verification-requests").post(verificationRequest()).futureValue
      response.status shouldBe 400
      response.body should include(body)

      (Json.parse(response.body) \ "code").as[String] shouldBe "BAD_EMAIL_REQUEST"
    }

    "return 500 error if email sending fails with 4xx" in new Setup {
      val body = "some-4xx-message"
      stubSendEmailRequest(404, body)
      val response = appClient("/verification-requests").post(verificationRequest()).futureValue
      response.status shouldBe 502
      response.body should include(body)
    }

    "return 409 if email is already verified" in new Setup {
      assumeEmailAlreadyVerified(emailToVerify)

      val response = appClient("/verification-requests").post(verificationRequest(emailToVerify)).futureValue
      response.status shouldBe 409
      response.body shouldBe
        Json.parse(
          """{
            |"code":"EMAIL_VERIFIED_ALREADY",
            |"message":"Email has already been verified"
            |}""".stripMargin).toString()
    }
  }

  def assumeEmailAlreadyVerified(email: String): Unit = {
    stubSendEmailRequest(202)
    appClient("/verification-requests").post(verificationRequest(email)).futureValue.status shouldBe 201
    val token = tokenFor(email)
    appClient("/verified-email-addresses").post(Json.obj("token" -> token)).futureValue.status shouldBe 201
  }

  trait Setup {
    val templateId = "my-lovely-template"
    val templateParams = Map("name" -> "Mr Joe Bloggs")
    val continueUrl = "http://some/url"

    val paramsJsonStr = Json.toJson(templateParams).toString()
    val expectedVerificationLink = "http://localhost:9890/verification?token=UG85NW1OcWdjR29xS29EM1pIQ1NqMlpzOEduemZCeUhvZVlLNUVtU2c3emp2TXZzRmFRSzlIdjJBTkFWVVFRUkg1M21MRUY4VE1TWDhOZ0hMNmQ0WHRQQy95NDZCditzNHd6ZUhpcEoyblNsT3F0bGJmNEw5RnhjOU0xNlQ3Y2o1dFdYVUE0NGFSUElURFRrSS9HRHhoTFZxdU9YRkw4OTZ4Z0tOTWMvQTJJd1ZqR3NJZ0pTNjRJNVRUc2RpcFZ1MjdOV1dhNUQ3OG9ITkVlSGJnaUJyUT09"
    val paramsWithVerificationLink = templateParams + ("verificationLink" -> expectedVerificationLink)
  }

}