package com.codinginflow.mvvmnewsapp.util

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

inline fun <CacheResult, NetworkResult> networkBoundResource(
    crossinline queryFromCache: () -> Flow<CacheResult>,
    crossinline fetchFromNetwork: suspend () -> NetworkResult,
    crossinline saveFetchResult: suspend (NetworkResult) -> Unit,
    crossinline shouldFetch: (CacheResult) -> Boolean = { true },
    crossinline onFetchSuccess: () -> Unit = { },
    crossinline onFetchFailed: (Throwable) -> Unit = { }
) = channelFlow {

    val data = queryFromCache().first()

    if (shouldFetch(data)) {
        val loading = launch {
            queryFromCache().collect {
                send(Resource.Loading(it))
            }
        }

        try {
            delay(2000)
            saveFetchResult(fetchFromNetwork())
            onFetchSuccess()
            loading.cancel()
            queryFromCache().collect {
                send(Resource.Success(it))
            }
        } catch (t: Throwable) {
            onFetchFailed(t)
            loading.cancel()
            queryFromCache().collect {
                send(Resource.Error(t, it))
            }
        }
    }
}