package ch.epfl.ognjanovic.stevan.tendermint.rpc.circe

import java.time.Instant
import java.util.Base64

import ch.epfl.ognjanovic.stevan.tendermint.rpc.types
import ch.epfl.ognjanovic.stevan.tendermint.rpc.types.SignedHeader
import ch.epfl.ognjanovic.stevan.tendermint.verified.light.TrustLevel
import ch.epfl.ognjanovic.stevan.tendermint.verified.types.CommitSignatures._
import ch.epfl.ognjanovic.stevan.tendermint.verified.types.Validators.Validator
import ch.epfl.ognjanovic.stevan.tendermint.verified.types._
import io.circe.Decoder
import stainless.annotation.ignore
import utils.ListMap

@ignore
object CirceDecoders {

  import circe._

  implicit val addressDecoder: Decoder[Address] = cursor =>
    for {
      value <- cursor.as[ByteArray](hexStringDecoder)
    } yield Address(value)

  implicit val partSetDecoder: Decoder[PartSetHeader] = cursor =>
    for {
      total <- cursor.downField("total").as[Int]
      hash <- cursor.downField("hash").as[ByteArray](hexStringDecoder)
    } yield {
      PartSetHeader(total, hash)
    }

  implicit val blockIdDecoder: Decoder[BlockId] = cursor =>
    for {
      hash <- cursor.downField("hash").as[ByteArray](hexStringDecoder)
      parts <- cursor.downField("parts").as[PartSetHeader]
    } yield {
      BlockId(hash, parts)
    }

  implicit val commitDecoder: Decoder[Commit] = cursor =>
    for {
      height <- cursor.downField("height").as[Long]
      round <- cursor.downField("round").as[Long]
      blockId <- cursor.downField("block_id").as[BlockId]
      signatures <- cursor.downField("signatures").as[List[CommitSignature]]
    } yield {
      Commit(Height(height), round, blockId, stainless.collection.List.fromScala(signatures))
    }

  implicit val consensusDecoder: Decoder[Consensus] = cursor =>
    for {
      block <- cursor.downField("block").as[Long]
      app <- cursor.downField("app").as[Long]
    } yield {
      Consensus(block, app)
    }

  implicit val keyDecoder: Decoder[Key] = cursor =>
    for {
      tpe <- cursor.downField("type").as[String]
      stringValue <- cursor.downField("value").as[String]
    } yield {
      Key(tpe, Base64.getDecoder.decode(stringValue).toVector)
    }

  implicit val signatureDecoder: Decoder[CommitSignature] = cursor =>
    for {
      blockFlagId <- cursor.downField("block_id_flag").as[Int]
      validatorAddress <- cursor.downField("validator_address").as[Option[Address]]
      timestamp <- cursor.downField("timestamp").as[Instant]
      signature <- cursor.downField("signature").as[Option[String]]
    } yield {
      val signatureOption = toStainlessOption(signature).map(value => Base64.getDecoder.decode(value).toVector)
      blockFlagId match {
        case 1 => BlockIDFlagAbsent
        case 2 => BlockIDFlagCommit(validatorAddress.get, timestamp, signatureOption.get)
        case 3 => BlockIdFlagNil(validatorAddress.get, timestamp, signatureOption.get)
        case _ => throw new IllegalArgumentException("Unknown \"block_id_flag\": " + blockFlagId)
      }
    }

  implicit val validatorDecoder: Decoder[Validator] = cursor =>
    for {
      address <- cursor.downField("address").as[Address]
      publicKey <- cursor.downField("pub_key").as[Key]
      votingPower <- cursor.downField("voting_power").as[Long]
      proposerPriority <- cursor.downField("proposer_priority").as[Long]
    } yield {
      Validator(address, publicKey, VotingPower(votingPower), proposerPriority)
    }

  implicit val validatorSetDecoder: Decoder[ValidatorSet] = cursor =>
    for {
      validators <- cursor.as[Array[Validator]]
    } yield {
      ValidatorSet(
        validators.foldLeft(VotingPower(0))((acc, validator) => acc + validator.votingPower),
        ListMap(stainless.collection.List.fromScala(validators.toList.map(value => (value.address, value))))
      )
    }

  implicit val conformanceTestValidatorSetDecoder: Decoder[ValidatorSet] = cursor =>
    for {
      validators <- cursor.downField("validators").as[Array[Validator]]
    } yield {
      ValidatorSet(
        validators.foldLeft(VotingPower(0))((acc, validator) => acc + validator.votingPower),
        ListMap(stainless.collection.List.fromScala(validators.toList.map(value => (value.address, value))))
      )
    }

  implicit val headerDecoder: Decoder[Header] = cursor =>
    for {
      version <- cursor.downField("version").as[Consensus]
      chanId <- cursor.downField("chain_id").as[String]
      height <- cursor.downField("height").as[Long]
      time <- cursor.downField("time").as[Instant]
      lastBlockId <- cursor.downField("last_block_id").as[BlockId]
      lastCommit <- cursor.downField("last_commit_hash").as[ByteArray](hexStringDecoder)
      data <- cursor.downField("data_hash").as[ByteArray](hexStringDecoder)
      validators <- cursor.downField("validators_hash").as[ByteArray](hexStringDecoder)
      nextValidators <- cursor.downField("next_validators_hash").as[ByteArray](hexStringDecoder)
      consensus <- cursor.downField("consensus_hash").as[ByteArray](hexStringDecoder)
      app <- cursor.downField("app_hash").as[ByteArray](hexStringDecoder)
      lastResults <- cursor.downField("last_results_hash").as[ByteArray](hexStringDecoder)
      evidence <- cursor.downField("evidence_hash").as[ByteArray](hexStringDecoder)
      proposer <- cursor.downField("proposer_address").as[Address]
    } yield {
      Header(
        version,
        chanId,
        Height(height),
        Timestamp(time.getEpochSecond, time.getNano),
        lastBlockId,
        lastCommit,
        data,
        validators,
        nextValidators,
        consensus,
        app,
        lastResults,
        evidence,
        proposer
      )
    }

  implicit val signedHeaderDecoder: Decoder[SignedHeader] = cursor =>
    for {
      header <- cursor.downField("header").as[Header]
      commit <- cursor.downField("commit").as[Commit]
    } yield {
      SignedHeader(header, commit)
    }

  implicit val trustLevelDecoder: Decoder[TrustLevel] = cursor =>
    for {
      numerator <- cursor.downField("numerator").as[Long]
      denominator <- cursor.downField("denominator").as[Long]
    } yield {
      TrustLevel(numerator, denominator)
    }

}
