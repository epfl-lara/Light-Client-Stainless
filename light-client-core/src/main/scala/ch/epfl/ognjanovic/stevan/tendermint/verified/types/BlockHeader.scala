package ch.epfl.ognjanovic.stevan.tendermint.verified.types

import ch.epfl.ognjanovic.stevan.tendermint.verified.types.Nodes._
import stainless.lang._

case class BlockHeader(height: Height, lastCommit: Set[Node], validatorSet: Validators, nextValidatorSet: Validators)