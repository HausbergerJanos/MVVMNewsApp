package com.codinginflow.mvvmnewsapp.data

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.room.withTransaction
import com.codinginflow.mvvmnewsapp.api.NewsApi
import com.codinginflow.mvvmnewsapp.util.Resource
import com.codinginflow.mvvmnewsapp.util.networkBoundResource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import retrofit2.HttpException
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class NewsRepository
@Inject
constructor(
    private val newsApi: NewsApi,
    private val newsArticleDb: NewsArticleDatabase
) {
    private val newsArticleDao = newsArticleDb.newsArticleDao()

    fun getBreakingNews(
        forceRefresh: Boolean,
        onFetchSuccess: () -> Unit,
        onFetchFailed: (Throwable) -> Unit
    ): Flow<Resource<List<NewsArticle>>> =
        networkBoundResource(
            queryFromCache = {
                newsArticleDao.getAllBreakingNewsArticles()
            },
            fetchFromNetwork = {
                val response = newsApi.getBreakingNews()
                response.articles
            },
            saveFetchResult = { serverBreakingNewsArticles ->
                val bookmarkedArticle = newsArticleDao.getAllBookmarkedArticles().first()

                val breakingNewsArticles =
                    serverBreakingNewsArticles.map { serverBreakingNewsArticle ->
                        val isBookmarked = bookmarkedArticle.any { bookmarkedArticle ->
                            bookmarkedArticle.url == serverBreakingNewsArticle.url
                        }

                        NewsArticle(
                            title = serverBreakingNewsArticle.title,
                            url = serverBreakingNewsArticle.url,
                            thumbnailUrl = serverBreakingNewsArticle.urlToImage,
                            isBookmarked = isBookmarked
                        )
                    }

                val breakingNews = breakingNewsArticles.map { article ->
                    BreakingNews(article.url)
                }

                newsArticleDb.withTransaction {
                    newsArticleDao.deleteAllBreakingNews()
                    newsArticleDao.insertArticles(breakingNewsArticles)
                    newsArticleDao.insertBreakingNews(breakingNews)
                }
            },
            shouldFetch = { cachedArticles ->
                if (forceRefresh) {
                    true
                } else {
                    val sortedArticles = cachedArticles.sortedBy { article ->
                        article.updatedAt
                    }

                    val oldestTimeStamp = sortedArticles.firstOrNull()?.updatedAt

                    oldestTimeStamp == null || oldestTimeStamp < System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(
                        5
                    )
                }
            },
            onFetchSuccess = onFetchSuccess,
            onFetchFailed = { throwable ->
                if (throwable !is HttpException && throwable !is IOException) {
                    throw throwable
                }
                onFetchFailed(throwable)
            }
        )

    fun getSearchResultsPaged(query: String) : Flow<PagingData<NewsArticle>> =
        Pager(
            config = PagingConfig(
                pageSize = 10,
                maxSize = 200
            ),
            remoteMediator = SearchNewsRemoteMediator(
                searchQuery = query,
                newsApi = newsApi,
                newsArticleDb = newsArticleDb
            ),
            pagingSourceFactory = {
                newsArticleDao.getSearchResultArticlesPaged(query)
            }
        ).flow

    fun getAllBookmarkedArticles(): Flow<List<NewsArticle>> =
        newsArticleDao.getAllBookmarkedArticles()

    suspend fun deleteNonBookmarkedArticlesOlderThan(timestampInMillis: Long) {
        newsArticleDao.deleteNonBookmarkedArticlesOlderThan(timestampInMillis)
    }

    suspend fun updateArticle(article: NewsArticle) {
        newsArticleDao.updateArticle(article)
    }

    suspend fun resetAllBookmarks() {
        newsArticleDao.resetAllBookmarks()
    }
}