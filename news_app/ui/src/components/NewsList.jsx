// src/components/NewsList.jsx
import React from 'react';
import './NewsList.css';

const NewsList = ({ articles, language }) => {
  // Function to open article in new tab
  const openArticle = (url) => {
    window.open(url, '_blank', 'noopener,noreferrer');
  };

  return (
      <div className="news-container">
        {articles.map((article) => (
            <article key={article.guid} className="news-article">
              <div className="article-date">
                {new Date(article.pubDate).toLocaleDateString('en-US', {
                  month: 'short',
                  day: 'numeric',
                  year: 'numeric'
                }).toUpperCase()}
              </div>

              <div className="article-content">
                <div className="article-text">
                  <h2
                      className="article-title"
                      onClick={() => openArticle(article.link)}
                  >
                    {article.title}
                  </h2>
                  <p
                      className="article-description"
                      onClick={() => openArticle(article.link)}
                  >
                    {article.description}
                  </p>
                  <p className="article-byline">
                    BY {article.creator || 'STAFF WRITER'}
                  </p>
                </div>

                {article.media && article.media.url && (
                    <div className="article-image">
                      <img
                          src={article.media.url}
                          alt={article.title}
                          onClick={() => openArticle(article.link)}
                      />
                    </div>
                )}
              </div>
            </article>
        ))}
      </div>
  );
};

export default NewsList;
