import React from 'react';
import './Header.css';

const Header = ({ date, onLanguageChange, language }) => {
  const formattedDate = new Date(date).toLocaleDateString('en-US', {
    weekday: 'short',
    day: 'numeric',
    month: 'short',
    year: 'numeric'
  });

  return (
      <header className="site-header">
        <div className="header-content">
          <div className="header-date">{formattedDate}</div>
          <div className="header-logo">
            <a href="https://www.nytimes.com/section/technology" target="_blank" rel="noopener noreferrer">
              <img
                  src="https://static01.nyt.com/images/misc/NYT_logo_rss_250x40.png"
                  alt="The New York Times"
                  className="nyt-logo"
              />
            </a>
          </div>
          <div className="header-language">
          <span
              className={language === 'eng' ? 'active' : ''}
              onClick={() => onLanguageChange('eng')}
          >
            ENG
          </span> |
            <span
                className={language === 'esp' ? 'active' : ''}
                onClick={() => onLanguageChange('esp')}
            >
            ESP
          </span>
          </div>
        </div>
      </header>
  );
};

export default Header;
