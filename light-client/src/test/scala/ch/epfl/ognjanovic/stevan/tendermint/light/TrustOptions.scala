package ch.epfl.ognjanovic.stevan.tendermint.light

import ch.epfl.ognjanovic.stevan.tendermint.rpc.circe.circe.ByteArray
import ch.epfl.ognjanovic.stevan.tendermint.rpc.circe.{CirceDecoders, circe}
import ch.epfl.ognjanovic.stevan.tendermint.verified.light.TrustLevel
import ch.epfl.ognjanovic.stevan.tendermint.verified.types.Height
import io.circe.Decoder


case class TrustOptions(trustPeriod: Long, trustedHeight: Height, hash: ByteArray, trustLevel: TrustLevel)

object TrustOptions {
  def decoder: Decoder[TrustOptions] = cursor => for {
    trustPeriod <- cursor.downField("period").as[Long]
    height <- cursor.downField("height").as[Long]
    hash <- cursor.downField("hash").as[ByteArray](circe.hashDecoder)
    trustLevel <- cursor.downField("trust_level").as[TrustLevel](CirceDecoders.trustLevelDecoder)
  } yield {
    TrustOptions(trustPeriod, Height(height), hash, trustLevel)
  }
}