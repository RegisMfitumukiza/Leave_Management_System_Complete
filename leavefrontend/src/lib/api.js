import axios from 'axios';

const authApi = axios.create({
    baseURL: import.meta.env.VITE_AUTH_SERVICE_URL,
    headers: {
        'Content-Type': 'application/json',
    },
    withCredentials: true,
});

const leaveApi = axios.create({
    baseURL: import.meta.env.VITE_LEAVE_SERVICE_URL,
    withCredentials: true,
});

// Add auth token to requests
const addAuthHeader = (config) => {
    const token = localStorage.getItem('token');
    if (token) {
        config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
};

authApi.interceptors.request.use(addAuthHeader);
leaveApi.interceptors.request.use(addAuthHeader);

export { authApi, leaveApi };