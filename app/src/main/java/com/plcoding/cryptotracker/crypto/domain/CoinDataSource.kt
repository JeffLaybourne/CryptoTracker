package com.plcoding.cryptotracker.crypto.domain

import com.plcoding.cryptotracker.core.domain.util.NetworkError
import com.plcoding.cryptotracker.core.domain.util.Result

/**
 * CoinDataSource is "what" we expect to have returned. Not "how" or "where
 * it's retrieved. As such this belongs in domain. The implementation of this
 * will reside in the crypto.data folder.
 **/

interface CoinDataSource {
    suspend fun getCoins(): Result<List<Coin>, NetworkError>
}