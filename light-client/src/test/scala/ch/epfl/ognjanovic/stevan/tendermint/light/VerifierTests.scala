package ch.epfl.ognjanovic.stevan.tendermint.light

import java.util.concurrent.TimeUnit

import ch.epfl.ognjanovic.stevan.tendermint.light.cases.{MultiStepTestCase, SingleStepTestCase}
import ch.epfl.ognjanovic.stevan.tendermint.rpc.Deserializer
import ch.epfl.ognjanovic.stevan.tendermint.verified.fork.PeerList
import ch.epfl.ognjanovic.stevan.tendermint.verified.light.LightBlockProviders.LightBlockProvider
import ch.epfl.ognjanovic.stevan.tendermint.verified.light.MultiStepVerifierFactories.DefaultMultiStepVerifierFactory
import ch.epfl.ognjanovic.stevan.tendermint.verified.light.NextHeightCalculators.BisectionHeightCalculator
import ch.epfl.ognjanovic.stevan.tendermint.verified.light.TimeValidatorFactories._
import ch.epfl.ognjanovic.stevan.tendermint.verified.light.VerificationTraces._
import ch.epfl.ognjanovic.stevan.tendermint.verified.light.Verifier
import ch.epfl.ognjanovic.stevan.tendermint.verified.light.VerifierFactories._
import ch.epfl.ognjanovic.stevan.tendermint.verified.light.VotingPowerVerifiers.{
  ParameterizedVotingPowerVerifier,
  VotingPowerVerifier
}
import ch.epfl.ognjanovic.stevan.tendermint.verified.types.{Height, LightBlock, PeerId}

import scala.concurrent.duration.Duration
import scala.io.Source

trait VerifierTests {
  def verifierFactory: VerifierFactory = new DefaultVerifierFactory(DefaultTimeValidatorFactory)

  def multiStepVerifierFactory = new DefaultMultiStepVerifierFactory(
    new DefaultVerifierFactory(DefaultTimeValidatorFactory),
    BisectionHeightCalculator)

  def buildTest(
    singleStepTestCase: SingleStepTestCase,
    votingPowerVerifier: VotingPowerVerifier): (Verifier, VerificationTrace, LightBlockProvider) = {
    val timeValidatorConfig = InstantTimeValidatorConfig(
      () ⇒ singleStepTestCase.initial.now,
      Duration.fromNanos(singleStepTestCase.initial.trusting_period),
      Duration.apply(1, TimeUnit.MICROSECONDS))

    val peerId = PeerId(singleStepTestCase.initial.next_validator_set.values.head.publicKey)

    val verificationTrace = StartingVerificationTrace(
      LightBlock(
        singleStepTestCase.initial.signed_header.header,
        singleStepTestCase.initial.signed_header.commit,
        singleStepTestCase.initial.next_validator_set,
        singleStepTestCase.initial.next_validator_set,
        peerId
      ),
      votingPowerVerifier
    )

    (
      verifierFactory.constructInstance(votingPowerVerifier, timeValidatorConfig),
      verificationTrace,
      InMemoryProvider.fromInput(
        singleStepTestCase.initial.signed_header.header.chainId,
        peerId,
        singleStepTestCase.input)
    )
  }

  def buildTest(multiStepTestCase: MultiStepTestCase)
    : (PeerList[PeerId, LightBlockProvider], StartingVerificationTrace, TimeValidatorConfig, Height) = {
    val trustVerifier = ParameterizedVotingPowerVerifier(multiStepTestCase.trust_options.trustLevel)

    val peerId = PeerId(multiStepTestCase.primary.lite_blocks(0).validator_set.values.head.publicKey)
    val witnessIds =
      multiStepTestCase.primary.lite_blocks(0).validator_set.values.tail.map(vals ⇒ PeerId(vals.publicKey)).toScala

    val primary =
      InMemoryProvider.fromInput(multiStepTestCase.primary.chain_id, peerId, multiStepTestCase.primary.lite_blocks)

    val witnesses: Map[PeerId, LightBlockProvider] =
      witnessIds
        .zip(multiStepTestCase.witnesses.getOrElse(Array.empty))
        .map(pair ⇒ (pair._1, InMemoryProvider.fromInput(pair._2.value.chain_id, pair._1, pair._2.value.lite_blocks)))
        .toMap

    (
      PeerList.fromScala(witnesses.updated(peerId, primary), peerId, witnesses.keys.toList, List.empty, List.empty),
      StartingVerificationTrace(primary.lightBlock(multiStepTestCase.trust_options.trustedHeight), trustVerifier),
      InstantTimeValidatorConfig(
        () ⇒ multiStepTestCase.now,
        Duration.fromNanos(multiStepTestCase.trust_options.trustPeriod.toNanoseconds.toLong),
        Duration.apply(1, TimeUnit.MICROSECONDS)),
      Height(multiStepTestCase.height_to_verify)
    )
  }

}

object VerifierTests {

  private def content(path: String): String = {
    val source = Source.fromURL(getClass.getResource(path))
    try source.mkString
    finally source.close()
  }

  def testCase[T](path: String)(implicit deserializer: Deserializer[T]): T = deserializer(VerifierTests.content(path))

}
