import React, { useEffect, useState } from 'react';
import { Box, Typography, CircularProgress, Alert } from '@mui/material';
import axios from 'axios';
import { authApi } from '@/lib/api';
import { useAuth } from '@/contexts/AuthContext';
import { useNavigate, useSearchParams } from 'react-router-dom';

const OAuthSuccess = () => {
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');
    const { login } = useAuth();
    const navigate = useNavigate();
    const [searchParams] = useSearchParams();

    useEffect(() => {
        // Read token and refreshToken from URL
        const params = new URLSearchParams(window.location.search);
        const token = params.get('token');
        const refreshToken = params.get('refreshToken');
        if (token) {
            localStorage.setItem('token', token);
            if (refreshToken) {
                localStorage.setItem('refreshToken', refreshToken);
            }
            // Set token in auth context and fetch profile
            (async () => {
                try {
                    const res = await axios.get('/api/auth/profile', {
                        headers: { Authorization: `Bearer ${token}` },
                        withCredentials: true,
                    });
                    login({ token, refreshToken, user: res.data });
                    navigate('/auth/redirect');
                } catch (err) {
                    setError('Failed to fetch user profile after OAuth login');
                } finally {
                    setLoading(false);
                }
            })();
        } else {
            setError('No token found in URL. OAuth login failed.');
            setLoading(false);
        }
    }, [login, navigate]);

    if (loading) {
        return (
            <Box
                display="flex"
                flexDirection="column"
                justifyContent="center"
                alignItems="center"
                minHeight="100vh"
                gap={2}
            >
                <CircularProgress />
                <Typography variant="body1" color="text.secondary">
                    Completing authentication...
                </Typography>
            </Box>
        );
    }

    if (error) {
        return (
            <Box
                display="flex"
                flexDirection="column"
                justifyContent="center"
                alignItems="center"
                minHeight="100vh"
                gap={2}
                p={2}
            >
                <Alert severity="error" sx={{ maxWidth: 400, width: '100%' }}>
                    {error}
                </Alert>
                <Typography variant="body1" color="text.secondary">
                    Redirecting to login page...
                </Typography>
            </Box>
        );
    }

    return null;
};

export default OAuthSuccess; 