// src/App.jsx
import React, { useState, useEffect } from 'react';
import Header from './components/Header';
import NewsList from './components/NewsList';
import { fetchNews } from './services/newsService';
import './App.css';

function App() {
  const [articles, setArticles] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [language, setLanguage] = useState('eng');
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);

  useEffect(() => {
    const loadArticles = async () => {
      try {
        setLoading(true);
        // Use the fetchNews function instead of inline fetch
        const data = await fetchNews(page, 10, 'pubDate,desc');
        setArticles(data.content);
        setTotalPages(data.totalPages);
        setLoading(false);
      } catch (err) {
        setError(err.message);
        setLoading(false);
      }
    };

    loadArticles();
  }, [page, language]); // Re-fetch when page or language changes

  const handleLanguageChange = (newLanguage) => {
    setLanguage(newLanguage);
  };

  const handlePrevious = () => {
    setPage((prevPage) => Math.max(0, prevPage - 1));
  };

  const handleNext = () => {
    setPage((prevPage) => prevPage + 1 < totalPages ? prevPage + 1 : prevPage);
  };

  if (loading) return <div className="loading-container"><div className="loading">Loading...</div></div>;
  if (error) return <div className="error-container"><div className="error">Error: {error}</div></div>;

  return (
      <div className="app">
        <Header
            date={new Date()}
            onLanguageChange={handleLanguageChange}
            language={language}
        />
        <main className="main-content">
          <NewsList articles={articles} language={language} />

          <div className="pagination">
            <button onClick={handlePrevious} disabled={page === 0}>Previous</button>
            <span>Page {page + 1} of {totalPages}</span>
            <button onClick={handleNext} disabled={page + 1 >= totalPages}>Next</button>
          </div>
        </main>
      </div>
  );
}

export default App;
