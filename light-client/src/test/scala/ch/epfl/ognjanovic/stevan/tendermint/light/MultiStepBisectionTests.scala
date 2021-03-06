package ch.epfl.ognjanovic.stevan.tendermint.light

import ch.epfl.ognjanovic.stevan.tendermint.light.cases.MultiStepTestCase
import ch.epfl.ognjanovic.stevan.tendermint.rpc.Deserializer
import ch.epfl.ognjanovic.stevan.tendermint.rpc.circe.CirceDeserializer
import ch.epfl.ognjanovic.stevan.tendermint.verified.light.FetchedStackFactories.InMemoryFetchedStackFactory
import ch.epfl.ognjanovic.stevan.tendermint.verified.light.VerificationErrors._
import ch.epfl.ognjanovic.stevan.tendermint.verified.light.VotingPowerVerifiers
import org.scalatest.flatspec.AnyFlatSpec
import stainless.lang._

sealed class MultiStepBisectionTests extends AnyFlatSpec with VerifierTests {
  private val fetchedStackFactory = new InMemoryFetchedStackFactory()

  implicit private val testCaseDeserializer: Deserializer[MultiStepTestCase] =
    new CirceDeserializer(MultiStepTestCase.decoder)

  private val votingPowerVerifier = VotingPowerVerifiers.defaultVotingPowerVerifier

  "Happy path bisection" should "succeed" in {
    val (peerList, verificationTrace, timeValidatorConfig, heightToVerify) =
      buildTest(VerifierTests.testCase("/bisection/single-peer/happy_path.json"))

    val verifier =
      multiStepVerifierFactory.constructVerifier(peerList.primary, votingPowerVerifier, timeValidatorConfig)

    val result = verifier.verifyUntrusted(verificationTrace, fetchedStackFactory.emptyWithTarget(heightToVerify))

    assert(result.outcome.isLeft)
  }

  "Trusted state expired" should "fail verification" in {
    val (peerList, verificationTrace, timeValidatorConfig, heightToVerify) =
      buildTest(VerifierTests.testCase("/bisection/single-peer/header_out_of_trusting_period.json"))

    val verifier =
      multiStepVerifierFactory.constructVerifier(peerList.primary, votingPowerVerifier, timeValidatorConfig)

    val result = verifier.verifyUntrusted(verificationTrace, fetchedStackFactory.emptyWithTarget(heightToVerify))

    assert(result.outcome.isRight && result.outcome.get == ExpiredVerifiedState)
  }

  //  "Invalid validator set" should "fail verification" in {
  //    val (verifier, verificationTrace, heightToVerify) = MultiStepVerifierTests.deserializeMultiStepTestCase(
  //      "/bisection/single-peer/invalid_validator_set.json")
  //
  //    val result = verifier.verifyUntrusted(verificationTrace, fetchedStackFactory.emptyWithTarget(heightToVerify))
  //
  //    assert(result == ExpiredVerifiedState)
  //  }

  "Not enough commits" should "fail verification" in {
    val (peerList, verificationTrace, timeValidatorConfig, heightToVerify) =
      buildTest(VerifierTests.testCase("/bisection/single-peer/not_enough_commits.json"))

    val verifier =
      multiStepVerifierFactory.constructVerifier(peerList.primary, votingPowerVerifier, timeValidatorConfig)

    val result = verifier.verifyUntrusted(verificationTrace, fetchedStackFactory.emptyWithTarget(heightToVerify))

    assert(result.outcome.isRight && result.outcome.get == InsufficientCommitPower)
  }

  "Worst case scenario for bisection" should "not influence the successful outcome" in {
    val (peerList, verificationTrace, timeValidatorConfig, heightToVerify) =
      buildTest(VerifierTests.testCase("/bisection/single-peer/worst_case.json"))

    val verifier =
      multiStepVerifierFactory.constructVerifier(peerList.primary, votingPowerVerifier, timeValidatorConfig)
    buildTest(VerifierTests.testCase("/bisection/single-peer/worst_case.json"))

    val result = verifier.verifyUntrusted(verificationTrace, fetchedStackFactory.emptyWithTarget(heightToVerify))

    assert(result.outcome == Left[Unit, VerificationError](()))
  }
}
