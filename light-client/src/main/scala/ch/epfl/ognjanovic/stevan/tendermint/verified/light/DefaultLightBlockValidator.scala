package ch.epfl.ognjanovic.stevan.tendermint.verified.light

import ch.epfl.ognjanovic.stevan.tendermint.hashing.Hashers.Hasher
import ch.epfl.ognjanovic.stevan.tendermint.verified.light.CommitValidators.CommitValidator
import ch.epfl.ognjanovic.stevan.tendermint.verified.light.LightBlockValidators.LightBlockValidator
import ch.epfl.ognjanovic.stevan.tendermint.verified.light.VerificationErrors._
import ch.epfl.ognjanovic.stevan.tendermint.verified.types.LightBlock
import stainless.lang.{Either, Right}

case class DefaultLightBlockValidator(
  timeValidator: TimeValidator,
  commitValidator: CommitValidator,
  headerHasher: Hasher)
    extends LightBlockValidator {

  override def validateUntrustedBlock(
    trustedLightBlock: LightBlock,
    untrustedLightBlock: LightBlock): Either[Unit, VerificationErrors.VerificationError] = {
    if (timeValidator.isExpired(trustedLightBlock))
      Right(ExpiredVerifiedState)
    else if (timeValidator.fromFuture(untrustedLightBlock))
      Right(HeaderFromFuture)
    else if (trustedLightBlock.header.chainId != untrustedLightBlock.header.chainId)
      Right(InvalidHeader)
    else if (untrustedLightBlock.header.validators != headerHasher.hashValidatorSet(untrustedLightBlock.validatorSet))
      Right(InvalidValidatorSetHash)
    else if (
      untrustedLightBlock.header.nextValidators != headerHasher.hashValidatorSet(untrustedLightBlock.nextValidatorSet)
    )
      Right(InvalidNextValidatorSetHash)
    else if (untrustedLightBlock.commit.blockId.bytes != headerHasher.hashHeader(untrustedLightBlock.header))
      Right(InvalidCommitValue)
    else if (untrustedLightBlock.header.time <= trustedLightBlock.header.time)
      Right(NonMonotonicBftTime)
    else
      commitValidator.validateCommit(untrustedLightBlock)
  }

}
