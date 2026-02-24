import axios from 'axios';

// Create an instance of axios with custom config
const instance = axios.create({
  // Set this to your Spring Boot server's base URL
  baseURL: 'http://localhost:8080/api/image',
  
  // Optional: Set a timeout (e.g., 10 seconds)
  timeout: 10000,
});

// Optional: Add an interceptor to handle errors globally
instance.interceptors.response.use(
  (response) => response,
  (error) => {
    console.error("API Error:", error.response?.data || error.message);
    return Promise.reject(error);
  }
);

export default instance;