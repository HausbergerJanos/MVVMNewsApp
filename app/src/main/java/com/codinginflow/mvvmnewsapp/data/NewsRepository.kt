package com.codinginflow.mvvmnewsapp.data

import com.codinginflow.mvvmnewsapp.api.NewsApi
import javax.inject.Inject

class NewsRepository
@Inject
constructor(
    private val newsApi: NewsApi,
    private val newsArticleDb: NewsArticleDatabase
) {
    private val newsArticleDao = newsArticleDb.newsArticleDao()

    suspend fun getBreakingNews(): List<NewsArticle> {
        val response = newsApi.getBreakingNews()
        val breakingNewsArticlesFromNetwork = response.articles
        val breakingNews = breakingNewsArticlesFromNetwork.map { newsArticleDto ->
            NewsArticle(
                title = newsArticleDto.title,
                url = newsArticleDto.url,
                thumbnailUrl = newsArticleDto.urlToImage,
                isBookmarked = false
            )
        }

        return breakingNews
    }
}