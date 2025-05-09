
const API_BASE_URL = process.env.REACT_APP_API_BASE_URL || 'http://localhost:8003';

export const fetchNews = async (page = 0, size = 10, sort = 'pubDate,desc') => {
  try {
    const response = await fetch(`${API_BASE_URL}/api/news?page=${page}&size=${size}&sort=${sort}`);

    if (!response.ok) {
      throw new Error(`Error: ${response.status}`);
    }

    return await response.json();
  } catch (error) {
    console.error('Error fetching news:', error);
    throw error;
  }
};
