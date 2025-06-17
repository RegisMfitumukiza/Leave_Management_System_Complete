import React, { createContext, useContext, useState, useEffect } from 'react';
import { authApi } from '@/lib/api';

const AuthContext = createContext(null);

export const useAuth = () => {
    const context = useContext(AuthContext);
    if (!context) {
        throw new Error('useAuth must be used within an AuthProvider');
    }
    return context;
};

export const AuthProvider = ({ children }) => {
    const [user, setUser] = useState(null);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        const checkAuth = async () => {
            try {
                const token = localStorage.getItem('token');
                if (token) {
                    // Fetch user data using authApi
                    const res = await authApi.get('/profile');
                    setUser(res.data);
                }
            } catch (err) {
                console.error('Auth check failed:', err);
                localStorage.removeItem('token');
            } finally {
                setLoading(false);
            }
        };

        checkAuth();
    }, []);

    const login = (data) => {
        const token = data.token || data.accessToken;
        const userData = data.user;
        localStorage.setItem('token', token);
        if (data.refreshToken) {
            localStorage.setItem('refreshToken', data.refreshToken);
        }
        setUser(userData);
    };

    const logout = () => {
        localStorage.removeItem('token');
        localStorage.removeItem('refreshToken');
        setUser(null);
    };

    const handleOAuthSuccess = async (token, refreshToken) => {
        localStorage.setItem('token', token);
        if (refreshToken) {
            localStorage.setItem('refreshToken', refreshToken);
        }
        try {
            const res = await authApi.get('/profile');
            setUser(res.data);
        } catch (err) {
            localStorage.removeItem('token');
            localStorage.removeItem('refreshToken');
            setUser(null);
            throw err;
        }
    };

    const value = {
        user,
        setUser,
        loading,
        login,
        logout,
        isAuthenticated: !!user,
        isAdmin: user?.role === 'ADMIN',
        isManager: user?.role === 'MANAGER',
        handleOAuthSuccess
    };

    return (
        <AuthContext.Provider value={value}>
            {!loading && children}
        </AuthContext.Provider>
    );
};

export default AuthContext; 