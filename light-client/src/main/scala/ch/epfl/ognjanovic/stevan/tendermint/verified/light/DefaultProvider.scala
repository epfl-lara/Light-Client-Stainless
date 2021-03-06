package ch.epfl.ognjanovic.stevan.tendermint.verified.light

import ch.epfl.ognjanovic.stevan.tendermint.rpc.Requester
import ch.epfl.ognjanovic.stevan.tendermint.verified.light.LightBlockProviders.LightBlockProvider
import ch.epfl.ognjanovic.stevan.tendermint.verified.types.{Height, LightBlock, PeerId}

/**
 * Only does the fetching and of the necessary data using a requester of a specified peer. Doesn't validate hashes, etc.
 */
sealed class DefaultProvider(override val chainId: String, private val requester: Requester)
    extends LightBlockProvider {

  /**
   * For a given height gives back the `LightBlock` of that height.
   *
   * @param height of the block, or 0 for the latest block
   * @return block for the specified height
   */
  override def lightBlock(height: Height): LightBlock = {
    val optionalHeight = Some(height)
    val signedHeader = requester.signedHeader(optionalHeight)
    val validatorSet = requester.validatorSet(optionalHeight)
    val nextValidatorSet = requester.validatorSet(Some(height + 1))
    LightBlock(signedHeader.header, signedHeader.commit, validatorSet, nextValidatorSet, requester.peerId)
  }

  override def currentHeight: Height = requester.signedHeader(None).header.height

  override def latestLightBlock: LightBlock = {
    val signedHeader = requester.signedHeader(None)
    val validatorSet = requester.validatorSet(Some(signedHeader.header.height))
    val nextValidatorSet = requester.validatorSet(Some(signedHeader.header.height + 1))
    LightBlock(signedHeader.header, signedHeader.commit, validatorSet, nextValidatorSet, requester.peerId)
  }

  override def peerId: PeerId = requester.peerId
}
